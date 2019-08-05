package com.xatkit.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.CommonPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.osgi.framework.Bundle;

import com.xatkit.common.ImportDeclaration;
import com.xatkit.common.LibraryImportDeclaration;
import com.xatkit.common.PlatformImportDeclaration;
import com.xatkit.core_resources.utils.LibraryLoaderUtils;
import com.xatkit.core_resources.utils.PlatformLoaderUtils;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionPackage;
import com.xatkit.intent.IntentPackage;
import com.xatkit.intent.Library;
import com.xatkit.platform.PlatformDefinition;
import com.xatkit.platform.PlatformPackage;

/**
 * A registry managing Platform and Library imports.
 * <p>
 * This class provides utility methods to load EMF {@link Resource}s from imports. Imports can point to workspace files,
 * absolute files on the file system, as well as files stored in imported {@code jars}.
 * <p>
 * This class also loads the {@link Platform} and {@link Library} models stored in the <i>core component</i> allowing to
 * use unqualified imports in execution models such as {@code import "CorePlatform"}.
 */
public class ImportRegistry {

	/**
	 * The singleton {@link ImportRegistry} instance.
	 */
	private static ImportRegistry INSTANCE;

	/**
	 * Returns the singleton {@link ImportRegistry} instance.
	 * 
	 * @return the singleton {@link ImportRegistry} instance
	 */
	public static ImportRegistry getInstance() {
		if (isNull(INSTANCE)) {
			INSTANCE = new ImportRegistry();
		}
		return INSTANCE;
	}

	/**
	 * Tracks the number of files loaded by the registry.
	 * <p>
	 * This field is used for debugging purposes.
	 */
	private static int FILE_LOADED_COUNT = 0;

	/**
	 * Increments and prints the {@link #FILE_LOADED_COUNT} value.
	 * <p>
	 * This method is used for debugging purposes.
	 * <p>
	 */
	private static void incrementLoadCalls() {
		FILE_LOADED_COUNT++;
		// TODO replace the System.out.println call by a log
		System.out.println("#File loaded " + FILE_LOADED_COUNT);
	}

	/**
	 * The {@link ResourceSet} used to load imported {@link Resource}s.
	 * <p>
	 * All the imported {@link Resource}s are stored in the same {@link ResourceSet} to allow proxy resolution between
	 * models (this is typically the case when an execution model imports a Platform and uses its actions).
	 */
	private ResourceSet rSet;

	/**
	 * Caches the loaded {@link Platform} instances.
	 * 
	 * @see #getImport(ImportDeclaration)
	 * @see ImportEntry
	 */
	private ConcurrentHashMap<ImportEntry, PlatformDefinition> platforms;

	/**
	 * Caches the loaded {@link Library} instances.
	 * 
	 * @see #getImport(ImportDeclaration)
	 * @see ImportEntry
	 */
	private ConcurrentHashMap<ImportEntry, Library> libraries;

	/**
	 * Constructs a new {@link ImportRegistry}.
	 * <p>
	 * This method is private, use {@link #getInstance()} to retrieve the singleton instance of this class.
	 */
	private ImportRegistry() {
		this.rSet = new ResourceSetImpl();
		this.rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		this.rSet.getPackageRegistry().put(ExecutionPackage.eINSTANCE.getNsURI(), ExecutionPackage.eINSTANCE);
		this.rSet.getPackageRegistry().put(PlatformPackage.eINSTANCE.getNsURI(), PlatformPackage.eINSTANCE);
		this.rSet.getPackageRegistry().put(IntentPackage.eINSTANCE.getNsURI(), IntentPackage.eINSTANCE);
		/*
		 * Load the core Platforms and Library in the constructor, they should not be reloaded later.
		 */
		loadXatkitCore();
		ExecutionPackage.eINSTANCE.eClass();
		PlatformPackage.eINSTANCE.eClass();
		IntentPackage.eINSTANCE.eClass();
		this.platforms = new ConcurrentHashMap<>();
		this.libraries = new ConcurrentHashMap<>();
	}

	/**
	 * Updates the {@link Library} cache with the provided {@code library}.
	 * <p>
	 * This method ensures that the imports referring to the provided {@code library} are always associated to the
	 * latest version of the {@link Library}. This is for example the case when updating an intent library along with an
	 * execution model.
	 * <p>
	 * This method is typically called by the <i>intent language</i> generator once the {@code .xmi} model associated to
	 * the provided {@code library} has been created.
	 * 
	 * @param library the {@link Library} to update the imports for
	 * @see #updateImport(PlatformDefinition)
	 */
	public void updateImport(Library library) {
		for (Entry<ImportEntry, Library> librariesEntry : libraries.entrySet()) {
			Library storedLibrary = librariesEntry.getValue();
			if (library.getName().equals(storedLibrary.getName())) {
				System.out.println("Updating library entry for " + library.getName());
				librariesEntry.setValue(library);
			}
		}
	}

	/**
	 * Updates the {@link Platform} cache with the provided {@code platform}.
	 * <p>
	 * This method ensures that the imports referring to the provided {@code platform} are always associated to the
	 * latest version of the {@link Platform}. This is for example the case when updating a platform along with an
	 * execution model.
	 * <p>
	 * This method is typically called by the <i>platform language</i> generator once the {@code .xmi} model associated
	 * to the provided {@code platform} has been created.
	 * 
	 * @param platform the {@link Platform} to update the imports for
	 * @see #updateImport(Library)
	 */
	public void updateImport(PlatformDefinition platform) {
		for (Entry<ImportEntry, PlatformDefinition> platformsEntry : platforms.entrySet()) {
			PlatformDefinition storedPlatform = platformsEntry.getValue();
			if (platform.getName().equals(storedPlatform.getName())) {
				System.out.println("Updating platform entry for " + platform.getName());
				platformsEntry.setValue(platform);
			}
		}
	}

	/**
	 * Retrieves the {@link Resource} associated to the provided {@code importDeclaration}.
	 * <p>
	 * This method checks whether the provided {@code importDeclaration} is already cached and returns it. If it is not
	 * the case the registry attempts to load the {@link Resource} corresponding to the provided {@code import} and
	 * caches it.
	 * 
	 * @param importDeclaration the {@link ImportDeclaration} to retrieve the {@link Resource} from
	 * @return the retrieved {@link Resource} if it exists, {@code null} otherwise
	 * 
	 * @see #getImport(ImportDeclaration)
	 * @see #loadImport(ImportDeclaration)
	 */
	public Resource getOrLoadImport(ImportDeclaration importDeclaration) {
		Resource resource = this.getImport(importDeclaration);
		if (resource == null) {
			System.out.println("The import " + importDeclaration.getPath()
					+ " wasn't found in the cache, loading the corresponding resource");
			resource = this.loadImport(importDeclaration);
		}
		/*
		 * If the resource is null this means that an error occurred when loading the import (e.g. the file doesn't
		 * exist, or the resource contains invalid content)
		 */
		return resource;
	}

	/**
	 * Returns the {@link PlatformDefinition}s imported by the provided {@code platform}.
	 * 
	 * @param platform the {@link PlatformDefinition} to retrieve the imported platforms of
	 * @return the {@link PlatformDefinition}s imported by the provided {@code platform}
	 */
	public Collection<PlatformDefinition> getImportedPlatforms(PlatformDefinition platform) {
		this.refreshRegisteredImports(platform.getImports());
		return this.platforms.values();
	}

	/**
	 * Returns the {@link PlatformDefinition}s imported by the provided {@code executionModel}
	 * 
	 * @param executionModel the {@link ExecutionModel} to retrieve the imported platforms of
	 * @return the {@link PlatformDefinition}s imported by the provided {@code executionModel}
	 */
	public Collection<PlatformDefinition> getImportedPlatforms(ExecutionModel executionModel) {
		this.refreshRegisteredImports(executionModel.getImports());
		return this.platforms.values();
	}

	/**
	 * Returns the {@link PlatformDefinition} imported by the provided {@code model} with the given
	 * {@code platformName}.
	 * 
	 * @param model the {@link ExecutionModel} to retrieve the imported {@link PlatformDefinition} from
	 * @param platformName the name of the {@link PlatformDefinition} to retrieve
	 * @return the imported {@link PlatformDefinition}, or {@code null} if there is no imported
	 *         {@link PlatformDefinition} matching the provided {@code platformName}
	 */
	public PlatformDefinition getImportedPlatform(ExecutionModel model, String platformName) {
		Optional<PlatformDefinition> result = this.getImportedPlatforms(model).stream()
				.filter(p -> p.getName().equals(platformName)).findFirst();
		if (result.isPresent()) {
			return result.get();
		} else {
			return null;
		}
	}

	/**
	 * Returns the {@link Library} instances imported by the provided {@code executionModel}
	 * 
	 * @param executionModel the {@link ExecutionModel} to retrieve the imported libraries of
	 * @return the {@link Library} instances imported by the provided {@code executionModel}
	 */
	public Collection<Library> getImportedLibraries(ExecutionModel executionModel) {
		this.refreshRegisteredImports(executionModel.getImports());
		return this.libraries.values();
	}

	/**
	 * Loads the core {@link Platform}s and {@link Library} instances.
	 * <p>
	 * The {@link Platform}s and {@link Library} instances are retrieved from the {@code xatkit.jar} file in the
	 * classpath. Note that this method should only be called once: the classpath is not supposed to change during the
	 * execution.
	 * 
	 * @see #loadXatkitCorePlatforms()
	 * @see #loadXatkitCoreLibraries()
	 */
	private void loadXatkitCore() {
		try {
			loadXatkitCorePlatforms();
		} catch (IOException | URISyntaxException e) {
			System.out.println("An error occurred when loading core platforms");
			e.printStackTrace();
		}
		try {
			loadXatkitCoreLibraries();
		} catch (IOException | URISyntaxException e) {
			System.out.println("An error occurred when loading core libraries");
			e.printStackTrace();
		}
	}

	/**
	 * Refreshes the {@link ImportRegistry} with the provided {@code newImports}.
	 * <p>
	 * This method ensures that the registry does not contain imports that are not used anymore, and that all the
	 * provided imports correspond to loadable EMF {@link Resource}s.
	 * <p>
	 * Note that this method can be called by different editors containing different sets of imports. Removing unused
	 * imports also ensures that imports are not shared between editors.
	 * 
	 * @param newImports the new {@link ImportDeclaration} to register
	 * @return the {@link Resource}s loaded from the provided {@code newImports}.
	 * 
	 * @see #removeUnusedImports(Collection)
	 */
	private Collection<Resource> refreshRegisteredImports(Collection<? extends ImportDeclaration> newImports) {
		removeUnusedImports(newImports);
		List<Resource> resources = new ArrayList<>();
		for (ImportDeclaration importDeclaration : newImports) {
			resources.add(getOrLoadImport(importDeclaration));
		}
		return resources;
	}

	/**
	 * Removes the registered {@link ImportDeclaration} that are not part of the provided {@code imports}.
	 * <p>
	 * This method ensure that deleted imports in the editors are reflected in the registry.
	 * <p>
	 * Note that this method can be called by different editors containing different sets of imports. Removing unused
	 * imports also ensures that imports are not shared between editors.
	 * 
	 * @param imports the {@link ImportDeclaration} to keep in the registry
	 */
	private void removeUnusedImports(Collection<? extends ImportDeclaration> imports) {
		List<ImportEntry> entries = imports.stream().map(i -> ImportEntry.from(i)).collect(Collectors.toList());
		for (ImportEntry importEntry : platforms.keySet()) {
			if (!entries.contains(importEntry)) {
				System.out.println("Removing " + importEntry.getPath() + " (alias = " + importEntry.getAlias() + ")");
				platforms.remove(importEntry);
			}
		}

		for (ImportEntry importEntry : libraries.keySet()) {
			if (!entries.contains(importEntry)) {
				System.out.println("Removing " + importEntry.getPath() + " (alias = " + importEntry.getAlias() + ")");
				libraries.remove(importEntry);
			}
		}
	}

	/**
	 * Retrieves the cached {@link Resource} associated to the provided {@code importDeclaration}.
	 * 
	 * @param importDeclaration the {@link ImportDeclaration} to retrieve the {@link Resource} from
	 * @return the retrieved {@link Resource} if it exists, {@code null} otherwise
	 * 
	 * @see #getOrLoadImport(ImportDeclaration)
	 */
	private Resource getImport(ImportDeclaration importDeclaration) {
		if (importDeclaration instanceof LibraryImportDeclaration) {
			Library library = this.libraries.get(ImportEntry.from(importDeclaration));
			if (nonNull(library)) {
				return library.eResource();
			} else {
				System.out.println("Cannot find the library " + importDeclaration.getPath());
				return null;
			}
		} else if (importDeclaration instanceof PlatformImportDeclaration) {
			PlatformDefinition platform = this.platforms.get(ImportEntry.from(importDeclaration));
			if (nonNull(platform)) {
				return platform.eResource();
			} else {
				System.out.println("Cannot find the platform " + importDeclaration.getPath());
				return null;
			}
		} else {
			throw new IllegalArgumentException(MessageFormat.format("Unknown {0} type {1}",
					ImportDeclaration.class.getSimpleName(), importDeclaration.getClass().getSimpleName()));
		}
	}

	/**
	 * Loads the {@link Resource} described by the provided {@code importDeclaration}.
	 * <p>
	 * The loaded {@link Resource} can be either a {@link Platform} {@link Resource}, or a {@link Library}
	 * {@link Resource}.
	 * 
	 * @param importDeclaration the {@link ImportDeclaration} to load
	 * @return the loaded {@link Resource}, or {@code null} if the {@link ImportRegistry} was not able to load it
	 */
	private Resource loadImport(ImportDeclaration importDeclaration) {
		incrementLoadCalls();
		String path = importDeclaration.getPath();
		String alias = importDeclaration.getAlias();
		System.out.println(MessageFormat.format("Loading import from path {0}", path));
		/*
		 * Try to load it as a core platform/library
		 */
		Resource resource = null;
		try {
			String uriPrefix;
			if (importDeclaration instanceof PlatformImportDeclaration) {
				uriPrefix = PlatformLoaderUtils.CORE_PLATFORM_PATHMAP;
			} else {
				uriPrefix = LibraryLoaderUtils.CORE_LIBRARY_PATHMAP;
			}
			resource = rSet.getResource(URI.createURI(uriPrefix + path + ".xmi"), false);
			if (isNull(resource)) {
				/*
				 * In case .xmi has been specified within the import
				 */
				resource = rSet.getResource(URI.createURI(uriPrefix + path), false);
			}
		} catch (Exception e) {
			System.out.println("Cannot load the import as a core platform/library");
		}
		/*
		 * The import is not a core platform, try to load it from its path, and register the resource using its alias.
		 * If the import declaration does not define an alias load the resource using its absolute path, meaning that
		 * the generated XMI will not be portable.
		 */
		if (isNull(resource)) {
			File importResourceFile = new File(path);
			URI importResourceFileURI = null;
			if (importResourceFile.exists()) {
				importResourceFileURI = URI.createFileURI(importResourceFile.getAbsolutePath());
			} else {
				/*
				 * Try to load the resource as a platform resource
				 */
				URI platformURI = URI.createPlatformResourceURI(path, false);
				/*
				 * Convert the URI to an absolute URI, platform:/ is not handled by the runtime engine
				 */
				importResourceFileURI = CommonPlugin.asLocalURI(platformURI);
			}
			URI importResourceURI = importResourceFileURI;
			if (nonNull(alias)) {
				URI importResourceAliasURI;
				if (importDeclaration instanceof PlatformImportDeclaration) {
					importResourceAliasURI = URI.createURI(PlatformLoaderUtils.CUSTOM_PLATFORM_PATHMAP + alias);
				} else if (importDeclaration instanceof LibraryImportDeclaration) {
					importResourceAliasURI = URI.createURI(LibraryLoaderUtils.CUSTOM_LIBRARY_PATHMAP + alias);
				} else {
					System.out.println(MessageFormat.format("Cannot load the provided import, unknown import type {0}",
							importDeclaration.eClass().getName()));
					return null;
				}

				/*
				 * Remove the existing alias if there is one, this allows to update the name of the alias.
				 */
				removeAliasForURI(importResourceURI);

				rSet.getURIConverter().getURIMap().put(importResourceAliasURI, importResourceFileURI);
				Iterator<Resource> registeredResources = rSet.getResources().iterator();
				/*
				 * Removes the Resource from the resource set that matches either the alias URI or the base URI. The
				 * alias URI needs to be removed if the resource URI to load has changed, otherwise the old resource is
				 * returned. The base URI needs to be removed if the base URI was used to register the resource without
				 * an alias before.
				 */
				while (registeredResources.hasNext()) {
					Resource registeredResource = registeredResources.next();
					/*
					 * Check the last segment, the URI may contained either CUSTOM_PLATFORM_PATHMAP or
					 * CUSTOM_LIBRARY_PATHMAP if it was previously registered as a Platform/Library
					 */
					if (nonNull(registeredResource.getURI().lastSegment())
							&& registeredResource.getURI().lastSegment().equals(alias)) {
						System.out.println(MessageFormat.format("Unregistering resource {0} from the ResourceSet",
								importResourceAliasURI));
						registeredResources.remove();
					}
					if (registeredResource.getURI().equals(importResourceURI)) {
						System.out.println(MessageFormat.format("Unregistering resource {0} from the ResourceSet",
								importResourceURI));
						registeredResources.remove();
					}
				}
				importResourceURI = importResourceAliasURI;
			} else {
				/*
				 * If there is an alias we need to remove the URIMap entry previously associated to the base URI. This
				 * allows to update aliases and remove them.
				 */
				removeAliasForURI(importResourceURI);
			}
			resource = rSet.getResource(importResourceURI, false);
			if (isNull(resource)) {
				resource = rSet.createResource(importResourceURI);
			}
		}
		if (nonNull(resource)) {
			try {
				resource.load(Collections.emptyMap());
			} catch (IOException e) {
				System.out.println("An error occurred when loading the resource");
				return null;
			} catch (IllegalArgumentException e) {
				System.out.println("An error occurred when loading the resource, invalid platform URI provided");
				return null;
			}
		} else {
			System.out.println(
					MessageFormat.format("Cannot find the resource asssociated to the import {0}", importDeclaration));
			return null;
		}
		System.out.println(MessageFormat.format("Resource with URI {0} loaded", resource.getURI()));
		if (resource.getContents().isEmpty()) {
			System.out.println("The loaded resource is empty");
			return null;
		}
		for (EObject e : resource.getContents()) {
			if (e instanceof PlatformDefinition) {
				PlatformDefinition platformDefinition = (PlatformDefinition) e;
				if (importDeclaration instanceof PlatformImportDeclaration) {
					if (this.platforms.containsKey(ImportEntry.from(importDeclaration))) {
						System.out.println(MessageFormat.format("The platform {0} is already loaded, erasing it",
								platformDefinition.getName()));
					}
					System.out.println(MessageFormat.format("Registering platform {0}", platformDefinition.getName()));
					this.platforms.put(ImportEntry.from(importDeclaration), platformDefinition);
				} else {
					System.out
							.println(MessageFormat.format("Trying to load a {0} using a {1}, please use a {2} instead",
									e.eClass().getName(), importDeclaration.getClass().getSimpleName(),
									PlatformImportDeclaration.class.getSimpleName()));
					return null;
				}
			} else if (e instanceof Library) {
				Library library = (Library) e;
				if (importDeclaration instanceof LibraryImportDeclaration) {
					if (this.libraries.containsKey(ImportEntry.from(importDeclaration))) {
						System.out.println(MessageFormat.format("The library {0} is already loaded, erasing it",
								library.getName()));
					}
					System.out.println(MessageFormat.format("Registering library {0}", library.getName()));
					this.libraries.put(ImportEntry.from(importDeclaration), library);
				} else {
					System.out
							.println(MessageFormat.format("Trying to load a {0} using a {1}, please use a {2} instead",
									e.eClass().getName(), importDeclaration.getClass().getSimpleName(),
									LibraryImportDeclaration.class.getSimpleName()));
					return null;
				}
			} else {
				/*
				 * The top level element is not a platform, we are not loading a valid Platform resource
				 */
				System.out.println(MessageFormat.format(
						"The loaded resource contains the unknown top-level element {0}", e.eClass().getName()));
				return null;
			}
		}
		return resource;
	}

	/**
	 * Removes the alias associated to the provided {@code uri}.
	 * <p>
	 * This method looks at the {@link ResourceSet}'s URI map for entries containing the {@code uri} as a value and
	 * remove them. Note that the comparison must be performed on the value since the registered alias for the provided
	 * {@code uri} is not known.
	 * 
	 * @param uri the {@link URI} to remove the alias for
	 */
	private void removeAliasForURI(URI uri) {
		Iterator<Map.Entry<URI, URI>> uriMapEntries = rSet.getURIConverter().getURIMap().entrySet().iterator();
		while (uriMapEntries.hasNext()) {
			Map.Entry<URI, URI> uriMapEntry = uriMapEntries.next();
			if (uriMapEntry.getValue().equals(uri)) {
				uriMapEntries.remove();
			}
		}
	}

	/**
	 * Loads the core {@link Library} instances.
	 * <p>
	 * These {@link Library} instances are retrieved from the {@code xatkit.jar} file in the classpath.
	 * 
	 * @throws IOException if the registry cannot find one of the core {@link Library} files
	 * @throws URISyntaxException if an error occurred when building one of the {@link Library}'s {@link URI}s
	 * 
	 * @see #getCoreResourcesPath(String)
	 */
	private void loadXatkitCoreLibraries() throws IOException, URISyntaxException {
		incrementLoadCalls();
		Path libraryPath = getCoreResourcesPath("libraries/xmi/");
		System.out.println(MessageFormat.format("Crawling libraries in {0}", libraryPath));
		Files.walk(libraryPath, 1).filter(filePath -> !Files.isDirectory(filePath)).forEach(modelPath -> {
			try {
				InputStream is = Files.newInputStream(modelPath);
				rSet.getURIConverter().getURIMap().put(
						URI.createURI(LibraryLoaderUtils.CORE_LIBRARY_PATHMAP + modelPath.getFileName()),
						URI.createURI(modelPath.getFileName().toString()));
				Resource modelResource = this.rSet.createResource(
						URI.createURI(LibraryLoaderUtils.CORE_LIBRARY_PATHMAP + modelPath.getFileName().toString()));
				modelResource.load(is, Collections.emptyMap());
				System.out.println(MessageFormat.format("Library resource {0} loaded (uri={1})",
						modelPath.getFileName(), modelResource.getURI()));
				is.close();
			} catch (IOException e) {
				// TODO check why this exception cannot be thrown back to the caller (probably some lambda shenanigans)
				System.out.println(MessageFormat.format("An error occurred when loading the library resource {0}",
						modelPath.getFileName()));
			}
		});
	}

	/**
	 * Loads the core {@link Platform}s.
	 * <p>
	 * These {@link Platform}s are retrieved from the {@code xatkit.jar} file in the classpath.
	 * 
	 * @throws IOException if the registry cannot find one of the core {@link Platform} files
	 * @throws URISyntaxException if an error occurred when building one of the {@link Platform}'s {@link URI}s
	 * 
	 * @see #getCoreResourcesPath(String)
	 */
	private void loadXatkitCorePlatforms() throws IOException, URISyntaxException {
		incrementLoadCalls();
		String xatkitPath = System.getenv("XATKIT");
		if(isNull(xatkitPath) || xatkitPath.isEmpty()) {
			System.out.println("XATKIT environment variable not set, no core platforms to import");
			return;
		}
		Files.walk(Paths.get(xatkitPath + "plugins" + File.separator + "platforms"), Integer.MAX_VALUE)
			.filter(filePath -> 
				!Files.isDirectory(filePath) && filePath.toString().endsWith(".xmi")
		).forEach(modelPath -> {
			try {
				InputStream is = Files.newInputStream(modelPath);
				rSet.getURIConverter().getURIMap().put(
						URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + modelPath.getFileName()),
						URI.createURI(modelPath.getFileName().toString()));
				Resource modelResource = this.rSet.createResource(
						URI.createURI(PlatformLoaderUtils.CORE_PLATFORM_PATHMAP + modelPath.getFileName().toString()));
				modelResource.load(is, Collections.emptyMap());
				System.out.println(MessageFormat.format("Platform resource {0} loaded (uri={1})",
						modelPath.getFileName(), modelResource.getURI()));
				is.close();
			} catch (IOException e) {
				// TODO check why this exception cannot be thrown back to the caller (probably some lambda shenanigans)
				System.out.println(MessageFormat.format("An error occurred when loading the platform resource {0}",
						modelPath.getFileName()));
			}
		});
	}

	/**
	 * Creates a valid {@link Path} for the provided {@code resourceLocation} within the {@code core_resources} bundle.
	 * 
	 * @param resourceLocation the location of the resource within the {@code core_resources} OSGI bundle
	 * @return a valid {@link Path} for the provided {@code resourceLocation} within the {@code core_resources} bundle
	 * @throws IOException if an error occurred when loading the resource at the provided location
	 * @throws URISyntaxException if an error occurred when building the URI associated to the resource location
	 */
	private Path getCoreResourcesPath(String resourceLocation) throws IOException, URISyntaxException {
		String bundleName = "com.xatkit.core_resources";
		Bundle bundle = org.eclipse.core.runtime.Platform.getBundle(bundleName);
		if (isNull(bundle)) {
			throw new RuntimeException(MessageFormat.format("Cannot find the bundle {0}", bundleName));
		}
		URL platformFolderURL = bundle.getEntry(resourceLocation);
		if (isNull(platformFolderURL)) {
			System.out.println(MessageFormat.format(
					"Cannot load the platforms/xmi/ folder from the bundle {0}, trying to load it in development mode",
					bundle));
			/*
			 * If the plugin is not installed (i.e. if we are running an eclipse application from a workspace that
			 * contains the plugin sources) the folder is located in src/main/resources.
			 */
			platformFolderURL = bundle.getEntry("src/main/resources/" + resourceLocation);
			if (isNull(platformFolderURL)) {
				throw new RuntimeException(MessageFormat.format(
						"Cannot load the platforms/xmi/ folder from the bundle {0} (development mode failed)", bundle));
			} else {
				System.out.println(MessageFormat.format("{0} folder loaded from the bundle in development mode",
						resourceLocation));
			}
		}

		java.net.URI resolvedPlatformFolderURI = FileLocator.resolve(platformFolderURL).toURI();
		System.out.println(MessageFormat.format("Resolved platforms/xmi/ folder URI: {0}", resolvedPlatformFolderURI));

		if (resolvedPlatformFolderURI.getScheme().equals("jar")) {
			try {
				/*
				 * Try to get the FileSystem if it exists, this may be the case if this method has been called to get
				 * the path of a resource stored in a jar file.
				 */
				FileSystems.getFileSystem(resolvedPlatformFolderURI);
			} catch (FileSystemNotFoundException e) {
				/*
				 * The FileSystem does not exist, try to create a new one with the provided URI. This is typically the
				 * case when loading a resource from a jar file for the first time.
				 */
				Map<String, String> env = new HashMap<>();
				env.put("create", "true");
				FileSystems.newFileSystem(resolvedPlatformFolderURI, Collections.emptyMap());
			}
		}
		return Paths.get(resolvedPlatformFolderURI);
	}

	/**
	 * A {@link Map} entry used to uniquely identify imported {@link Platform} and {@link Library} instances.
	 * <p>
	 * {@link ImportDeclaration} instance cannot be used as {@link Map} entries because they do not provide a
	 * field-based implementation of {@code equals}, and multiple instances of the same import can be created from the
	 * same editor. Overriding {@link EObject#equals(Object)} is considered as a bad practice since some of the core
	 * components of the EMF framework rely on object equality (see
	 * <a href="https://www.eclipse.org/forums/index.php/t/663829/">this link</a> for more information).
	 * <p>
	 * Note that there is no public constructor for this class. {@link ImportEntry} instances can be created using the
	 * {{@link #from(ImportDeclaration)} method.
	 * 
	 * @see ImportDeclaration
	 */
	private static class ImportEntry {

		/**
		 * Creates an {@link ImportEntry} from the provided {@code importDeclaration}.
		 * 
		 * @param importDeclaration the {@link ImportDeclaration} to create an entry from
		 * @return the created {@link ImportEntry}
		 */
		public static ImportEntry from(ImportDeclaration importDeclaration) {
			return new ImportEntry(importDeclaration.getPath(), importDeclaration.getAlias());
		}

		/**
		 * The path of the {@link ImportDeclaration} used to create the entry.
		 */
		private String path;

		/**
		 * The alias of the {@link ImportDeclaration} used to create the entry.
		 */
		private String alias;

		/**
		 * Constructs an {@link ImportEntry} with the provided {@code path} and {@code alias}.
		 * <p>
		 * This method is private, use {@link #from(ImportDeclaration)} to create {@link ImportEntry} instances from
		 * {@link ImportDeclaration}s.
		 * 
		 * @param path the path of the {@link ImportDeclaration} used to create the entry
		 * @param alias the alias of the {@link ImportDeclaration} used to create the entry
		 * 
		 * @see #from(ImportDeclaration)
		 */
		private ImportEntry(String path, String alias) {
			this.path = path;
			this.alias = alias;
		}

		/**
		 * Returns the path of the entry.
		 * 
		 * @return the path of the entry
		 */
		public String getPath() {
			return this.path;
		}

		/**
		 * Returns the alias of the entry.
		 * 
		 * @return the alias of the entry
		 */
		public String getAlias() {
			return this.alias;
		}

		/**
		 * Returns whether the {@link ImportEntry} and the provided {@code obj} are equal.
		 * <p>
		 * This method checks whether the {@code path} and {@code alias} values of the objects are equal. This allows to
		 * compare different {@link ImportEntry} instances representing the same {@link ImportDeclaration}.
		 * 
		 * @return {@code true} if the provided {@code obj} is equal to the {@link ImportEntry}, {@code false} otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ImportEntry) {
				ImportEntry otherEntry = (ImportEntry) obj;
				/*
				 * Use Objects.equals, the path or the alias can be null
				 */
				return Objects.equals(this.path, otherEntry.path) && Objects.equals(this.alias, otherEntry.alias);
			} else {
				return super.equals(obj);
			}
		}

		@Override
		public int hashCode() {
			/*
			 * Use Objects.hashCode, the path or the alias can be null
			 */
			return Objects.hashCode(this.path) + Objects.hashCode(this.alias);
		}

	}

}

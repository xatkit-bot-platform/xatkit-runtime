package fr.zelus.jarvis.language.util;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.osgi.framework.Bundle;

import fr.zelus.jarvis.core_modules.utils.ModulesLoaderUtils;
import fr.zelus.jarvis.intent.IntentPackage;
import fr.zelus.jarvis.orchestration.ImportDeclaration;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.orchestration.OrchestrationPackage;
import fr.zelus.jarvis.platform.Platform;

public class PlatformRegistry {

	private static PlatformRegistry INSTANCE;
	
	public static PlatformRegistry getInstance() {
		if(isNull(INSTANCE)) {
			INSTANCE = new PlatformRegistry();
		}
		return INSTANCE;
	}
	
	private ResourceSet rSet;
	
	private Map<String, Platform> platforms;
	
	private PlatformRegistry() {
		this.rSet = new ResourceSetImpl();
		this.rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		this.rSet.getPackageRegistry().put(OrchestrationPackage.eINSTANCE.getNsURI(), OrchestrationPackage.eINSTANCE);
		this.rSet.getPackageRegistry().put(IntentPackage.eINSTANCE.getNsURI(), IntentPackage.eINSTANCE);
		try {
			loadJarvisCorePlatforms();
		} catch(IOException | URISyntaxException e) {
			System.out.println("An error occurred when loading core platforms");
			e.printStackTrace();
		}
		OrchestrationPackage.eINSTANCE.eClass();
		IntentPackage.eINSTANCE.eClass();
		this.platforms = new HashMap<>();
	}	
	
	public Collection<Platform> loadOrchestrationModelPlatforms(OrchestrationModel model) {
		/*
		 * Remove the loaded platforms, we are reloading them all
		 */
		this.platforms.clear();
		for(ImportDeclaration imp : model.getImports()) {
			loadPlatform(imp);
		}
		return this.platforms.values();
	}
	
	public Resource loadPlatform(ImportDeclaration importDeclaration)  {
		String path = importDeclaration.getPath();
		String alias = importDeclaration.getAlias();
		System.out.println(MessageFormat.format("Loading platform from path {0}", path));
		/*
		 * Try to load it as a core platform
		 */
		Resource resource = null;
		try {
			resource = rSet.getResource(URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + path + ".xmi"), false);
			if(isNull(resource)) {
				/*
				 * In case .xmi has been specified
				 */
				resource = rSet.getResource(URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + path), false);
			}
		} catch(Exception e) {
			System.out.println("Cannot load the platform as a core platform");
		}
		/*
		 * The platform is not a core platform, try to load it from its path, and register the resource using its alias.
		 * If the import declaration does not define an alias load the resource using its absolute path, meaning that the generated XMI
		 * will not be portable.
		 */
		if(isNull(resource)) {
			File platformResourceFile = new File(path);
			URI platformResourceFileURI = URI.createFileURI(platformResourceFile.getAbsolutePath());
			URI platformResourceURI = platformResourceFileURI;
			if(nonNull(alias)) {
				URI platformResourceAliasURI = URI.createURI(ModulesLoaderUtils.CUSTOM_MODULE_PATHMAP + alias);
				rSet.getURIConverter().getURIMap().put(platformResourceAliasURI, platformResourceFileURI);
				Iterator<Resource> registeredResources = rSet.getResources().iterator();
				/*
				 * Removes the Resource from the resource set that matches either the alias URI or the base URI.
				 * The alias URI needs to be removed if the resource URI to load has changed, otherwise the old resource is returned.
				 * The base URI needs to be removed if the base URI was used to register the resource without an alias before.
				 */
				while(registeredResources.hasNext()) {
					Resource registeredResource = registeredResources.next();
					if(registeredResource.getURI().equals(platformResourceAliasURI)) {
						System.out.println(MessageFormat.format("Unregistering resource {0} from the ResourceSet", platformResourceAliasURI));
						registeredResources.remove();
					}
					if(registeredResource.getURI().equals(platformResourceURI)) {
						System.out.println(MessageFormat.format("Unregistering resource {0} from the ResourceSet", platformResourceURI));
						registeredResources.remove();
					}
				}
				platformResourceURI = platformResourceAliasURI;
			} else {
				/*
				 * If there is an alias we need to remove the URIMap entry previously associated to the base URI. This allows to update aliases
				 * and remove them.
				 */
				Iterator<Map.Entry<URI,URI>> uriMapEntries = rSet.getURIConverter().getURIMap().entrySet().iterator();
				while(uriMapEntries.hasNext()) {
					Map.Entry<URI, URI> uriMapEntry = uriMapEntries.next();
					if(uriMapEntry.getValue().equals(platformResourceURI)) {
						uriMapEntries.remove();
					}
				}
			}
			resource = rSet.getResource(platformResourceURI, false);
			if(isNull(resource)) {
				resource = rSet.createResource(platformResourceURI);
			}
		}
		if(nonNull(resource)) {
			try {
				resource.load(Collections.emptyMap());
			} catch(IOException e) {
				System.out.println("An error occurred when loading the resource");
				return null;
			}
		} else {
			System.out.println(MessageFormat.format("Cannot find the platform resource asssociated to the import {0}", importDeclaration));
			return null;
		}
		System.out.println(MessageFormat.format("Resource with URI {0} loaded", resource.getURI()));
		if(resource.getContents().isEmpty()) {
			System.out.println("The loaded resource is empty");
			return null;
		}
		for(EObject e : resource.getContents()) {
			if(e instanceof Platform) {
				Platform platform = (Platform)e;
				if(this.platforms.containsKey((platform.getName()))) {
					System.out.println(MessageFormat.format("The platform {0} is already loaded, erasing it", platform.getName()));
				}
				System.out.println(MessageFormat.format("Registering platform {0}", platform.getName()));
				this.platforms.put(platform.getName(), platform);
			} else {
				/*
				 * The top level element is not a platform, we are not loading a valid Platform resource
				 */
				System.out.println(MessageFormat.format("The loaded resource contains the unknown top-level element {0}", e.eClass().getName()));
				return null;
			}
		}
		return resource;
	}
	
	public void loadJarvisCorePlatforms() throws IOException, URISyntaxException {
		Bundle bundle = org.eclipse.core.runtime.Platform.getBundle("fr.zelus.jarvis.core_modules");
		if(isNull(bundle)) {
			throw new RuntimeException("Cannot find the bundle fr.zelus.jarvis.core_modules");
		}
		URL platformFolderURL = bundle.getEntry("modules/xmi/");
		if(isNull(platformFolderURL)) {
			System.out.println(MessageFormat.format("Cannot load the modules/xmi/ folder from the bundle {0}, trying to load it in development mode", bundle));
			/*
			 * If the plugin is not installed (i.e. if we are running an eclipse application from a workspace that contains
			 * the plugin sources) the folder is located in src/main/resources.
			 */
			platformFolderURL = bundle.getEntry("src/main/resources/modules/xmi/");
			if(isNull(platformFolderURL)) {
				throw new RuntimeException(MessageFormat.format("Cannot load the modules/xmi/ folder from the bundle {0} (development mode failed)", bundle));
			} else {
				System.out.println("modules/xmi/ folder loaded from the bundle in development mode");
			}
		}
		
		java.net.URI resolvedPlatformFolderURI = FileLocator.resolve(platformFolderURL).toURI();
		System.out.println(MessageFormat.format("Resolved modules/xmi/ folder URI: {0}", resolvedPlatformFolderURI));
		
		if(resolvedPlatformFolderURI.getScheme().equals("jar")) {
			FileSystem fs = FileSystems.newFileSystem(resolvedPlatformFolderURI, Collections.emptyMap());

		}
		Path platformPath = Paths.get(resolvedPlatformFolderURI);
		System.out.println(MessageFormat.format("Crawling platforms in {0}", platformPath));
		Files.walk(platformPath, 1).filter(filePath -> !Files.isDirectory(filePath)).forEach(modelPath ->
			{
				try {
					InputStream is = Files.newInputStream(modelPath);
					rSet.getURIConverter().getURIMap().put(URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + modelPath.getFileName()), URI.createURI(modelPath.getFileName().toString()));
					Resource modelResource = this.rSet.createResource(URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + modelPath.getFileName().toString()));
					modelResource.load(is, Collections.emptyMap());
					System.out.println(MessageFormat.format("Platform resource {0} loaded (uri={1})", modelPath.getFileName(), modelResource.getURI()));
					is.close();
				} catch(IOException e) {
					System.out.println(MessageFormat.format("An error occurred when loading the platform resource {0}", modelPath.getFileName()));
				}
			}
		);
	}
	
}

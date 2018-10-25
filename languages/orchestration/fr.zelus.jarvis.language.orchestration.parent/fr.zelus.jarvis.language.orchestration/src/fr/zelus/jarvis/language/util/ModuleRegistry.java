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
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.osgi.framework.Bundle;

import fr.zelus.jarvis.core_modules.utils.ModulesLoaderUtils;
import fr.zelus.jarvis.intent.IntentPackage;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.orchestration.ImportDeclaration;
import fr.zelus.jarvis.orchestration.OrchestrationModel;
import fr.zelus.jarvis.orchestration.OrchestrationPackage;

public class ModuleRegistry {

	private static ModuleRegistry INSTANCE;
	
	public static ModuleRegistry getInstance() {
		if(isNull(INSTANCE)) {
			INSTANCE = new ModuleRegistry();
		}
		return INSTANCE;
	}
	
	private ResourceSet rSet;
	
	private Map<String, Module> modules;
	
	private ModuleRegistry() {
		this.rSet = new ResourceSetImpl();
		this.rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		this.rSet.getPackageRegistry().put(OrchestrationPackage.eINSTANCE.getNsURI(), OrchestrationPackage.eINSTANCE);
		this.rSet.getPackageRegistry().put(IntentPackage.eINSTANCE.getNsURI(), IntentPackage.eINSTANCE);
		try {
			loadJarvisCoreModules();
		} catch(IOException | URISyntaxException e) {
			System.out.println("An error occurred when loading core modules");
			e.printStackTrace();
		}
		OrchestrationPackage.eINSTANCE.eClass();
		IntentPackage.eINSTANCE.eClass();
		this.modules = new HashMap<>();
	}	
	
	public Collection<Module> loadOrchestrationModelModules(OrchestrationModel model) {
		/*
		 * Remove the loaded modules, we are reloading them all
		 */
		this.modules.clear();
		for(ImportDeclaration imp : model.getImports()) {
			loadModule(imp);
		}
		return this.modules.values();
	}
	
	public Resource loadModule(ImportDeclaration importDeclaration)  {
		String path = importDeclaration.getPath();
		String alias = importDeclaration.getAlias();
		System.out.println(MessageFormat.format("Loading module from path {0}", path));
		/*
		 * Try to load it as a core module
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
			System.out.println("Cannot load the module as a core module");
		}
		/*
		 * The module is not a core module, try to load it from its path, and register the resource using its alias.
		 * If the import declaration does not define an alias load the resource using its absolute path, meaning that the generated XMI
		 * will not be portable.
		 */
		if(isNull(resource)) {
			File moduleResourceFile = new File(path);
			URI moduleResourceFileURI = URI.createFileURI(moduleResourceFile.getAbsolutePath());
			URI moduleResourceURI = moduleResourceFileURI;
			if(nonNull(alias)) {
				URI moduleResourceAliasURI = URI.createURI(ModulesLoaderUtils.CUSTOM_MODULE_PATHMAP + alias);
				rSet.getURIConverter().getURIMap().put(moduleResourceAliasURI, moduleResourceFileURI);
				Iterator<Resource> registeredResources = rSet.getResources().iterator();
				/*
				 * Removes the Resource from the resource set that matches either the alias URI or the base URI.
				 * The alias URI needs to be removed if the resource URI to load has changed, otherwise the old resource is returned.
				 * The base URI needs to be removed if the base URI was used to register the resource without an alias before.
				 */
				while(registeredResources.hasNext()) {
					Resource registeredResource = registeredResources.next();
					if(registeredResource.getURI().equals(moduleResourceAliasURI)) {
						System.out.println(MessageFormat.format("Unregistering resource {0} from the ResourceSet", moduleResourceAliasURI));
						registeredResources.remove();
					}
					if(registeredResource.getURI().equals(moduleResourceURI)) {
						System.out.println(MessageFormat.format("Unregistering resource {0} from the ResourceSet", moduleResourceURI));
						registeredResources.remove();
					}
				}
				moduleResourceURI = moduleResourceAliasURI;
			} else {
				/*
				 * If there is an alias we need to remove the URIMap entry previously associated to the base URI. This allows to update aliases
				 * and remove them.
				 */
				Iterator<Map.Entry<URI,URI>> uriMapEntries = rSet.getURIConverter().getURIMap().entrySet().iterator();
				while(uriMapEntries.hasNext()) {
					Map.Entry<URI, URI> uriMapEntry = uriMapEntries.next();
					if(uriMapEntry.getValue().equals(moduleResourceURI)) {
						uriMapEntries.remove();
					}
				}
			}
			resource = rSet.getResource(moduleResourceURI, false);
			if(isNull(resource)) {
				resource = rSet.createResource(moduleResourceURI);
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
			System.out.println(MessageFormat.format("Cannot find the module resource asssociated to the import {0}", importDeclaration));
			return null;
		}
		System.out.println(MessageFormat.format("Resource with URI {0} loaded", resource.getURI()));
		if(resource.getContents().isEmpty()) {
			System.out.println("The loaded resource is empty");
			return null;
		}
		for(EObject e : resource.getContents()) {
			if(e instanceof Module) {
				Module module = (Module)e;
				if(this.modules.containsKey((module.getName()))) {
					System.out.println(MessageFormat.format("The module {0} is already loaded, erasing it", module.getName()));
				}
				System.out.println(MessageFormat.format("Registering module {0}", module.getName()));
				this.modules.put(module.getName(), module);
			} else {
				/*
				 * The top level element is not a module, we are not loading a valid Module resource
				 */
				System.out.println(MessageFormat.format("The loaded resource contains the unknown top-level element {0}", e.eClass().getName()));
				return null;
			}
		}
		return resource;
	}
	
	public void loadJarvisCoreModules() throws IOException, URISyntaxException {
		Bundle bundle = Platform.getBundle("fr.zelus.jarvis.core_modules");
		if(isNull(bundle)) {
			throw new RuntimeException("Cannot find the bundle fr.zelus.jarvis.core_modules");
		}
		URL moduleFolderURL = bundle.getEntry("modules/");
		if(isNull(moduleFolderURL)) {
			System.out.println(MessageFormat.format("Cannot load the modules/ folder from the bundle {0}, trying to load it in development mode", bundle));
			/*
			 * If the plugin is not installed (i.e. if we are running an eclipse application from a workspace that contains
			 * the plugin sources) the folder is located in src/main/resources.
			 */
			moduleFolderURL = bundle.getEntry("src/main/resources/modules/");
			if(isNull(moduleFolderURL)) {
				throw new RuntimeException(MessageFormat.format("Cannot load the modules/ folder from the bundle {0} (development mode failed)", bundle));
			} else {
				System.out.println("modules/ folder loaded from the bundle in development mode");
			}
		}
		
		java.net.URI resolvedModuleFolderURI = FileLocator.resolve(moduleFolderURL).toURI();
		System.out.println(MessageFormat.format("Resolved modules/ folder URI: {0}", resolvedModuleFolderURI));
		
		if(resolvedModuleFolderURI.getScheme().equals("jar")) {
			FileSystem fs = FileSystems.newFileSystem(resolvedModuleFolderURI, Collections.emptyMap());

		}
		Path modulePath = Paths.get(resolvedModuleFolderURI);
		System.out.println(MessageFormat.format("Crawling modules in {0}", modulePath));
		Files.walk(modulePath, 1).filter(filePath -> !Files.isDirectory(filePath)).forEach(modelPath ->
			{
				try {
					InputStream is = Files.newInputStream(modelPath);
					rSet.getURIConverter().getURIMap().put(URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + modelPath.getFileName()), URI.createURI(modelPath.getFileName().toString()));
					Resource modelResource = this.rSet.createResource(URI.createURI(ModulesLoaderUtils.CORE_MODULE_PATHMAP + modelPath.getFileName().toString()));
					modelResource.load(is, Collections.emptyMap());
					System.out.println(MessageFormat.format("Module resource {0} loaded (uri={1})", modelPath.getFileName(), modelResource.getURI()));
					is.close();
				} catch(IOException e) {
					System.out.println(MessageFormat.format("An error occurred when loading the module resource {0}", modelPath.getFileName()));
				}
			}
		);
	}
	
}

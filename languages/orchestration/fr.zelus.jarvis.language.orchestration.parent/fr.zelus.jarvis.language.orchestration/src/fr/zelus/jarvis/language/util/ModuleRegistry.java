package fr.zelus.jarvis.language.util;

import static java.util.Objects.isNull;

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

import fr.zelus.jarvis.core_modules.CoreModulesUtils;
import fr.zelus.jarvis.intent.IntentPackage;
import fr.zelus.jarvis.module.Module;
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
	
	public Collection<Module> loadOrchestrationModelModules(OrchestrationModel model) throws IOException {
		/*
		 * Remove the loaded modules, we are reloading them all
		 */
		this.modules.clear();
		for(String imp : model.getImports()) {
			loadModule(imp);
		}
		return this.modules.values();
	}
	
	public Resource loadModule(String path) throws IOException {
		System.out.println("Loading module from path " + path);
		/*
		 * Try to load it as a core module
		 */
		Resource resource = null;
		try {
			resource = rSet.getResource(URI.createURI(CoreModulesUtils.CORE_MODULE_PATHMAP + path + ".xmi"), false);
			if(isNull(resource)) {
				/*
				 * In case .xmi has been specified
				 */
				resource = rSet.getResource(URI.createURI(CoreModulesUtils.CORE_MODULE_PATHMAP + path), false);
			}
		} catch(Exception e) {
			System.out.println("Cannot load the module as a core module");
		}
		if(isNull(resource)) {
			File f = new File(path);
			URI moduleURI = URI.createFileURI(f.getAbsolutePath());
			resource = rSet.getResource(moduleURI, false);
			if(isNull(resource)) {
				/*
				 * getResource() returned null, trying to retrieve the resource with createResource()
				 */
				resource = rSet.createResource(moduleURI);
			}
			resource = rSet.createResource(URI.createFileURI(f.getAbsolutePath()));
			resource.load(Collections.emptyMap());
		}
		System.out.println("Using resource with URI " + resource.getURI());
		if(resource.getContents().isEmpty()) {
			throw new IOException("The loaded resource is empty");
		}
		for(EObject e : resource.getContents()) {
			if(e instanceof Module) {
				Module module = (Module)e;
				if(this.modules.containsKey((module.getName()))) {
					System.out.println("The module " + module.getName() + " is already loaded, erasing it");
				}
				System.out.println("Registering module " + module.getName());
				this.modules.put(module.getName(), module);
			} else {
				/*
				 * The top level element is not a module, we are not loading a valid Module resource
				 */
				throw new IOException("The loaded resource contains the unknown top level element " + e.eClass().getName());
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
					rSet.getURIConverter().getURIMap().put(URI.createURI(CoreModulesUtils.CORE_MODULE_PATHMAP + modelPath.getFileName()), URI.createURI(modelPath.getFileName().toString()));
					Resource modelResource = this.rSet.createResource(URI.createURI(CoreModulesUtils.CORE_MODULE_PATHMAP + modelPath.getFileName().toString()));
					modelResource.load(is, Collections.emptyMap());
					System.out.println(MessageFormat.format("Module resource {0} loaded (uri={1})", modelPath.getFileName(), modelResource.getURI()));
				} catch(IOException e) {
					System.out.println(MessageFormat.format("An error occurred when loading the module resource {0}", modelPath.getFileName()));
				}
			}
		);
	}
	
}

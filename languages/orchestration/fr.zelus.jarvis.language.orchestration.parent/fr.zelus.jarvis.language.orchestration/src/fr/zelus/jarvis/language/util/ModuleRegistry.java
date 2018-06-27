package fr.zelus.jarvis.language.util;

import static java.util.Objects.isNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

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
		File f = new File(path);
		URI moduleURI = URI.createFileURI(f.getAbsolutePath());
		Resource resource = rSet.getResource(moduleURI, false);
		if(isNull(resource)) {
			/*
			 * getResource() returned null, trying to retrieve the resource with createResource()
			 */
			resource = rSet.createResource(moduleURI);
		}
		resource = rSet.createResource(URI.createFileURI(f.getAbsolutePath()));
		resource.load(Collections.emptyMap());
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
	
}

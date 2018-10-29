package fr.zelus.jarvis.language;

import org.eclipse.emf.ecore.EPackage;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import fr.zelus.jarvis.intent.IntentPackage;
import fr.zelus.jarvis.module.ModulePackage;
import fr.zelus.jarvis.orchestration.OrchestrationPackage;

/**
 * Starts the orchestration plugin and registers the related metamodels.
 * <p>
 * This class registers the <i>orchestration</i>, <i>module</i>, and <i>intent</i> metamodels to the global registry,
 * allowing to load the corresponding editors from the Eclipse platform.
 * <p>
 * This activator is registered in the module manifest and its {@link #start(BundleContext)} method is called when the
 * plugin is loaded.
 *
 */
public class Activator implements BundleActivator {

	/**
	 * Starts the orchestration plugin and registers the corresponding metamodels.
	 * 
	 * @param context
	 *            the OSGI context
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("Registering EPackages (Activator)");
		EPackage.Registry.INSTANCE.put(IntentPackage.eNS_URI, IntentPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(ModulePackage.eNS_URI, ModulePackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(OrchestrationPackage.eNS_URI, OrchestrationPackage.eINSTANCE);

	}

	/**
	 * Stops the orchestration plugin.
	 * 
	 * @param context
	 *            the OSGI context
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
	}

}

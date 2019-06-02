package edu.uoc.som.jarvis.language.execution;

import org.eclipse.emf.ecore.EPackage;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import edu.uoc.som.jarvis.common.CommonPackage;
import edu.uoc.som.jarvis.execution.ExecutionPackage;
import edu.uoc.som.jarvis.intent.IntentPackage;
import edu.uoc.som.jarvis.platform.PlatformPackage;

/**
 * Starts the execution plugin and registers the related metamodels.
 * <p>
 * This class registers the <i>execution</i>, <i>platform</i>, and <i>intent</i> metamodels to the global registry,
 * allowing to load the corresponding editors from the Eclipse platform.
 * <p>
 * This activator is registered in the platform manifest and its {@link #start(BundleContext)} method is called when the
 * plugin is loaded.
 *
 */
public class Activator implements BundleActivator {

	/**
	 * Starts the execution plugin and registers the corresponding metamodels.
	 * 
	 * @param context
	 *            the OSGI context
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("Registering Jarvis Language EPackages");
		EPackage.Registry.INSTANCE.put(CommonPackage.eNS_URI, CommonPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(IntentPackage.eNS_URI, IntentPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(PlatformPackage.eNS_URI, PlatformPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(ExecutionPackage.eNS_URI, ExecutionPackage.eINSTANCE);
	}

	/**
	 * Stops the execution plugin.
	 * 
	 * @param context
	 *            the OSGI context
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
	}

}

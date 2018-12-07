package edu.uoc.som.jarvis.language.intent;

import org.eclipse.emf.ecore.EPackage;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import edu.uoc.som.jarvis.intent.IntentPackage;

/**
 * Starts the intent plugin and registers the related metamodels.
 * <p>
 * This class registers the <i>intent</i>metamodels to the global registry, allowing to load the corresponding editors
 * from the Eclipse platform.
 * <p>
 * This activator is registered in the platform manifest and its {@link #start(BundleContext)} method is called when the
 * plugin is loaded.
 *
 */
public class Activator implements BundleActivator {

	/**
	 * Starts the intent plugin and registers the corresponding metamodel.
	 * 
	 * @param context
	 *            the OSGI context
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("Registering Jarvis Language EPackages");
		EPackage.Registry.INSTANCE.put(IntentPackage.eNS_URI, IntentPackage.eINSTANCE);
	}

	/**
	 * Stops the intent plugin.
	 * 
	 * @param context
	 *            the OSGI context
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
	}

}

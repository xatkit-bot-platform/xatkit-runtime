package fr.zelus.jarvis.language.platform;

import org.eclipse.emf.ecore.EPackage;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import fr.zelus.jarvis.intent.IntentPackage;
import fr.zelus.jarvis.platform.PlatformPackage;

/**
 * Starts the platform plugin and registers the related metamodels.
 * <p>
 * This class registers the <i>intent</i> and <i>platform</i> metamodel to the global registry, allowing to load the
 * corresponding editors from the Eclipse platform.
 * <p>
 * <b>Note:</b> this class does not register the <i>orchestration</i> metamodel, that is not required to load the
 * <i>platform</i> editor.
 * <p>
 * This activator is registered in the platform manifest and its {@link #start(BundleContext)} method is called when the
 * plugin is loaded.
 *
 */
public class Activator implements BundleActivator {

	/**
	 * Starts the platform plugin and registers the corresponding metamodels.
	 * 
	 * @param context
	 *            the OSGI context
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		System.out.println("Registering EPackages (Activator)");
		EPackage.Registry.INSTANCE.put(IntentPackage.eNS_URI, IntentPackage.eINSTANCE);
		EPackage.Registry.INSTANCE.put(PlatformPackage.eNS_URI, PlatformPackage.eINSTANCE);
	}

	/**
	 * Stops the platform plugin.
	 * 
	 * @param context
	 *            the OSGI context
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
	}

}

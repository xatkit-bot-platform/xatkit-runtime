package fr.zelus.jarvis.plugins.emf.module;

import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisModule;

/**
 * A {@link JarvisModule} concrete implementation provided EMF-related functionality that can be used in
 * orchestration models.
 * <p>
 * This module defines the following {@link fr.zelus.jarvis.core.JarvisAction}s:
 * <ul>
 * <li>{@link fr.zelus.jarvis.plugins.emf.module.action.CreateResource}: create an EMF
 * {@link org.eclipse.emf.ecore.resource.Resource} at a given {@code uri} with the given {@code content}.</li>
 * <li>{@link fr.zelus.jarvis.plugins.emf.module.action.SaveResourceInFile}: save the provided EMF
 * {@link org.eclipse.emf.ecore.resource.Resource} and returns the persisted {@link java.io.File}</li>
 * </ul>
 * This class is part of jarvis' core modules, and can be used in an orchestration model by importing the <i>core
 * .EMFModule</i> package.
 *
 * @see fr.zelus.jarvis.plugins.emf.module.action.CreateResource
 * @see fr.zelus.jarvis.plugins.emf.module.action.SaveResourceInFile
 */
public class EMFModule extends JarvisModule {

    /**
     * Constructs a new {@link EMFModule} from the provided {@link JarvisCore}.
     *
     * @param jarvisCore the {@link JarvisCore} instance associated to this module
     * @throws NullPointerException if the provided {@code jarvisCore} is {@code null}
     */
    public EMFModule(JarvisCore jarvisCore) {
        super(jarvisCore);
    }
}

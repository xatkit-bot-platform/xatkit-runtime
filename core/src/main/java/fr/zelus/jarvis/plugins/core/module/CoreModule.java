package fr.zelus.jarvis.plugins.core.module;

import fr.zelus.jarvis.core.JarvisModule;

/**
 * A {@link JarvisModule} concrete implementation providing core functionality that can be used in orchestration models.
 * <p>
 * This module defines a set of high level {@link fr.zelus.jarvis.core.JarvisAction}s:
 * <ul>
 *     <li>{@link fr.zelus.jarvis.plugins.core.module.action.GetTime}: return the current time</li>
 *     <li>{@link fr.zelus.jarvis.plugins.core.module.action.GetDate}: return the current date</li>
 * </ul>
 * <p>
 * This class is part of jarvis' core modules, and can be used in an orchestration model by importing the <i>core
 * .CoreModule</i> package.
 *
 * @see fr.zelus.jarvis.plugins.core.module.action.GetTime
 * @see fr.zelus.jarvis.plugins.core.module.action.GetDate
 */
public class CoreModule extends JarvisModule {
}

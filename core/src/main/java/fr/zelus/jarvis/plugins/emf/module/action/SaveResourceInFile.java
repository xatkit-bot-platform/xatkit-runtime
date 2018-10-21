package fr.zelus.jarvis.plugins.emf.module.action;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.emf.module.EMFModule;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A {@link JarvisAction} that saves the provided EMF {@link Resource} and returns the persisted {@link File}.
 */
public class SaveResourceInFile extends JarvisAction<EMFModule> {

    /**
     * The EMF {@link Resource} to save.
     */
    private Resource resource;

    /**
     * Constructs a new {@link SaveResourceInFile} action from the provided {@code containingModule}, {@code
     * session}, and {@code resource}.
     *
     * @param containingModule the {@link EMFModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param resource         the EMF {@link Resource} to save
     * @throws NullPointerException     if the provided {@code containingModule}, {@code session}, or {@code
     *                                  resource} is
     *                                  {@code null}
     * @throws IllegalArgumentException if the provided {@code resource} {@code uri} is {@code null}
     */
    public SaveResourceInFile(EMFModule containingModule, JarvisSession session, Resource resource) {
        super(containingModule, session);
        checkNotNull(resource, "Cannot construct a %s with the provided %s %s", this.getClass().getSimpleName(),
                Resource.class.getSimpleName(), resource);
        checkArgument(nonNull(resource.getURI()), "Cannot construct a %s with the provided %s %s %s", this.getClass()
                .getSimpleName(), Resource.class.getSimpleName(), URI.class.getSimpleName(), resource.getURI());
        this.resource = resource;
    }

    /**
     * Saves the EMF {@link Resource} and returns the persisted {@link File}.
     *
     * @return the persisted {@link File}, or {@code null} if an error occurred when saving the provided
     * {@link Resource}
     */
    @Override
    public Object compute() {
        try {
            Log.info("Saving resource {0}", resource.getURI().toFileString());
            resource.save(Collections.emptyMap());
            File resourceFile = new File(resource.getURI().toFileString());
            if (resourceFile.exists()) {
                Log.info("Resource {0} saved, returning the persisted file {1}", resource.getURI().toFileString(),
                        resourceFile.getAbsolutePath());
                return resourceFile;
            } else {
                Log.error("The Resource {0} hasn't been saved, unable to map the resource's URI to a persisted file " +
                        "path", resource.getURI().toFileString());
                return null;
            }
        } catch (IOException e) {
            Log.error("The Resource {0} hasn't been saved, unable to map the resource's URI to a persisted file " +
                    "path", resource.getURI().toFileString());
            return null;
        }
    }
}

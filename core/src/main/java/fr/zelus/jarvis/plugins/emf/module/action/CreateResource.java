package fr.zelus.jarvis.plugins.emf.module.action;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.emf.module.EMFModule;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import static fr.inria.atlanmod.commons.Preconditions.checkArgument;
import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * A {@link JarvisAction} that creates an EMF {@link Resource} with a given {@code uri} and {@code content}.
 */
public class CreateResource extends JarvisAction<EMFModule> {

    /**
     * The {@link String} representation of the {@link Resource}'s {@link URI}.
     */
    private String uri;

    /**
     * The top-level element of the {@link Resource} to create.
     */
    private EObject contents;

    /**
     * Constructs a new {@link CreateResource} action from the provided {@code containingModule}, {@code session},
     * {@code uri}, and {@code content}.
     *
     * @param containingModule the {@link EMFModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param uri              the {@link String} representation of the {@link Resource}'s {@link URI}
     * @param contents         the top-level element of the {@link Resource} to create
     * @throws NullPointerException     if the provided {@code containingModule}, {@code session}, {@code uri}, or
     *                                  {@code contents} is {@code null}
     * @throws IllegalArgumentException if the provided {@code uri} is empty
     */
    public CreateResource(EMFModule containingModule, JarvisSession session, String uri, EObject contents) {
        super(containingModule, session);
        checkNotNull(uri, "Cannot construct a %s with the provided %s %s", this.getClass().getSimpleName(), String
                .class.getSimpleName(), uri);
        checkArgument(!uri.isEmpty(), "Cannot construct a %s with an empty uri", this.getClass().getSimpleName());
        checkNotNull(contents, "Cannot construct a %s with the provided %s %s", this.getClass().getSimpleName(),
                EObject.class.getSimpleName(), contents);
        this.uri = uri;
        this.contents = contents;
    }

    /**
     * Creates the EMF {@link Resource} with the provided {@code uri} and {@code contents}.
     *
     * @return the created EMF {@link Resource}, or {@code null} if an error occurred when creating the
     * {@link Resource}
     */
    @Override
    public Object compute() {
        ResourceSet rSet = new ResourceSetImpl();
        rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        Resource resource = rSet.createResource(URI.createURI(this.uri));
        if (nonNull(resource)) {
            resource.getContents().add(contents);
            return resource;
        } else {
            Log.error("The created {0} is {1}", Resource.class.getSimpleName(), resource);
            return null;
        }
    }
}

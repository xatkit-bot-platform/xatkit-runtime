package fr.zelus.jarvis.plugins.generic_chat.module.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.plugins.generic_chat.module.GenericChatModule;

import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A generic {@link JarvisAction} wrapping concrete chatting sub-module {@link JarvisAction}s.
 * <p>
 * This class is initialized by the {@link GenericChatModule}, and is used as a wrapper for the {@link JarvisAction}
 * returned by the {@link GenericChatModule}'s sub-module. In particular, this class allows to execute multiple
 * {@link JarvisAction}s (from the different concrete chatting modules) through the standard {@link JarvisAction}
 * interface, and can be processed by the regular
 * {@link fr.zelus.jarvis.core.JarvisCore#handleEvent(EventInstance, JarvisSession)} process.
 *
 * @see GenericChatModule
 */
public class GenericChatAction extends JarvisAction<GenericChatModule> {

    /**
     * The concrete chatting {@link JarvisAction}s to execute.
     */
    private List<JarvisAction> concreteActions;

    /**
     * Constructs a new {@link GenericChatAction} from the provided {@code containingModule}, {@code session}, and
     * {@code concreteActions}.
     * @param containingModule the {@link GenericChatModule} containing this action
     * @param session the {@link JarvisSession} associated to this action
     * @param concreteActions the concrete chatting {@link JarvisAction}s to execute
     * @throws NullPointerException if the provided {@code containingModule}, {@code session}, or {@code
     * concreteActions} is {@code null}
     */
    public GenericChatAction(GenericChatModule containingModule, JarvisSession session, List<JarvisAction>
            concreteActions) {
        super(containingModule, session);
        checkNotNull(concreteActions, "Cannot construct a %s with the provided %s list %s", this.getClass()
                .getSimpleName(), JarvisAction.class.getSimpleName(), concreteActions);
        this.concreteActions = concreteActions;
    }

    /**
     * Calls all the concrete chatting {@link JarvisAction} associated to this {@link GenericChatAction}.
     * <p>
     * <b>Note: </b> this method does not ensure that all the concrete {@link JarvisAction}s will be executed if an
     * exception is thrown when executing one of them.
     * @return {@code null}
     */
    @Override
    public Object compute() {
        for (JarvisAction concreteAction : concreteActions) {
            concreteAction.call();
        }
        return null;
    }
}

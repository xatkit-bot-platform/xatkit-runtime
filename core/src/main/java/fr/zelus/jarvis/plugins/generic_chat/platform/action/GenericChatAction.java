package fr.zelus.jarvis.plugins.generic_chat.platform.action;

import fr.zelus.jarvis.core.ExecutionService;
import fr.zelus.jarvis.core.RuntimeAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.plugins.generic_chat.platform.GenericChatPlatform;

import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * A generic {@link RuntimeAction} wrapping concrete chatting sub-runtimePlatform {@link RuntimeAction}s.
 * <p>
 * This class is initialized by the {@link GenericChatPlatform}, and is used as a wrapper for the {@link RuntimeAction}
 * returned by the {@link GenericChatPlatform}'s sub-runtimePlatform. In particular, this class allows to execute multiple
 * {@link RuntimeAction}s (from the different concrete chatting platforms) through the standard {@link RuntimeAction}
 * interface, and can be processed by the regular
 * {@link ExecutionService#handleEventInstance(EventInstance, JarvisSession)} process.
 *
 * @see GenericChatPlatform
 */
public class GenericChatAction extends RuntimeAction<GenericChatPlatform> {

    /**
     * The concrete chatting {@link RuntimeAction}s to execute.
     */
    private List<RuntimeAction> concreteActions;

    /**
     * Constructs a new {@link GenericChatAction} from the provided {@code runtimePlatform}, {@code session}, and
     * {@code concreteActions}.
     * @param runtimePlatform the {@link GenericChatPlatform} containing this action
     * @param session the {@link JarvisSession} associated to this action
     * @param concreteActions the concrete chatting {@link RuntimeAction}s to execute
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code
     * concreteActions} is {@code null}
     */
    public GenericChatAction(GenericChatPlatform runtimePlatform, JarvisSession session, List<RuntimeAction>
            concreteActions) {
        super(runtimePlatform, session);
        checkNotNull(concreteActions, "Cannot construct a %s with the provided %s list %s", this.getClass()
                .getSimpleName(), RuntimeAction.class.getSimpleName(), concreteActions);
        this.concreteActions = concreteActions;
    }

    /**
     * Calls all the concrete chatting {@link RuntimeAction} associated to this {@link GenericChatAction}.
     * <p>
     * <b>Note: </b> this method does not ensure that all the concrete {@link RuntimeAction}s will be executed if an
     * exception is thrown when executing one of them.
     * @return {@code null}
     */
    @Override
    public Object compute() {
        for (RuntimeAction concreteAction : concreteActions) {
            concreteAction.call();
        }
        return null;
    }
}

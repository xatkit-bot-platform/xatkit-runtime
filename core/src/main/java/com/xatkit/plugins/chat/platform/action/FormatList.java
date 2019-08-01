package com.xatkit.plugins.chat.platform.action;

import com.xatkit.core.platform.Formatter;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.chat.platform.ChatPlatform;

import javax.annotation.Nullable;
import java.util.List;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * Formats the provided {@link List} and stores it in the user's {@link XatkitSession}.
 * <p>
 * The concrete formatting of the {@link List} is defined in the {@link #formatList()} method, that must be
 * implemented by concrete subclasses.
 * <p>
 * Storing the {@link List} in the session allows to easily retrieve the value selected by the user in the following
 * actions.
 * <p>
 * This is an abstract action that can be implemented in concrete {@link ChatPlatform} subclasses.
 *
 * @param <T> the concrete {@link ChatPlatform} subclass containing this action
 */
public abstract class FormatList<T extends ChatPlatform> extends RuntimeAction<T> {

    /**
     * The {@link XatkitSession}'s key used to stored the {@link List}.
     * <p>
     * This key can be used by custom action to retrieve the {@link List} and access the element selected by the user
     * using, for example, an {@code integer}, {@code ordinal}, or {@code cardinal} entity type.
     */
    public static final String LAST_FORMATTED_LIST = "xatkit.plugins.chat.last_formatted_list";

    /**
     * The {@link List} to format.
     */
    protected List<?> list;

    protected Formatter formatter;

    /**
     * Constructs a {@link FormatList} with the provided {@code runtimePlatform}, {@code session}, and {@code list}.
     *
     * @param runtimePlatform the concrete {@link ChatPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param list            the {@link List} to format
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code list} is
     *                              {@code null}
     */
    public FormatList(T runtimePlatform, XatkitSession session, List<?> list, @Nullable String formatterName) {
        super(runtimePlatform, session);
        checkNotNull(list, "Cannot construct a %s action from the provided %s %s", this.getClass().getSimpleName(),
                List.class.getSimpleName(), list);
        this.list = list;
        if(isNull(formatterName)) {
            this.formatter = this.runtimePlatform.getXatkitCore().getFormatter("Default");
        } else {
            this.formatter = this.runtimePlatform.getXatkitCore().getFormatter(formatterName);
        }
    }

    /**
     * Formats the provided {@link List} and stores it in the {@link XatkitSession}.
     * <p>
     * The stored {@link List} can be retrieved using {@code session.get(LAST_FORMATTED_LIST}.
     *
     * @return the formatted {@link List}
     * @see #LAST_FORMATTED_LIST
     * @see #formatList()
     */
    @Override
    protected final Object compute() throws Exception {
        Object formattedList = this.formatList();
        this.session.store(LAST_FORMATTED_LIST, list);
        return formattedList;
    }

    /**
     * Formats the provided {@link List}.
     * <p>
     * This method must be implemented by concrete subclasses.
     *
     * @return the formatted {@link List}
     */
    protected abstract Object formatList();
}

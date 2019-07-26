package com.xatkit.plugins.slack.platform.action;

import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import java.util.List;
import java.util.stream.Collectors;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * Formats the provided {@link List} into a set of items that can be embedded in Slack messages.
 * <p>
 * This action supports any kind of {@link List}, and call the {@link Object#toString()} method on each object.
 * <p>
 * <b>Note:</b> this action does not perform any operation on the Slack API, but returns a formatted {@link String}
 * that can be reused as part of a Slack message using {@link Reply} or {@link PostMessage}.
 */
public class ItemizeList extends RuntimeAction<SlackPlatform> {

    /**
     * The list to format as a set of items.
     */
    private List<?> list;

    /**
     * Constructs a {@link ItemizeList} with the provided {@code runtimePlatform}, {@code session}, and {@code list}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param list            the {@link List} to format as a set of items
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code list} is
     *                              {@code null}.
     */
    public ItemizeList(SlackPlatform runtimePlatform, XatkitSession session, List<?> list) {
        super(runtimePlatform, session);
        checkNotNull(list, "Cannot construct a %s action from the provided %s %s", this.getClass().getSimpleName(),
                List.class.getSimpleName(), list);
        this.list = list;
    }

    /**
     * Formats the provided {@link List} into a set of items.
     * <p>
     * Each item is represented with the following pattern: {@code "- item.toString()\n"}.
     *
     * @return the formatted {@link String}
     */
    @Override
    protected Object compute() throws Exception {
        if (list.isEmpty()) {
            return "";
        } else {
            return "- " + String.join("\n- ", list.stream().map(Object::toString).collect(Collectors.toList()));
        }
    }
}

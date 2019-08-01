package com.xatkit.plugins.slack.platform.action;

import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.chat.platform.action.FormatList;
import com.xatkit.plugins.slack.platform.SlackPlatform;

import java.util.List;

/**
 * Formats the provided {@link List} into an enumeration that can be embedded in Slack messages.
 * <p>
 * The provided {@link List} is stored in the {@link XatkitSession} with the {@link FormatList#LAST_FORMATTED_LIST}
 * key, allowing to retrieve and manipulate it in custom actions.
 * <p>
 * This action supports any kind of {@link List}, and call the {@link Object#toString()} method on each object.
 * <p>
 * <b>Note:</b> this action does not perform any operation on the Slack API, but returns a formatted {@link String}
 * that can be reused as part of a Slack message using {@link Reply} or {@link PostMessage}.
 */
public class EnumerateList extends FormatList<SlackPlatform> {

    /**
     * Constructs an {@link EnumerateList} with the provided {@code runtimePlatform}, {@code session}, and {@code list}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param list            the {@link List} to format as a set of items
     * @throws NullPointerException if the provided {@code runtimePlatform}, {@code session}, or {@code list} is
     *                              {@code null}
     */
    public EnumerateList(SlackPlatform runtimePlatform, XatkitSession session, List<?> list) {
        super(runtimePlatform, session, list, null);
    }

    public EnumerateList(SlackPlatform runtimePlatform, XatkitSession session, List<?> list, String formatterName) {
        super(runtimePlatform, session, list, formatterName);
    }

    /**
     * Formats the provided {@link List} into an enumeration.
     * <p>
     * Each item is represented with the following pattern: {@code "[index] item.toString()\n"}.
     *
     * @return the formatted {@link String}
     */
    @Override
    protected Object formatList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append("[")
                    .append(i)
                    .append("] ")
                    .append(formatter.format(list.get(i)))
                    .append("\n");
        }
        return sb.toString();
    }
}

package com.xatkit.plugins.core.platform.action;

import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.core.platform.CorePlatform;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A {@link RuntimeAction} that retrieves the current date and returns it.
 * <p>
 * This class relies on {@link SimpleDateFormat} to format the retrieved date with the following template:
 * <i>dd/MM/yyyy</i>.
 *
 * @see GetTime
 */
public class GetDate extends RuntimeAction<CorePlatform> {

    /**
     * Constructs a new {@link GetDate} action from the provided {@code runtimePlatform} and {@code session}.
     *
     * @param runtimePlatform the {@link CorePlatform} containing this action
     * @param session          the {@link XatkitSession} associated to this action
     * @throws NullPointerException if the provided {@code runtimePlatform} or {@code session} is {@code null}
     */
    public GetDate(CorePlatform runtimePlatform, XatkitSession session) {
        super(runtimePlatform, session);
    }

    /**
     * Retrieves the current date and formats it.
     *
     * @return the formatted date.
     */
    @Override
    public Object compute() {
        return new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
    }
}

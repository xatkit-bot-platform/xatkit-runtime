package com.xatkit.core.recognition.dialogflow;

import com.google.cloud.dialogflow.v2.SessionName;
import com.xatkit.AbstractXatkitTest;
import com.xatkit.test.util.VariableLoaderHelper;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import java.util.AbstractMap;

import static org.assertj.core.api.Assertions.assertThat;

public class DialogFlowStateContextImplTest extends AbstractXatkitTest {

    private static String VALID_PROJECT_ID = VariableLoaderHelper.getVariable(DialogFlowConfiguration.PROJECT_ID_KEY);

    private DialogFlowStateContext stateContext;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test(expected = NullPointerException.class)
    public void constructNullSessionName() {
        stateContext = new DialogFlowStateContextImpl(null);
    }

    @Test
    public void constructValidSessionName() {
        SessionName sessionName = getValidSessionName();
        stateContext = new DialogFlowStateContextImpl(sessionName);
        assertDialogFlowStateContextHasName(stateContext, sessionName);
    }

    @Test(expected = NullPointerException.class)
    public void constructNullConfiguration() {
        SessionName sessionName = getValidSessionName();
        stateContext = new DialogFlowStateContextImpl(sessionName, null);
    }

    @Test
    public void constructEmptyConfiguration() {
        SessionName sessionName = getValidSessionName();
        stateContext = new DialogFlowStateContextImpl(sessionName, new BaseConfiguration());
        assertDialogFlowStateContextHasName(stateContext, sessionName);
    }

    @Test
    public void constructConfigurationWithProperty() {
        SessionName sessionName = getValidSessionName();
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty(DialogFlowConfiguration.PROJECT_ID_KEY, "test");
        stateContext = new DialogFlowStateContextImpl(sessionName, configuration);
        assertDialogFlowStateContextHasName(stateContext, sessionName);
        assertThat(stateContext.getConfiguration()).contains(new AbstractMap.SimpleEntry<>(DialogFlowConfiguration.PROJECT_ID_KEY,
                "test"));
    }

    private SessionName getValidSessionName() {
        return SessionName.of(VALID_PROJECT_ID, "demo");
    }

    private void assertDialogFlowStateContextHasName(DialogFlowStateContext context, SessionName expectedSessionName) {
        assertThat(context.getContextId()).isEqualTo(expectedSessionName.toString());
        assertThat(context.getSessionName()).isEqualTo(expectedSessionName);
    }
}

package com.xatkit.test.util.models;

//import com.xatkit.execution.ActionInstance;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionRule;
import com.xatkit.platform.ActionDefinition;
import com.xatkit.test.util.ElementFactory;
import org.eclipse.xtext.xbase.XMemberFeatureCall;

public class TestExecutionModel {

    private TestPlatformModel platformModel;

    private TestIntentModel intentModel;

    private ExecutionModel executionModel;

    private ExecutionRule executionRule;

    private XMemberFeatureCall featureCall;

    public TestExecutionModel() {
        this.platformModel = new TestPlatformModel();
        this.intentModel = new TestIntentModel();
        executionModel = ExecutionFactory.eINSTANCE.createExecutionModel();
        executionRule = ExecutionFactory.eINSTANCE.createExecutionRule();
        executionRule.setEvent(intentModel.getIntentDefinition());
        ActionDefinition actionDefinition = platformModel.getActionDefinition();
        featureCall = ElementFactory.createXMemberFeatureCall(actionDefinition);
        executionRule.getExpressions().add(featureCall);
        executionModel.getExecutionRules().add(executionRule);
    }

    public TestPlatformModel getTestPlatformModel() {
        return platformModel;
    }

    public TestIntentModel getTestIntentModel() {
        return intentModel;
    }

    public ExecutionModel getExecutionModel() {
        return executionModel;
    }

    public ExecutionRule getExecutionRule() {
        return executionRule;
    }

    public XMemberFeatureCall getRuleFeatureCall() {
        return featureCall;
    }


}

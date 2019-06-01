package com.xatkit.test.util.models;

import com.xatkit.execution.ActionInstance;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.ExecutionModel;
import com.xatkit.execution.ExecutionRule;

public class TestExecutionModel {

    private TestPlatformModel platformModel;

    private TestIntentModel intentModel;

    private ExecutionModel executionModel;

    private ExecutionRule executionRule;

    private ActionInstance ruleActionInstance;

    public TestExecutionModel() {
        this.platformModel = new TestPlatformModel();
        this.intentModel = new TestIntentModel();
        executionModel = ExecutionFactory.eINSTANCE.createExecutionModel();
        executionRule = ExecutionFactory.eINSTANCE.createExecutionRule();
        executionRule.setEvent(intentModel.getIntentDefinition());
        ruleActionInstance = ExecutionFactory.eINSTANCE.createActionInstance();
        ruleActionInstance.setAction(platformModel.getActionDefinition());
        executionRule.getInstructions().add(ruleActionInstance);
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

    public ActionInstance getRuleActionInstance() {
        return ruleActionInstance;
    }


}

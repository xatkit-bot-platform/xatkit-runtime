package edu.uoc.som.jarvis.test.util.models;

import edu.uoc.som.jarvis.execution.ActionInstance;
import edu.uoc.som.jarvis.execution.ExecutionFactory;
import edu.uoc.som.jarvis.execution.ExecutionModel;
import edu.uoc.som.jarvis.execution.ExecutionRule;

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
        executionRule.getActions().add(ruleActionInstance);
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

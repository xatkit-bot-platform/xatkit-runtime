package fr.zelus.jarvis.plugins.log.module;

import fr.zelus.jarvis.AbstractJarvisTest;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.module.ModulePackage;
import fr.zelus.jarvis.orchestration.ActionInstance;
import fr.zelus.jarvis.orchestration.OrchestrationFactory;
import fr.zelus.jarvis.orchestration.OrchestrationPackage;
import fr.zelus.jarvis.orchestration.ParameterValue;
import fr.zelus.jarvis.plugins.log.module.action.LogInfo;
import org.apache.commons.configuration2.BaseConfiguration;
import org.assertj.core.api.JUnitSoftAssertions;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogModuleTest extends AbstractJarvisTest {

    private LogModule logModule;

    private static Module loadedModule;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        EPackage.Registry.INSTANCE.put(ModulePackage.eINSTANCE.getNsURI(), ModulePackage.eINSTANCE);
        EPackage.Registry.INSTANCE.put(OrchestrationPackage.eINSTANCE.getNsURI(), OrchestrationPackage.eINSTANCE);
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        String file = LogModuleTest.class.getClassLoader().getResource("modules/LogModule.xmi").getFile();
        Resource moduleResource = resourceSet.getResource(URI.createURI(file), true);
        loadedModule = (Module) moduleResource.getContents().get(0);
    }

    @Before
    public void setUp() {
        logModule = new LogModule(new BaseConfiguration());
    }

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void getName() {
        softly.assertThat(logModule.getName()).as("Valid name").isEqualTo(LogModule.class.getSimpleName());
        softly.assertThat(logModule.getName()).as("Jarvis module name is consistent with defined name").isEqualTo
                (loadedModule.getName());
    }

    @Test(expected = JarvisException.class)
    public void enableActionNotModuleAction() {
        Action action = createStubAction();
        logModule.enableAction(action);
    }

    @Test
    public void enableActionModuleAction() {
        Action infoAction = getInfoAction();
        logModule.enableAction(infoAction);
        assertThat(logModule.getActions()).as("Action map contains the enabled action").contains(LogInfo.class);
    }

    @Test
    public void enableActionAllActions() {
        /*
         * Enable all the actions first.
         */
        for (Action action : loadedModule.getActions()) {
            logModule.enableAction(action);
        }
        softly.assertThat(logModule.getActions()).as("The action map contains all the defined actions").hasSize
                (loadedModule.getActions().size());
        /*
         * Check that all the actions have been enabled.
         */
        for (Action action : loadedModule.getActions()) {
            boolean found = false;
            for (Class<? extends JarvisAction> jarvisAction : logModule.getActions()) {
                if (jarvisAction.getSimpleName().equals(action.getName())) {
                    found = true;
                }
            }
            softly.assertThat(found).as("Found " + action.getName() + " in the action map").isTrue();
        }
    }

    @Test
    public void disableActionNotModuleAction() {
        Action action = createStubAction();
        logModule.disableAction(action);
    }

    @Test
    public void disableActionModuleAction() {
        for (Action action : loadedModule.getActions()) {
            logModule.enableAction(action);
        }
        Action infoAction = getInfoAction();
        logModule.disableAction(infoAction);
        assertThat(logModule.getActions()).as("The action map does not contain the LofInfo action").doesNotContain
                (LogInfo.class);
    }

    @Test
    public void disableAllActions() {
        for (Action action : loadedModule.getActions()) {
            logModule.enableAction(action);
        }
        logModule.disableAllActions();
        assertThat(logModule.getActions()).as("The action map is empty").isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void createJarvisActionNullActionInstance() {
        logModule.createJarvisAction(null, new JarvisSession("id"));
    }

    @Test(expected = JarvisException.class)
    public void createJarvisActionNotEnabledAction() {
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(getInfoAction());
        logModule.createJarvisAction(actionInstance, new JarvisSession("id"));
    }

    @Test
    public void createJarvisActionWithParameterValue() {
        String validLogMessage = "test message";
        logModule.enableAction(getInfoAction());
        ActionInstance actionInstance = OrchestrationFactory.eINSTANCE.createActionInstance();
        actionInstance.setAction(getInfoAction());
        ParameterValue parameterValue = OrchestrationFactory.eINSTANCE.createParameterValue();
        parameterValue.setParameter(getInfoAction().getParameters().get(0));
        parameterValue.setValue(validLogMessage);
        actionInstance.getValues().add(parameterValue);
        JarvisAction action = logModule.createJarvisAction(actionInstance, new JarvisSession("id"));
        assertThat(action).as("LogInfo action").isInstanceOf(LogInfo.class);
        LogInfo logAction = (LogInfo) action;
        assertThat(logAction.getMessage()).as("Not null message").isNotNull();
        assertThat(logAction.getMessage()).as("Valid message").isEqualTo(validLogMessage);
    }

    private Action createStubAction() {
        Action action = ModuleFactory.eINSTANCE.createAction();
        action.setName("StubAction");
        return action;
    }

    private Action getInfoAction() {
        for (Action action : loadedModule.getActions()) {
            if (action.getName().equals("LogInfo")) {
                return action;
            }
        }
        throw new IllegalStateException("Cannot find the LogInfo Action");
    }

}

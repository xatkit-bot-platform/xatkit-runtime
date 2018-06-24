package fr.zelus.jarvis.util;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.module.Parameter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.IOException;
import java.util.Collections;

/**
 * An utility class that creates {@link Resource} instances representing jarvis core {@link Module}s.
 * <p>
 * This class is used to create instances of the models that are stored under the {@code resources/modules} folder,
 * and can be used in tests to easily retrieve loaded instances of these models.
 */
public class ModuleCreatorUtil {

    /**
     * Creates an instance of the {@link fr.zelus.jarvis.core.module.log.LogModule} model
     * <p>
     * This method attempts to save the created model in the {@code /tmp/jarvis/test} folder. If the model can not be
     * saved (because of permission restrictions or invalid path) the in-memory {@link Resource} is returned and a
     * warning log message is displayed.
     *
     * @return an instance of the {@link fr.zelus.jarvis.core.module.log.LogModule} model
     */
    public static Resource createLogModule() {
        ModuleFactory moduleFactory = ModuleFactory.eINSTANCE;
        Module module = moduleFactory.createModule();
        module.setName("Log");
        module.setJarvisModulePath("fr.zelus.jarvis.core.module.log.LogModule");
        Action infoAction = moduleFactory.createAction();
        infoAction.setName("LogInfo");
        Parameter infoParam = moduleFactory.createParameter();
        infoParam.setKey("message");
        infoParam.setType("String");
        infoAction.getParameters().add(infoParam);

        Action warningAction = moduleFactory.createAction();
        warningAction.setName("LogWarning");
        Parameter warningParam = moduleFactory.createParameter();
        warningParam.setKey("message");
        warningParam.setType("String");
        warningAction.getParameters().add(warningParam);

        Action errorAction = moduleFactory.createAction();
        errorAction.setName("LogError");
        Parameter errorParam = moduleFactory.createParameter();
        errorParam.setKey("message");
        errorParam.setType("String");
        errorAction.getParameters().add(errorParam);

        module.getActions().add(infoAction);
        module.getActions().add(warningAction);
        module.getActions().add(errorAction);

        ResourceSet rSet = new ResourceSetImpl();
        rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        Resource resource = rSet.createResource(URI.createURI("/tmp/jarvis/test/logModule.xmi"));
        resource.getContents().clear();
        resource.getContents().add(module);
        try {
            resource.save(Collections.emptyMap());
        } catch (IOException e) {
            Log.warn("Cannot save the LogModule resource (uri={0}), returning an in-memory instance", resource.getURI
                    ());
        }
        return resource;
    }

    public static Resource createSlackModule() {
        ModuleFactory moduleFactory = ModuleFactory.eINSTANCE;
        Module module = moduleFactory.createModule();
        module.setName("Slack");
        module.setJarvisModulePath("fr.zelus.jarvis.slack.module.SlackModule");
        Action postMessageAction = moduleFactory.createAction();
        postMessageAction.setName("PostMessage");
        Parameter messageParam = moduleFactory.createParameter();
        messageParam.setKey("message");
        messageParam.setType("String");
        Parameter channelParam = moduleFactory.createParameter();
        channelParam.setKey("channel");
        channelParam.setType("String");
        postMessageAction.getParameters().add(messageParam);
        postMessageAction.getParameters().add(channelParam);

        module.getActions().add(postMessageAction);

        ResourceSet rSet = new ResourceSetImpl();
        rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        Resource resource = rSet.createResource(URI.createURI("/tmp/jarvis/test/slackModule.xmi"));
        resource.getContents().clear();
        resource.getContents().add(module);
        try {
            resource.save(Collections.emptyMap());
        } catch (IOException e) {
            Log.warn("Cannot save the LogModule resource (uri={0}), returning an in-memory instance", resource.getURI
                    ());
        }
        return resource;
    }
}

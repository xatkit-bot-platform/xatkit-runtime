package fr.zelus.jarvis.util;

import fr.zelus.jarvis.module.Action;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.module.Parameter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.util.Collections;

public class ModuleCreatorUtil {

    public static Resource createLogModule() throws Exception {
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
        Resource resource = rSet.createResource(URI.createURI("/tmp/test/logModule.xmi"));
        resource.getContents().clear();
        resource.getContents().add(module);
        resource.save(Collections.emptyMap());
        return resource;
    }
}

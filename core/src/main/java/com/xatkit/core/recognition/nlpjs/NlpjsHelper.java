package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.nlpjs.model.EntityType;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.EntityDefinition;

import java.util.List;

public class NlpjsHelper {

    public static int getEntityCount(EntityDefinition entityType, List<Context> outContexts) {
        int count = 0;
        for (Context context : outContexts) {
            for (ContextParameter parameter : context.getParameters()) {
                if (entityType.getName().equals(parameter.getEntity().getReferredEntity().getName()))
                    count++;
            }
        }
        return count;
    }

    public static int getEntityTypeIndex(String textFragment, EntityDefinition entityType, List<Context> outContexts) {
        int suffix = 0;
        boolean found = false;
        for (Context context : outContexts) {
            for (ContextParameter parameter : context.getParameters()) {
                if(entityType.getName().equals(parameter.getEntity().getReferredEntity().getName()))
                    suffix++;
                if (textFragment.equals(parameter.getTextFragment())) {
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }
        return suffix;
    }

}

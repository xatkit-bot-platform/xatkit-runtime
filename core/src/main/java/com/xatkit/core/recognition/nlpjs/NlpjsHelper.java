package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.nlpjs.mapper.NlpjsEntityReferenceMapper;
import com.xatkit.intent.*;

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

    public static Context getContextFromNlpEntity(String nlpjsEntityType, List<Context> outContexts, NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        //TODO take care of suffixed entity types
        for (Context context : outContexts) {
            for (ContextParameter parameter : context.getParameters()) {
                if(parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
                    if (nlpjsEntityReferenceMapper.getReversedEntity(nlpjsEntityType).stream().anyMatch(entityType -> entityType.equals(parameter.getEntity().getReferredEntity().getName())))
                        return context;
                } else if (parameter.getEntity().getReferredEntity() instanceof CustomEntityDefinition) {
                        CustomEntityDefinition customEntityDefinition = (CustomEntityDefinition) parameter.getEntity().getReferredEntity();
                        if(customEntityDefinition.getName().equals(nlpjsEntityType))
                            return context;

                    }

            }
        }
        return null;
    }

    public static ContextParameter getContextParameterFromNlpEntity(String nlpjsEntityType, List<Context> outContexts, NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        //TODO take care of suffixed entity types
        for (Context context : outContexts) {
            for (ContextParameter parameter : context.getParameters()) {
                if(parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
                    if (nlpjsEntityReferenceMapper.getReversedEntity(nlpjsEntityType).stream().anyMatch(entityType -> entityType.equals(parameter.getEntity().getReferredEntity().getName())))
                        return parameter;
                } else if (parameter.getEntity().getReferredEntity() instanceof CustomEntityDefinition) {
                    CustomEntityDefinition customEntityDefinition = (CustomEntityDefinition) parameter.getEntity().getReferredEntity();
                    if(customEntityDefinition.getName().equals(nlpjsEntityType))
                        return parameter;

                }

            }
        }
        return null;
    }


}

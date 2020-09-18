package com.xatkit.core.recognition.nlpjs;

import com.xatkit.core.recognition.nlpjs.mapper.NlpjsEntityReferenceMapper;
import com.xatkit.intent.*;

import java.util.List;

public class NlpjsHelper {

    public static int getEntityCount(EntityDefinition entityType, List<Context> outContexts) {
        int count = 0;
        for (Context context : outContexts) {
            for (ContextParameter parameter : context.getParameters()) {
                if (entityType.getName().equals(parameter.getEntity().getReferredEntity().getName())) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int getEntityTypeIndex(String textFragment, EntityDefinition entityType, List<Context> outContexts) {
        int suffix = 0;
        boolean found = false;
        for (Context context : outContexts) {
            for (ContextParameter parameter : context.getParameters()) {
                if (entityType.getName().equals(parameter.getEntity().getReferredEntity().getName())) {
                    suffix++;
                }
                if (textFragment.equals(parameter.getTextFragment())) {
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        return suffix;
    }

    // This method still needs some improvement in terms of redundant code and performance
    public static Context getContextFromNlpEntity(String nlpjsEntityType, RecognizedIntent recognizedIntent, NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        String[] splitEntity = nlpjsEntityType.split("_");
        // The suffixed types do not work with v4 of NLP.js. We need to wait for the update from the guys of NLP.js
        if (splitEntity.length > 1 && isNumeric(splitEntity[splitEntity.length - 1])) {
            int index = Integer.parseInt(splitEntity[splitEntity.length - 1]);
            String baseEntityType = nlpjsEntityType.substring(0, nlpjsEntityType.lastIndexOf("_"));
            int i = 0;
            for (Context context : recognizedIntent.getDefinition().getOutContexts()) {
                for (ContextParameter parameter : context.getParameters()) {
                    if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
                        if (((BaseEntityDefinition) parameter.getEntity().getReferredEntity()).getEntityType().equals(EntityType.ANY)
                                && nlpjsEntityType.equals(recognizedIntent.getDefinition().getName() + parameter.getName() + "Any")) {
                            return context;
                        }
                        if (nlpjsEntityReferenceMapper.getReversedEntity(baseEntityType).stream().anyMatch(entityType -> entityType.equals(parameter.getEntity().getReferredEntity().getName())) &&
                                (++i == index)) {
                            return context;
                        }
                    } else if (parameter.getEntity().getReferredEntity() instanceof CustomEntityDefinition) {
                        CustomEntityDefinition customEntityDefinition = (CustomEntityDefinition) parameter.getEntity().getReferredEntity();
                        if (customEntityDefinition.getName().equals(baseEntityType) && (++i == index))
                            return context;
                    }
                }
            }
        } else {
            for (Context context : recognizedIntent.getDefinition().getOutContexts()) {
                for (ContextParameter parameter : context.getParameters()) {
                    if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
                        if (((BaseEntityDefinition) parameter.getEntity().getReferredEntity()).getEntityType().equals(EntityType.ANY)
                                && nlpjsEntityType.equals(recognizedIntent.getDefinition().getName() + parameter.getName() + "Any")) {
                            return context;
                        }
                        if (nlpjsEntityReferenceMapper.getReversedEntity(nlpjsEntityType).stream().anyMatch(entityType -> entityType.equals(parameter.getEntity().getReferredEntity().getName()))) {
                            return context;
                        }
                    } else if (parameter.getEntity().getReferredEntity() instanceof CustomEntityDefinition) {
                        CustomEntityDefinition customEntityDefinition = (CustomEntityDefinition) parameter.getEntity().getReferredEntity();
                        if (customEntityDefinition.getName().equals(nlpjsEntityType))
                            return context;

                    }
                }
            }
        }
        return null;
    }

    // This method still needs some improvement in terms of redundant code and performance
    public static ContextParameter getContextParameterFromNlpEntity(String nlpjsEntityType, RecognizedIntent recognizedIntent, NlpjsEntityReferenceMapper nlpjsEntityReferenceMapper) {
        // The suffixed types do not work with v4 of NLP.js. We need to wait for the update from the guys of NLP.js
        String[] splitEntity = nlpjsEntityType.split("_");
        if (splitEntity.length > 1 && isNumeric(splitEntity[splitEntity.length - 1])) {
            int index = Integer.parseInt(splitEntity[splitEntity.length - 1]);
            String baseEntityType = nlpjsEntityType.substring(0, nlpjsEntityType.lastIndexOf("_"));
            int i = 0;
            for (Context context : recognizedIntent.getDefinition().getOutContexts()) {
                for (ContextParameter parameter : context.getParameters()) {
                    if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
                        if (((BaseEntityDefinition) parameter.getEntity().getReferredEntity()).getEntityType().equals(EntityType.ANY)
                                && nlpjsEntityType.equals(recognizedIntent.getDefinition().getName() + parameter.getName() + "Any")) {
                            return parameter;
                        }
                        if (nlpjsEntityReferenceMapper.getReversedEntity(baseEntityType).stream().anyMatch(entityType -> entityType.equals(parameter.getEntity().getReferredEntity().getName())) &&
                                (++i == index)) {
                            return parameter;
                        }
                    } else if (parameter.getEntity().getReferredEntity() instanceof CustomEntityDefinition) {
                        CustomEntityDefinition customEntityDefinition = (CustomEntityDefinition) parameter.getEntity().getReferredEntity();
                        if (customEntityDefinition.getName().equals(baseEntityType) && (++i == index)) {
                            return parameter;
                        }
                    }
                }
            }
        } else {
            for (Context context : recognizedIntent.getDefinition().getOutContexts()) {
                for (ContextParameter parameter : context.getParameters()) {
                    if (parameter.getEntity().getReferredEntity() instanceof BaseEntityDefinition) {
                        if (((BaseEntityDefinition) parameter.getEntity().getReferredEntity()).getEntityType().equals(EntityType.ANY)
                                && nlpjsEntityType.equals(recognizedIntent.getDefinition().getName() + parameter.getName() + "Any")) {
                            return parameter;
                        }
                        if (nlpjsEntityReferenceMapper.getReversedEntity(nlpjsEntityType).stream().anyMatch(entityType -> entityType.equals(parameter.getEntity().getReferredEntity().getName()))) {
                            return parameter;
                        }
                    } else if (parameter.getEntity().getReferredEntity() instanceof CustomEntityDefinition) {
                        CustomEntityDefinition customEntityDefinition = (CustomEntityDefinition) parameter.getEntity().getReferredEntity();
                        if (customEntityDefinition.getName().equals(nlpjsEntityType)) {
                            return parameter;
                        }
                    }
                }
            }
        }
        return null;
    }


    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}

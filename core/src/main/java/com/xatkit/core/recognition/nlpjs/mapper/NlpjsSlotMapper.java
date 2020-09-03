package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.EntityMapper;
import com.xatkit.intent.CustomEntityDefinition;

import java.text.MessageFormat;

import static com.xatkit.intent.EntityType.*;

public class NlpjsSlotMapper extends EntityMapper {

    public NlpjsSlotMapper() {
        super();
        this.registerEntities();
        this.setFallbackEntityMapping("any");
    }

    private void registerEntities() {
        this.addEntityMapping(ANY, "any");
        this.addEntityMapping(EMAIL,"email");
        this.addEntityMapping(PHONE_NUMBER,"phonenumber");
        this.addEntityMapping(URL,"url");
        this.addEntityMapping(NUMBER,"number");
        this.addEntityMapping(PERCENTAGE,"percentage");
    }

}

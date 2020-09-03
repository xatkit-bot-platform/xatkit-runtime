package com.xatkit.core.recognition.nlpjs.mapper;

import com.xatkit.core.recognition.EntityMapper;

import static com.xatkit.intent.EntityType.*;

public class NlpjsSlotMapper extends EntityMapper {

    public NlpjsSlotMapper() {
        super();
        this.registerEntities();
        this.setFallbackEntityMapping("@sys.any");
    }

    private void registerEntities() {
        this.addEntityMapping(EMAIL,"email");
        this.addEntityMapping(PHONE_NUMBER,"phonenumber");
        this.addEntityMapping(URL,"url");
        this.addEntityMapping(NUMBER,"number");
        this.addEntityMapping(PERCENTAGE,"percentage");
    }






}

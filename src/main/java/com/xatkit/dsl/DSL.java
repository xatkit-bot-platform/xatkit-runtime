package com.xatkit.dsl;

import com.xatkit.dsl.entity.CompositeEntryStep;
import com.xatkit.dsl.entity.MappingEntryStep;
import com.xatkit.dsl.entity.impl.CompositeEntityDefinitionBuilder;
import com.xatkit.dsl.entity.impl.MappingEntityDefinitionBuilder;
import com.xatkit.dsl.intent.EventContextParameterStep;
import com.xatkit.dsl.intent.EventDefinitionProvider;
import com.xatkit.dsl.intent.IntentDefinitionProvider;
import com.xatkit.dsl.intent.IntentMandatoryTrainingSentenceStep;
import com.xatkit.dsl.intent.impl.EventDefinitionBuilder;
import com.xatkit.dsl.intent.impl.IntentDefinitionBuilder;
import com.xatkit.dsl.library.EntityStep;
import com.xatkit.dsl.library.impl.LibraryBuilder;
import com.xatkit.dsl.model.UseEventStep;
import com.xatkit.dsl.model.impl.ExecutionModelBuilder;
import com.xatkit.dsl.state.BodyStep;
import com.xatkit.dsl.state.FallbackBodyStep;
import com.xatkit.dsl.state.impl.StateBuilder;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.BaseEntityDefinitionReference;
import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.EventDefinition;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.execution.predicate.IsEventDefinitionPredicate;
import com.xatkit.execution.predicate.IsIntentDefinitionPredicate;
import lombok.NonNull;

import java.util.function.Predicate;

public class DSL {

    public static @NonNull UseEventStep model() {
        return new ExecutionModelBuilder();
    }

    public static @NonNull EntityStep library(String name) {
        LibraryBuilder libraryBuilder = new LibraryBuilder();
        libraryBuilder.name(name);
        return libraryBuilder;
    }

    public static @NonNull BodyStep state(@NonNull String name) {
        StateBuilder stateBuilder = new StateBuilder();
        stateBuilder.name(name);
        return stateBuilder;
    }

    public static @NonNull FallbackBodyStep fallbackState() {
        StateBuilder stateBuilder = new StateBuilder();
        stateBuilder.name("Default_Fallback");
        return stateBuilder;
    }

    public static @NonNull IntentMandatoryTrainingSentenceStep intent(@NonNull String name) {
        IntentDefinitionBuilder intentDefinitionBuilder = new IntentDefinitionBuilder();
        intentDefinitionBuilder.name(name);
        return intentDefinitionBuilder;
    }

    public static @NonNull EventContextParameterStep event(@NonNull String name) {
        EventDefinitionBuilder eventDefinitionBuilder = new EventDefinitionBuilder();
        eventDefinitionBuilder.name(name);
        return eventDefinitionBuilder;
    }

    public static @NonNull MappingEntryStep mapping(@NonNull String name) {
        MappingEntityDefinitionBuilder mappingEntityDefinitionBuilder = new MappingEntityDefinitionBuilder();
        mappingEntityDefinitionBuilder.name(name);
        return mappingEntityDefinitionBuilder;
    }

    public static @NonNull CompositeEntryStep composite(@NonNull String name) {
        CompositeEntityDefinitionBuilder compositeEntityDefinitionBuilder = new CompositeEntityDefinitionBuilder();
        compositeEntityDefinitionBuilder.name(name);
        return compositeEntityDefinitionBuilder;
    }

    public static @NonNull Predicate<StateContext> intentIs(@NonNull IntentDefinitionProvider intentProvider) {
        return intentIs(intentProvider.getIntentDefinition());
    }

    public static @NonNull Predicate<StateContext> intentIs(@NonNull IntentDefinition intent) {
        return new IsIntentDefinitionPredicate(intent);
    }

    public static @NonNull Predicate<StateContext> eventIs(@NonNull EventDefinitionProvider eventProvider) {
        return eventIs(eventProvider.getEventDefinition());
    }

    public static @NonNull Predicate<StateContext> eventIs(@NonNull EventDefinition event) {
        return new IsEventDefinitionPredicate(event);
    }

    public static @NonNull EntityDefinitionReference any() {
        return getEntityDefinitionReference(EntityType.ANY);
    }

    public static @NonNull EntityDefinitionReference date() {
        return getEntityDefinitionReference(EntityType.DATE);
    }

    public static @NonNull EntityDefinitionReference dateTime() {
        return getEntityDefinitionReference(EntityType.DATE_TIME);
    }

    public static @NonNull EntityDefinitionReference datePeriod() {
        return getEntityDefinitionReference(EntityType.DATE_PERIOD);
    }

    public static @NonNull EntityDefinitionReference time() {
        return getEntityDefinitionReference(EntityType.TIME);
    }

    public static @NonNull EntityDefinitionReference timePeriod() {
        return getEntityDefinitionReference(EntityType.TIME_PERIOD);
    }

    public static @NonNull EntityDefinitionReference number() {
        return getEntityDefinitionReference(EntityType.NUMBER);
    }

    public static @NonNull EntityDefinitionReference cardinal() {
        return getEntityDefinitionReference(EntityType.CARDINAL);
    }

    public static @NonNull EntityDefinitionReference ordinal() {
        return getEntityDefinitionReference(EntityType.ORDINAL);
    }

    public static @NonNull EntityDefinitionReference integer() {
        return getEntityDefinitionReference(EntityType.INTEGER);
    }

    public static @NonNull EntityDefinitionReference numberSequence() {
        return getEntityDefinitionReference(EntityType.NUMBER_SEQUENCE);
    }

    public static @NonNull EntityDefinitionReference flightNumber() {
        return getEntityDefinitionReference(EntityType.FLIGHT_NUMBER);
    }

    public static @NonNull EntityDefinitionReference unitArea() {
        return getEntityDefinitionReference(EntityType.UNIT_AREA);
    }

    public static @NonNull EntityDefinitionReference unitCurrency() {
        return  getEntityDefinitionReference(EntityType.UNIT_CURRENCY);
    }

    public static @NonNull EntityDefinitionReference unitLength() {
        return getEntityDefinitionReference(EntityType.UNIT_LENGTH);
    }

    public static @NonNull EntityDefinitionReference unitSpeed() {
        return getEntityDefinitionReference(EntityType.UNIT_SPEED);
    }

    public static @NonNull EntityDefinitionReference unitVolume() {
        return getEntityDefinitionReference(EntityType.UNIT_VOLUME);
    }

    public static @NonNull EntityDefinitionReference unitWeight() {
        return getEntityDefinitionReference(EntityType.UNIT_WEIGHT);
    }

    public static @NonNull EntityDefinitionReference unitInformation() {
        return getEntityDefinitionReference(EntityType.UNIT_INFORMATION);
    }

    public static @NonNull EntityDefinitionReference percentage() {
        return getEntityDefinitionReference(EntityType.PERCENTAGE);
    }

    public static @NonNull EntityDefinitionReference temperature() {
        return getEntityDefinitionReference(EntityType.TEMPERATURE);
    }

    public static @NonNull EntityDefinitionReference duration() {
        return getEntityDefinitionReference(EntityType.DURATION);
    }

    public static @NonNull EntityDefinitionReference age() {
        return getEntityDefinitionReference(EntityType.AGE);
    }

    public static @NonNull EntityDefinitionReference address() {
        return getEntityDefinitionReference(EntityType.ADDRESS);
    }

    public static @NonNull EntityDefinitionReference streetAddress() {
        return getEntityDefinitionReference(EntityType.STREET_ADDRESS);
    }

    public static @NonNull EntityDefinitionReference zipCode() {
        return getEntityDefinitionReference(EntityType.ZIP_CODE);
    }

    public static @NonNull EntityDefinitionReference capital() {
        return getEntityDefinitionReference(EntityType.CAPITAL);
    }

    public static @NonNull EntityDefinitionReference country() {
        return getEntityDefinitionReference(EntityType.COUNTRY);
    }

    public static @NonNull EntityDefinitionReference countryCode() {
        return getEntityDefinitionReference(EntityType.COUNTRY_CODE);
    }

    public static @NonNull EntityDefinitionReference city() {
        return getEntityDefinitionReference(EntityType.CITY);
    }

    public static @NonNull EntityDefinitionReference state() {
        return getEntityDefinitionReference(EntityType.STATE);
    }

    public static @NonNull EntityDefinitionReference cityUs() {
        return getEntityDefinitionReference(EntityType.CITY_US);
    }

    public static @NonNull EntityDefinitionReference stateUs() {
        return getEntityDefinitionReference(EntityType.STATE_US);
    }

    public static @NonNull EntityDefinitionReference countyUs() {
        return getEntityDefinitionReference(EntityType.COUNTY_US);
    }

    public static @NonNull EntityDefinitionReference cityGb() {
        return getEntityDefinitionReference(EntityType.CITY_GB);
    }

    public static @NonNull EntityDefinitionReference stateGb() {
        return getEntityDefinitionReference(EntityType.STATE_GB);
    }

    public static @NonNull EntityDefinitionReference countyGb() {
        return getEntityDefinitionReference(EntityType.COUNTY_GB);
    }

    public static @NonNull EntityDefinitionReference placeAttractionUs() {
        return getEntityDefinitionReference(EntityType.PLACE_ATTRACTION_US);
    }

    public static @NonNull EntityDefinitionReference placeAttractionGb() {
        return getEntityDefinitionReference(EntityType.PLACE_ATTRACTION_GB);
    }

    public static @NonNull EntityDefinitionReference placeAttraction() {
        return getEntityDefinitionReference(EntityType.PLACE_ATTRACTION);
    }

    public static @NonNull EntityDefinitionReference airport() {
        return getEntityDefinitionReference(EntityType.AIRPORT);
    }

    public static @NonNull EntityDefinitionReference location() {
        return getEntityDefinitionReference(EntityType.LOCATION);
    }

    public static @NonNull EntityDefinitionReference email() {
        return getEntityDefinitionReference(EntityType.EMAIL);
    }

    public static @NonNull EntityDefinitionReference phoneNumber() {
        return getEntityDefinitionReference(EntityType.PHONE_NUMBER);
    }

    public static @NonNull EntityDefinitionReference givenName() {
        return getEntityDefinitionReference(EntityType.GIVEN_NAME);
    }

    public static @NonNull EntityDefinitionReference lastName() {
        return getEntityDefinitionReference(EntityType.LAST_NAME);
    }

    public static @NonNull EntityDefinitionReference musicArtist() {
        return getEntityDefinitionReference(EntityType.MUSIC_ARTIST);
    }

    public static @NonNull EntityDefinitionReference musicGenre() {
        return getEntityDefinitionReference(EntityType.MUSIC_GENRE);
    }

    public static @NonNull EntityDefinitionReference color() {
        return getEntityDefinitionReference(EntityType.COLOR);
    }

    public static @NonNull EntityDefinitionReference language() {
        return getEntityDefinitionReference(EntityType.LANGUAGE);
    }

    public static @NonNull EntityDefinitionReference url() {
        return getEntityDefinitionReference(EntityType.URL);
    }


    private static @NonNull EntityDefinitionReference getEntityDefinitionReference(@NonNull EntityType entityType) {
        BaseEntityDefinition baseEntityDefinition = IntentFactory.eINSTANCE.createBaseEntityDefinition();
        baseEntityDefinition.setEntityType(entityType);
        BaseEntityDefinitionReference reference = IntentFactory.eINSTANCE.createBaseEntityDefinitionReference();
        reference.setBaseEntity(baseEntityDefinition);
        return reference;
    }
}

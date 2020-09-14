package com.xatkit.library.core;

import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.impl.LibraryImpl;
import lombok.NonNull;

import java.util.Arrays;

import static com.xatkit.dsl.DSL.*;

public class CoreLibrary extends LibraryImpl {

    public static IntentDefinition Yes = intent("Yes")
            .trainingSentence("Yes")
            .trainingSentence("yes")
            .trainingSentence("fine")
            .trainingSentence("okay")
            .trainingSentence("ok")
            .trainingSentence("yea")
            .trainingSentence("affirmative")
            .trainingSentence("sure")
            .trainingSentence("yep")
            .getIntentDefinition();

    public static IntentDefinition No = intent("No")
            .trainingSentence("No")
            .trainingSentence("no")
            .trainingSentence("nope")
            .trainingSentence("negative")
            .getIntentDefinition();

    public static IntentDefinition Maybe = intent("Maybe")
            .trainingSentence("maybe")
            .trainingSentence("perchance")
            .trainingSentence("perhaps")
            .trainingSentence("could be")
            .trainingSentence("might be")
            .getIntentDefinition();

    public static IntentDefinition Help = intent("Help")
            .trainingSentence("Help")
            .trainingSentence("help")
            .trainingSentence("Assist")
            .trainingSentence("I need support")
            .trainingSentence("I need some help")
            .trainingSentence("I need some assistance")
            .getIntentDefinition();

    public static IntentDefinition Greetings = intent("Greetings")
            .trainingSentence("Hello")
            .trainingSentence("Hi")
            .trainingSentence("Howdy")
            .trainingSentence("Yo")
            .trainingSentence("What's up?")
            .trainingSentence("Greetings")
            .trainingSentence("Good morning")
            .trainingSentence("Good afternoon")
            .getIntentDefinition();

    public static IntentDefinition HowAreYou = intent("HowAreYou")
            .trainingSentence("How are you?")
            .trainingSentence("How do you feel?")
            .trainingSentence("How do you do?")
            .trainingSentence("How is it going?")
            .trainingSentence("How are you doing?")
            .trainingSentence("How's everything?")
            .trainingSentence("What's going on?")
            .getIntentDefinition();

    public static IntentDefinition WhoAreYou = intent("WhoAreYou")
            .trainingSentence("Who are you?")
            .trainingSentence("What are you?")
            .getIntentDefinition();

    public static IntentDefinition GoodBye = intent("GoodBye")
            .trainingSentence("Goodbye")
            .trainingSentence("Good-bye")
            .trainingSentence("Farewell")
            .trainingSentence("Bye")
            .trainingSentence("Bye-bye")
            .trainingSentence("Bye bye")
            .trainingSentence("Good day")
            .trainingSentence("Good night")
            .trainingSentence("See you later")
            .trainingSentence("See you")
            .getIntentDefinition();

    public static IntentDefinition Thanks = intent("Thanks")
            .trainingSentence("Thank you")
            .trainingSentence("Thanks")
            .trainingSentence("Thanking")
            .trainingSentence("Thanks a lot")
            .trainingSentence("Appreciate")
            .trainingSentence("Thank you very much")
            .trainingSentence("Many thanks")
            .trainingSentence("You have my gratitude")
            .getIntentDefinition();

    /*
     * Raw data type intents.
     */
    public static IntentDefinition AddressValue = createDataTypeIntent("AddressValue", address());

    public static IntentDefinition AgeValue = createDataTypeIntent("AgeValue", age());

    public static IntentDefinition AirportValue = createDataTypeIntent("AirportValue", airport());

    public static IntentDefinition AnyValue = createDataTypeIntent("AnyValue", any());

    public static IntentDefinition CapitalValue = createDataTypeIntent("CapitalValue", capital());

    public static IntentDefinition CardinalValue = createDataTypeIntent("CardinalValue", cardinal());

    public static IntentDefinition CityValue = createDataTypeIntent("CityValue", city());

    public static IntentDefinition CityGBValue = createDataTypeIntent("CityGBValue", cityGb());

    public static IntentDefinition CityUSValue = createDataTypeIntent("CityUSValue", cityUs());

    public static IntentDefinition ColorValue = createDataTypeIntent("ColorValue", color());

    public static IntentDefinition CountryValue = createDataTypeIntent("CountryValue", country());

    public static IntentDefinition CountryCodeValue = createDataTypeIntent("CountryCodeValue", countryCode());

    public static IntentDefinition CountyGBValue = createDataTypeIntent("CountryGBValue", countyGb());

    public static IntentDefinition CountyUSValue = createDataTypeIntent("CountyUSValue", countyUs());

    public static IntentDefinition DateValue = createDataTypeIntent("DateValue", date());

    public static IntentDefinition DatePeriodValue = createDataTypeIntent("DatePeriodValue", datePeriod());

    public static IntentDefinition DateTimeValue = createDataTypeIntent("DateTimeValue", dateTime());

    public static IntentDefinition DurationValue = createDataTypeIntent("DurationValue", duration());

    public static IntentDefinition EmailValue = createDataTypeIntent("EmailValue", email());

    public static IntentDefinition FlightNumberValue = createDataTypeIntent("FlightNumberValue", flightNumber());

    public static IntentDefinition GivenNameValue = createDataTypeIntent("GiveNameValue", givenName());

    public static IntentDefinition IntegerValue = createDataTypeIntent("IntegerValue", integer());

    public static IntentDefinition LanguageValue = createDataTypeIntent("LanguageValue", language());

    public static IntentDefinition LastNameValue = createDataTypeIntent("LastNameValue", lastName());

    public static IntentDefinition LocationValue = createDataTypeIntent("LocationValue", location());

    public static IntentDefinition MusicArtistValue = createDataTypeIntent("MusicArtistValue", musicArtist());

    public static IntentDefinition MusicGenreValue = createDataTypeIntent("MusicGenreValue", musicGenre());

    public static IntentDefinition NumberValue = createDataTypeIntent("NumberValue", number());

    public static IntentDefinition NumberSequenceValue = createDataTypeIntent("NumberSequenceValue", numberSequence());

    public static IntentDefinition OrdinalValue = createDataTypeIntent("OrdinalValue", ordinal());

    public static IntentDefinition PercentageValue = createDataTypeIntent("PercentageValue", percentage());

    public static IntentDefinition PhoneNumberValue = createDataTypeIntent("PhoneNumberValue", phoneNumber());

    public static IntentDefinition PlaceAttractionValue = createDataTypeIntent("PlaceAttractionValue", placeAttraction());

    public static IntentDefinition PlaceAttractionGBValue = createDataTypeIntent("PlaceAttractionGBValue",
            placeAttractionGb());

    public static IntentDefinition PlaceAttractionUSValue = createDataTypeIntent("PlaceAttractionUSValue",
            placeAttractionUs());

    public static IntentDefinition StateValue = createDataTypeIntent("StateValue", state());

    public static IntentDefinition StateGBValue = createDataTypeIntent("StateGBValue", stateGb());

    public static IntentDefinition StateUSValue = createDataTypeIntent("StateUSValue", stateUs());

    public static IntentDefinition StreetAddressValue = createDataTypeIntent("StreetAddressValue", streetAddress());

    public static IntentDefinition TemperatureValue = createDataTypeIntent("TemperatureValue", temperature());

    public static IntentDefinition TimeValue = createDataTypeIntent("TimeValue", time());

    public static IntentDefinition TimePeriodValue = createDataTypeIntent("TimePeriodValue", timePeriod());

    public static IntentDefinition UnitAreaValue = createDataTypeIntent("UnitAreaValue", unitArea());

    public static IntentDefinition UnitCurrencyValue = createDataTypeIntent("UnitCurrencyValue", unitCurrency());

    public static IntentDefinition UnitInformationValue = createDataTypeIntent("UnitInformationValue", unitInformation());

    public static IntentDefinition UnitLengthValue = createDataTypeIntent("UnitLengthValue", unitLength());

    public static IntentDefinition UnitSpeedValue = createDataTypeIntent("UnitSpeedValue", unitSpeed());

    public static IntentDefinition UnitVolumeValue = createDataTypeIntent("UnitVolumeValue", unitVolume());

    public static IntentDefinition UnitWeightValue = createDataTypeIntent("UnitWeightValue", unitWeight());

    public static IntentDefinition URLValue = createDataTypeIntent("URLValue", url());

    public static IntentDefinition ZipCodeValue = createDataTypeIntent("ZipCodeValue", zipCode());

    /**
     * Creates the <i>value</i> {@link IntentDefinition} for the provided {@code type}.
     * <p>
     * Value {@link IntentDefinition}s are intents with a single "VALUE" training sentence that is mapped to a
     * parameter of the provided {@code type}. They allow to extract values from <i>pure</i> user inputs (i.e. user
     * inputs without anything else than the value itself).
     * @param name the name of the {@link IntentDefinition} to create
     * @param type the entity of the parameter to set in the created {@link IntentDefinition}
     * @return the created {@link IntentDefinition}
     */
    private static @NonNull IntentDefinition createDataTypeIntent(@NonNull String name, @NonNull EntityDefinitionReference type) {
        return intent(name)
                .trainingSentence("VALUE")
                .context("Value")
                    .parameter("value")
                    .fromFragment("VALUE")
                    .entity(type)
                .getIntentDefinition();
    }

    public CoreLibrary() {
        super();
        this.getEventDefinitions().addAll(Arrays.asList(
                Yes,
                No,
                Maybe,
                Help,
                Greetings,
                HowAreYou,
                WhoAreYou,
                GoodBye,
                Thanks,
                AddressValue,
                AgeValue,
                AirportValue,
                AnyValue,
                CapitalValue,
                CardinalValue,
                CityValue,
                CityGBValue,
                CityUSValue,
                ColorValue,
                CountryValue,
                CountryCodeValue,
                CountyGBValue,
                CountyUSValue,
                DateValue,
                DatePeriodValue,
                DateTimeValue,
                DurationValue,
                EmailValue,
                FlightNumberValue,
                GivenNameValue,
                IntegerValue,
                LanguageValue,
                LastNameValue,
                LocationValue,
                MusicArtistValue,
                MusicGenreValue,
                NumberValue,
                NumberSequenceValue,
                OrdinalValue,
                PercentageValue,
                PhoneNumberValue,
                PlaceAttractionValue,
                PlaceAttractionGBValue,
                PlaceAttractionUSValue,
                StateValue,
                StateGBValue,
                StateUSValue,
                StreetAddressValue,
                TemperatureValue,
                TimeValue,
                TimePeriodValue,
                UnitAreaValue,
                UnitCurrencyValue,
                UnitInformationValue,
                UnitLengthValue,
                UnitSpeedValue,
                UnitVolumeValue,
                UnitWeightValue,
                URLValue,
                ZipCodeValue
        ));
    }
}

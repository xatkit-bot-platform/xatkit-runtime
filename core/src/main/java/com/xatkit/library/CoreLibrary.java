package com.xatkit.library;

import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.impl.LibraryImpl;
import lombok.NonNull;

import java.util.Arrays;

import static com.xatkit.dsl.DSL.*;

public class CoreLibrary extends LibraryImpl {

    public IntentDefinition Yes = intent("Yes")
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

    public IntentDefinition No = intent("No")
            .trainingSentence("No")
            .trainingSentence("no")
            .trainingSentence("nope")
            .trainingSentence("negative")
            .getIntentDefinition();

    public IntentDefinition Maybe = intent("Maybe")
            .trainingSentence("maybe")
            .trainingSentence("perchance")
            .trainingSentence("perhaps")
            .trainingSentence("could be")
            .trainingSentence("might be")
            .getIntentDefinition();

    public IntentDefinition Help = intent("Help")
            .trainingSentence("Help")
            .trainingSentence("help")
            .trainingSentence("Assist")
            .trainingSentence("I need support")
            .trainingSentence("I need some help")
            .trainingSentence("I need some assistance")
            .getIntentDefinition();

    public IntentDefinition Greetings = intent("Greetings")
            .trainingSentence("Hello")
            .trainingSentence("Hi")
            .trainingSentence("Howdy")
            .trainingSentence("Yo")
            .trainingSentence("What's up?")
            .trainingSentence("Greetings")
            .trainingSentence("Good morning")
            .trainingSentence("Good afternoon")
            .getIntentDefinition();

    public IntentDefinition HowAreYou = intent("HowAreYou")
            .trainingSentence("How are you?")
            .trainingSentence("How do you feel?")
            .trainingSentence("How do you do?")
            .trainingSentence("How is it going?")
            .trainingSentence("How are you doing?")
            .trainingSentence("How's everything?")
            .trainingSentence("What's going on?")
            .getIntentDefinition();

    public IntentDefinition WhoAreYou = intent("WhoAreYou")
            .trainingSentence("Who are you?")
            .trainingSentence("What are you?")
            .getIntentDefinition();

    public IntentDefinition GoodBye = intent("GoodBye")
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

    public IntentDefinition Thanks = intent("Thanks")
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
    public IntentDefinition AddressValue = createDataTypeIntent("AddressValue", address());

    public IntentDefinition AgeValue = createDataTypeIntent("AgeValue", age());

    public IntentDefinition AirportValue = createDataTypeIntent("AirportValue", airport());

    public IntentDefinition AnyValue = createDataTypeIntent("AnyValue", any());

    public IntentDefinition CapitalValue = createDataTypeIntent("CapitalValue", capital());

    public IntentDefinition CardinalValue = createDataTypeIntent("CardinalValue", cardinal());

    public IntentDefinition CityValue = createDataTypeIntent("CityValue", city());

    public IntentDefinition CityGBValue = createDataTypeIntent("CityGBValue", cityGb());

    public IntentDefinition CityUSValue = createDataTypeIntent("CityUSValue", cityUs());

    public IntentDefinition ColorValue = createDataTypeIntent("ColorValue", color());

    public IntentDefinition CountryValue = createDataTypeIntent("CountryValue", country());

    public IntentDefinition CountryCodeValue = createDataTypeIntent("CountryCodeValue", countryCode());

    public IntentDefinition CountyGBValue = createDataTypeIntent("CountryGBValue", countyGb());

    public IntentDefinition CountyUSValue = createDataTypeIntent("CountyUSValue", countyUs());

    public IntentDefinition DateValue = createDataTypeIntent("DateValue", date());

    public IntentDefinition DatePeriodValue = createDataTypeIntent("DatePeriodValue", datePeriod());

    public IntentDefinition DateTimeValue = createDataTypeIntent("DateTimeValue", dateTime());

    public IntentDefinition DurationValue = createDataTypeIntent("DurationValue", duration());

    public IntentDefinition EmailValue = createDataTypeIntent("EmailValue", email());

    public IntentDefinition FlightNumberValue = createDataTypeIntent("FlightNumberValue", flightNumber());

    public IntentDefinition GivenNameValue = createDataTypeIntent("GiveNameValue", givenName());

    public IntentDefinition IntegerValue = createDataTypeIntent("IntegerValue", integer());

    public IntentDefinition LanguageValue = createDataTypeIntent("LanguageValue", language());

    public IntentDefinition LastNameValue = createDataTypeIntent("LastNameValue", lastName());

    public IntentDefinition LocationValue = createDataTypeIntent("LocationValue", location());

    public IntentDefinition MusicArtistValue = createDataTypeIntent("MusicArtistValue", musicArtist());

    public IntentDefinition MusicGenreValue = createDataTypeIntent("MusicGenreValue", musicGenre());

    public IntentDefinition NumberValue = createDataTypeIntent("NumberValue", number());

    public IntentDefinition NumberSequenceValue = createDataTypeIntent("NumberSequenceValue", numberSequence());

    public IntentDefinition OrdinalValue = createDataTypeIntent("OrdinalValue", ordinal());

    public IntentDefinition PercentageValue = createDataTypeIntent("PercentageValue", percentage());

    public IntentDefinition PhoneNumberValue = createDataTypeIntent("PhoneNumberValue", phoneNumber());

    public IntentDefinition PlaceAttractionValue = createDataTypeIntent("PlaceAttractionValue", placeAttraction());

    public IntentDefinition PlaceAttractionGBValue = createDataTypeIntent("PlaceAttractionGBValue",
            placeAttractionGb());

    public IntentDefinition PlaceAttractionUSValue = createDataTypeIntent("PlaceAttractionUSValue",
            placeAttractionUs());

    public IntentDefinition StateValue = createDataTypeIntent("StateValue", state());

    public IntentDefinition StateGBValue = createDataTypeIntent("StateGBValue", stateGb());

    public IntentDefinition StateUSValue = createDataTypeIntent("StateUSValue", stateUs());

    public IntentDefinition StreetAddressValue = createDataTypeIntent("StreetAddressValue", streetAddress());

    public IntentDefinition TemperatureValue = createDataTypeIntent("TemperatureValue", temperature());

    public IntentDefinition TimeValue = createDataTypeIntent("TimeValue", time());

    public IntentDefinition TimePeriodValue = createDataTypeIntent("TimePeriodValue", timePeriod());

    public IntentDefinition UnitAreaValue = createDataTypeIntent("UnitAreaValue", unitArea());

    public IntentDefinition UnitCurrencyValue = createDataTypeIntent("UnitCurrencyValue", unitCurrency());

    public IntentDefinition UnitInformationValue = createDataTypeIntent("UnitInformationValue", unitInformation());

    public IntentDefinition UnitLengthValue = createDataTypeIntent("UnitLengthValue", unitLength());

    public IntentDefinition UnitSpeedValue = createDataTypeIntent("UnitSpeedValue", unitSpeed());

    public IntentDefinition UnitVolumeValue = createDataTypeIntent("UnitVolumeValue", unitVolume());

    public IntentDefinition UnitWeightValue = createDataTypeIntent("UnitWeightValue", unitWeight());

    public IntentDefinition URLValue = createDataTypeIntent("URLValue", url());

    public IntentDefinition ZipCodeValue = createDataTypeIntent("ZipCodeValue", zipCode());

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
    private @NonNull IntentDefinition createDataTypeIntent(@NonNull String name, @NonNull EntityDefinitionReference type) {
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

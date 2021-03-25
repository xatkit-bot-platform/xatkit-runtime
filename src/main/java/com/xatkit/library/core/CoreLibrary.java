package com.xatkit.library.core;

import com.xatkit.intent.EntityDefinitionReference;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.impl.LibraryImpl;
import lombok.NonNull;

import java.util.Arrays;

import static com.xatkit.dsl.DSL.address;
import static com.xatkit.dsl.DSL.age;
import static com.xatkit.dsl.DSL.airport;
import static com.xatkit.dsl.DSL.any;
import static com.xatkit.dsl.DSL.capital;
import static com.xatkit.dsl.DSL.cardinal;
import static com.xatkit.dsl.DSL.city;
import static com.xatkit.dsl.DSL.cityGb;
import static com.xatkit.dsl.DSL.cityUs;
import static com.xatkit.dsl.DSL.color;
import static com.xatkit.dsl.DSL.country;
import static com.xatkit.dsl.DSL.countryCode;
import static com.xatkit.dsl.DSL.countyGb;
import static com.xatkit.dsl.DSL.countyUs;
import static com.xatkit.dsl.DSL.date;
import static com.xatkit.dsl.DSL.datePeriod;
import static com.xatkit.dsl.DSL.dateTime;
import static com.xatkit.dsl.DSL.duration;
import static com.xatkit.dsl.DSL.email;
import static com.xatkit.dsl.DSL.flightNumber;
import static com.xatkit.dsl.DSL.givenName;
import static com.xatkit.dsl.DSL.integer;
import static com.xatkit.dsl.DSL.intent;
import static com.xatkit.dsl.DSL.language;
import static com.xatkit.dsl.DSL.lastName;
import static com.xatkit.dsl.DSL.location;
import static com.xatkit.dsl.DSL.musicArtist;
import static com.xatkit.dsl.DSL.musicGenre;
import static com.xatkit.dsl.DSL.number;
import static com.xatkit.dsl.DSL.numberSequence;
import static com.xatkit.dsl.DSL.ordinal;
import static com.xatkit.dsl.DSL.percentage;
import static com.xatkit.dsl.DSL.phoneNumber;
import static com.xatkit.dsl.DSL.placeAttraction;
import static com.xatkit.dsl.DSL.placeAttractionGb;
import static com.xatkit.dsl.DSL.placeAttractionUs;
import static com.xatkit.dsl.DSL.state;
import static com.xatkit.dsl.DSL.stateGb;
import static com.xatkit.dsl.DSL.stateUs;
import static com.xatkit.dsl.DSL.streetAddress;
import static com.xatkit.dsl.DSL.temperature;
import static com.xatkit.dsl.DSL.time;
import static com.xatkit.dsl.DSL.timePeriod;
import static com.xatkit.dsl.DSL.unitArea;
import static com.xatkit.dsl.DSL.unitCurrency;
import static com.xatkit.dsl.DSL.unitInformation;
import static com.xatkit.dsl.DSL.unitLength;
import static com.xatkit.dsl.DSL.unitSpeed;
import static com.xatkit.dsl.DSL.unitVolume;
import static com.xatkit.dsl.DSL.unitWeight;
import static com.xatkit.dsl.DSL.url;
import static com.xatkit.dsl.DSL.zipCode;

@SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:ConstantName"})
public class CoreLibrary extends LibraryImpl {
    /*
     * The following checkstyle rules are disabled for this class:
     * - VisibilityModifier
     * - ConstantName
     * This library is accessed in transitions with the following pattern: intentIs(CoreLibrary.Yes), changing them
     * to private attributes would force bot code to use getters, making it harder to write/read. The name of the
     * variables is not in upper case for styling purposes.
     */

    public static final IntentDefinition Yes = intent("Yes")
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

    public static final IntentDefinition No = intent("No")
            .trainingSentence("No")
            .trainingSentence("no")
            .trainingSentence("nope")
            .trainingSentence("negative")
            .getIntentDefinition();

    public static final IntentDefinition Maybe = intent("Maybe")
            .trainingSentence("maybe")
            .trainingSentence("perchance")
            .trainingSentence("perhaps")
            .trainingSentence("could be")
            .trainingSentence("might be")
            .getIntentDefinition();

    public static final IntentDefinition Help = intent("Help")
            .trainingSentence("Help")
            .trainingSentence("help")
            .trainingSentence("Assist")
            .trainingSentence("I need support")
            .trainingSentence("I need some help")
            .trainingSentence("I need some assistance")
            .getIntentDefinition();

    public static final IntentDefinition Greetings = intent("Greetings")
            .trainingSentence("Hello")
            .trainingSentence("Hi")
            .trainingSentence("Howdy")
            .trainingSentence("Yo")
            .trainingSentence("What's up?")
            .trainingSentence("Greetings")
            .trainingSentence("Good morning")
            .trainingSentence("Good afternoon")
            .getIntentDefinition();

    public static final IntentDefinition HowAreYou = intent("HowAreYou")
            .trainingSentence("How are you?")
            .trainingSentence("How do you feel?")
            .trainingSentence("How do you do?")
            .trainingSentence("How is it going?")
            .trainingSentence("How are you doing?")
            .trainingSentence("How's everything?")
            .trainingSentence("What's going on?")
            .getIntentDefinition();

    public static final IntentDefinition WhoAreYou = intent("WhoAreYou")
            .trainingSentence("Who are you?")
            .trainingSentence("What are you?")
            .getIntentDefinition();

    public static final IntentDefinition GoodBye = intent("GoodBye")
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

    public static final IntentDefinition Thanks = intent("Thanks")
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
    public static final IntentDefinition AddressValue = createDataTypeIntent("AddressValue", address());

    public static final IntentDefinition AgeValue = createDataTypeIntent("AgeValue", age());

    public static final IntentDefinition AirportValue = createDataTypeIntent("AirportValue", airport());

    public static final IntentDefinition AnyValue = createDataTypeIntent("AnyValue", any());

    public static final IntentDefinition CapitalValue = createDataTypeIntent("CapitalValue", capital());

    public static final IntentDefinition CardinalValue = createDataTypeIntent("CardinalValue", cardinal());

    public static final IntentDefinition CityValue = createDataTypeIntent("CityValue", city());

    public static final IntentDefinition CityGBValue = createDataTypeIntent("CityGBValue", cityGb());

    public static final IntentDefinition CityUSValue = createDataTypeIntent("CityUSValue", cityUs());

    public static final IntentDefinition ColorValue = createDataTypeIntent("ColorValue", color());

    public static final IntentDefinition CountryValue = createDataTypeIntent("CountryValue", country());

    public static final IntentDefinition CountryCodeValue = createDataTypeIntent("CountryCodeValue", countryCode());

    public static final IntentDefinition CountyGBValue = createDataTypeIntent("CountryGBValue", countyGb());

    public static final IntentDefinition CountyUSValue = createDataTypeIntent("CountyUSValue", countyUs());

    public static final IntentDefinition DateValue = createDataTypeIntent("DateValue", date());

    public static final IntentDefinition DatePeriodValue = createDataTypeIntent("DatePeriodValue", datePeriod());

    public static final IntentDefinition DateTimeValue = createDataTypeIntent("DateTimeValue", dateTime());

    public static final IntentDefinition DurationValue = createDataTypeIntent("DurationValue", duration());

    public static final IntentDefinition EmailValue = createDataTypeIntent("EmailValue", email());

    public static final IntentDefinition FlightNumberValue = createDataTypeIntent("FlightNumberValue", flightNumber());

    public static final IntentDefinition GivenNameValue = createDataTypeIntent("GiveNameValue", givenName());

    public static final IntentDefinition IntegerValue = createDataTypeIntent("IntegerValue", integer());

    public static final IntentDefinition LanguageValue = createDataTypeIntent("LanguageValue", language());

    public static final IntentDefinition LastNameValue = createDataTypeIntent("LastNameValue", lastName());

    public static final IntentDefinition LocationValue = createDataTypeIntent("LocationValue", location());

    public static final IntentDefinition MusicArtistValue = createDataTypeIntent("MusicArtistValue", musicArtist());

    public static final IntentDefinition MusicGenreValue = createDataTypeIntent("MusicGenreValue", musicGenre());

    public static final IntentDefinition NumberValue = createDataTypeIntent("NumberValue", number());

    public static final IntentDefinition NumberSequenceValue = createDataTypeIntent("NumberSequenceValue",
            numberSequence());

    public static final IntentDefinition OrdinalValue = createDataTypeIntent("OrdinalValue", ordinal());

    public static final IntentDefinition PercentageValue = createDataTypeIntent("PercentageValue", percentage());

    public static final IntentDefinition PhoneNumberValue = createDataTypeIntent("PhoneNumberValue", phoneNumber());

    public static final IntentDefinition PlaceAttractionValue = createDataTypeIntent("PlaceAttractionValue",
            placeAttraction());

    public static final IntentDefinition PlaceAttractionGBValue = createDataTypeIntent("PlaceAttractionGBValue",
            placeAttractionGb());

    public static final IntentDefinition PlaceAttractionUSValue = createDataTypeIntent("PlaceAttractionUSValue",
            placeAttractionUs());

    public static final IntentDefinition StateValue = createDataTypeIntent("StateValue", state());

    public static final IntentDefinition StateGBValue = createDataTypeIntent("StateGBValue", stateGb());

    public static final IntentDefinition StateUSValue = createDataTypeIntent("StateUSValue", stateUs());

    public static final IntentDefinition StreetAddressValue = createDataTypeIntent("StreetAddressValue",
            streetAddress());

    public static final IntentDefinition TemperatureValue = createDataTypeIntent("TemperatureValue", temperature());

    public static final IntentDefinition TimeValue = createDataTypeIntent("TimeValue", time());

    public static final IntentDefinition TimePeriodValue = createDataTypeIntent("TimePeriodValue", timePeriod());

    public static final IntentDefinition UnitAreaValue = createDataTypeIntent("UnitAreaValue", unitArea());

    public static final IntentDefinition UnitCurrencyValue = createDataTypeIntent("UnitCurrencyValue", unitCurrency());

    public static final IntentDefinition UnitInformationValue = createDataTypeIntent("UnitInformationValue",
            unitInformation());

    public static final IntentDefinition UnitLengthValue = createDataTypeIntent("UnitLengthValue", unitLength());

    public static final IntentDefinition UnitSpeedValue = createDataTypeIntent("UnitSpeedValue", unitSpeed());

    public static final IntentDefinition UnitVolumeValue = createDataTypeIntent("UnitVolumeValue", unitVolume());

    public static final IntentDefinition UnitWeightValue = createDataTypeIntent("UnitWeightValue", unitWeight());

    public static final IntentDefinition URLValue = createDataTypeIntent("URLValue", url());

    public static final IntentDefinition ZipCodeValue = createDataTypeIntent("ZipCodeValue", zipCode());

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
    private static @NonNull IntentDefinition createDataTypeIntent(@NonNull String name,
                                                                  @NonNull EntityDefinitionReference type) {
        return intent(name)
                .trainingSentence("VALUE")
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

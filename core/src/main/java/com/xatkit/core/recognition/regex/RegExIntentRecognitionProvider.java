package com.xatkit.core.recognition.regex;

import com.xatkit.core.XatkitCore;
import com.xatkit.core.recognition.AbstractIntentRecognitionProvider;
import com.xatkit.core.recognition.EntityMapper;
import com.xatkit.core.recognition.IntentRecognitionProviderFactory;
import com.xatkit.core.recognition.RecognitionMonitor;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.BaseEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinition;
import com.xatkit.intent.CompositeEntityDefinitionEntry;
import com.xatkit.intent.Context;
import com.xatkit.intent.ContextInstance;
import com.xatkit.intent.ContextParameter;
import com.xatkit.intent.ContextParameterValue;
import com.xatkit.intent.CustomEntityDefinition;
import com.xatkit.intent.EntityDefinition;
import com.xatkit.intent.EntityTextFragment;
import com.xatkit.intent.EntityType;
import com.xatkit.intent.IntentDefinition;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.LiteralTextFragment;
import com.xatkit.intent.MappingEntityDefinition;
import com.xatkit.intent.MappingEntityDefinitionEntry;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.intent.TextFragment;
import fr.inria.atlanmod.commons.log.Log;
import lombok.NonNull;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A {@link AbstractIntentRecognitionProvider} relying on RegExp to match user inputs.
 * <p>
 * This intent provider is designed to handle bot prototyping with a minimal support to match user inputs using
 * RegExp {@link Pattern}s. This provider should not be used if advanced input extraction capabilities are required.
 * <p>
 * <b>Note</b>: this class uses strict patterns that perform <b>exact</b> matches of the input. This exact
 * matching is case sensitive. You can check alternative
 * {@link com.xatkit.core.recognition.IntentRecognitionProvider}s if you need to support advanced features such as
 * partial matches.
 * <p>
 * <b>Note</b>: the {@link RegExIntentRecognitionProvider} translates {@link EntityType}s into single-word
 * patterns. This means that the {@code any} entity will match "test", but not "test test", you can check
 * alternative {@link com.xatkit.core.recognition.IntentRecognitionProvider}s if you need to support such features.
 * <p>
 * The {@link RegExIntentRecognitionProvider} will be used by Xatkit if the application's {@link Configuration}
 * file does not contain specific {@link com.xatkit.core.recognition.IntentRecognitionProvider} properties (see
 * {@link IntentRecognitionProviderFactory#getIntentRecognitionProvider(XatkitCore, Configuration)}).
 *
 * @see IntentRecognitionProviderFactory
 */
public class RegExIntentRecognitionProvider extends AbstractIntentRecognitionProvider {

    /**
     * The delimiter used to separate context and parameter names in RegExp group names.
     * <p>
     * RegExp group names only accept A-z and 0-9 characters as identifiers (see https://docs.oracle
     * .com/javase/7/docs/api/java/util/regex/Pattern.html#groupname) for more information.
     */
    private static String REGEXP_GROUP_NAME_DELIMITER = "0000";

    /**
     * The {@link Pattern} matching all the reserved RegExp characters.
     * <p>
     * This {@link Pattern} is used to escape the RegExp characters that are contained in the training sentences and
     * entities.
     *
     * @see #escapeRegExpReservedCharacters(String)
     */
    private static Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    /**
     * The application's {@link Configuration}.
     * <p>
     * This {@link Configuration} is used to customize the created {@link XatkitSession}s.
     */
    private Configuration configuration;

    /**
     * A boolean storing whether the provider has been shut down.
     * <p>
     * The {@link RegExIntentRecognitionProvider} is not connected to any remote API, and calling its
     * {@link #shutdown()} method only sets this value (and the {@link #isShutdown()} return value) to {@code true},
     * allowing to properly close the application.
     */
    private boolean isShutdown;

    /**
     * The {@link EntityMapper} used to store system and dynamic entity mappings.
     *
     * @see #registerEntityDefinition(EntityDefinition)
     */
    private EntityMapper entityMapper;

    /**
     * The {@link Map} used to store RegExp {@link Pattern}s associated to the registered {@link IntentDefinition}.
     * <p>
     * {@link Pattern}s in this map are iterated when an input is received to retrieve the corresponding
     * {@link IntentDefinition}. Note that the stored {@link Pattern}s are strict and match exactly the training
     * sentence used to create them.
     */
    private Map<IntentDefinition, List<Pattern>> intentPatterns;

    @Nullable
    private RecognitionMonitor recognitionMonitor;

    /**
     * Constructs a {@link RegExIntentRecognitionProvider} with the provided {@code configuration}.
     *
     * @param configuration the {@link Configuration} used to customize the created {@link XatkitSession}s
     *                      * @param recognitionMonitor the {@link RecognitionMonitor} instance storing intent
     *                      matching information
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     */
    public RegExIntentRecognitionProvider(@NonNull Configuration configuration,
                                          @Nullable RecognitionMonitor recognitionMonitor) {
        Log.info("Starting {0}", this.getClass().getSimpleName());
        this.configuration = configuration;
        this.isShutdown = false;
        this.entityMapper = new RegExEntityMapper();
        this.intentPatterns = new HashMap<>();
        this.recognitionMonitor = recognitionMonitor;
    }

    /**
     * Registers the provided {@code entityDefinition}.
     * <p>
     * Registered {@link EntityDefinition} are reused when registering intents to produce RegExp {@link Pattern}s that
     * are matched against user inputs.
     *
     * @param entityDefinition the {@link EntityDefinition} to register to the underlying intent recognition provider
     * @throws NullPointerException if the provided {@code entityDefinition} is {@code null}
     * @see #registerIntentDefinition(IntentDefinition)
     */
    @Override
    public void registerEntityDefinition(@NonNull EntityDefinition entityDefinition) {
        if (entityDefinition instanceof BaseEntityDefinition) {
            BaseEntityDefinition baseEntityDefinition = (BaseEntityDefinition) entityDefinition;
            Log.trace("Skipping registration of {0} ({1}), {0} are natively supported",
                    BaseEntityDefinition.class.getSimpleName(), baseEntityDefinition.getEntityType().getLiteral());
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            Log.debug("Registering {0} {1}", CustomEntityDefinition.class.getSimpleName(), entityDefinition.getName());
            this.registerCustomEntityDefinition((CustomEntityDefinition) entityDefinition);
        }
    }

    /**
     * Registers the provided {@code customEntityDefinition}.
     * <p>
     * This method registers both {@link MappingEntityDefinition} and {@link CompositeEntityDefinition}. Note that
     * the registered {@link MappingEntityDefinition}s does not allow for synonym matching.
     *
     * @param entityDefinition the {@link CustomEntityDefinition} to register
     * @throws NullPointerException if the provided {@code entityDefinition} is {@code null}
     */
    private void registerCustomEntityDefinition(@NonNull CustomEntityDefinition entityDefinition) {
        if (entityDefinition instanceof MappingEntityDefinition) {
            MappingEntityDefinition mappingEntityDefinition = (MappingEntityDefinition) entityDefinition;
            List<String> entityValues = new ArrayList<>();
            for (MappingEntityDefinitionEntry entry : mappingEntityDefinition.getEntries()) {
                entityValues.add(escapeRegExpReservedCharacters(entry.getReferenceValue()));
                /*
                 * Note: this method does not take into account synonyms
                 */
            }
            String patternPart = String.join("|", entityValues);
            this.entityMapper.addEntityMapping(entityDefinition, patternPart);
        } else if (entityDefinition instanceof CompositeEntityDefinition) {
            CompositeEntityDefinition compositeEntityDefinition = (CompositeEntityDefinition) entityDefinition;
            registerReferencedEntityDefinitions(compositeEntityDefinition);
            List<String> patterns = new ArrayList<>();
            for (CompositeEntityDefinitionEntry entry : compositeEntityDefinition.getEntries()) {
                StringBuilder sb = new StringBuilder();
                sb.append("(");
                for (TextFragment fragment : entry.getFragments()) {
                    if (fragment instanceof LiteralTextFragment) {
                        /*
                         * Add spaces around pure textual fragments, they are removed by the Xtext parser.
                         */
                        sb.append(" " + escapeRegExpReservedCharacters(((LiteralTextFragment) fragment).getValue()) + " ");
                    } else if (fragment instanceof EntityTextFragment) {
                        EntityDefinition fragmentEntity =
                                ((EntityTextFragment) fragment).getEntityReference().getReferredEntity();
                        sb.append("(");
                        sb.append(entityMapper.getMappingFor(fragmentEntity));
                        sb.append(")");
                    }
                }
                sb.append(")");
                patterns.add(sb.toString());
            }
            String entityPattern = String.join("|", patterns);
            entityMapper.addEntityMapping(entityDefinition, entityPattern);
        }
    }

    /**
     * Registers the {@link EntityDefinition}s referenced by the provided {@link CompositeEntityDefinition}.
     * <p>
     * This method ensures that all the {@link CustomEntityDefinition}s used by the provided
     * {@link CompositeEntityDefinition} are registered before registering itself. Note that this method does not
     * register {@link BaseEntityDefinition} since they are natively supported by the {@link RegExEntityMapper}.
     *
     * @param entityDefinition the {@link CompositeEntityDefinition} to register the referenced entities from
     * @throws NullPointerException if the provided {@code entityDefinition} is {@code null}
     */
    private void registerReferencedEntityDefinitions(@NonNull CompositeEntityDefinition entityDefinition) {
        for (CompositeEntityDefinitionEntry entry : entityDefinition.getEntries()) {
            for (EntityDefinition referredEntityDefinition : entry.getEntities()) {
                if (referredEntityDefinition instanceof CustomEntityDefinition) {
                    /*
                     * Do not register base entity definition, they are already matched
                     */
                    registerEntityDefinition(referredEntityDefinition);
                }
            }
        }
    }

    /**
     * Registers the provided {@link IntentDefinition}.
     * <p>
     * This method creates a set of RegExp patterns that can be matched against user inputs. Note that
     * {@link CustomEntityDefinition} used in the provided {@code intentDefinition} must have been registered using
     * {@link #registerEntityDefinition(EntityDefinition)}.
     *
     * @param intentDefinition the {@link IntentDefinition} to register to the underlying intent recognition provider
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     * @see #registerEntityDefinition(EntityDefinition)
     */
    @Override
    public void registerIntentDefinition(@NonNull IntentDefinition intentDefinition) {
        /*
         * This method does not register the parent of the provided intentDefinition. This is not required: if the
         * parent is not registered the intent will not be matched anyways (see #getMatchableIntentDefinition).
         */
        List<Pattern> patterns = createPatterns(intentDefinition);
        this.intentPatterns.put(intentDefinition, patterns);
    }

    /**
     * Creates the RegExp {@link Pattern}s from the provided {@code intentDefinition}.
     * <p>
     * This method creates one {@link Pattern} for each training sentence in the provided {@code intentDefinition}.
     * Note that the produced {@link Pattern}s are strict and exactly match the training sentence used to create them.
     *
     * @param intentDefinition the {@link IntentDefinition} to create {@link Pattern}s from
     * @return the created {@link Pattern}s
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     */
    private List<Pattern> createPatterns(@NonNull IntentDefinition intentDefinition) {
        List<Pattern> patterns = new ArrayList<>();
        for (String trainingSentence : intentDefinition.getTrainingSentences()) {
            trainingSentence = escapeRegExpReservedCharacters(trainingSentence);
            if (intentDefinition.getOutContexts().isEmpty()) {
                patterns.add(Pattern.compile("^(?i)" + trainingSentence + "$"));
            } else {
                String preparedTrainingSentence = trainingSentence;
                for (Context context : intentDefinition.getOutContexts()) {
                    for (ContextParameter parameter : context.getParameters()) {
                        if (preparedTrainingSentence.contains(parameter.getTextFragment())) {
                            /*
                             * only support single word for now
                             */
                            preparedTrainingSentence = preparedTrainingSentence.replace(parameter.getTextFragment(),
                                    buildRegExpGroup(context, parameter, parameter.getEntity().getReferredEntity()));
                        }
                    }
                }
                patterns.add(Pattern.compile("^" + preparedTrainingSentence + "$"));
            }
        }
        return patterns;
    }

    /**
     * Escapes the RegExp special characters from the provided {@link String}.
     *
     * @param from the {@link String} to replace the RegExp special characters of
     * @return a new {@link String} with the RegExp special characters escaped
     * @throws NullPointerException if the provided {@link String} is {@code null}
     */
    private String escapeRegExpReservedCharacters(@NonNull String from) {
        return SPECIAL_REGEX_CHARS.matcher(from).replaceAll("\\\\$0");
    }

    /**
     * Creates a RegExp named group from the provided {@code context}, {@code parameter}, and {@code entityDefinition}.
     * <p>
     * This method procudes RegExp group names with the following pattern: {@code <ContextName>0000<Parameter>}. Note
     * that '0000' is used as a delimiter because named groups only support alphanumeric values.
     * <p>
     * This intent provider only creates named groups for context parameters, and thus requires a non-null {@code
     * entityDefinition} value.
     *
     * @param context          the {@link Context} to build a named group from
     * @param parameter        the {@link ContextParameter} to build a named group from
     * @param entityDefinition the {@link EntityDefinition} to build a named group from
     * @return the {@link String} representing the built RegExp group
     * @throws NullPointerException if the provided {@code context}, {@code parameter}, or {@code entityDefinition}
     *                              is {@code null}
     */
    private String buildRegExpGroup(@NonNull Context context, @NonNull ContextParameter parameter,
                                    @NonNull EntityDefinition entityDefinition) {
        StringBuilder sb = new StringBuilder();
        sb.append("(?<");
        sb.append(context.getName());
        sb.append(REGEXP_GROUP_NAME_DELIMITER);
        sb.append(parameter.getName());
        sb.append(">");
        sb.append(entityMapper.getMappingFor(entityDefinition));
        sb.append(")");
        return sb.toString();
    }

    /**
     * Deletes the provided {@code entityDefinition}.
     *
     * @param entityDefinition the {@link EntityDefinition} to delete from the underlying intent recognition provider
     * @throws NullPointerException if the provided {@code entityDefinition} is {@code null}
     */
    @Override
    public void deleteEntityDefinition(@NonNull EntityDefinition entityDefinition) {
        /*
         * Quick fix: should be done properly.
         */
        this.entityMapper.removeMappingFor(entityDefinition);
    }

    /**
     * Deletes the provided {@code intentDefinition}.
     * <p>
     * This method deletes the RegExp {@link Pattern}s associated to the provided {@code intentDefinition}, meaning
     * that the intent won't be matched by the provider anymore.
     *
     * @param intentDefinition the {@link IntentDefinition} to delete from the underlying intent recognition provider
     * @throws NullPointerException if the provided {@code intentDefinition} is {@code null}
     */
    @Override
    public void deleteIntentDefinition(@NonNull IntentDefinition intentDefinition) {
        this.intentPatterns.remove(intentDefinition);
    }

    /**
     * This provider does not rely on any ML engine, calling this method does not do anything.
     * <p>
     * Use valid {@link com.xatkit.core.recognition.IntentRecognitionProvider}s to enable ML training.
     */
    @Override
    public void trainMLEngine() {
        /*
         * Do nothing, there is no ML engine in this provider.
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XatkitSession createContext(@NonNull String sessionId) {
        return new XatkitSession(sessionId, configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public RecognitionMonitor getRecognitionMonitor() {
        return recognitionMonitor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        this.intentPatterns = null;
        this.isShutdown = true;
        if (nonNull(this.recognitionMonitor)) {
            this.recognitionMonitor.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    /**
     * Computes the {@link RecognizedIntent} associated to the provided {@code input}.
     * <p>
     * This method relies on the RegExp patterns created when calling
     * {@link #registerIntentDefinition(IntentDefinition)} to match the provided input. The provided {@code context}
     * is used to retrieve the intents that can be matched according to the current contexts.
     * <p>
     * If the {@link RegExIntentRecognitionProvider} cannot find a valid {@link IntentDefinition} for the provided
     * {@code input} the returned {@link RecognizedIntent}'s definition will be the {@link #DEFAULT_FALLBACK_INTENT}.
     * <p>
     * <b>Note</b>: this class uses strict patterns that perform <b>exact</b> matches of the input. This exact
     * matching is case sensitive. You can check alternative
     * {@link com.xatkit.core.recognition.IntentRecognitionProvider}s if you need to support advanced features such
     * as partial matches.
     * <p>
     * <b>Note</b>: the {@link RegExIntentRecognitionProvider} translates {@link EntityType}s into single-word
     * patterns. This means that the {@code any} entity will match "test", but not "test test", you can check
     * alternative {@link com.xatkit.core.recognition.IntentRecognitionProvider}s if you need to support such features.
     *
     * @param input   the {@link String} representing the textual input to process and extract the intent from
     * @param context the {@link XatkitSession} used to access context information
     * @return the {@link RecognizedIntent} matched from the provided {@code input}
     * @throws NullPointerException if the provided {@code input} or {@code context} is {@code null}
     */
    @Override
    protected RecognizedIntent getIntentInternal(@NonNull String input, @NonNull StateContext context) {
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        /*
         * The recognitionConfidence is always 1 with the RegExIntentRecognitionProvider since it always returns
         * exact matches or default fallback intent.
         */
        recognizedIntent.setRecognitionConfidence(1);
        recognizedIntent.setMatchedInput(input);
        List<IntentDefinition> matchableIntents = getMatchableIntents(intentPatterns.keySet(), context);
        for (IntentDefinition intentDefinition : matchableIntents) {
            List<Pattern> patterns = intentPatterns.get(intentDefinition);
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(input);
                if (matcher.matches()) {
                    recognizedIntent.setDefinition(intentDefinition);
                    if (matcher.groupCount() > 0) {
                        setContextParameterValuesFromMatcher(matcher, intentDefinition, recognizedIntent);
                    }
                    /*
                     * Sets additional values that are not part of the matched expressions. These values can be
                     * follow-up contexts, or empty contexts.
                     */
                    setEmptyContexts(intentDefinition, recognizedIntent);
                    /*
                     * Return the first one we find, no need to iterate the rest of the map
                     */
                    if (nonNull(this.recognitionMonitor)) {
                        this.recognitionMonitor.logRecognizedIntent(context, recognizedIntent);
                    }
                    return recognizedIntent;
                }
            }
        }
        /*
         * Can't find an intent matching the provided input, return the default fallback intent
         */
        recognizedIntent.setDefinition(DEFAULT_FALLBACK_INTENT);
        if (nonNull(recognitionMonitor)) {
            this.recognitionMonitor.logRecognizedIntent(context, recognizedIntent);
        }
        return recognizedIntent;
    }

    /**
     * Retrieves the {@link IntentDefinition}s that can be matched according to the provided {@code context}.
     * <p>
     * An intent can be matched iff:
     * <ul>
     * <li>All its {@code inContexts} are defined in the context</li>
     * <li>the {@code follow-up} context of the followed intent (if there is such intent) is defined in the
     * context</li>
     * </ul>
     *
     * @param intentDefinitions the {@link Set} of {@link IntentDefinition} to retrieve the matchable intents from
     * @param context           the {@link XatkitSession} storing contextual values
     * @return the {@link List} of {@link IntentDefinition} that can be matched according to the provided {@code
     * context}
     * @throws NullPointerException if the provided {@code intentDefinitions} or {@code context} is {@code null}
     */
    private List<IntentDefinition> getMatchableIntents(@NonNull Set<IntentDefinition> intentDefinitions,
                                                       @NonNull StateContext context) {
        List<IntentDefinition> result = new ArrayList<>();
        for (IntentDefinition intentDefinition : intentDefinitions) {
            if (context.getNlpContext().containsKey("Enable" + intentDefinition.getName())) {
                result.add(intentDefinition);
            }
        }
        return result;
    }

    /**
     * Sets the {@link ContextParameterValue}s of the provided {@code recognizedIntent} from the given {@code matcher}.
     * <p>
     * This method iterates the matched named groups and creates, for each one, the corresponding
     * {@link ContextParameterValue}.
     *
     * @param matcher          the matcher to retrieve the {@link ContextParameter}s from
     * @param intentDefinition the {@link IntentDefinition} containing the {@link ContextParameter} to retrieve
     * @param recognizedIntent the {@link RecognizedIntent} to set the created {@link ContextParameterValue} of
     * @throws NullPointerException if the provided {@code matcher}, {@code intentDefinition}, or {@code
     *                              recognizedIntent} is {@code null}
     * @see #getOrCreateContextInstance(RecognizedIntent, Context)
     * @see #createContextParameterValue(ContextParameter, String)
     */
    private void setContextParameterValuesFromMatcher(@NonNull Matcher matcher,
                                                      @NonNull IntentDefinition intentDefinition,
                                                      @NonNull RecognizedIntent recognizedIntent) {
        for (Context context : intentDefinition.getOutContexts()) {
            for (ContextParameter contextParameter : context.getParameters()) {
                String groupName =
                        context.getName() + REGEXP_GROUP_NAME_DELIMITER + contextParameter.getName();
                String matchedValue;
                try {
                    matchedValue = matcher.group(groupName);
                } catch (IllegalArgumentException e) {
                    /*
                     * The group with the name Context:Parameter does not exist (this can be the case if the intent
                     * contains multiple inputs setting different parameters).
                     */
                    Log.warn("Cannot set the value of the context parameter {0}.{1}, the parameter hasn't been " +
                            "matched from the provided input \"\"", context.getName(), contextParameter.getName());
                    continue;
                }
                ContextParameterValue contextParameterValue = createContextParameterValue(contextParameter,
                        matchedValue);
                ContextInstance contextInstance = getOrCreateContextInstance(recognizedIntent, context);
                contextInstance.getValues().add(contextParameterValue);
            }
        }
    }

    /**
     * Creates and sets the {@link ContextInstance}s corresponding to empty {@link Context}s from the provided {@code
     * intentDefinition}.
     *
     * @param intentDefinition the {@link IntentDefinition} containing the {@link Context} definitions
     * @param recognizedIntent the {@link RecognizedIntent} to set the {@link ContextInstance}s of
     * @throws NullPointerException if the provided {@code intentDefinition} or {@code recognizedIntent} is {@code null}
     */
    private void setEmptyContexts(@NonNull IntentDefinition intentDefinition,
                                  @NonNull RecognizedIntent recognizedIntent) {
        intentDefinition.getOutContexts().stream()
                .filter(context -> context.getParameters().isEmpty())
                .forEach(context -> getOrCreateContextInstance(recognizedIntent, context));
    }

    /**
     * Creates the {@link ContextParameterValue} associated to the provided {@code contextParameter}.
     * <p>
     * This method creates a new instance of {@link ContextParameterValue}, sets its {@link ContextParameter} with
     * the provided {@code contextParameter}, and sets its {@code value}.
     *
     * @param contextParameter the {@link ContextParameter} to create a value of
     * @param value            the value to set to the created {@link ContextParameterValue}
     * @return the created {@link ContextParameterValue}
     * @throws NullPointerException if the provided {@code contextParameter} or {@code value} is {@code null}
     */
    private ContextParameterValue createContextParameterValue(@NonNull ContextParameter contextParameter,
                                                              @NonNull String value) {
        ContextParameterValue contextParameterValue = IntentFactory.eINSTANCE.createContextParameterValue();
        contextParameterValue.setContextParameter(contextParameter);
        contextParameterValue.setValue(value);
        return contextParameterValue;
    }

    /**
     * Retrieves or creates the {@link ContextInstance} associated to the provided {@code context} in the given
     * {@code recognizedIntent}.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to retrieve the {@link ContextInstance} from
     * @param context          the {@link Context} to retrieve an instance of
     * @return the {@link ContextInstance}
     * @throws NullPointerException if the provided {@code recognizedIntent} or {@code context} is {@code null}
     */
    private ContextInstance getOrCreateContextInstance(@NonNull RecognizedIntent recognizedIntent,
                                                       @NonNull Context context) {
        ContextInstance contextInstance = recognizedIntent.getOutContextInstance(context.getName());
        if (isNull(contextInstance)) {
            contextInstance = IntentFactory.eINSTANCE.createContextInstance();
            recognizedIntent.getOutContextInstances().add(contextInstance);
            contextInstance.setDefinition(context);
            contextInstance.setLifespanCount(context.getLifeSpan());
        }
        return contextInstance;
    }
}

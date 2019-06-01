package edu.uoc.som.jarvis.core.recognition;

import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.core.session.RuntimeContexts;
import edu.uoc.som.jarvis.intent.*;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.Configuration;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A default {@link IntentRecognitionProvider} that relies on RegExp to match user inputs.
 * <p>
 * This intent provider is designed to handle bot prototyping with a minimal support to match user inputs using
 * RegExp {@link Pattern}s. This provider should not be used if advanced input extraction capabilities are required.
 * <p>
 * <b>Note</b>: this class uses strict patterns that perform <b>exact</b> matches of the input. This exact
 * matching is case sensitive. You can check alternative {@link IntentRecognitionProvider}s if you need to
 * support advanced features such as partial matches.
 * <p>
 * <b>Note</b>: the {@link DefaultIntentRecognitionProvider} translates {@link EntityType}s into single-word
 * patterns. This means that the {@code any} entity will match "test", but not "test test", you can check
 * alternative {@link IntentRecognitionProvider}s if you need to support such features.
 * <p>
 * The {@link DefaultIntentRecognitionProvider} will be used by Jarvis if the application's {@link Configuration}
 * file does not contain specific {@link IntentRecognitionProvider} properties (see
 * {@link IntentRecognitionProviderFactory#getIntentRecognitionProvider(JarvisCore, Configuration)}).
 *
 * @see IntentRecognitionProviderFactory
 */
public class DefaultIntentRecognitionProvider implements IntentRecognitionProvider {

    /**
     * The Default Fallback Intent that is returned when the user input does not match any registered Intent.
     */
    /*
     * TODO this attribute is copied from DialogFlowApi, and should be refactored in a common class accessible by all
     * providers.
     */
    public static IntentDefinition DEFAULT_FALLBACK_INTENT = IntentFactory.eINSTANCE.createIntentDefinition();

    /*
     * Initializes the {@link #DEFAULT_FALLBACK_INTENT}'s name.
     * <p>
     * TODO this block is copied from DialogFlowApi, and should be refactored in a common class accessible by all
     * providers.
     */
    static {
        DEFAULT_FALLBACK_INTENT.setName("Default_Fallback_Intent");
    }

    /**
     * The context name suffix used to identify follow-up contexts.
     *
     * @see #setFollowUpContexts(IntentDefinition, RecognizedIntent)
     */
    protected static String FOLLOW_CONTEXT_NAME_SUFFIX = "_follow";

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
     * This {@link Configuration} is used to customize the created {@link JarvisSession}s.
     */
    private Configuration configuration;

    /**
     * A boolean storing whether the provider has been shut down.
     * <p>
     * The {@link DefaultIntentRecognitionProvider} is not connected to any remote API, and calling its
     * {@link #shutdown()} method only sets this value (and the {@link #isShutdown()} return value) to {@code true},
     * allowing to properly close the application.
     */
    private boolean isShutdown;

    /**
     * The {@link EntityMapper} used to store system and dynamic entity mappings.
     *
     * @see #registerEntityDefinition(EntityDefinition)
     */
    protected EntityMapper entityMapper;

    /**
     * The {@link Map} used to store RegExp {@link Pattern}s associated to the registered {@link IntentDefinition}.
     * <p>
     * {@link Pattern}s in this map are iterated when an input is received to retrieve the corresponding
     * {@link IntentDefinition}. Note that the stored {@link Pattern}s are strict and match exactly the training
     * sentence used to create them.
     */
    protected Map<IntentDefinition, List<Pattern>> intentPatterns;

    @Nullable
    private RecognitionMonitor recognitionMonitor;

    /**
     * Constructs a {@link DefaultIntentRecognitionProvider} with the provided {@link Configuration}.
     * <p>
     * This constructor is a placeholder for
     * {@link #DefaultIntentRecognitionProvider(Configuration, RecognitionMonitor)} with a {@code null}
     * {@link RecognitionMonitor}.
     *
     * @param configuration the {@link Configuration} the {@link Configuration} used to customize the created
     *                      {@link JarvisSession}s
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     * @see #DefaultIntentRecognitionProvider(Configuration, RecognitionMonitor)
     */
    public DefaultIntentRecognitionProvider(Configuration configuration) {
        this(configuration, null);
    }

    /**
     * Constructs a {@link DefaultIntentRecognitionProvider} with the provided {@code configuration}.
     *
     * @param configuration the {@link Configuration} used to customize the created {@link JarvisSession}s
     *                      * @param recognitionMonitor the {@link RecognitionMonitor} instance storing intent
     *                      matching information
     * @throws NullPointerException if the provided {@code configuration} is {@code null}
     */
    public DefaultIntentRecognitionProvider(Configuration configuration,
                                            @Nullable RecognitionMonitor recognitionMonitor) {
        checkNotNull(configuration, "Cannot create a %s with the provided %s %s", this.getClass().getSimpleName(),
                Configuration.class.getSimpleName(), configuration);
        Log.info("Starting {0}", this.getClass().getSimpleName());
        this.configuration = configuration;
        this.isShutdown = false;
        this.entityMapper = new DefaultEntityMapper();
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
     * @see #registerIntentDefinition(IntentDefinition)
     */
    @Override
    public void registerEntityDefinition(EntityDefinition entityDefinition) {
        if (entityDefinition instanceof BaseEntityDefinition) {
            BaseEntityDefinition baseEntityDefinition = (BaseEntityDefinition) entityDefinition;
            Log.trace("Skipping registration of {0} ({1}), {0} are natively supported",
                    BaseEntityDefinition.class.getSimpleName(), baseEntityDefinition.getEntityType().getLiteral());
        } else if (entityDefinition instanceof CustomEntityDefinition) {
            Log.info("Registering {0} {1}", CustomEntityDefinition.class.getSimpleName(), entityDefinition.getName());
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
     */
    private void registerCustomEntityDefinition(CustomEntityDefinition entityDefinition) {
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
                        sb.append(escapeRegExpReservedCharacters(((LiteralTextFragment) fragment).getValue()));
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
     * register {@link BaseEntityDefinition} since they are natively supported by the {@link DefaultEntityMapper}.
     *
     * @param entityDefinition the {@link CompositeEntityDefinition} to register the referenced entities from
     */
    private void registerReferencedEntityDefinitions(CompositeEntityDefinition entityDefinition) {
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
     * @see #registerEntityDefinition(EntityDefinition)
     */
    @Override
    public void registerIntentDefinition(IntentDefinition intentDefinition) {
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
     */
    private List<Pattern> createPatterns(IntentDefinition intentDefinition) {
        List<Pattern> patterns = new ArrayList<>();
        for (String trainingSentence : intentDefinition.getTrainingSentences()) {
            trainingSentence = escapeRegExpReservedCharacters(trainingSentence);
            if (intentDefinition.getOutContexts().isEmpty()) {
                patterns.add(Pattern.compile("^" + trainingSentence + "$"));
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
     */
    private String escapeRegExpReservedCharacters(String from) {
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
     */
    private String buildRegExpGroup(Context context, ContextParameter parameter, EntityDefinition entityDefinition) {
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
     */
    @Override
    public void deleteEntityDefinition(EntityDefinition entityDefinition) {
        /*
         * Quick fix: should be done properly.
         */
        this.entityMapper.entities.remove(entityDefinition.getName());
    }

    /**
     * Deletes the provided {@code intentDefinition}.
     * <p>
     * This method deletes the RegExp {@link Pattern}s associated to the provided {@code intentDefinition}, meaning
     * that the intent won't be matched by the provider anymore.
     *
     * @param intentDefinition the {@link IntentDefinition} to delete from the underlying intent recognition provider
     */
    @Override
    public void deleteIntentDefinition(IntentDefinition intentDefinition) {
        this.intentPatterns.remove(intentDefinition);
    }

    /**
     * This method is not implemented and throws an {@link UnsupportedOperationException}.
     * <p>
     * Use valid {@link IntentRecognitionProvider}s to enable ML training.
     *
     * @throws UnsupportedOperationException when called
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
    public JarvisSession createSession(String sessionId) {
        return new JarvisSession(sessionId, configuration);
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
        if(nonNull(this.recognitionMonitor)) {
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
     * {@link #registerIntentDefinition(IntentDefinition)} to match the provided input. The provided {@code session}
     * is used to retrieve the intents that can be matched according to the current contexts.
     * <p>
     * If the {@link DefaultIntentRecognitionProvider} cannot find a valid {@link IntentDefinition} for the provided
     * {@code input} the returned {@link RecognizedIntent}'s definition will be the {@link #DEFAULT_FALLBACK_INTENT}.
     * <p>
     * <b>Note</b>: this class uses strict patterns that perform <b>exact</b> matches of the input. This exact
     * matching is case sensitive. You can check alternative {@link IntentRecognitionProvider}s if you need to
     * support advanced features such as partial matches.
     * <p>
     * <b>Note</b>: the {@link DefaultIntentRecognitionProvider} translates {@link EntityType}s into single-word
     * patterns. This means that the {@code any} entity will match "test", but not "test test", you can check
     * alternative {@link IntentRecognitionProvider}s if you need to support such features.
     *
     * @param input   the {@link String} representing the textual input to process and extract the intent from
     * @param session the {@link JarvisSession} used to access context information
     * @return the {@link RecognizedIntent} matched from the provided {@code input}
     */
    @Override
    public RecognizedIntent getIntent(String input, JarvisSession session) {
        RecognizedIntent recognizedIntent = IntentFactory.eINSTANCE.createRecognizedIntent();
        List<IntentDefinition> matchableIntents = getMatchableIntents(intentPatterns.keySet(), session);
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
                    setFollowUpContexts(intentDefinition, recognizedIntent);
                    setEmptyContexts(intentDefinition, recognizedIntent);
                    /*
                     * Return the first one we find, no need to iterate the rest of the map
                     */
                    if (nonNull(this.recognitionMonitor)) {
                        this.recognitionMonitor.registerMatchedInput(input, intentDefinition);
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
            this.recognitionMonitor.registerUnmatchedInput(input);
        }
        return recognizedIntent;
    }

    /**
     * Retrieves the {@link IntentDefinition}s that can be matched according to the provided {@code session}.
     * <p>
     * An intent can be matched iff:
     * <ul>
     * <li>All its {@code inContexts} are defined in the session</li>
     * <li>the {@code follow-up} context of the followed intent (if there is such intent) is defined in the
     * session</li>
     * </ul>
     *
     * @param intentDefinitions the {@link Set} of {@link IntentDefinition} to retrieve the matchable intents from
     * @param session           the {@link JarvisSession} storing contextual values
     * @return the {@link List} of {@link IntentDefinition} that can be matched according to the provided {@code
     * session}
     * @see #setFollowUpContexts(IntentDefinition, RecognizedIntent)
     */
    private List<IntentDefinition> getMatchableIntents(Set<IntentDefinition> intentDefinitions, JarvisSession session) {
        RuntimeContexts runtimeContexts = session.getRuntimeContexts();
        List<IntentDefinition> result = new ArrayList<>();
        for (IntentDefinition intentDefinition : intentDefinitions) {
            if (nonNull(intentDefinition.getFollows())) {
                // cannot use getVariables because it always return an empty map
                if (isNull(runtimeContexts.getContextVariables(intentDefinition.getFollows().getName() + FOLLOW_CONTEXT_NAME_SUFFIX))) {
                    continue;
                }
            }
            boolean error = false;
            for (Context inContext : intentDefinition.getInContexts()) {
                if (isNull(runtimeContexts.getContextVariables(inContext.getName()))) {
                    error = true;
                    break;
                }
            }
            if (error) {
                continue;
            }
            result.add(intentDefinition);
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
     * @throws NullPointerException if the provided {@code matcher}, {@code intentDefintiion}, or {@code
     *                              recognizedIntent} is {@code null}
     * @see #getOrCreateContextInstance(RecognizedIntent, Context)
     * @see #createContextParameterValue(ContextParameter, String)
     */
    private void setContextParameterValuesFromMatcher(Matcher matcher, IntentDefinition intentDefinition,
                                                      RecognizedIntent recognizedIntent) {
        checkNotNull(matcher, "Cannot retrieve the %s from the provided %s %s",
                ContextParameterValue.class.getSimpleName(), Matcher.class.getSimpleName(), matcher);
        checkNotNull(intentDefinition, "Cannot retrieve the %s from the provided %s %s",
                ContextParameterValue.class.getSimpleName(), IntentDefinition.class.getSimpleName(), intentDefinition);
        checkNotNull(recognizedIntent, "Cannot set the %s of the provided %s %s",
                ContextParameterValue.class.getSimpleName(), RecognizedIntent.class.getSimpleName(), recognizedIntent);
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
     * Sets the {@link ContextInstance} representing implicit follow-up contexts in the provided {@code
     * reccognizedIntent}.
     * <p>
     * This method sets the {@link ContextInstance}'s lifespan count to 3, allowing to recover from an unmatched
     * intent before discarding it.
     *
     * @param intentDefinition the {@link IntentDefinition} defining the follow-up relationship
     * @param recognizedIntent the {@link RecognizedIntent} to set the {@link ContextInstance} of
     */
    private void setFollowUpContexts(IntentDefinition intentDefinition, RecognizedIntent recognizedIntent) {
        if (!intentDefinition.getFollowedBy().isEmpty()) {
            Context followContext = IntentFactory.eINSTANCE.createContext();
            followContext.setName(intentDefinition.getName() + FOLLOW_CONTEXT_NAME_SUFFIX);
            followContext.setLifeSpan(2);
            getOrCreateContextInstance(recognizedIntent, followContext);
        }
    }

    /**
     * Creates and sets the {@link ContextInstance}s corresponding to empty {@link Context}s from the provided {@code
     * intentDefinition}.
     *
     * @param intentDefinition the {@link IntentDefinition} containing the {@link Context} definitions
     * @param recognizedIntent the {@link RecognizedIntent} to set the {@link ContextInstance}s of
     */
    private void setEmptyContexts(IntentDefinition intentDefinition, RecognizedIntent recognizedIntent) {
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
    private ContextParameterValue createContextParameterValue(ContextParameter contextParameter, String value) {
        checkNotNull(contextParameter, "Cannot create a %s from the provided %s %s",
                ContextParameterValue.class.getSimpleName(), ContextParameter.class.getSimpleName(), contextParameter);
        checkNotNull(value, "Cannot create a %s from the provided value %s",
                ContextParameterValue.class.getSimpleName(), value);
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
    private ContextInstance getOrCreateContextInstance(RecognizedIntent recognizedIntent, Context context) {
        checkNotNull(recognizedIntent, "Cannot retrieve context instance from the provided %s %s",
                RecognizedIntent.class.getSimpleName(), recognizedIntent);
        checkNotNull("Cannot retrieve the context instance from the provided %s %s", Context.class.getSimpleName(),
                context);
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

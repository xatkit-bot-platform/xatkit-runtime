package fr.zelus.jarvis.io;

import fr.zelus.jarvis.core.RuntimePlatform;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.RecognizedIntent;
import fr.zelus.jarvis.recognition.IntentRecognitionProvider;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

/**
 * A specialised {@link EventProvider} that extracts {@link RecognizedIntent} from textual user inputs.
 * <p>
 * This class wraps a {@link IntentRecognitionProvider} instance that is used to extract {@link RecognizedIntent}s
 * from textual user inputs. Note that the {@link IntentRecognitionProvider} instance is not directly accessible by
 * subclasses to avoid uncontrolled accesses such as intent creation, removal, and context manipulation. Subclasses
 * should use {@link #getRecognizedIntent(String, JarvisSession)} to retrieve {@link RecognizedIntent}s from textual
 * user inputs.
 *
 * @param <T> the concrete {@link RuntimePlatform} subclass type containing the provider
 */
public abstract class IntentProvider<T extends RuntimePlatform> extends EventProvider<T> {

    /**
     * The {@link IntentRecognitionProvider} used to parse user input and retrieve {@link RecognizedIntent}s.
     * <p>
     * <b>Note:</b> this attribute is {@code private} to avoid uncontrolled accesses to the
     * {@link IntentRecognitionProvider} from {@link IntentProvider}s (such as intent creation, removal, and context
     * manipulation).
     */
    private IntentRecognitionProvider intentRecognitionProvider;

    /**
     * Constructs a new {@link IntentProvider} from the provided {@code jarvisCore}.
     * <p>
     * This constructor sets the internal {@link IntentRecognitionProvider} instance that is used to parse user input
     * and retrieve {@link RecognizedIntent}s.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link IntentProvider}s that do not require additional
     * parameters to be initialized. In that case see {@link #IntentProvider(RuntimePlatform, Configuration)}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link IntentProvider}
     * @throws NullPointerException if the provided {@code runtimePlatform} is {@code null}
     */
    public IntentProvider(T runtimePlatform) {
        this(runtimePlatform, new BaseConfiguration());
    }

    /**
     * Constructs a new {@link IntentProvider} with the provided {@code runtimePlatform} and {@code configuration}.
     * <p>
     * This constructor sets the internal {@link IntentRecognitionProvider} instance that is used to parse user input
     * and retrieve {@link RecognizedIntent}s.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link fr.zelus.jarvis.core.JarvisCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param runtimePlatform the {@link RuntimePlatform} containing this {@link IntentProvider}
     * @param configuration    the {@link Configuration} used to initialize the {@link IntentProvider}
     * @throws NullPointerException if the provided {@code runtimePlatform} is {@code null}
     */
    public IntentProvider(T runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
        this.intentRecognitionProvider = jarvisCore.getIntentRecognitionProvider();
    }

    /**
     * Returns the {@link RecognizedIntent} from the provided user {@code input} and {@code session}.
     * <p>
     * This method wraps the access to the underlying {@link IntentRecognitionProvider}, and avoid uncontrolled
     * accesses to the {@link IntentRecognitionProvider} from {@link IntentProvider}s (such as intent creation,
     * removal, and context manipulation).
     * <p>
     * <b>Note:</b> this method decrements the lifespan counts of the variables in the current context (context
     * lifespan are used to represent the number of user interaction to handled before deleting the variable).
     * <b>Client classes must call this method before setting any context variable</b> otherwise there lifespan
     * counts may be inconsistent from their expected values (e.g. context variables with a lifespan count of {@code
     * 1} will be immediately removed by the {@link JarvisContext#decrementLifespanCounts()} call).
     *
     * @param input   the textual user input to extract the {@link RecognizedIntent} from
     * @param session the {@link JarvisSession} wrapping the underlying {@link IntentRecognitionProvider}'s session
     * @return the {@link RecognizedIntent} computed by the {@link IntentRecognitionProvider}
     * @throws NullPointerException                                           if the provided {@code text} or {@code
     *                                                                        session} is {@code null}
     * @throws IllegalArgumentException                                       if the provided {@code text} is empty
     * @throws fr.zelus.jarvis.recognition.IntentRecognitionProviderException if the
     *                                                                        {@link IntentRecognitionProvider} is
     *                                                                        shutdown or if an exception is thrown
     *                                                                        by the underlying intent recognition
     *                                                                        engine
     */
    public final RecognizedIntent getRecognizedIntent(String input, JarvisSession session) {
        /*
         * We are trying to recognize an intent from a new input, decrement the current context variables lifespan
         * counts.
         */
        session.getJarvisContext().decrementLifespanCounts();
        return intentRecognitionProvider.getIntent(input, session);
    }
}

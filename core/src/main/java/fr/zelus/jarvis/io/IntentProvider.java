package fr.zelus.jarvis.io;

import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.dialogflow.DialogFlowApi;
import fr.zelus.jarvis.intent.RecognizedIntent;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

/**
 * A specialised {@link EventProvider} that extracts {@link RecognizedIntent} from textual user inputs.
 * <p>
 * This class wraps a {@link DialogFlowApi} instance that is used to extract {@link RecognizedIntent}s from textual
 * user inputs. Note that the {@link DialogFlowApi} instance is not directly accessible by subclasses to avoid
 * uncontrolled accesses such as intent creation, removal, and context manipulation. Subclasses should use
 * {@link #getRecognizedIntent(String, JarvisSession)} to retrieve {@link RecognizedIntent}s from textual user inputs.
 *
 * @param <T> the concrete {@link JarvisModule} subclass type containing the provider
 */
public abstract class IntentProvider<T extends JarvisModule> extends EventProvider<T> {

    /**
     * The {@link DialogFlowApi} used to parse user input and retrieve {@link RecognizedIntent}s.
     * <p>
     * <b>Note:</b> this attribute is {@code private} to avoid uncontrolled accesses to the {@link DialogFlowApi} from
     * {@link IntentProvider}s (such as intent creation, removal, and context manipulation).
     */
    private DialogFlowApi dialogFlowApi;

    /**
     * Constructs a new {@link IntentProvider} from the provided {@code jarvisCore}.
     * <p>
     * This constructor sets the internal {@link DialogFlowApi} instance that is used to parse user input and
     * retrieve {@link RecognizedIntent}s.
     * <p>
     * <b>Note</b>: this constructor should be used by {@link IntentProvider}s that do not require additional
     * parameters to be initialized. In that case see {@link #IntentProvider(JarvisModule, Configuration)}.
     *
     * @param containingModule the {@link JarvisModule} containing this {@link IntentProvider}
     * @throws NullPointerException if the provided {@code containingModule} is {@code null}
     */
    public IntentProvider(T containingModule) {
        this(containingModule, new BaseConfiguration());
    }

    /**
     * Constructs a new {@link IntentProvider} with the provided {@code containingModule} and {@code configuration}.
     * <p>
     * This constructor sets the internal {@link DialogFlowApi} instance that is used to parse user input and
     * retrieve {@link RecognizedIntent}s.
     * <p>
     * <b>Note</b>: this constructor will be called by jarvis internal engine when initializing the
     * {@link fr.zelus.jarvis.core.JarvisCore} component. Subclasses implementing this constructor typically
     * need additional parameters to be initialized, that can be provided in the {@code configuration}.
     *
     * @param containingModule the {@link JarvisModule} containing this {@link IntentProvider}
     * @param configuration    the {@link Configuration} used to initialize the {@link IntentProvider}
     */
    public IntentProvider(T containingModule, Configuration configuration) {
        super(containingModule, configuration);
        this.dialogFlowApi = jarvisCore.getDialogFlowApi();
    }

    /**
     * Returns the {@link RecognizedIntent} from the provided user {@code input} and {@code session}.
     * <p>
     * This method wraps the access to the underlying {@link DialogFlowApi}, and avoid uncontrolled accesses to the
     * {@link DialogFlowApi} from {@link IntentProvider}s (such as intent creation, removal, and context manipulation).
     *
     * @param input   the textual user input to extract the {@link RecognizedIntent} from
     * @param session the {@link JarvisSession} wrapping the underlying DialogFlow session
     * @return
     * @throws NullPointerException                           if the provided {@code text} or {@code session} is
     *                                                        {@code null}
     * @throws IllegalArgumentException                       if the provided {@code text} is empty
     * @throws fr.zelus.jarvis.dialogflow.DialogFlowException if the {@link DialogFlowApi} is shutdown or if an
     *                                                        exception is thrown by the underlying DialogFlow engine
     */
    public final RecognizedIntent getRecognizedIntent(String input, JarvisSession session) {
        return dialogFlowApi.getIntent(input, session);
    }
}

package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;
import edu.stanford.nlp.pipeline.Annotation;
import fr.inria.atlanmod.commons.log.Log;

import static java.util.Objects.isNull;

/**
 * A {@link IntentPostProcessor} using Stanford NLP library to extract information from the user input.
 * <p>
 * This class should be extended by any {@link IntentPostProcessor} relying on the {@link StanfordNLPService}. It
 * provides a default implementation of the {@link #init()} methods that warms-up the NLP pipeline with a fake input,
 * and provides the {@link #getAnnotation(String, StateContext)} helpers that allows to retrieve an
 * {@link Annotation} from a given input by looking in the {@link StateContext}.
 *
 * @see StanfordNLPService
 */
public abstract class StanfordNLPPostProcessor implements IntentPostProcessor {

    /**
     * The context key used to store Stanford NLP information.
     */
    protected static final String NLP_CONTEXT_KEY = "nlp.stanford";

    /**
     * The session key used to store the last annotated input.
     * <p>
     * This value is stored in the {@link StateContext#getSession()} to let multiple post-processors access it to
     * compute specific metrics.
     */
    protected static final String NLP_INPUT_SESSION_KEY = "xatkit.nlp.stanford.input";

    /**
     * The session key used to store the last annotation.
     * <p>
     * This value is stored in the {@link StateContext#getSession()} to let multiple post-processors access it to
     * compute specific metrics.
     */
    protected static final String NLP_ANNOTATION_SESSION_KEY = "xatkit.nlp.stanford.annotation";

    /**
     * Initialize the underlying {@link StanfordNLPService} and performs a warm-up annotation on it.
     * <p>
     * This method is used to avoid delays on the first query performed on a deployed chatbot.
     *
     * @see StanfordNLPService#init()
     */
    @Override
    public void init() {
        StanfordNLPService.getInstance().init();
        /*
         * Get an annotation to warm up the NLP engine.
         */
        Annotation annotation = StanfordNLPService.getInstance().annotate("Starting Xatkit!");
        Log.debug("Warming up {0} with annotation {1}", StanfordNLPService.class.getSimpleName(), annotation);
    }

    /**
     * Computes the {@link Annotation} for the provided {@code input}.
     * <p>
     * This method looks in the provided {@code session} if an {@link Annotation} has been already computed for the
     * provided {@code input} and returns it. If it is not the case a new {@link Annotation} is computed using
     * {@link StanfordNLPService#annotate(String)} and stored in the {@code session}.
     *
     * @param input        the textual input to annotate
     * @param stateContext the {@link StateContext} corresponding to the provided {@code input}
     * @return the {@link Annotation} corresponding to the provided {@code input}
     */
    protected final Annotation getAnnotation(String input, StateContext stateContext) {
        String nlpInput = (String) stateContext.getSession().get(NLP_INPUT_SESSION_KEY);
        Annotation annotation = (Annotation) stateContext.getSession().get(NLP_ANNOTATION_SESSION_KEY);
        if (isNull(annotation) || isNull(nlpInput) || !nlpInput.equals(input)) {
            Log.debug("There is no annotation for \"{0}\" in the session, computing the annotation with {1}", input,
                    StanfordNLPService.class.getSimpleName());
            annotation = StanfordNLPService.getInstance().annotate(input);
            stateContext.getSession().put(NLP_INPUT_SESSION_KEY, input);
            stateContext.getSession().put(NLP_ANNOTATION_SESSION_KEY, annotation);
        } else {
            Log.debug("Reusing annotation for \"{0}\" from the session", input);
        }
        return annotation;
    }
}

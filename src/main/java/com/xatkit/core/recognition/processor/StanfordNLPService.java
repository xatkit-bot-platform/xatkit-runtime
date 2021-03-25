package com.xatkit.core.recognition.processor;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import fr.inria.atlanmod.commons.log.Log;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A singleton class that wraps a {@link StanfordCoreNLP} pipeline used to process user messages.
 * <p>
 * This class is configured with the {@link #addAnnotator(String)} method that allows to add NLP annotators used by
 * pre/post processors. Once fully configured the actual NLP service can be initialized with {@link #init()}.
 * <p>
 * This class should be used by all the pre/post processors relying on {@link StanfordCoreNLP} in order to optimize
 * the memory consumption and the execution time.
 */
public final class StanfordNLPService {

    /**
     * The singleton instance of this class.
     *
     * @see #getInstance()
     */
    private static StanfordNLPService INSTANCE;

    /**
     * Returns the singleton instance of this class.
     *
     * @return the singleton instance of this class
     */
    public static StanfordNLPService getInstance() {
        if (isNull(INSTANCE)) {
            INSTANCE = new StanfordNLPService();
        }
        return INSTANCE;
    }

    /**
     * The list of annotators to use in the internal NLP pipeline.
     * <p>
     * These annotators are provided using their String representation, see the
     * <a href="https://nlp.stanford.edu/">Stanford NLP documentation</a> for more information.
     */
    private List<String> annotators;

    /**
     * The internal {@link StanfordCoreNLP} pipeline used to annotate user inputs.
     */
    private StanfordCoreNLP nlpPipeline;

    /**
     * Constructs the singleton instance of this class.
     * <p>
     * This constructor is private to ensure that there is only one instance of it in the application, that can be
     * accessed using {@link #getInstance()}.
     */
    private StanfordNLPService() {
        this.annotators = new ArrayList<>();
    }

    /**
     * Initialize the underlying {@link StanfordCoreNLP} pipeline with the provided annotators.
     * <p>
     * This method should be called once <b>all</b> the annotators have been specified. Adding annotators after
     * calling this method will throw an {@link IllegalArgumentException}.
     * <p>
     * <b>Note</b>: this method does not re-initialize the underlying {@link StanfordCoreNLP} if it has been
     * previously initialized.
     */
    public void init() {
        if (isNull(nlpPipeline)) {
            Properties props = new Properties();
            props.setProperty("annotators", String.join(",", annotators));
            props.setProperty("parse.maxlen", "100");
            this.nlpPipeline = new StanfordCoreNLP(props);
        } else {
            Log.debug("Skipping initialization of {0}, the NLP pipeline is already initialized",
                    StanfordCoreNLP.class.getSimpleName());
        }
    }

    /**
     * Adds the provided {@code annotator} to the service.
     * <p>
     * The provided {@code annotator} is added iff it has not been specified before.
     * <p>
     * The annotator is provided using its String representation, see the
     * <a href="https://nlp.stanford.edu/">Stanford NLP documentation</a> for more information.
     *
     * @param annotator the annotator to add to the service
     * @throws IllegalArgumentException if the underlying NLP service is already started
     */
    public void addAnnotator(String annotator) {
        if (nonNull(nlpPipeline)) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot add annotator {0}: the NLP pipeline is "
                    + "already created", annotator));
        }
        if (!annotators.contains(annotator)) {
            this.annotators.add(annotator);
        }
    }

    /**
     * Adds the provided list of {@code annotators} to the service.
     * <p>
     * Each provided {@code annotator} is added iff it has not been specified before.
     * <p>
     * These annotators are provided using their String representation, see the
     * <a href="https://nlp.stanford.edu/">Stanford NLP documentation</a> for more information.
     *
     * @param annotators the list of annotators to add to the service
     * @throws IllegalArgumentException if the underlying NLP service is already started
     */
    public void addAnnotators(List<String> annotators) {
        for (String annotator : annotators) {
            this.addAnnotator(annotator);
        }
    }

    /**
     * Annotates the provided {@code input} with the specified {@code annotators}.
     * <p>
     * The {@code annotators} used to annotate the provided {@code input} are specified using
     * {@link #addAnnotator(String)}.
     *
     * @param input the textual input to annotate
     * @return the computed {@link Annotation}
     */
    public Annotation annotate(String input) {
        if (isNull(nlpPipeline)) {
            // This first call takes too long (~2s to load everything, not acceptable)
            Log.warn("The {0} hasn't been initialized correctly, doing it right now (this may take a few seconds). To"
                    + " avoid this make sure to init the service before any intent is matched.");
            init();
        }
        Annotation annotation = new Annotation(input);
        nlpPipeline.annotate(annotation);
        return annotation;
    }
}

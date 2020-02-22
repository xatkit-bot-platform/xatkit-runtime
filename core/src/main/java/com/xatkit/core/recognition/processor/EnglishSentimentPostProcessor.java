package com.xatkit.core.recognition.processor;

import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.RecognizedIntent;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.util.Arrays;
import java.util.List;

/**
 * Computes the sentiment associated to the last sentence of the user input.
 * <p>
 * The extracted sentiment is one of {@code {Very Negative, Negative, Neutral, Positive, Very Positive}}. This
 * post-processor sets the context variable {@code nlp.sentiment} with the result of the analysis.
 */
public class EnglishSentimentPostProcessor extends StanfordNLPPostProcessor {

    /**
     * The context parameter key used to store the sentiment extracted from the user input.
     */
    protected final static String SENTIMENT_PARAMETER_KEY = "sentiment";

    /**
     * The default value for the {@link #SENTIMENT_PARAMETER_KEY} parameter.
     */
    protected final static String DEFAULT_SENTIMENT_VALUE = "Neutral";

    /**
     * Constructs an instance of this post-processor.
     * <p>
     * This method sets the NLP annotators required to perform the analysis.
     */
    public EnglishSentimentPostProcessor() {
        /*
         * Note: each addAnnotators() invocation should be self-contained, i.e. it should not assume that another
         * processor has set one of the required annotators.
         * Adding the same annotator multiple times is handled by StanfordNLPService#addAnnotators.
         */
        StanfordNLPService.getInstance().addAnnotators(Arrays.asList("tokenize", "ssplit", "pos", "parse", "sentiment"
        ));
    }

    /**
     * Processes the provided {@code recognizedIntent} and sets the {@code nlp.sentiment} context parameter.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param session          the {@link XatkitSession} associated to the {@code recognizedIntent}
     * @return the unmodified {@code recognizedIntent}
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, XatkitSession session) {
        session.getRuntimeContexts().setContextValue(NLP_CONTEXT_KEY, 1, SENTIMENT_PARAMETER_KEY,
                DEFAULT_SENTIMENT_VALUE);
        Annotation annotation = getAnnotation(recognizedIntent.getMatchedInput(), session);
        /*
         * We only get the sentiment in the last sentence, we need some heuristics to compute the sentiment of a
         * whole corpus (or use some other API from the NLP pipeline).
         */
        List<CoreMap> sentenceAnnotations = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        String sentimentValue =
                sentenceAnnotations.get(sentenceAnnotations.size() - 1).get(SentimentCoreAnnotations.SentimentClass.class);
        session.getRuntimeContexts().setContextValue(NLP_CONTEXT_KEY, 1, SENTIMENT_PARAMETER_KEY, sentimentValue);
        return recognizedIntent;
    }
}

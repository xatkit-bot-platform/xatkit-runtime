package com.xatkit.core.recognition.processor;

import com.xatkit.execution.StateContext;
import com.xatkit.intent.RecognizedIntent;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.LabeledScoredConstituentFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import fr.inria.atlanmod.commons.log.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

/**
 * Computes whether the last sentence of the user input is a yes/no question.
 * <p>
 * This post-processor sets the context variable {@code nlp.isYesNo} that contains a boolean value.
 * <p>
 * The analysis of the input is performed using POS tagging with constituent labelling.
 */
public class IsEnglishYesNoQuestionPostProcessor extends StanfordNLPPostProcessor {

    /**
     * The context parameter key used to store whether the user input is a yes/no question.
     */
    protected final static String IS_YES_NO_PARAMETER_KEY = NLP_CONTEXT_KEY + ".isYesNo";

    /**
     * The default value of the {@link #IS_YES_NO_PARAMETER_KEY} parameter.
     */
    protected final static boolean DEFAULT_IS_YES_NO_VALUE = false;

    /**
     * Constructs an instance of this post-processor.
     * <p>
     * This method sets the NLP annotators required to perform the analysis.
     */
    public IsEnglishYesNoQuestionPostProcessor() {
        /*
         * Note: each addAnnotators() invocation should be self-contained, i.e. it should not assume that another
         * processor has set one of the required annotators.
         * Adding the same annotator multiple times is handled by StanfordNLPService#addAnnotators.
         */
        StanfordNLPService.getInstance().addAnnotators(Arrays.asList("tokenize", "ssplit", "pos", "parse"));
    }

    /**
     * Processes the provided {@code recognizedIntent} and sets the {@code nlp.isYesNo} context parameter.
     * <p>
     * This method sets the {@code nlp.isYesNo} context parameter to {@code true} if the provided {@code
     * recognizedIntent}'s input is a yes/no question, and {@code false} otherwise.
     * <p>
     * <b>Note</b>: {@code nlp.isYesNo == false} does not mean that the provided input is not a yes/no question, but
     * that it could not be matched to the yes/no patterns supported by this processor.
     *
     * @param recognizedIntent the {@link RecognizedIntent} to process
     * @param context          the {@link StateContext} associated to the {@code recognizedIntent}
     * @return the unmodified {@code recognizedIntent}
     */
    @Override
    public RecognizedIntent process(RecognizedIntent recognizedIntent, StateContext context) {
        recognizedIntent.getNlpData().put(IS_YES_NO_PARAMETER_KEY, DEFAULT_IS_YES_NO_VALUE);
        Annotation annotation = getAnnotation(recognizedIntent.getMatchedInput(), context);
        List<CoreMap> sentenceAnnotations = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        /*
         * We want to know if the latest sentence is a yes/no question, previous sentences do not matter in this
         * processor.
         */
        Tree tree =
                sentenceAnnotations.get(sentenceAnnotations.size() - 1).get(TreeCoreAnnotations.TreeAnnotation.class);
        Log.debug(tree.toString());
        Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
        List<Constituent> sqConstituents =
                treeConstituents.stream().filter(c -> nonNull(c.label()) && c.label().toString().equals("SQ")).collect(Collectors.toList());
        for (Constituent sqConstituent : sqConstituents) {
            if (sqConstituent.start() == 0) {
                recognizedIntent.getNlpData().put(IS_YES_NO_PARAMETER_KEY, true);
            }
        }
        return recognizedIntent;
    }
}

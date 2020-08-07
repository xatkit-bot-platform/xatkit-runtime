package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.library.CoreLibrary;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

public class AbstractIntentRecognitionProviderTest extends AbstractXatkitTest  {

    private AbstractIntentRecognitionProvider provider;

    private RecognizedIntent help;

    private RecognizedIntent greetings;

    @Before
    public void setUp() {
        provider = mock(AbstractIntentRecognitionProvider.class);
        doCallRealMethod().when(provider).getBestCandidate(anyCollection(), any());
        CoreLibrary coreLibrary = new CoreLibrary();
        help = IntentFactory.eINSTANCE.createRecognizedIntent();
        help.setDefinition(coreLibrary.Help);
        help.setRecognitionConfidence(1);
        greetings = IntentFactory.eINSTANCE.createRecognizedIntent();
        greetings.setDefinition(coreLibrary.Greetings);
        greetings.setRecognitionConfidence(.5f);
    }

    @Test
    public void getBestCandidateSingleSolution() {
        Collection<RecognizedIntent> candidates = Arrays.asList(help, greetings);
        XatkitSession session = new XatkitSession("SessionID");
        session.getRuntimeContexts().setContext("EnableHelp", 2);
        RecognizedIntent bestCandidate = provider.getBestCandidate(candidates, session);
        assertThat(bestCandidate).isEqualTo(help);
    }

    @Test
    public void getBestCandidateMultipleSolutions() {
        Collection<RecognizedIntent> candidates = Arrays.asList(help, greetings);
        XatkitSession session = new XatkitSession("SessionID");
        session.getRuntimeContexts().setContext("EnableHelp", 2);
        session.getRuntimeContexts().setContext("EnableGreetings", 2);
        RecognizedIntent bestCandidate = provider.getBestCandidate(candidates, session);
        /*
         * Make sure we have the best option here, even if the other one is possible.
         */
        assertThat(bestCandidate).isEqualTo(help);
    }

    @Test
    public void getBestCandidateMultipleSolutionResultNotFirstInCollection() {
        Collection<RecognizedIntent> candidates = Arrays.asList(greetings, help);
        XatkitSession session = new XatkitSession("SessionID");
        session.getRuntimeContexts().setContext("EnableHelp", 2);
        session.getRuntimeContexts().setContext("EnableGreetings", 2);
        RecognizedIntent bestCandidate = provider.getBestCandidate(candidates, session);
        /*
         * Make sure we have the best option here, even if it's not the first match in the collection.
         */
        assertThat(bestCandidate).isEqualTo(help);
    }

    @Test
    public void getBestCandidateNoSolution() {
        Collection<RecognizedIntent> candidates = Arrays.asList(help, greetings);
        XatkitSession session = new XatkitSession("SessionID");
        RecognizedIntent bestCandidate = provider.getBestCandidate(candidates, session);
        assertThat(bestCandidate.getDefinition()).isEqualTo(IntentRecognitionProvider.DEFAULT_FALLBACK_INTENT);
    }
}

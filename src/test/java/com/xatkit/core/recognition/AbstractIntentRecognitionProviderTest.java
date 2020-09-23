package com.xatkit.core.recognition;

import com.xatkit.AbstractXatkitTest;
import com.xatkit.execution.ExecutionFactory;
import com.xatkit.execution.State;
import com.xatkit.execution.StateContext;
import com.xatkit.intent.IntentFactory;
import com.xatkit.intent.RecognizedIntent;
import com.xatkit.library.core.CoreLibrary;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractIntentRecognitionProviderTest extends AbstractXatkitTest  {

    private AbstractIntentRecognitionProvider provider;

    private RecognizedIntent help;

    private RecognizedIntent greetings;

    private StateContext stateContext;

    private State initState;

    @Before
    public void setUp() {
        provider = mock(AbstractIntentRecognitionProvider.class);
        doCallRealMethod().when(provider).getBestCandidate(anyCollection(), any());
        help = IntentFactory.eINSTANCE.createRecognizedIntent();
        help.setDefinition(CoreLibrary.Help);
        help.setRecognitionConfidence(1);
        greetings = IntentFactory.eINSTANCE.createRecognizedIntent();
        greetings.setDefinition(CoreLibrary.Greetings);
        greetings.setRecognitionConfidence(.5f);
        stateContext = ExecutionFactory.eINSTANCE.createStateContext();
        stateContext.setContextId("ContextID");
        initState = mock(State.class);
        stateContext.setState(initState);
    }

    @Test
    public void getBestCandidateSingleSolution() {
        Collection<RecognizedIntent> candidates = Arrays.asList(help, greetings);
        when(initState.getAllAccessedIntents()).thenReturn(Collections.singletonList(CoreLibrary.Help));
        RecognizedIntent bestCandidate = provider.getBestCandidate(candidates, stateContext);
        assertThat(bestCandidate).isEqualTo(help);
    }

    @Test
    public void getBestCandidateMultipleSolutions() {
        Collection<RecognizedIntent> candidates = Arrays.asList(help, greetings);
        when(initState.getAllAccessedIntents()).thenReturn(Arrays.asList(CoreLibrary.Help, CoreLibrary.Greetings));
        RecognizedIntent bestCandidate = provider.getBestCandidate(candidates, stateContext);
        /*
         * Make sure we have the best option here, even if the other one is possible.
         */
        assertThat(bestCandidate).isEqualTo(help);
    }

    @Test
    public void getBestCandidateMultipleSolutionResultNotFirstInCollection() {
        Collection<RecognizedIntent> candidates = Arrays.asList(greetings, help);
        when(initState.getAllAccessedIntents()).thenReturn(Arrays.asList(CoreLibrary.Help, CoreLibrary.Greetings));
        RecognizedIntent bestCandidate = provider.getBestCandidate(candidates, stateContext);
        /*
         * Make sure we have the best option here, even if it's not the first match in the collection.
         */
        assertThat(bestCandidate).isEqualTo(help);
    }

    @Test
    public void getBestCandidateNoSolution() {
        Collection<RecognizedIntent> candidates = Arrays.asList(help, greetings);
        when(initState.getAllAccessedIntents()).thenReturn(Collections.emptyList());
        RecognizedIntent bestCandidate = provider.getBestCandidate(candidates, stateContext);
        assertThat(bestCandidate.getDefinition()).isEqualTo(IntentRecognitionProvider.DEFAULT_FALLBACK_INTENT);
    }
}

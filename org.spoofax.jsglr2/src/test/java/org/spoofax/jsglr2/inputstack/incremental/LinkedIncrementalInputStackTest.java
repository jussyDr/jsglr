package org.spoofax.jsglr2.inputstack.incremental;

import java.util.Arrays;

import org.spoofax.jsglr2.incremental.EditorUpdate;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;

public class LinkedIncrementalInputStackTest extends AbstractIncrementalInputStackTest {

    @Override protected IIncrementalInputStack getStack(IncrementalParseNode previousResult, String inputString,
        EditorUpdate... editorUpdates) {
        return new LinkedIncrementalInputStack(inputString, previousResult, Arrays.asList(editorUpdates));
    }

}

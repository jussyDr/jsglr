package org.spoofax.jsglr2.measure.parsing;

public enum ParsingMeasurement {
    name, size, characters, activeStacksIsEmptyChecks, activeStacksIsSingleChecks, activeStacksFindsWithState,
    activeStacksAdds, activeStacksForActorAdds, activeStacksForActorDelayedAdds, activeStacksAddAllToForActors,
    activeStacksMaxSize, activeStacksForActorMaxSize, activeStacksForActorDelayedMaxSize, activeStacksClears,
    activeStacksForActorHasNextChecks, activeStacksForLimitedReductions, activeStacksIterators, stackNodes,
    stackNodesSingleLink, stackLinks, stackLinksRejected, deterministicDepthResets, parseNodes,
    parseNodesSingleDerivation, parseNodesAmbiguous, parseNodesContextFree, parseNodesContextFreeAmbiguous,
    parseNodesLexical, parseNodesLexicalAmbiguous, parseNodesLayout, parseNodesLayoutAmbiguous, parseNodesLiteral,
    parseNodesLiteralAmbiguous, characterNodes, actors, doReductions, doLimitedReductions, doReductionsLR,
    doReductionsDeterministicGLR, doReductionsNonDeterministicGLR, reducers, reducersElkhound
}

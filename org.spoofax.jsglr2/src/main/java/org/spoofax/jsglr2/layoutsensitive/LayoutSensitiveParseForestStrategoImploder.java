package org.spoofax.jsglr2.layoutsensitive;

import java.util.List;

import org.metaborg.parsetable.IProduction;
import org.spoofax.jsglr2.imploder.StrategoTermImploder;

public class LayoutSensitiveParseForestStrategoImploder
    extends StrategoTermImploder<LayoutSensitiveParseForest, LayoutSensitiveParseNode, LayoutSensitiveDerivation> {

    public LayoutSensitiveParseForestStrategoImploder() {
        super();
    }

    @Override protected IProduction parseNodeProduction(LayoutSensitiveParseNode parseNode) {
        return parseNode.production;
    }

    @Override protected LayoutSensitiveDerivation parseNodeOnlyDerivation(LayoutSensitiveParseNode parseNode) {
        return parseNode.getFirstDerivation();
    }

    @Override protected List<LayoutSensitiveDerivation>
        parseNodePreferredAvoidedDerivations(LayoutSensitiveParseNode parseNode) {
        return parseNode.getPreferredAvoidedDerivations();
    }

    @Override protected List<LayoutSensitiveDerivation>
        longestMatchedDerivations(List<LayoutSensitiveDerivation> derivations) {

        return derivations;
    }
}

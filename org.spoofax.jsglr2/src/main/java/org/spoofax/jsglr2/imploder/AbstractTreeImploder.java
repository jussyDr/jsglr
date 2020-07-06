package org.spoofax.jsglr2.imploder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.metaborg.parsetable.symbols.IMetaVarSymbol;
import org.spoofax.jsglr2.layoutsensitive.ILayoutSensitiveParseNode;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;

public abstract class AbstractTreeImploder
//@formatter:off
   <ParseForest extends IParseForest,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    Derivation  extends IDerivation<ParseForest>,
    IntermediateResult,
    Cache,
    AbstractSyntaxTree,
    Result      extends IImplodeResult<IntermediateResult, Cache, AbstractSyntaxTree>>
//@formatter:on
    implements IImploder<ParseForest, IntermediateResult, Cache, AbstractSyntaxTree, Result> {

    protected List<List<ParseForest>> implodeAmbiguousLists(List<Derivation> derivations) {
        List<List<ParseForest>> alternatives = new ArrayList<>();

        for(Derivation derivation : derivations) {
            ParseForest[] children = derivation.parseForests();
            if(children.length == 0) {
                alternatives.add(Collections.emptyList());
            } else if(children.length == 1) {
                alternatives.add(Collections.singletonList(children[0]));
            } else {
                List<ParseForest> subTrees = Arrays.asList(children);

                ParseNode head = (ParseNode) children[0];

                if(head.production().isList() && head.getPreferredAvoidedDerivations().size() > 1) {
                    List<ParseForest> tail = subTrees.subList(1, subTrees.size());

                    List<List<ParseForest>> headExpansions =
                        implodeAmbiguousLists(head.getPreferredAvoidedDerivations());

                    for(List<ParseForest> headExpansion : headExpansions) {
                        List<ParseForest> headExpansionWithTail = new ArrayList<>(headExpansion);
                        headExpansionWithTail.addAll(tail);
                        alternatives.add(headExpansionWithTail);
                    }
                } else {
                    alternatives.add(subTrees);
                }
            }
        }

        return alternatives;
    }

    protected ParseNode implodeInjection(ParseNode parseNode) {
        for(Derivation derivation : parseNode.getDerivations()) {
            if(derivation.parseForests().length == 1 && (derivation.parseForests()[0] instanceof IParseNode)) {
                ParseNode injectedParseNode = (ParseNode) derivation.parseForests()[0];

                // Meta variables are injected:
                // https://github.com/metaborg/strategoxt/blob/master/strategoxt/stratego-libraries/sglr/lib/stratego/asfix/implode/injection.str#L68-L69
                if(injectedParseNode.production().lhs() instanceof IMetaVarSymbol) {
                    return injectedParseNode;
                }
            }
        }

        return parseNode;
    }

    protected List<Derivation> applyDisambiguationFilters(ParseNode parseNode) {
        if(!parseNode.isAmbiguous())
            return Collections.singletonList(parseNode.getFirstDerivation());

        List<Derivation> result;
        // TODO always filter longest-match?
        if(parseNode instanceof ILayoutSensitiveParseNode) {
            ((ILayoutSensitiveParseNode) parseNode).filterLongestMatchDerivations();
        }
        // TODO always filter prefer/avoid?
        result = parseNode.getPreferredAvoidedDerivations();

        return result;
    }

}

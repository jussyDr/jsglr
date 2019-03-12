package org.spoofax.jsglr2.layoutsensitive;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.IProduction;
import org.metaborg.parsetable.actions.IReduce;
import org.metaborg.sdf2table.grammar.LayoutConstraintAttribute;
import org.metaborg.sdf2table.parsetable.ParseTableProduction;
import org.spoofax.jsglr2.parseforest.AbstractParseForest;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestManager;
import org.spoofax.jsglr2.parser.AbstractParse;
import org.spoofax.jsglr2.reducing.ReduceManager;
import org.spoofax.jsglr2.stack.AbstractStackNode;
import org.spoofax.jsglr2.stack.StackLink;
import org.spoofax.jsglr2.stack.StackManager;
import org.spoofax.jsglr2.stack.paths.StackPath;

public class LayoutSensitiveReduceManager<ParseForest extends AbstractParseForest, ParseNode extends ParseForest, Derivation, StackNode extends AbstractStackNode<ParseForest>>
    extends ReduceManager<ParseForest, ParseNode, Derivation, StackNode> {

    LayoutConstraintEvaluator<ParseForest> lce = new LayoutConstraintEvaluator<>();

    public LayoutSensitiveReduceManager(IParseTable parseTable, StackManager<ParseForest, StackNode> stackManager,
        ParseForestManager<ParseForest, ParseNode, Derivation> parseForestManager,
        ParseForestConstruction parseForestConstruction) {
        super(parseTable, stackManager, parseForestManager, parseForestConstruction);
    }

    @Override protected void doReductionsHelper(AbstractParse<ParseForest, StackNode> parse, StackNode stack, IReduce reduce,
        StackLink<ParseForest, StackNode> throughLink) {
        for(StackPath<ParseForest, StackNode> path : stackManager.findAllPathsOfLength(stack, reduce.arity())) {
            if(throughLink == null || path.contains(throughLink)) {
                StackNode pathBegin = path.head();
                ParseForest[] parseNodes = stackManager.getParseForests(parseForestManager, path);

                boolean skipReduce = false;

                IProduction prod = reduce.production();
                if(prod instanceof ParseTableProduction) {
                    if(!((ParseTableProduction) prod).getLayoutConstraints().isEmpty()) {
                        for(LayoutConstraintAttribute lca : ((ParseTableProduction) prod).getLayoutConstraints()) {
                            try {
                                if(!lce.evaluate(lca.getLayoutConstraint(), parseNodes)) {
                                    skipReduce = true;
                                    break;
                                }
                            } catch(Exception e) {
                                System.err.println(e.getMessage());
                                skipReduce = true;
                                break;
                            }
                        }
                    }
                }

                if(skipReduce) {
                    continue;
                }

                reducer(parse, pathBegin, reduce, parseNodes);
            }
        }
    }

}

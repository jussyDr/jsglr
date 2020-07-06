package org.spoofax.jsglr2.imploder;

import java.util.*;

import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.symbols.IMetaVarSymbol;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.imploder.input.IImplodeInputFactory;
import org.spoofax.jsglr2.imploder.input.ImplodeInput;
import org.spoofax.jsglr2.imploder.treefactory.ITreeFactory;
import org.spoofax.jsglr2.parseforest.ICharacterNode;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;

public class TreeImploder
//@formatter:off
   <ParseForest extends IParseForest,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    Derivation  extends IDerivation<ParseForest>,
    Cache,
    Tree,
    Input       extends ImplodeInput>
//@formatter:on
    extends
    AbstractTreeImploder<ParseForest, ParseNode, Derivation, TreeImploder.SubTree<Tree>, Cache, Tree, ImplodeResult<TreeImploder.SubTree<Tree>, Cache, Tree>> {

    protected final IImplodeInputFactory<Input> inputFactory;
    protected final ITreeFactory<Tree> treeFactory;

    public TreeImploder(IImplodeInputFactory<Input> inputFactory, ITreeFactory<Tree> treeFactory) {
        this.inputFactory = inputFactory;
        this.treeFactory = treeFactory;
    }

    @Override public ImplodeResult<TreeImploder.SubTree<Tree>, Cache, Tree> implode(JSGLR2Request request,
        ParseForest parseForest, Cache resultCache) {
        SubTree<Tree> result = implodeParseNode(inputFactory.get(request.input), parseForest, 0);

        return new ImplodeResult<>(result, null, result.tree, result.containsAmbiguity);
    }

    protected SubTree<Tree> implodeParseNode(Input input, ParseForest parseForest, int startOffset) {
        if(parseForest instanceof ICharacterNode) {
            return new SubTree<>(treeFactory.createCharacterTerminal(((ICharacterNode) parseForest).character()), null,
                parseForest.width(), true);
        }

        @SuppressWarnings("unchecked") ParseNode parseNode = implodeInjection((ParseNode) parseForest);

        IProduction production = parseNode.production();

        if(production.isContextFree() && !production.isSkippableInParseForest()) {
            List<Derivation> filteredDerivations = applyDisambiguationFilters(parseNode);

            if(filteredDerivations.size() > 1) {
                List<Tree> trees = new ArrayList<>(filteredDerivations.size());
                List<SubTree<Tree>> subTrees = new ArrayList<>(filteredDerivations.size());

                if(production.isList()) {
                    for(List<ParseForest> derivationParseForests : implodeAmbiguousLists(filteredDerivations)) {
                        SubTree<Tree> result = implodeDerivationChildren(input, production,
                            getChildParseForests(production, derivationParseForests), startOffset);
                        trees.add(result.tree);
                        subTrees.add(result);
                    }
                } else {
                    for(Derivation derivation : filteredDerivations) {
                        SubTree<Tree> result = implodeDerivation(input, derivation, startOffset);
                        trees.add(result.tree);
                        subTrees.add(result);
                    }
                }

                return new SubTree<>(treeFactory.createAmb(trees), subTrees, null, subTrees.get(0).width, false, true,
                    false);
            } else
                return implodeDerivation(input, filteredDerivations.get(0), startOffset);
        } else {
            int width = parseNode.width();

            return new SubTree<>(createLexicalTerm(production, input.inputString, startOffset, width), production,
                width, false);
        }
    }

    protected SubTree<Tree> implodeDerivation(Input input, Derivation derivation, int startOffset) {
        IProduction production = derivation.production();

        if(!production.isContextFree())
            throw new RuntimeException("non context free imploding not supported");

        return implodeDerivationChildren(input, production, getChildParseForests(derivation), startOffset);
    }

    protected SubTree<Tree> implodeDerivationChildren(Input input, IProduction production,
        List<ParseForest> childParseForests, int startOffset) {

        List<Tree> childASTs = new ArrayList<>();
        List<SubTree<Tree>> subTrees = new ArrayList<>();

        for(ParseForest childParseForest : childParseForests) {
            SubTree<Tree> subTree = this.implodeParseNode(input, childParseForest, startOffset);

            if(subTree.tree != null) {
                childASTs.add(subTree.tree);
            }
            subTrees.add(subTree);
            startOffset += subTree.width;
        }

        Tree contextFreeTerm = createContextFreeTerm(production, childASTs);
        return new SubTree<>(contextFreeTerm, subTrees, production,
            childASTs.size() == 1 && contextFreeTerm == childASTs.get(0));
    }

    protected List<ParseForest> getChildParseForests(Derivation derivation) {
        return getChildParseForests(derivation.production(), Arrays.asList(derivation.parseForests()));
    }

    protected List<ParseForest> getChildParseForests(IProduction production, List<ParseForest> parseForests) {
        // Make sure lists are flattened
        if(production.isList()) {
            LinkedList<ParseForest> listQueueDone = new LinkedList<>();
            LinkedList<ParseForest> listQueueTodo = new LinkedList<>(parseForests);

            // Check child parse forest from front to back
            while(!listQueueTodo.isEmpty()) {
                ParseForest childParseForest = listQueueTodo.removeFirst();

                @SuppressWarnings("unchecked") ParseNode childParseNode = (ParseNode) childParseForest;

                IProduction childProduction = childParseNode.production();

                // If child is also a list, add all its children to the front of the unprocessed list
                if(childProduction.isList() && childProduction.constructor() == null) {
                    List<Derivation> filteredDerivations = applyDisambiguationFilters(childParseNode);
                    if(filteredDerivations.size() <= 1) {
                        listQueueTodo.addAll(0, Arrays.asList(filteredDerivations.get(0).parseForests()));
                        continue;
                    }
                }

                // Else, add child to processed list
                listQueueDone.add(childParseForest);
            }
            return listQueueDone;
        } else {
            return parseForests;
        }
    }

    protected Tree createLexicalTerm(IProduction production, String inputString, int startOffset, int width) {
        if(production.isLayout() || production.isLiteral()) {
            return null;
        } else if(production.isLexical()) {
            String substring = inputString.substring(startOffset, startOffset + width);
            if(production.lhs() instanceof IMetaVarSymbol)
                return treeFactory.createMetaVar((IMetaVarSymbol) production.lhs(), substring);
            else
                return treeFactory.createStringTerminal(production.lhs(), substring);
        } else {
            throw new RuntimeException("invalid term type");
        }
    }

    protected Tree createContextFreeTerm(IProduction production, List<Tree> childASTs) {
        String constructor = production.constructor();

        if(constructor != null)
            return treeFactory.createNonTerminal(production.lhs(), constructor, childASTs);
        else if(production.isOptional())
            return treeFactory.createOptional(production.lhs(), childASTs);
        else if(production.isList())
            return treeFactory.createList(childASTs);
        else if(childASTs.size() == 1)
            return childASTs.get(0);
        else
            return treeFactory.createTuple(childASTs);
    }

    public static class SubTree<Tree> {

        public final Tree tree;
        public final List<SubTree<Tree>> children;
        public final IProduction production;
        public final int width;

        /**
         * True whenever the `tree` field of this node and its (only) child node are equal. Tokenizers should annotate
         * ASTs with the sort/cons of the production that is closest to the node. This means that injections should be
         * skipped when adding the ImploderAttachment. E.g. The program `x` with AST `Exp()` should be annotated with
         * `Exp.Exp` and not with `Start` in the following grammar:
         *
         * <code>
         * context-free syntax
         *     Start = Stmt
         *     Stmt = Exp
         *     Exp.Exp = "x"
         * </code>
         */
        public final boolean isInjection;
        public final boolean isAmbiguous;
        public final boolean containsAmbiguity;
        public final boolean isCharacterTerminal;

        public SubTree(Tree tree, List<SubTree<Tree>> children, IProduction production, int width, boolean isInjection,
            boolean isAmbiguous, boolean isCharacterTerminal) {
            this.tree = tree;
            this.children = children;
            this.production = production;
            this.width = width;
            this.isInjection = isInjection;
            this.isAmbiguous = isAmbiguous;
            this.containsAmbiguity = isAmbiguous || childrenContainAmbiguity(children);
            this.isCharacterTerminal = isCharacterTerminal;
        }

        /** This constructor infers the width from the sum of widths of its children. */
        public SubTree(Tree tree, List<SubTree<Tree>> children, IProduction production, boolean isInjection) {
            this(tree, children, production, sumWidth(children), isInjection, false, false);
        }

        /** This constructor corresponds to a terminal/lexical node without children. */
        public SubTree(Tree tree, IProduction production, int width, boolean isCharacterTerminal) {
            this(tree, Collections.emptyList(), production, width, false, false, isCharacterTerminal);
        }

        private static <Tree> int sumWidth(List<SubTree<Tree>> children) {
            int result = 0;
            for(SubTree<Tree> child : children) {
                result += child.width;
            }
            return result;
        }

        private static <Tree> boolean childrenContainAmbiguity(List<SubTree<Tree>> children) {
            for(SubTree<Tree> child : children) {
                if(child.containsAmbiguity)
                    return true;
            }
            return false;
        }

    }
}

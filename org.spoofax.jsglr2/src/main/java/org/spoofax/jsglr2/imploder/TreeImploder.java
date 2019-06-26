package org.spoofax.jsglr2.imploder;

import java.util.*;

import org.metaborg.parsetable.IProduction;
import org.spoofax.jsglr2.imploder.treefactory.ITreeFactory;
import org.spoofax.jsglr2.layoutsensitive.LayoutSensitiveParseNode;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;

public class TreeImploder
//@formatter:off
   <ParseForest extends IParseForest,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    Derivation  extends IDerivation<ParseForest>,
    Tree>
//@formatter:on
    implements IImploder<ParseForest, TreeImploder.SubTree<Tree>> {

    protected final ITreeFactory<Tree> treeFactory;

    public TreeImploder(ITreeFactory<Tree> treeFactory) {
        this.treeFactory = treeFactory;
    }

    @Override public SubTree<Tree> implode(String input, String filename, ParseForest parseForest) {
        @SuppressWarnings("unchecked") ParseNode topParseNode = (ParseNode) parseForest;

        return implodeParseNode(input, topParseNode, 0);
    }

    protected SubTree<Tree> implodeParseNode(String inputString, ParseNode parseNode, int startOffset) {
        IProduction production = parseNode.production();

        if(production.isContextFree()) {
            List<Derivation> filteredDerivations = applyDisambiguationFilters(parseNode);

            if(filteredDerivations.size() > 1) {
                List<Tree> trees = new ArrayList<>(filteredDerivations.size());
                List<SubTree<Tree>> subTrees = new ArrayList<>(filteredDerivations.size());

                for(Derivation derivation : filteredDerivations) {
                    SubTree<Tree> result = implodeDerivation(inputString, derivation, startOffset);
                    trees.add(result.tree);
                    subTrees.add(result);
                }

                return new SubTree<>(treeFactory.createAmb(production.sort(), trees), subTrees, null, null,
                    subTrees.get(0).width);
            } else
                return implodeDerivation(inputString, filteredDerivations.get(0), startOffset);
        } else {
            String substring = inputString.substring(startOffset, startOffset + parseNode.width());

            return new SubTree<>(createLexicalTerm(production, substring), production, substring);
        }
    }

    protected SubTree<Tree> implodeDerivation(String inputString, Derivation derivation, int startOffset) {
        IProduction production = derivation.production();

        if(!production.isContextFree())
            throw new RuntimeException("non context free imploding not supported");

        List<Tree> childASTs = new ArrayList<>();
        List<SubTree<Tree>> subTrees = new ArrayList<>();

        for(ParseForest childParseForest : getChildParseForests(derivation)) {
            if(childParseForest != null) { // Can be null in the case of a layout subtree parse node that is not created
                @SuppressWarnings("unchecked") ParseNode childParseNode = (ParseNode) childParseForest;

                SubTree<Tree> subTree = implodeParseNode(inputString, childParseNode, startOffset);

                if(subTree.tree != null) {
                    childASTs.add(subTree.tree);
                }
                subTrees.add(subTree);
                startOffset += subTree.width;
            }
        }

        return new SubTree<>(createContextFreeTerm(production, childASTs), subTrees, derivation.production());
    }

    protected List<ParseForest> getChildParseForests(Derivation derivation) {
        // Make sure lists are flattened
        if(derivation.production().isList()) {
            LinkedList<ParseForest> listQueueTodo = new LinkedList<>();
            LinkedList<ParseForest> listQueueDone = new LinkedList<>();
            Collections.addAll(listQueueTodo, derivation.parseForests());

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
            return Arrays.asList(derivation.parseForests());
        }
    }

    protected List<Derivation> applyDisambiguationFilters(ParseNode parseNode) {
        if(!parseNode.isAmbiguous())
            return Collections.singletonList(parseNode.getFirstDerivation());

        List<Derivation> result;
        // TODO always filter longest-match?
        if(parseNode instanceof LayoutSensitiveParseNode) {
            ((LayoutSensitiveParseNode) parseNode).filterLongestMatchDerivations();
        }
        // TODO always filter prefer/avoid?
        result = parseNode.getPreferredAvoidedDerivations();

        return result;
    }

    protected Tree createLexicalTerm(IProduction production, String substring) {
        if(production.isLayout() || production.isLiteral()) {
            return null;
        } else if(production.isLexical() || production.isLexicalRhs()) {
            return treeFactory.createStringTerminal(production.sort(), substring);
        } else {
            throw new RuntimeException("invalid term type");
        }
    }

    protected Tree createContextFreeTerm(IProduction production, List<Tree> childASTs) {
        String constructor = production.constructor();

        if(production.isList())
            return treeFactory.createList(production.sort(), childASTs);
        else if(production.isOptional())
            return treeFactory.createOptional(production.sort(), childASTs);
        else if(constructor != null)
            return treeFactory.createNonTerminal(production.sort(), constructor, childASTs);
        else if(childASTs.size() == 1)
            return childASTs.get(0);
        else
            return treeFactory.createTuple(production.sort(), childASTs);
    }

    public static class SubTree<Tree> {

        public final Tree tree;
        public final List<SubTree<Tree>> children;
        public final IProduction production;
        public final String string; // Only set for lexical nodes.
        public final int width;

        public SubTree(Tree tree, List<SubTree<Tree>> children, IProduction production, String string, int width) {
            this.tree = tree;
            this.children = children;
            this.production = production;
            this.string = string;
            this.width = width;
        }

        /** This constructor infers the width from the sum of widths of its children. */
        public SubTree(Tree tree, List<SubTree<Tree>> children, IProduction production) {
            this(tree, children, production, null, sumWidth(children));
        }

        /** This constructor corresponds to a terminal/lexical node without children. */
        public SubTree(Tree tree, IProduction production, String string) {
            this(tree, Collections.emptyList(), production, string, string.length());
        }

        private static <Tree> int sumWidth(List<SubTree<Tree>> children) {
            int result = 0;
            for(SubTree child : children) {
                result += child.width;
            }
            return result;
        }

    }
}

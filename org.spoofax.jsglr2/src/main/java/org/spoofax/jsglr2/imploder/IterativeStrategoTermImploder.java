package org.spoofax.jsglr2.imploder;

import static java.util.Collections.singletonList;

import java.util.*;
import java.util.stream.Collectors;

import org.metaborg.parsetable.IProduction;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class IterativeStrategoTermImploder
//@formatter:off
   <ParseForest extends IParseForest,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    Derivation  extends IDerivation<ParseForest>>
//@formatter:on
    extends StrategoTermImploder<ParseForest, ParseNode, Derivation> {

    private class ListTuple {
        Derivation derivation;
        List<SubTree> subTrees;

        public ListTuple(Derivation derivation, List<SubTree> subTrees) {
            this.derivation = derivation;
            this.subTrees = subTrees;
        }
    }

    private class Tuple {
        ParseForest parseNode;
        SubTree subTree;

        public Tuple(ParseForest parseForest, SubTree subTree) {
            this.parseNode = parseForest;
            this.subTree = subTree;
        }
    }

    private class PseudoNode {
        final ParseNode parseNode;
        final List<Derivation> derivations;
        final int beginOffset;
        int pivotOffset;

        PseudoNode(ParseNode parseNode, List<Derivation> derivations, int beginOffset) {
            this.parseNode = parseNode;
            this.derivations = derivations;
            this.beginOffset = beginOffset;
            this.pivotOffset = beginOffset;
        }
    }

    // @formatter:off
    /*
    @Override protected SubTree implodeParseNode(String inputString, ParseNode parseNode, int startOffset) {
        LinkedList<ParseNode> todo = new LinkedList<>();
        LinkedList<List<List<SubTree>>> done = new LinkedList<>();
        todo.add(parseNode);
        done.add(Collections.singletonList(new ArrayList<>()));

        while(!todo.isEmpty()) {
            ParseNode currentTodo = todo.removeLast();
            List<Derivation> derivations = applyDisambiguationFilters(currentTodo);
            List<List<SubTree>> currentDone = done.removeLast();
            int i = currentDone.size() - 1;
            int j = currentDone.get(i).size();
            if(j == derivations.get(i).parseForests().length) {
                i++;
                if(i == derivations.size()) {
                    {
                        List<IStrategoTerm> trees = new ArrayList<>(currentDone.size());
                        List<SubTree> subTrees = new ArrayList<>(currentDone.size());
                        for(ListTuple listTuple : Iterables2.zip(derivations, currentDone, ListTuple::new)) {
                            List<IStrategoTerm> childASTs = new ArrayList<>();
                            List<SubTree> subTrees1 = new ArrayList<>();

                            for(Tuple tuple : Iterables2.zip(Iterables2.from(listTuple.derivation.parseForests()),
                                listTuple.subTrees, Tuple::new)) {
                                SubTree subTree = tuple.subTree;
                                if(subTree.tree != null) {
                                    childASTs.add(subTree.tree);
                                }
                                subTrees1.add(subTree);
                                startOffset += subTree.width;
                            }

                            IProduction production1 = listTuple.derivation.production();
                            IStrategoTerm tree = createContextFreeTerm(production1, childASTs);
                            trees.add(tree);
                            subTrees.add(new SubTree(tree, subTrees1, production1));
                        }

                        if(subTrees.size() > 1)
                            return new SubTree(treeFactory.createAmb(parseNode.production().sort(), trees), subTrees,
                                null, null, subTrees.get(0).width);
                        else
                            return subTrees.get(0);
                    }
                    continue;
                }
                j = 0;
                currentDone.add(new ArrayList<>());
            }
            parseNode = (ParseNode) derivations.get(i).parseForests()[j];

            IProduction production = parseNode.production();

            if(production.isContextFree()) {
                List<Derivation> filteredDerivations = applyDisambiguationFilters(parseNode);

                for(Derivation derivation : filteredDerivations) {
                    IProduction production1 = derivation.production();

                    if(!production1.isContextFree())
                        throw new RuntimeException("non context free imploding not supported");

                    for(ParseForest childParseForest : getChildParseForests(derivation)) {
                        // Can be null in the case of a layout subtree parse node that is not created
                        if(childParseForest != null) {
                            @SuppressWarnings("unchecked") ParseNode childParseNode = (ParseNode) childParseForest;

                            todo.add(childParseNode);
                            done.add(new ArrayList<>(new ArrayList<>()));
                        }
                    }
                }
            } else {
                String substring = inputString.substring(startOffset, startOffset + parseNode.width());

                return new SubTree(createLexicalTerm(production, substring), production, substring);
            }
        }
        return done.get(0).get(0).get(0);
    }

    protected SubTree implodeParseNodeOneFunc(String inputString, ParseNode parseNode, int startOffset) {
        IProduction production = parseNode.production();

        if(production.isContextFree()) {
            List<Derivation> filteredDerivations = applyDisambiguationFilters(parseNode);

            List<IStrategoTerm> trees = new ArrayList<>(filteredDerivations.size());
            List<SubTree> subTrees = new ArrayList<>(filteredDerivations.size());

            for(Derivation derivation : filteredDerivations) {
                int pivotOffset = startOffset;
                IProduction production1 = derivation.production();

                if(!production1.isContextFree())
                    throw new RuntimeException("non context free imploding not supported");

                List<IStrategoTerm> childASTs = new ArrayList<>();
                List<SubTree> subTrees1 = new ArrayList<>();

                for(ParseForest childParseForest : getChildParseForests(derivation)) {
                    @SuppressWarnings("unchecked") ParseNode childParseNode = (ParseNode) childParseForest;

                    SubTree subTree = implodeParseNode(inputString, childParseNode, pivotOffset);

                    if(subTree.tree != null) {
                        childASTs.add(subTree.tree);
                    }
                    subTrees1.add(subTree);
                    pivotOffset += subTree.width;
                }

                IStrategoTerm tree = createContextFreeTerm(production1, childASTs);
                trees.add(tree);
                subTrees.add(new SubTree(tree, subTrees1, derivation.production()));
            }

            if(subTrees.size() > 1)
                return new SubTree(treeFactory.createAmb(production.sort(), trees), subTrees, null, null,
                        subTrees.get(0).width);
            else
                return subTrees.get(0);
        } else {
            String substring = inputString.substring(startOffset, startOffset + parseNode.width());

            return new SubTree(createLexicalTerm(production, substring), production, substring);
        }
    }

    @Override protected SubTree implodeParseNodeStreams(String inputString, ParseNode parseNode, int startOffset) {
        IProduction production = parseNode.production();

        if(!production.isContextFree()) {
            return createLexicalSubTree(inputString, parseNode, startOffset, production);
        } else {
            List<SubTree> subTrees = applyDisambiguationFilters(parseNode).stream().map(derivation -> {
                int pivotOffset = startOffset;

                if(!derivation.production().isContextFree())
                    throw new RuntimeException("non context free imploding of Derivations not supported");

                List<SubTree> subTrees1 = Iterables2.stream(getChildParseForests(derivation)).map(childParseForest -> {
                    @SuppressWarnings("unchecked") ParseNode childParseNode = (ParseNode) childParseForest;

                    SubTree subTree = implodeParseNode(inputString, childParseNode, pivotOffset);

                    return subTree;
                    // TODO pivotOffset += subTree.width;
                }).collect(Collectors.toList());

                return createNonTerminalSubTree(derivation, subTrees1);
            }).collect(Collectors.toList());

            return createPossiblyAmbiguousSubTree(parseNode, subTrees);
        }
    }
     */
    // @formatter:on

    @Override protected SubTree implodeParseNode(String inputString, ParseNode rootNode, int startOffset) {
        // This stack contains the parse nodes that we still need to process
        Stack<PseudoNode> parseNodeStack = new Stack<>();
        // These stack elements contain: one list for each derivation, and per derivation: one list for all subtrees.
        // The elements on the input stack are processed from the front,
        // after which they are pushed to the back of the elements on the output stack.
        Stack<LinkedList<LinkedList<ParseNode>>> inputStack = new Stack<>();
        Stack<LinkedList<LinkedList<SubTree>>> outputStack = new Stack<>();

        parseNodeStack.add(new PseudoNode(rootNode, applyDisambiguationFilters(rootNode), startOffset));
        inputStack.add(newNestedList(rootNode));
        outputStack.add(newNestedList());

        while(true) {
            PseudoNode pseudoNode = parseNodeStack.peek();
            LinkedList<LinkedList<ParseNode>> currentIn = inputStack.peek();
            LinkedList<LinkedList<SubTree>> currentOut = outputStack.peek();

            if(currentIn.getFirst().isEmpty()) { // If we're finished with the current derivation
                currentIn.removeFirst(); // Remove the current derivation
                if(currentIn.isEmpty()) { // If the stack entry is now empty
                    inputStack.pop(); // That means it's done, so remove it from the stack
                    if(inputStack.isEmpty()) // If it was the last stack node, we're done
                        break;
                    parseNodeStack.pop(); // Also remove the current pseudo node
                    outputStack.pop(); // Also remove `currentOut` from stack

                    SubTree possiblyAmbiguousSubTree = // Merge resulting SubTrees from `currentOut` into one SubTree
                        createPossiblyAmbiguousSubTree(pseudoNode.parseNode, Lists.newArrayList(
                            Iterables2.zip(pseudoNode.derivations, currentOut, this::createNonTerminalSubTree)));
                    outputStack.peek().getLast().add(possiblyAmbiguousSubTree); // And add it to the output
                    parseNodeStack.peek().pivotOffset += possiblyAmbiguousSubTree.width;
                } else { // If the stack entry is not yet empty, that means we're processing an alternate derivation
                    currentOut.add(new LinkedList<>()); // Initialize a new derivation list in the output
                    pseudoNode.pivotOffset = pseudoNode.beginOffset; // And reset the pivotOffset
                }
            } else {
                ParseNode parseNode = currentIn.getFirst().removeFirst(); // Process the next parse node
                IProduction production = parseNode.production();
                if(!production.isContextFree()) { // If the current parse node is a lexical node
                    SubTree lexicalSubTree =
                        createLexicalSubTree(inputString, parseNode, pseudoNode.pivotOffset, production);
                    currentOut.getLast().add(lexicalSubTree); // Add a new SubTree to the output
                    pseudoNode.pivotOffset += lexicalSubTree.width;
                } else { // If the current parse node is a context-free node
                    // Then push it on top of the stacks
                    List<Derivation> derivations = applyDisambiguationFilters(parseNode);
                    inputStack.add(getDerivationLists(derivations));
                    parseNodeStack.add(new PseudoNode(parseNode, derivations, pseudoNode.pivotOffset));
                    outputStack.add(newNestedList());
                }
            }
        }
        return outputStack.peek().getFirst().getFirst();
    }

    private LinkedList<LinkedList<ParseNode>> getDerivationLists(List<Derivation> derivations) {
        LinkedList<LinkedList<ParseNode>> derivationLists = new LinkedList<>();
        for(Derivation derivation : derivations) {
            if(!derivation.production().isContextFree())
                throw new RuntimeException("non context free imploding of Derivations not supported");

            LinkedList<ParseNode> derivationList = new LinkedList<>();
            for(ParseForest childParseForest : getChildParseForests(derivation)) {
                @SuppressWarnings("unchecked") ParseNode childParseNode = (ParseNode) childParseForest;

                derivationList.add(childParseNode);
            }
            derivationLists.add(derivationList);
        }
        return derivationLists;
    }

    private SubTree createPossiblyAmbiguousSubTree(ParseNode parseNode, List<SubTree> subTrees) {
        if(subTrees.size() > 1) {
            return createAmbiguousSubTree(parseNode, subTrees);
        } else
            return subTrees.get(0);
    }

    private SubTree createAmbiguousSubTree(ParseNode parseNode, List<SubTree> subTrees) {
        return new SubTree(
            treeFactory.createAmb(parseNode.production().sort(), subTrees.stream().map(t -> t.tree)::iterator),
            subTrees, null, null, subTrees.get(0).width);
    }

    private SubTree createNonTerminalSubTree(Derivation derivation, List<SubTree> subTrees) {
        return new SubTree(
            createContextFreeTerm(derivation.production(),
                subTrees.stream().filter(t -> t.tree != null).map(t -> t.tree).collect(Collectors.toList())),
            subTrees, derivation.production());
    }

    private SubTree createLexicalSubTree(String inputString, ParseNode parseNode, int startOffset,
        IProduction production) {
        String substring = inputString.substring(startOffset, startOffset + parseNode.width());

        return new SubTree(createLexicalTerm(production, substring), production, substring);
    }

    @Override protected Iterable<ParseForest> getChildParseForests(Derivation derivation) {
        // Can be null in the case of a layout subtree parse node that is not created
        return Iterables.filter(super.getChildParseForests(derivation), Objects::nonNull);
    }

    @SafeVarargs private static <E> LinkedList<LinkedList<E>> newNestedList(E... elements) {
        return new LinkedList<>(singletonList(new LinkedList<>(Arrays.asList(elements))));
    }
}

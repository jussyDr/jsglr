package org.spoofax.jsglr2.imploder;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.putImploderAttachment;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.metaborg.parsetable.IProduction;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.IToken;

public class TermTreeFactory implements ITreeFactory<IStrategoTerm> {

    private final ITermFactory termFactory;

    public TermTreeFactory(ITermFactory termFactory) {
        this.termFactory = termFactory;
    }

    @Override
    public IStrategoTerm createStringTerminal(String sort, String value, IToken leftToken, IToken rightToken) {
        IStrategoTerm stringTerminalTerm = termFactory.makeString(value);

        configure(stringTerminalTerm, sort, leftToken, rightToken);

        return stringTerminalTerm;
    }

    @Override
    public IStrategoTerm createNonTerminal(String sort, String constructor, List<IStrategoTerm> childASTs,
        IToken leftToken, IToken rightToken) {
        IStrategoConstructor constructorTerm =
            termFactory.makeConstructor(constructor != null ? constructor : sort, childASTs.size());
        IStrategoTerm nonTerminalTerm = termFactory.makeAppl(constructorTerm, toArray(childASTs));

        configure(nonTerminalTerm, sort, leftToken, rightToken);

        return nonTerminalTerm;
    }

    @Override
    public IStrategoTerm createList(String sort, List<IStrategoTerm> children, IToken leftToken, IToken rightToken) {
        IStrategoTerm listTerm = termFactory.makeList(toArray(children));

        configure(listTerm, sort, leftToken, rightToken);

        return listTerm;
    }

    @Override
    public IStrategoTerm createOptional(String sort, List<IStrategoTerm> children, IToken leftToken,
        IToken rightToken) {
        String constructor = children == null || children.isEmpty() ? "None" : "Some";

        return createNonTerminal(sort, constructor, children, leftToken, rightToken);
    }

    @Override
    public IStrategoTerm createTuple(String sort, List<IStrategoTerm> children, IToken leftToken, IToken rightToken) {
        IStrategoTerm tupleTerm = termFactory.makeTuple(toArray(children));

        configure(tupleTerm, sort, leftToken, rightToken);

        return tupleTerm;
    }

    @Override
    public IStrategoTerm createAmb(String sort, List<IStrategoTerm> alternatives, IToken leftToken, IToken rightToken) {
        IStrategoTerm alternativesListTerm = createList(null, alternatives, leftToken, rightToken);

        IStrategoTerm ambTerm =
            createNonTerminal(null, "amb", Arrays.asList(alternativesListTerm), leftToken, rightToken);

        return ambTerm;
    }

    @Override
    public IStrategoTerm concatLists(IProduction production, IStrategoTerm leftChild, IStrategoTerm rightChild, IToken leftToken,
            IToken rightToken) {
        final IStrategoTerm term;
        boolean consistent = leftChild instanceof IStrategoList == production.isListLeftChild()
                && rightChild instanceof IStrategoList == production.isListRightChild();
        if (!consistent) {
            term = termFactory.makeAppl(termFactory.makeConstructor("Conc", 2),
                    new IStrategoTerm[] { leftChild, rightChild });
        } else if (leftChild instanceof IStrategoList) {
            if (rightChild instanceof IStrategoList) {
                IStrategoList list = (IStrategoList) rightChild;
                ListIterator<IStrategoTerm> it = Arrays.asList(leftChild.getAllSubterms())
                        .listIterator(leftChild.getSubtermCount());
                while (it.hasPrevious()) {
                    list = termFactory.makeListCons(it.previous(), list);
                }
                term = list;
            } else {
                IStrategoList list = termFactory.makeList(rightChild);
                ListIterator<IStrategoTerm> it = Arrays.asList(leftChild.getAllSubterms())
                        .listIterator(leftChild.getSubtermCount());
                while (it.hasPrevious()) {
                    list = termFactory.makeListCons(it.previous(), list);
                }
                term = list;
            }
        } else {
            if (rightChild instanceof IStrategoList) {
                IStrategoList list = (IStrategoList) rightChild;
                term = termFactory.makeListCons(leftChild, list);
            } else {
                // Not sure if this particular combination actually occurs in practice 
                term = termFactory.makeList(leftChild, rightChild);
            }
        }

        configure(term, production.sort(), leftToken, rightToken);

        return term;
    }

    private static IStrategoTerm[] toArray(List<IStrategoTerm> children) {
        return children.toArray(new IStrategoTerm[children.size()]);
    }

    protected void configure(IStrategoTerm term, String sort, IToken leftToken, IToken rightToken) {
        // rightToken can be null, e.g. for an empty string lexical
        putImploderAttachment(term, false, sort, leftToken, rightToken != null ? rightToken : leftToken, false, false,
            false, false);
    }

}
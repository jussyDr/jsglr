package org.spoofax.jsglr2.imploder;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr2.imploder.treefactory.TokenizedTermTreeFactory;

public class IterativeStrategoTermTokenizer extends IterativeTreeTokenizer<IStrategoTerm> {
    @Override protected void configure(IStrategoTerm term, String sort, IToken leftToken, IToken rightToken) {
        TokenizedTermTreeFactory.configure(term, sort, leftToken, rightToken);
    }

    @Override protected void tokenTreeBinding(IToken token, IStrategoTerm term) {
        token.setAstNode(term);
    }
}

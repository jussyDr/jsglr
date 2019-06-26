package org.spoofax.jsglr2.imploder;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.parseforest.IParseForest;

public class NullStrategoImploder<ParseForest extends IParseForest>
    implements IImploder<ParseForest, TokenizeResult<IStrategoTerm>> {

    @Override public TokenizeResult<IStrategoTerm> implode(String input, String filename, ParseForest forest) {
        return new TokenizeResult<>(null, null);
    }

}

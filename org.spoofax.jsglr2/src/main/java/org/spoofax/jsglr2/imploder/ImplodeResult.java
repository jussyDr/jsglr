package org.spoofax.jsglr2.imploder;

import org.spoofax.jsglr2.JSGLR2Result;
import org.spoofax.jsglr2.parseforest.AbstractParseForest;
import org.spoofax.jsglr2.parser.AbstractParse;

public class ImplodeResult<ParseForest extends AbstractParseForest, AbstractSyntaxTree>
    extends JSGLR2Result<ParseForest, AbstractSyntaxTree> {

    public final AbstractSyntaxTree ast;

    public ImplodeResult(AbstractParse<ParseForest, ?> parse, AbstractSyntaxTree ast) {
        super(parse, true);

        this.ast = ast;
    }

}

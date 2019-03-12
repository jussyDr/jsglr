package org.spoofax.jsglr2;

import org.metaborg.characterclasses.CharacterClassFactory;
import org.metaborg.parsetable.IParseTable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.JSGLR2Variants.ParserVariant;
import org.spoofax.jsglr2.actions.ActionsFactory;
import org.spoofax.jsglr2.imploder.IImploder;
import org.spoofax.jsglr2.imploder.ImplodeResult;
import org.spoofax.jsglr2.parseforest.AbstractParseForest;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.parseforest.hybrid.HybridParseForest;
import org.spoofax.jsglr2.parser.IParser;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.parsetable.ParseTableReadException;
import org.spoofax.jsglr2.parsetable.ParseTableReader;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.collections.ActiveStacksRepresentation;
import org.spoofax.jsglr2.stack.collections.ForActorStacksRepresentation;
import org.spoofax.jsglr2.states.StateFactory;

public class JSGLR2<ParseForest extends AbstractParseForest, AbstractSyntaxTree> {

    public IParser<ParseForest, ?> parser;
    public IImploder<ParseForest, AbstractSyntaxTree> imploder;

    @SuppressWarnings("unchecked") public static JSGLR2<HybridParseForest, IStrategoTerm>
        standard(IParseTable parseTable) throws ParseTableReadException {
        return (JSGLR2<HybridParseForest, IStrategoTerm>) JSGLR2Variants.getJSGLR2(parseTable,
            new ParserVariant(ActiveStacksRepresentation.ArrayList, ForActorStacksRepresentation.ArrayDeque,
                ParseForestRepresentation.Hybrid, ParseForestConstruction.Optimized, StackRepresentation.HybridElkhound,
                Reducing.Elkhound));
    }

    @SuppressWarnings("unchecked") public static JSGLR2<HybridParseForest, IStrategoTerm>
        dataDependent(IParseTable parseTable) throws ParseTableReadException {
        return (JSGLR2<HybridParseForest, IStrategoTerm>) JSGLR2Variants.getJSGLR2(parseTable,
            new ParserVariant(ActiveStacksRepresentation.ArrayList, ForActorStacksRepresentation.ArrayDeque,
                ParseForestRepresentation.DataDependent, ParseForestConstruction.Optimized, StackRepresentation.Basic,
                Reducing.DataDependent));
    }
    
    @SuppressWarnings("unchecked") public static JSGLR2<HybridParseForest, IStrategoTerm>
    layoutSensitive(IParseTable parseTable) throws ParseTableReadException {
    return (JSGLR2<HybridParseForest, IStrategoTerm>) JSGLR2Variants.getJSGLR2(parseTable,
        new ParserVariant(ActiveStacksRepresentation.ArrayList, ForActorStacksRepresentation.ArrayDeque,
            ParseForestRepresentation.LayoutSensitive, ParseForestConstruction.Optimized, StackRepresentation.Basic,
            Reducing.DataDependent));
}

    public static JSGLR2<HybridParseForest, IStrategoTerm> standard(IStrategoTerm parseTableTerm)
        throws ParseTableReadException {
        IParseTable parseTable =
            new ParseTableReader(new CharacterClassFactory(true, true), new ActionsFactory(true), new StateFactory())
                .read(parseTableTerm);

        return standard(parseTable);
    }

    public JSGLR2(IParser<ParseForest, ?> parser, IImploder<ParseForest, AbstractSyntaxTree> imploder) {
        this.parser = parser;
        this.imploder = imploder;
    }

    public AbstractSyntaxTree parse(String input, String filename, String startSymbol) {
        ParseResult<ParseForest, ?> parseResult = parser.parse(input, filename, startSymbol);

        if(!parseResult.isSuccess)
            return null;

        ParseSuccess<ParseForest, ?> parseSuccess = (ParseSuccess<ParseForest, ?>) parseResult;

        ImplodeResult<ParseForest, AbstractSyntaxTree> implodeResult =
            imploder.implode(parseSuccess.parse, parseSuccess.parseResult);

        return implodeResult.ast;
    }

    @SuppressWarnings("unchecked") public JSGLR2Result<ParseForest, AbstractSyntaxTree> parseResult(String input,
        String filename, String startSymbol) {
        ParseResult<ParseForest, ?> parseResult = parser.parse(input, filename, startSymbol);

        if(!parseResult.isSuccess)
            return (JSGLR2Result<ParseForest, AbstractSyntaxTree>) parseResult;

        ParseSuccess<ParseForest, ?> parseSuccess = (ParseSuccess<ParseForest, ?>) parseResult;

        ImplodeResult<ParseForest, AbstractSyntaxTree> implodeResult =
            imploder.implode(parseSuccess.parse, parseSuccess.parseResult);

        return implodeResult;
    }

    public AbstractSyntaxTree parse(String input) {
        return parse(input, "", null);
    }

    public AbstractSyntaxTree parseUnsafe(String input, String filename, String startSymbol) throws ParseException {
        ParseResult<ParseForest, ?> result = parser.parse(input, filename, startSymbol);

        if(result.isSuccess) {
            ParseSuccess<ParseForest, ?> success = (ParseSuccess<ParseForest, ?>) result;

            ImplodeResult<ParseForest, AbstractSyntaxTree> implodeResult =
                imploder.implode(success.parse, success.parseResult);

            return implodeResult.ast;
        } else {
            ParseFailure<ParseForest, ?> failure = (ParseFailure<ParseForest, ?>) result;

            throw failure.exception();
        }
    }

    public AbstractSyntaxTree parseUnsafe(String input) throws ParseException {
        return parseUnsafe(input, "", null);
    }

}

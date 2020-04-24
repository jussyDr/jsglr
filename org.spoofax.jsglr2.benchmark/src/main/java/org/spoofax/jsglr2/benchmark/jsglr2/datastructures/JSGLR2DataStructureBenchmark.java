package org.spoofax.jsglr2.benchmark.jsglr2.datastructures;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.ParseTableReadException;
import org.metaborg.parsetable.ParseTableReader;
import org.openjdk.jmh.annotations.Setup;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.benchmark.BaseBenchmark;
import org.spoofax.jsglr2.inputstack.IInputStack;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.parseforest.basic.IBasicDerivation;
import org.spoofax.jsglr2.parseforest.basic.IBasicParseForest;
import org.spoofax.jsglr2.parseforest.basic.IBasicParseNode;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.IObservableParser;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.parser.ParserVariant;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.basic.BasicStackNode;
import org.spoofax.jsglr2.stack.collections.ActiveStacksRepresentation;
import org.spoofax.jsglr2.stack.collections.ForActorStacksRepresentation;
import org.spoofax.jsglr2.testset.TestSetWithParseTableReader;
import org.spoofax.jsglr2.testset.testinput.StringInput;
import org.spoofax.terms.ParseError;

public abstract class JSGLR2DataStructureBenchmark extends BaseBenchmark<String, StringInput> {

    protected TestSetWithParseTableReader<String, StringInput> testSetReader;

    protected IObservableParser<IBasicParseForest, IBasicDerivation<IBasicParseForest>, IBasicParseNode<IBasicParseForest, IBasicDerivation<IBasicParseForest>>, BasicStackNode<IBasicParseForest>, AbstractParseState<IInputStack, BasicStackNode<IBasicParseForest>>> parser;

    @SuppressWarnings("unchecked") @Setup public void parserSetup() throws ParseError, ParseTableReadException {
        IParseTable parseTable = readParseTable(testSetReader.getParseTableTerm());

        parser =
            (IObservableParser<IBasicParseForest, IBasicDerivation<IBasicParseForest>, IBasicParseNode<IBasicParseForest, IBasicDerivation<IBasicParseForest>>, BasicStackNode<IBasicParseForest>, AbstractParseState<IInputStack, BasicStackNode<IBasicParseForest>>>) new ParserVariant(
                ActiveStacksRepresentation.ArrayList, ForActorStacksRepresentation.ArrayDeque,
                ParseForestRepresentation.Basic, ParseForestConstruction.Full, StackRepresentation.Basic,
                Reducing.Basic, false).getParser(parseTable);

        postParserSetup();

        try {
            for(StringInput input : inputs)
                parser.parseUnsafe(input.content, null);
        } catch(ParseException e) {
            throw new IllegalStateException("setup of benchmark should not fail");
        }
    }

    protected IParseTable readParseTable(IStrategoTerm parseTableTerm) throws ParseTableReadException {
        return new ParseTableReader().read(testSetReader.getParseTableTerm());
    }

    abstract protected void postParserSetup();

}

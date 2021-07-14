package org.spoofax.jsglr2.measure.parsing;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.parsetable.ParseTableReadException;
import org.metaborg.parsetable.ParseTableReader;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.measure.CSV;
import org.spoofax.jsglr2.measure.Config;
import org.spoofax.jsglr2.measure.MeasureTestSetWithParseTableReader;
import org.spoofax.jsglr2.measure.Measurements;
import org.spoofax.jsglr2.parseforest.*;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.IObservableParser;
import org.spoofax.jsglr2.parser.ParserVariant;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.collections.ActiveStacksRepresentation;
import org.spoofax.jsglr2.testset.TestSetWithParseTable;
import org.spoofax.jsglr2.testset.testinput.StringInput;

public class ParsingMeasurements extends Measurements<String, StringInput> {

    public static final ParserVariant variantStandard =
    //@formatter:off
        new ParserVariant(
            ActiveStacksRepresentation.ArrayList,
            ParseForestRepresentation.Hybrid,
            ParseForestConstruction.Full,
            StackRepresentation.Hybrid,
            Reducing.Basic,
            false
        );
        //@formatter:on

    public static final ParserVariant optimizedParseForest =
    //@formatter:off
        new ParserVariant(
            ActiveStacksRepresentation.ArrayList,
            ParseForestRepresentation.Hybrid,
            ParseForestConstruction.Optimized,
            StackRepresentation.Hybrid,
            Reducing.Basic,
            false
        );
        //@formatter:on

    public static final ParserVariant variantElkhound =
    //@formatter:off
        new ParserVariant(
            ActiveStacksRepresentation.ArrayList,
            ParseForestRepresentation.Hybrid,
            ParseForestConstruction.Full,
            StackRepresentation.HybridElkhound,
            Reducing.Elkhound,
            false
        );
    //@formatter:on

    public static final ParserVariant variantIncremental =
    //@formatter:off
        new ParserVariant(
            ActiveStacksRepresentation.ArrayList,
            ParseForestRepresentation.Incremental,
            ParseForestConstruction.Full,
            StackRepresentation.Hybrid,
            Reducing.Incremental,
            false
        );
    //@formatter:on

    public ParsingMeasurements(TestSetWithParseTable<String, StringInput> testSet) {
        super(testSet);
    }

    @Override public void measure(Config<String, StringInput> config) throws ParseTableReadException, IOException {
        CSV<ParsingMeasurement> output = new CSV<>(ParsingMeasurement.values());

        IParseTable parseTable = new ParseTableReader().read(testSetReader.getParseTableTerm());

        //@formatter:off
        output.addRows(measure("standard",     variantStandard,      parseTable, new StandardParserMeasureObserver<>()));
        output.addRows(measure("optimized-pf", optimizedParseForest, parseTable, new StandardParserMeasureObserver<>()));
        output.addRows(measure("elkhound",     variantElkhound,      parseTable, new ElkhoundParserMeasureObserver<>()));
        output.addRows(measure("incremental",  variantIncremental,   parseTable, new StandardParserMeasureObserver<>()));
        //@formatter:on

        output.write(config.prefix(testSet) + "parsing.csv");
    }

    private
//@formatter:off
   <ParseForest extends IParseForest,
    Derivation  extends IDerivation<ParseForest>,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<?, StackNode>>
//@formatter:on
    List<Map<ParsingMeasurement, String>> measure(String name, ParserVariant variant, IParseTable parseTable,
        ParserMeasureObserver<ParseForest, Derivation, ParseNode, StackNode, ParseState> measureObserver)
        throws IOException {
        return testSetReader.getInputBatches().map(inputBatch -> {
            MeasureActiveStacksFactory measureActiveStacksFactory = new MeasureActiveStacksFactory();

            @SuppressWarnings("unchecked") IObservableParser<ParseForest, Derivation, ParseNode, StackNode, ParseState> parser =
                (IObservableParser<ParseForest, Derivation, ParseNode, StackNode, ParseState>) variant
                    .getParser(parseTable, measureActiveStacksFactory);

            parser.observing().attachObserver(measureObserver);

            try {
                for(StringInput input : inputBatch.inputs)
                    parser.parse(new JSGLR2Request(input.content, input.fileName, null), null, null);
            } catch(Exception e) {
                throw new IllegalStateException("Parsing failed with variant " + variant.name() + ": "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            return toOutput(name, inputBatch, measureActiveStacksFactory, measureObserver);
        }).collect(Collectors.toList());
    }

    private
//@formatter:off
   <ParseForest extends IParseForest,
    Derivation  extends IDerivation<ParseForest>,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<?, StackNode>>
//@formatter:on
    Map<ParsingMeasurement, String> toOutput(String name,
        MeasureTestSetWithParseTableReader<String, StringInput>.InputBatch inputBatch,
        MeasureActiveStacksFactory measureActiveStacksFactory,
        ParserMeasureObserver<ParseForest, Derivation, ParseNode, StackNode, ParseState> measureObserver) {
        return Arrays.stream(ParsingMeasurement.values()).collect(Collectors.toMap(Function.identity(), measurement -> {
            switch(measurement) {
                case name:
                    return name;
                case size:
                    return "" + inputBatch.size;
                case characters:
                    return "" + measureObserver.length;
                case activeStacksIsEmptyChecks:
                    return "" + measureActiveStacksFactory.measureActiveStacks.isEmptyChecks;
                case activeStacksIsSingleChecks:
                    return "" + measureActiveStacksFactory.measureActiveStacks.isSingleChecks;
                case activeStacksFindsWithState:
                    return "" + measureActiveStacksFactory.measureActiveStacks.findsWithState;
                case activeStacksAdds:
                    return "" + measureActiveStacksFactory.measureActiveStacks.adds;
                case activeStacksForActorAdds:
                    return "" + measureActiveStacksFactory.measureActiveStacks.forActorAdds;
                case activeStacksForActorDelayedAdds:
                    return "" + measureActiveStacksFactory.measureActiveStacks.forActorDelayedAdds;
                case activeStacksAddAllToForActors:
                    return "" + measureActiveStacksFactory.measureActiveStacks.addAllToForActors;
                case activeStacksMaxSize:
                    return "" + measureActiveStacksFactory.measureActiveStacks.maxSize;
                case activeStacksForActorMaxSize:
                    return "" + measureActiveStacksFactory.measureActiveStacks.forActorMaxSize;
                case activeStacksForActorDelayedMaxSize:
                    return "" + measureActiveStacksFactory.measureActiveStacks.forActorDelayedMaxSize;
                case activeStacksClears:
                    return "" + measureActiveStacksFactory.measureActiveStacks.clears;
                case activeStacksForActorHasNextChecks:
                    return "" + measureActiveStacksFactory.measureActiveStacks.forActorHasNextChecks;
                case activeStacksForLimitedReductions:
                    return "" + measureActiveStacksFactory.measureActiveStacks.forLimitedReductions;
                case activeStacksIterators:
                    return "" + measureActiveStacksFactory.measureActiveStacks.iterators;
                case stackNodes:
                    return "" + measureObserver.stackNodes;
                case stackNodesSingleLink:
                    return "" + measureObserver.stackNodesSingleLink;
                case stackLinks:
                    return "" + measureObserver.stackLinks;
                case stackLinksRejected:
                    return "" + measureObserver.stackLinksRejected;
                case deterministicDepthResets:
                    return "" + measureObserver.deterministicDepthResets;
                case parseNodes:
                    return "" + measureObserver.parseNodes;
                case parseNodesAmbiguous:
                    return "" + measureObserver.parseNodesAmbiguous;
                case parseNodesContextFree:
                    return "" + measureObserver.parseNodesContextFree;
                case parseNodesContextFreeAmbiguous:
                    return "" + measureObserver.parseNodesContextFreeAmbiguous;
                case parseNodesLexical:
                    return "" + measureObserver.parseNodesLexical;
                case parseNodesLexicalAmbiguous:
                    return "" + measureObserver.parseNodesLexicalAmbiguous;
                case parseNodesLayout:
                    return "" + measureObserver.parseNodesLayout;
                case parseNodesLayoutAmbiguous:
                    return "" + measureObserver.parseNodesLayoutAmbiguous;
                case parseNodesLiteral:
                    return "" + measureObserver.parseNodesLiteral;
                case parseNodesLiteralAmbiguous:
                    return "" + measureObserver.parseNodesLiteralAmbiguous;
                case parseNodesSingleDerivation:
                    return "" + measureObserver.parseNodesSingleDerivation;
                case characterNodes:
                    return "" + measureObserver.characterNodes;
                case actors:
                    return "" + measureObserver.actors;
                case doReductions:
                    return "" + measureObserver.doReductions;
                case doLimitedReductions:
                    return "" + measureObserver.doLimitedReductions;
                case doReductionsLR:
                    return "" + measureObserver.doReductionsLR;
                case doReductionsDeterministicGLR:
                    return "" + measureObserver.doReductionsDeterministicGLR;
                case doReductionsNonDeterministicGLR:
                    return "" + measureObserver.doReductionsNonDeterministicGLR;
                case reducers:
                    return "" + measureObserver.reducers;
                case reducersElkhound:
                    return "" + measureObserver.reducersElkhound;
                default:
                    return "";
            }
        }));
    }

}

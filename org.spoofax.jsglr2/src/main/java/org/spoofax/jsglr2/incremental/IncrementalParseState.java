package org.spoofax.jsglr2.incremental;

import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.parser.ParseStateFactory;
import org.spoofax.jsglr2.parser.ParserVariant;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.*;

public class IncrementalParseState<StackNode extends IStackNode>
    extends AbstractParseState<IIncrementalInputStack, StackNode> implements IIncrementalParseState {

    private boolean multipleStates = false;

    public IncrementalParseState(JSGLR2Request request, IIncrementalInputStack inputStack, IStacks<StackNode> stacks) {
        super(request, inputStack, stacks);
    }

    public static <StackNode_ extends IStackNode>
        ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IIncrementalInputStack, StackNode_, IncrementalParseState<StackNode_>>
        factory(ParserVariant variant) {
        //@formatter:off
        return factory(
            new ActiveStacksFactory(variant.activeStacksRepresentation),
            new ForActorStacksFactory(variant.forActorStacksRepresentation)
        );
        //@formatter:on
    }

    public static <StackNode_ extends IStackNode>
        ParseStateFactory<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, IIncrementalInputStack, StackNode_, IncrementalParseState<StackNode_>>
        factory(IActiveStacksFactory activeStacksFactory, IForActorStacksFactory forActorStacksFactory) {
        return (request, inputStack, observing) -> {
            IStacks<StackNode_> stacks = new StacksArrayList<>(observing);

            return new IncrementalParseState<>(request, inputStack, stacks);
        };
    }

    @Override public void nextParseRound(ParserObserving observing) throws ParseException {
        super.nextParseRound(observing);

        setMultipleStates(stacks.isMultiple());
    }

    @Override public boolean newParseNodesAreReusable() {
        return !multipleStates;
    }

    @Override public void setMultipleStates(boolean multipleStates) {
        this.multipleStates = multipleStates;
    }
}

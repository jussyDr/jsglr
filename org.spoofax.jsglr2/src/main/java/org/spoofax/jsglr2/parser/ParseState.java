package org.spoofax.jsglr2.parser;

import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.inputstack.IInputStack;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.*;

public class ParseState<InputStack extends IInputStack, StackNode extends IStackNode>
    extends AbstractParseState<InputStack, StackNode> {

    protected ParseState(JSGLR2Request request, InputStack inputStack, IActiveStacks<StackNode> activeStacks) {
        super(request, inputStack, activeStacks);
    }

    public static
//@formatter:off
   <ParseForest_ extends IParseForest,
    Derivation_  extends IDerivation<ParseForest_>,
    ParseNode_   extends IParseNode<ParseForest_, Derivation_>,
    StackNode_   extends IStackNode,
    InputStack_  extends IInputStack>
//@formatter:on
    ParseStateFactory<ParseForest_, Derivation_, ParseNode_, InputStack_, StackNode_, ParseState<InputStack_, StackNode_>>
        factory(ParserVariant variant) {
        return factory(new ActiveStacksFactory(variant.activeStacksRepresentation));
    }

    public static
//@formatter:off
   <ParseForest_ extends IParseForest,
    Derivation_  extends IDerivation<ParseForest_>,
    ParseNode_   extends IParseNode<ParseForest_, Derivation_>,
    StackNode_   extends IStackNode,
    InputStack_  extends IInputStack>
//@formatter:on
    ParseStateFactory<ParseForest_, Derivation_, ParseNode_, InputStack_, StackNode_, ParseState<InputStack_, StackNode_>>
        factory(IActiveStacksFactory activeStacksFactory) {
        return (request, inputStack, observing) -> {
            IActiveStacks<StackNode_> activeStacks = activeStacksFactory.get(observing);

            return new ParseState<>(request, inputStack, activeStacks);
        };
    }

}

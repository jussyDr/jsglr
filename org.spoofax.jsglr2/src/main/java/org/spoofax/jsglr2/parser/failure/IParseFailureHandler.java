package org.spoofax.jsglr2.parser.failure;

import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.Position;
import org.spoofax.jsglr2.parser.result.ParseFailureCause;
import org.spoofax.jsglr2.stack.IStackNode;

public interface IParseFailureHandler
//@formatter:off
   <ParseForest extends IParseForest,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<?, StackNode>>
//@formatter:on
{

    boolean onFailure(ParseState parseState);

    default ParseFailureCause failureCause(ParseState parseState) {
        Position position = Position.atOffset(parseState.inputStack.inputString(),
            Math.max(Math.min(parseState.inputStack.offset(), parseState.inputStack.length() - 1), 0));

        if(parseState.inputStack.offset() < parseState.inputStack.length())
            return new ParseFailureCause(ParseFailureCause.Type.UnexpectedInput, position);
        else
            return new ParseFailureCause(ParseFailureCause.Type.UnexpectedEOF, position);
    }

}

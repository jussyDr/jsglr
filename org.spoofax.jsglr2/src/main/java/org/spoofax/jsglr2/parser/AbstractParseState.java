package org.spoofax.jsglr2.parser;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

import org.metaborg.parsetable.query.ParsingMode;
import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.JSGLR2Request;
import org.spoofax.jsglr2.inputstack.IInputStack;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;
import org.spoofax.jsglr2.stack.collections.IStacks;

public abstract class AbstractParseState<InputStack extends IInputStack, StackNode extends IStackNode> {

    final public JSGLR2Request request;

    // TODO would be nice if this is final, but resetting a recovery point requires overwriting it...
    public InputStack inputStack;
    public ParsingMode mode;

    public final IStacks<StackNode> stacks;
    public final Queue<ForShifterElement<StackNode>> forShifter = new ArrayDeque<>();

    public StackNode acceptingStack;

    protected AbstractParseState(JSGLR2Request request, InputStack inputStack, IStacks<StackNode> stacks) {
        this.request = request;
        this.inputStack = inputStack;
        this.mode = ParsingMode.Standard;
        this.stacks = stacks;
    }

    public void nextParseRound(ParserObserving observing) throws ParseException {
        observing.notify(observer -> observer.parseRound(this, stacks));
    }

    public Collection<Message> postProcessMessages(Collection<Message> messages, ITokens tokens) {
        return messages;
    }

}

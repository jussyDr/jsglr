package org.spoofax.jsglr2.stack.collections;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;

public abstract class ForActorStacks<ParseForest extends IParseForest, StackNode extends IStackNode>
    implements IForActorStacks<StackNode> {

    private final ParserObserving<ParseForest, StackNode> observing;
    protected final Queue<StackNode> forActorDelayed;

    protected ForActorStacks(ParserObserving<ParseForest, StackNode> observing) {
        this.observing = observing;

        // TODO: implement priority (see P9707 Section 8.4)
        Comparator<StackNode> stackNodePriorityComparator = (StackNode stackNode1, StackNode stackNode2) -> 0;

        this.forActorDelayed = new PriorityQueue<>(stackNodePriorityComparator);
    }

    protected abstract void forActorAdd(StackNode stack);

    protected abstract boolean forActorContains(StackNode stack);

    protected abstract boolean forActorNonEmpty();

    protected abstract StackNode forActorRemove();

    @Override public void add(StackNode stack) {
        observing.notify(observer -> observer.addForActorStack(stack));

        if(stack.state().isRejectable())
            forActorDelayed.add(stack);
        else
            forActorAdd(stack);
    }

    @Override public boolean contains(StackNode stack) {
        return forActorContains(stack) || forActorDelayed.contains(stack);
    }

    @Override public boolean nonEmpty() {
        return forActorNonEmpty() || !forActorDelayed.isEmpty();
    }

    @Override public StackNode remove() {
        // First return all actors in forActor
        if(forActorNonEmpty())
            return forActorRemove();

        // Then return actors from forActorDelayed
        return forActorDelayed.remove();
    }

}

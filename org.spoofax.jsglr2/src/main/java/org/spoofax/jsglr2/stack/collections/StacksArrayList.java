package org.spoofax.jsglr2.stack.collections;

import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class StacksArrayList
//@formatter:off
   <ParseForest extends IParseForest,
    Derivation  extends IDerivation<ParseForest>,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<?, StackNode>>
//@formatter:on
    implements IStacks<StackNode> {

    private final ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing;
    private final List<StackNode> stacks;
    private int forActorSize;
    private int forActorDelayedSize;

    public StacksArrayList(ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing) {
        this.observing = observing;

        stacks = new ArrayList<>();
        forActorSize = 0;
        forActorDelayedSize = 0;
    }

    @Override public boolean isEmpty() {
        return stacks.isEmpty();
    }

    @Override public boolean isSingle() {
        return stacks.size() == 1;
    }

    @Override public boolean isMultiple() {
        return stacks.size() > 1;
    }

    @Override public StackNode getSingle() {
        return stacks.get(0);
    }

    @Override public StackNode findWithState(IState state) {
        observing.notify(observer -> observer.findActiveStackWithState(state));

        for(StackNode stack : stacks)
            if(stack.state().id() == state.id())
                return stack;

        return null;
    }

    @Override public void add(StackNode stack) {
        observing.notify(observer -> observer.addActiveStack(stack));

        stacks.add(stack);
    }

    @Override public void addForActor(StackNode stack) {
        observing.notify(observer -> observer.addForActorStack(stack));

        add(stack);

        Collections.swap(stacks, forActorSize, stacks.size() - 1);

        if(stack.state().isRejectable()) {
            // TODO: implement priority (see P9707 Section 8.4)

            Collections.swap(stacks, forActorDelayedSize, forActorSize);

            forActorDelayedSize++;
        }

        forActorSize++;
    }

    @Override public void addAllForActor() {
        forActorSize = stacks.size();
    }

    @Override public void clear() {
        stacks.clear();
        forActorSize = 0;
        forActorDelayedSize = 0;
    }

    @Override public Iterator<StackNode> forActor() {
        return new Iterator<StackNode>() {

            @Override public boolean hasNext() {
                return forActorSize > 0;
            }

            @Override public StackNode next() {
                if(forActorSize == forActorDelayedSize)
                    forActorDelayedSize--;

                return stacks.get(--forActorSize);
            }

        };
    }

    @Override public Iterable<StackNode> forLimitedReductions() {
        return () -> new Iterator<StackNode>() {

            private final int size = stacks.size();
            private int cursor = forActorSize;

            @Override public boolean hasNext() {
                while(cursor < size && stacks.get(cursor).allLinksRejected())
                    cursor++;

                return cursor < size;
            }

            @Override public StackNode next() {
                return stacks.get(cursor++);
            }

        };
    }

    @Override public Iterable<StackNode> forActorStacks() {
        return () -> new Iterator<StackNode>() {

            private int cursor = 0;

            @Override public boolean hasNext() {
                return cursor < forActorSize;
            }

            @Override public StackNode next() {
                return stacks.get(cursor++);
            }

        };
    }

    @Override public Iterator<StackNode> iterator() {
        return stacks.iterator();
    }

}

package org.spoofax.jsglr2.stack.collections;

import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;

import java.util.*;

/*
 IActiveStacks implementation as a single List consisting of three segments in order:
 for-actor-delayed, for-actor, for-limited-reductions.
 */
public class ActiveStacksArrayList
//@formatter:off
   <ParseForest extends IParseForest,
    Derivation  extends IDerivation<ParseForest>,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<?, StackNode>>
//@formatter:on
    implements IActiveStacks<StackNode> {

    private final ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing;
    protected final List<StackNode> activeStacks;
    protected int forActorSize;
    protected int forActorDelayedSize;

    public ActiveStacksArrayList(ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing) {
        this.observing = observing;

        activeStacks = new ArrayList<>();
        forActorSize = 0;
        forActorDelayedSize = 0;
    }

    @Override public boolean isEmpty() {
        return activeStacks.isEmpty();
    }

    @Override public boolean isSingle() {
        return activeStacks.size() == 1;
    }

    @Override public boolean isMultiple() {
        return activeStacks.size() > 1;
    }

    @Override public StackNode getSingle() {
        return activeStacks.get(0);
    }

    @Override public StackNode findWithState(IState state) {
        observing.notify(observer -> observer.findActiveStackWithState(state));

        for(StackNode stack : activeStacks)
            if(stack.state().id() == state.id())
                return stack;

        return null;
    }

    @Override public void add(StackNode stack) {
        observing.notify(observer -> observer.addActiveStack(stack));

        activeStacks.add(stack);
    }

    /*
     We can prevent array-copies by moving the first element of the for-actor and for-limited-reductions segments
     to the end and inserting the new element in its place.
     */
    @Override public void addForActor(StackNode stack) {
        observing.notify(observer -> observer.addForActorStack(stack));

        activeStacks.add(stack);

        Collections.swap(activeStacks, forActorSize, activeStacks.size() - 1);

        if(stack.state().isRejectable()) {
            Collections.swap(activeStacks, forActorDelayedSize, forActorSize);

            forActorDelayedSize++;
        }

        forActorSize++;
    }

    @Override public void addAllForActor() {
        forActorSize = activeStacks.size();
    }

    @Override public void clear() {
        activeStacks.clear();
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

                return activeStacks.get(--forActorSize);
            }

        };
    }

    @Override public Iterable<StackNode> forLimitedReductions() {
        return new ArrayDeque<>(activeStacks.subList(forActorSize, activeStacks.size()));
    }

    @Override public Iterable<StackNode> forActorStacks() {
        return activeStacks.subList(0, forActorSize);
    }

    @Override public Iterator<StackNode> iterator() {
        return activeStacks.iterator();
    }

}

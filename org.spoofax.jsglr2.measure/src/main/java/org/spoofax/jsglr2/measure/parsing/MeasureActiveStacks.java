package org.spoofax.jsglr2.measure.parsing;

import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.ActiveStacksArrayList;

import java.util.Iterator;

public class MeasureActiveStacks
//@formatter:off
   <ParseForest extends IParseForest,
    Derivation  extends IDerivation<ParseForest>,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<?, StackNode>>
//@formatter:on
    extends ActiveStacksArrayList<ParseForest, Derivation, ParseNode, StackNode, ParseState> {

    long isEmptyChecks = 0, isSingleChecks = 0, findsWithState = 0, adds = 0, forActorAdds = 0, forActorDelayedAdds = 0,
        addAllToForActors = 0, maxSize = 0, forActorMaxSize = 0, forActorDelayedMaxSize = 0, clears = 0,
        forActorHasNextChecks = 0, forLimitedReductions = 0, iterators = 0;

    public MeasureActiveStacks(ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing) {
        super(observing);
    }

    @Override public boolean isEmpty() {
        isEmptyChecks++;

        return super.isEmpty();
    }

    @Override public boolean isSingle() {
        isSingleChecks++;

        return super.isSingle();
    }

    @Override public StackNode findWithState(IState state) {
        findsWithState++;

        return super.findWithState(state);
    }

    @Override public void add(StackNode stack) {
        adds++;

        super.add(stack);

        maxSize = Math.max(maxSize, activeStacks.size());
    }

    @Override public void addForActor(StackNode stack) {
        if(stack.state().isRejectable())
            forActorDelayedAdds++;
        else
            forActorAdds++;

        super.addForActor(stack);

        maxSize = Math.max(maxSize, activeStacks.size());
        forActorMaxSize = Math.max(forActorMaxSize, forActorSize);
        forActorDelayedMaxSize = Math.max(forActorDelayedMaxSize, forActorDelayedSize);
    }

    @Override public void addAllForActor() {
        addAllToForActors++;

        super.addAllForActor();

        forActorMaxSize = Math.max(forActorMaxSize, forActorSize);
    }

    @Override public void clear() {
        clears++;

        super.clear();
    }

    @Override public Iterator<StackNode> forActor() {
        Iterator<StackNode> forActor = super.forActor();

        return new Iterator<StackNode>() {

            @Override public boolean hasNext() {
                forActorHasNextChecks++;

                return forActor.hasNext();
            }

            @Override public StackNode next() {
                return forActor.next();
            }

        };
    }

    @Override public Iterable<StackNode> forLimitedReductions() {
        forLimitedReductions++;

        return super.forLimitedReductions();
    }

    @Override public Iterator<StackNode> iterator() {
        iterators++;

        return super.iterator();
    }

}

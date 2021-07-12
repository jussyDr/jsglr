package org.spoofax.jsglr2.stack.collections;

import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.stack.IStackNode;

import java.util.Iterator;

public interface IStacks<StackNode extends IStackNode> extends Iterable<StackNode> {

    boolean isEmpty();

    boolean isSingle();

    boolean isMultiple();

    StackNode getSingle();

    StackNode findWithState(IState state);

    void add(StackNode stack);

    void addForActor(StackNode stack);

    void addAllForActor();

    void clear();

    Iterator<StackNode> forActor();

    Iterable<StackNode> forLimitedReductions();

    Iterable<StackNode> forActorStacks();

}

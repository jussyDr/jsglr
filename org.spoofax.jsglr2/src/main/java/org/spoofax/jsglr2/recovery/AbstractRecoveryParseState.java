package org.spoofax.jsglr2.recovery;

import java.util.Stack;

import org.metaborg.parsetable.characterclasses.CharacterClassFactory;
import org.spoofax.jsglr2.inputstack.IInputStack;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;

public abstract class AbstractRecoveryParseState
//@formatter:off
   <InputStack           extends IInputStack,
    StackNode            extends IStackNode,
    BacktrackChoicePoint extends IBacktrackChoicePoint<InputStack, StackNode>>
//@formatter:on
    extends AbstractParseState<InputStack, StackNode>
    implements IRecoveryParseState<InputStack, StackNode, BacktrackChoicePoint> {

    Stack<BacktrackChoicePoint> backtrackChoicePoints = new Stack<>();
    private RecoveryJob recoveryJob = null;

    public AbstractRecoveryParseState(InputStack inputStack, IActiveStacks<StackNode> activeStacks,
        IForActorStacks<StackNode> forActorStacks) {
        super(inputStack, activeStacks, forActorStacks);
    }

    @Override public void nextParseRound(ParserObserving observing) {
        super.nextParseRound(observing);

        // Record backtrack choice points per line. If in recovery mode, only record new choice points when parsing
        // after the point that initiated recovery.
        int currentOffset = inputStack.offset();
        if((currentOffset == 0 || CharacterClassFactory.isNewLine(inputStack.getChar(currentOffset - 1)))
            && (!isRecovering() || lastBacktrackChoicePoint().inputStack().offset() < currentOffset)) {
            IBacktrackChoicePoint<?, StackNode> choicePoint = saveBacktrackChoicePoint();

            observing.notify(
                observer -> observer.recoveryBacktrackChoicePoint(backtrackChoicePoints().size() - 1, choicePoint));
        }

        if(successfulRecovery(currentOffset)) {
            endRecovery();

            observing.notify(observer -> observer.endRecovery(this));
        }
    }

    @Override public Stack<BacktrackChoicePoint> backtrackChoicePoints() {
        return backtrackChoicePoints;
    }

    @Override public void startRecovery(int offset) {
        recoveryJob = new RecoveryJob(offset, RecoveryConfig.RECOVERY_ITERATIONS_QUOTA);
    }

    @Override public void endRecovery() {
        recoveryJob = null;
    }

    @Override public RecoveryJob recoveryJob() {
        return recoveryJob;
    }

    @Override public boolean nextRecoveryIteration() {
        if(recoveryJob().hasNextIteration()) {
            int iteration = recoveryJob().nextIteration();

            for(int i = iteration; i > 0 && backtrackChoicePoints.size() > 1; i--)
                backtrackChoicePoints.pop();

            resetToBacktrackChoicePoint(backtrackChoicePoints.peek());

            return true;
        } else
            return false;
    }

    @SuppressWarnings("unchecked") protected void
        resetToBacktrackChoicePoint(BacktrackChoicePoint backtrackChoicePoint) {
        // This cast is ugly, but there's no way around it.
        // The subclasses of `IInputStack` specialize the return type of `clone` to be their own class,
        // but this information cannot be stored in the type parameter `InputStack` of this class.
        // As programmers, we assume that the backtrack choice points contain an input stack of the same type each time.
        this.inputStack = (InputStack) backtrackChoicePoint.inputStack().clone();

        this.activeStacks.clear();

        for(StackNode activeStack : backtrackChoicePoint.activeStacks())
            this.activeStacks.add(activeStack);
    }

}
package org.spoofax.jsglr2.inputstack.incremental;

import static org.spoofax.jsglr2.incremental.parseforest.IncrementalCharacterNode.EOF_NODE;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.spoofax.jsglr2.incremental.EditorUpdate;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalCharacterNode;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalSkippedNode;

public class InlinedEagerIncrementalInputStack extends AbstractInputStack implements IIncrementalInputStack {
    /**
     * The stack contains all subtrees that are yet to be popped. The top of the stack also contains the subtree that
     * has been returned last time. The stack initially only contains EOF and the root, and will already have been
     * broken down as much as required for the list of EditorUpdates.
     */
    protected final Stack<IncrementalParseForest> stack = new Stack<>();

    private final List<EditorUpdate> editorUpdates;
    private int currentUpdateIndex = 0;
    private EditorUpdate currentUpdate = null;
    private boolean updateIsExposed = false;

    private int currentOffsetInPrevious = 0;

    InlinedEagerIncrementalInputStack(InlinedEagerIncrementalInputStack original) {
        super(original.inputString);
        this.currentOffset = original.currentOffset;

        for(IncrementalParseForest node : original.stack) {
            this.stack.push(node);
        }
        this.editorUpdates = original.editorUpdates;
        this.currentUpdateIndex = original.currentUpdateIndex;
        this.currentUpdate = original.currentUpdate;
        this.updateIsExposed = original.updateIsExposed;
        this.currentOffsetInPrevious = original.currentOffsetInPrevious;
    }

    public InlinedEagerIncrementalInputStack(String inputString) {
        super(inputString);
        editorUpdates = Collections.emptyList();

        stack.push(EOF_NODE);
        pushCharactersToStack(inputString);
    }

    private void pushCharactersToStack(String inputString) {
        int[] chars = inputString.codePoints().toArray();
        for(int i = chars.length - 1; i >= 0; i--) {
            stack.push(new IncrementalCharacterNode(chars[i]));
        }
    }

    public InlinedEagerIncrementalInputStack(IncrementalParseForest previousResult, String input,
        List<EditorUpdate> editorUpdates) {
        super(input);
        stack.push(EOF_NODE);
        stack.push(previousResult);

        this.editorUpdates = editorUpdates;

        if(editorUpdates.isEmpty())
            return;

        this.currentUpdate = editorUpdates.get(0);

        // Optimization: if everything is deleted/replaced: then return a tree created from the inserted string
        if(editorUpdates.size() == 1 && currentUpdate.deletedStart == 0
            && currentUpdate.deletedEnd == previousResult.width()) {
            stack.pop();
            pushCharactersToStack(currentUpdate.inserted);
            return;
        }

        checkUpdate();
    }

    @Override public InlinedEagerIncrementalInputStack clone() {
        return new InlinedEagerIncrementalInputStack(this);
    }

    @Override public void breakDown() {
        do {
            if(stack.isEmpty())
                return;

            IncrementalParseForest current = stack.peek();
            if(current.isTerminal())
                return;

            if(current instanceof IncrementalSkippedNode) {
                // Break down a skipped node by explicitly instantiating character nodes for the skipped part
                stack.pop();
                for(int i = currentOffset + current.width(), c; i > currentOffset; i -= Character.charCount(c)) {
                    c = inputString.codePointBefore(i);
                    stack.push(new IncrementalCharacterNode(c));
                }
                return;
            }

            stack.pop(); // always pop last lookahead, whether it has children or not
            IncrementalParseForest[] children = ((IncrementalParseNode) current).getFirstDerivation().parseForests();
            // Push all children to stack in reverse order
            for(int i = children.length - 1; i >= 0; i--) {
                stack.push(children[i]);
            }

            if(updateIsAtStartOfNextNode())
                updateIsExposed = true;
        } while(currentNodeHasChange());
    }

    @Override public void next() {
        IncrementalParseForest popped = stack.pop();
        int increase = popped.width();

        assert isCorrectYield(popped, increase) : "Yield of popped node must be equal to the substring in the input";

        currentOffset += increase;
        currentOffsetInPrevious += increase;

        checkUpdate();
    }

    // This method is only used when assertions are enabled
    private boolean isCorrectYield(IncrementalParseForest popped, int increase) {
        try {
            return popped.getYield().equals(inputString.substring(Integer.min(currentOffset, inputLength),
                Integer.min(currentOffset + increase, inputLength)));
        } catch(UnsupportedOperationException e) {
            if(e.getMessage().equals("Cannot get yield of skipped parse node"))
                return true; // Ignore this exception
            else
                throw e;
        }
    }

    private void checkUpdate() {
        if(currentUpdate != null && currentOffsetInPrevious == currentUpdate.deletedStart) {
            while(currentOffsetInPrevious < currentUpdate.deletedEnd)
                if(currentOffsetInPrevious + stack.peek().width() > currentUpdate.deletedEnd)
                    breakDown();
                else
                    currentOffsetInPrevious += stack.pop().width();
            // Also delete any null-yield trees at position `currentUpdate.deletedEnd`
            while(stack.peek().width() == 0)
                stack.pop();
            currentOffsetInPrevious -= currentUpdate.insertedLength();
            pushCharactersToStack(currentUpdate.inserted);
            currentUpdate = ++currentUpdateIndex >= editorUpdates.size() ? null : editorUpdates.get(currentUpdateIndex);
            updateIsExposed = false;
        }

        if(currentNodeHasChange())
            breakDown();
    }

    private boolean currentNodeHasChange() {
        IncrementalParseForest node = getNode();
        if(node == null || currentUpdate == null)
            return false;

        // Examples: (current node width indicated with [])
        // 0 [1  2 ]3  D  5     => 4 < 1 + 2 + 1 => false
        // 0 [1  2  3 ]D  5     => 4 < 1 + 3 + 1 => true
        // 0 [1  2  D ]4  5     => 3 < 1 + 3 + 1 => true
        // TODO Instead of +1, it would be cleaner to check the follow-restriction length of the production
        return currentUpdate.deletedStart < currentOffsetInPrevious + node.width() + 1;
    }

    private boolean updateIsAtStartOfNextNode() {
        IncrementalParseForest node = getNode();
        if(node == null || currentUpdate == null)
            return false;

        // Examples: (current node width indicated with [])
        // 0 [1  2 ]3  D  5     => 4 == 1 + 2 => false
        // 0 [1  2  3 ]D  5     => 4 == 1 + 3 => true
        // 0 [1  2  D ]4  5     => 3 == 1 + 3 => false
        // TODO If current node has follow-restriction length > 1, the first example should ALSO return true
        return currentUpdate.deletedStart == currentOffsetInPrevious + node.width();
    }

    @Override public IncrementalParseForest getNode() {
        return stack.isEmpty() ? null : stack.peek();
    }

    @Override public boolean lookaheadIsUnchanged() {
        return !updateIsExposed;
    }
}

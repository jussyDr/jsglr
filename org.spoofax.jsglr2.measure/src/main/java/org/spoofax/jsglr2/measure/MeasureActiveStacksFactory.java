package org.spoofax.jsglr2.measure;

import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IActiveStacksFactory;

public class MeasureActiveStacksFactory implements IActiveStacksFactory {

    MeasureActiveStacks<?, ?> measureActiveStacks = null;

    @SuppressWarnings("unchecked") @Override public <ParseForest extends IParseForest, StackNode extends IStackNode>
        IActiveStacks<StackNode> get(ParserObserving<ParseForest, StackNode> observing) {
        if(this.measureActiveStacks == null) {
            MeasureActiveStacks<ParseForest, StackNode> measureActiveStacks = new MeasureActiveStacks<>(observing);

            this.measureActiveStacks = measureActiveStacks;

            return measureActiveStacks;
        } else
            return (MeasureActiveStacks<ParseForest, StackNode>) this.measureActiveStacks;
    }

}

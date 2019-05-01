package org.spoofax.jsglr2.parser.observing;

import java.util.ArrayList;
import java.util.List;

import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.stack.IStackNode;

public class ParserObserving
//@formatter:off
   <ParseForest extends IParseForest,
    StackNode   extends IStackNode>
//@formatter:on
{

    private final List<IParserObserver<ParseForest, StackNode>> observers;

    public ParserObserving() {
        this.observers = new ArrayList<>();
    }

    public void notify(IParserNotification<ParseForest, StackNode> notification) {
        if(observers.isEmpty())
            return;

        for(IParserObserver<ParseForest, StackNode> observer : observers)
            notification.notify(observer);
    }

    public void attachObserver(IParserObserver<ParseForest, StackNode> observer) {
        observers.add(observer);
    }

}

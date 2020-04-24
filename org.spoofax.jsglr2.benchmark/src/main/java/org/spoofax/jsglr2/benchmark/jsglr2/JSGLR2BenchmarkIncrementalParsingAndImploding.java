package org.spoofax.jsglr2.benchmark.jsglr2;

import org.openjdk.jmh.infra.Blackhole;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.testset.testinput.IncrementalStringInput;

public abstract class JSGLR2BenchmarkIncrementalParsingAndImploding extends JSGLR2BenchmarkIncremental {

    @Override protected boolean implode() {
        return true;
    }

    @Override protected Object action(Blackhole bh, IncrementalStringInput input) throws ParseException {
        // TODO
        // correctCache(input);

        if(i >= 0)
            return jsglr2.parseUnsafe(input.content[i], input.fileName, null);

        if(i == -2) {
            for(String content : uniqueInputs.get(input)) {
                bh.consume(jsglr2.parseUnsafe(content, input.fileName, null));
            }
            return null;
        }

        // if (i == -1)
        for(String content : input.content) {
            // TODO
            // possiblyClearCache();
            bh.consume(jsglr2.parseUnsafe(content, input.fileName, null));
        }
        return null;
    }

}

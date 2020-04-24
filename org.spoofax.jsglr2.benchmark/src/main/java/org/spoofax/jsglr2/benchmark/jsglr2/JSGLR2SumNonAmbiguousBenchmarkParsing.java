package org.spoofax.jsglr2.benchmark.jsglr2;

import org.openjdk.jmh.annotations.Param;
import org.spoofax.jsglr2.benchmark.BenchmarkTestSetWithParseTableReader;
import org.spoofax.jsglr2.testset.TestSet;

public class JSGLR2SumNonAmbiguousBenchmarkParsing extends JSGLR2BenchmarkParsing {

    public JSGLR2SumNonAmbiguousBenchmarkParsing() {
        setTestSetReader(new BenchmarkTestSetWithParseTableReader<>(TestSet.sumNonAmbiguous));
    }

    @Param({ "4000", "8000", "16000", "32000", "64000" }) public int n;

}

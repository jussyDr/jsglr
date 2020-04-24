package org.spoofax.jsglr2.benchmark;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.spoofax.jsglr2.testset.TestSet;
import org.spoofax.jsglr2.testset.TestSetReader;
import org.spoofax.jsglr2.testset.testinput.TestInput;

public class BenchmarkTestSetReader<ContentType, Input extends TestInput<ContentType>>
    extends TestSetReader<ContentType, Input> {

    public BenchmarkTestSetReader(TestSet<ContentType, Input> testSet) {
        super(testSet);
    }

    @Override protected String basePath() {
        try {
            URL url = BenchmarkTestSetReader.class.getProtectionDomain().getCodeSource().getLocation();
            String path = url.toURI().getPath();

            return new File(path).getParent() + "/classes/";
        } catch(URISyntaxException e) {
            throw new IllegalStateException("base path for benchmarks could not be retrieved");
        }
    }

}

package org.spoofax.jsglr2.integrationtest.features;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.terms.ParseError;

public class OptionalsTest extends BaseTestWithSdf3ParseTables {

    public OptionalsTest() {
        super("optionals.sdf3");
    }

    @TestFactory public Stream<DynamicTest> testEmpty() throws ParseError {
        return testSuccessByExpansions("", "None");
    }

    @TestFactory public Stream<DynamicTest> testSingleX() throws ParseError {
        return testSuccessByExpansions("X", "Some(X)");
    }

    @TestFactory public Stream<DynamicTest> testIncrementalOptionals() throws ParseError {
        //@formatter:off
        return testIncrementalSuccessByExpansions(
            new String[] { "X",       "",     "X" },
            new String[] { "Some(X)", "None", "Some(X)" }
        );
        //@formatter:off
    }

}
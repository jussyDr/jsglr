package org.spoofax.jsglr2.integrationtest.features;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.terms.ParseError;

public class SdfSyntaxTest extends BaseTestWithSdf3ParseTables {

    public SdfSyntaxTest() {
        super("sdf-syntax.sdf3");
    }

    @TestFactory public Stream<DynamicTest> identifier() throws ParseError {
        return testSuccessByExpansions("x", "\"x\"");
    }

}
package org.spoofax.jsglr2.integrationtest.grammars;

import java.io.IOException;
import org.junit.Test;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.jsglr2.parsetable.ParseTableReadException;
import org.spoofax.terms.ParseError;

public class SumAmbiguousTest extends BaseTestWithSdf3ParseTables {

    public SumAmbiguousTest() {
        super("sum-ambiguous.sdf3");
    }

    @Test
    public void one() throws ParseError, ParseTableReadException, IOException {
        testSuccessByExpansions("x", "Term()");
    }

    @Test
    public void two() throws ParseError, ParseTableReadException, IOException {
        testSuccessByExpansions("x+x", "Add(Term(),Term())");
    }

    @Test
    public void three() throws ParseError, ParseTableReadException, IOException {
        testSuccessByExpansions("x+x+x", "amb([Add(Add(Term,Term),Term), Add(Term,Add(Term,Term))])");
    }

}
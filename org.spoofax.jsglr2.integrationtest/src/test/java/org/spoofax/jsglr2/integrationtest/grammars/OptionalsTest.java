package org.spoofax.jsglr2.integrationtest.grammars;

import java.io.IOException;
import org.junit.Test;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.jsglr2.parsetable.ParseTableReadException;
import org.spoofax.terms.ParseError;

public class OptionalsTest extends BaseTestWithSdf3ParseTables {

    public OptionalsTest(){
        super("optionals.sdf3");
    }

    @Test
    public void testEmpty() throws ParseError, ParseTableReadException, IOException {
        testSuccessByExpansions("", "None");
    }

    @Test
    public void testSingleX() throws ParseError, ParseTableReadException, IOException {
        testSuccessByExpansions("X", "Some(X)");
    }

}
package org.spoofax.jsglr2.integrationtest.features;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.terms.ParseError;

public class LookaheadTest extends BaseTestWithSdf3ParseTables {

    public LookaheadTest() {
        super("lookahead.sdf3");
    }

    @TestFactory public Stream<DynamicTest> oneCharFollowRestricted1() throws ParseError {
        return testSuccessByExpansions("1[x]", "OneCharFollowRestricted(\"1[x]\")");
    }

    @TestFactory public Stream<DynamicTest> oneCharFollowRestricted2() throws ParseError {
        return testParseFailure("1[ax]");
    }

    @TestFactory public Stream<DynamicTest> oneCharFollowRestricted3() throws ParseError {
        return testParseFailure("1[abx]");
    }

    @TestFactory public Stream<DynamicTest> oneCharFollowRestricted4() throws ParseError {
        return testParseFailure("1[abcx]");
    }

    @TestFactory public Stream<DynamicTest> twoCharFollowRestricted1() throws ParseError {
        return testSuccessByExpansions("2[x]", "TwoCharFollowRestricted(\"2[x]\")");
    }

    @TestFactory public Stream<DynamicTest> twoCharFollowRestricted2() throws ParseError {
        return testSuccessByExpansions("2[ax]", "TwoCharFollowRestricted(\"2[ax]\")");
    }

    @TestFactory public Stream<DynamicTest> twoCharFollowRestricted3() throws ParseError {
        return testParseFailure("2[abx]");
    }

    @TestFactory public Stream<DynamicTest> twoCharFollowRestricted4() throws ParseError {
        return testParseFailure("2[abcx]");
    }

    @TestFactory public Stream<DynamicTest> threeCharFollowRestricted1() throws ParseError {
        return testSuccessByExpansions("3[x]", "ThreeCharFollowRestricted(\"3[x]\")");
    }

    @TestFactory public Stream<DynamicTest> threeCharFollowRestricted2() throws ParseError {
        return testSuccessByExpansions("3[ax]", "ThreeCharFollowRestricted(\"3[ax]\")");
    }

    @TestFactory public Stream<DynamicTest> threeCharFollowRestricted3() throws ParseError {
        return testSuccessByExpansions("3[abx]", "ThreeCharFollowRestricted(\"3[abx]\")");
    }

    @TestFactory public Stream<DynamicTest> threeCharFollowRestricted4() throws ParseError {
        return testParseFailure("3[abcx]");
    }

}
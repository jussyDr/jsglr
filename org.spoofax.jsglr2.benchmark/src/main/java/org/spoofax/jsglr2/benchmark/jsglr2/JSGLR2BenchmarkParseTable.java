package org.spoofax.jsglr2.benchmark.jsglr2;

import org.metaborg.sdf2table.parsetable.query.ActionsForCharacterRepresentation;
import org.metaborg.sdf2table.parsetable.query.ProductionToGotoRepresentation;
import org.openjdk.jmh.annotations.Param;
import org.spoofax.jsglr2.JSGLR2Variants.ParseTableVariant;
import org.spoofax.jsglr2.JSGLR2Variants.ParserVariant;
import org.spoofax.jsglr2.JSGLR2Variants.Variant;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.collections.ActiveStacksRepresentation;
import org.spoofax.jsglr2.stack.collections.ForActorStacksRepresentation;
import org.spoofax.jsglr2.testset.TestSet;

public abstract class JSGLR2BenchmarkParseTable extends JSGLR2Benchmark {

    protected JSGLR2BenchmarkParseTable(TestSet testSet) {
        super(testSet);
    }

    @Param({ "false" }) public boolean implode;

    @Param({ "Separated", "DisjointSorted" }) ActionsForCharacterRepresentation actionsForCharacterRepresentation;

    @Param({ "ForLoop", "JavaHashMap" }) ProductionToGotoRepresentation productionToGotoRepresentation;

    @Param({ "ArrayList" }) public ActiveStacksRepresentation activeStacksRepresentation;

    @Param({ "ArrayDeque" }) public ForActorStacksRepresentation forActorStacksRepresentation;

    @Param({ "Basic" }) public ParseForestRepresentation parseForestRepresentation;

    @Param({ "Full" }) public ParseForestConstruction parseForestConstruction;

    @Param({ "Basic" }) public StackRepresentation stackRepresentation;

    @Param({ "Basic" }) public Reducing reducing;

    @Override protected Variant variant() {
        Variant variant =
            new Variant(new ParseTableVariant(actionsForCharacterRepresentation, productionToGotoRepresentation),
                new ParserVariant(activeStacksRepresentation, forActorStacksRepresentation, parseForestRepresentation,
                    parseForestConstruction, stackRepresentation, reducing));
        System.out.println("JSGLR2 PT Var: " + variant.name());
        if(variant.equals(new Variant(new ParseTableVariant(ActionsForCharacterRepresentation.DisjointSorted,
            ProductionToGotoRepresentation.JavaHashMap), naiveParserVariant)))
            throw new IllegalStateException("naive variant is only benchmarked once");
        else
            return variant;
    }

    @Override protected boolean implode() {
        return implode;
    }

}

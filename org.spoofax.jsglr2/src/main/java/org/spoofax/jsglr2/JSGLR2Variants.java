package org.spoofax.jsglr2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.metaborg.parsetable.IParseTable;
import org.metaborg.sdf2table.parsetable.query.ActionsForCharacterRepresentation;
import org.metaborg.sdf2table.parsetable.query.ProductionToGotoRepresentation;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.datadependent.*;
import org.spoofax.jsglr2.elkhound.*;
import org.spoofax.jsglr2.imploder.IImploder;
import org.spoofax.jsglr2.imploder.NullParseForestStrategoImploder;
import org.spoofax.jsglr2.imploder.StrategoTermImploder;
import org.spoofax.jsglr2.layoutsensitive.*;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestManager;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.parseforest.basic.BasicDerivation;
import org.spoofax.jsglr2.parseforest.basic.BasicParseForest;
import org.spoofax.jsglr2.parseforest.basic.BasicParseForestManager;
import org.spoofax.jsglr2.parseforest.basic.BasicParseNode;
import org.spoofax.jsglr2.parseforest.empty.NullParseForestManager;
import org.spoofax.jsglr2.parseforest.hybrid.HybridDerivation;
import org.spoofax.jsglr2.parseforest.hybrid.HybridParseForest;
import org.spoofax.jsglr2.parseforest.hybrid.HybridParseForestManager;
import org.spoofax.jsglr2.parseforest.hybrid.HybridParseNode;
import org.spoofax.jsglr2.parser.IParser;
import org.spoofax.jsglr2.parser.Parse;
import org.spoofax.jsglr2.parser.Parser;
import org.spoofax.jsglr2.reducing.ReduceManager;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.AbstractStackManager;
import org.spoofax.jsglr2.stack.StackNode;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.basic.BasicStackManager;
import org.spoofax.jsglr2.stack.collections.*;
import org.spoofax.jsglr2.stack.hybrid.HybridStackManager;

public class JSGLR2Variants {

    public static class Variant {
        public ParseTableVariant parseTable;
        public ParserVariant parser;

        public Variant(ParseTableVariant parseTableVariant, ParserVariant parserVariant) {
            this.parseTable = parseTableVariant;
            this.parser = parserVariant;
        }

        public String name() {
            return parseTable.name() + "/" + parser.name();
        }

        @Override public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }

            Variant that = (Variant) o;

            return parseTable.equals(that.parseTable) && parser.equals(that.parser);
        }
    }

    public static class ParseTableVariant {
        public ActionsForCharacterRepresentation actionsForCharacterRepresentation;
        public ProductionToGotoRepresentation productionToGotoRepresentation;

        public ParseTableVariant(ActionsForCharacterRepresentation actionsForCharacterRepresentation,
            ProductionToGotoRepresentation productionToGotoRepresentation) {
            this.actionsForCharacterRepresentation = actionsForCharacterRepresentation;
            this.productionToGotoRepresentation = productionToGotoRepresentation;
        }

        public String name() {
            return "ActionsForCharacterRepresentation:" + actionsForCharacterRepresentation
                + "/ProductionToGotoRepresentation:" + productionToGotoRepresentation;
        }

        @Override public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }

            ParseTableVariant that = (ParseTableVariant) o;

            return actionsForCharacterRepresentation == that.actionsForCharacterRepresentation
                && productionToGotoRepresentation == that.productionToGotoRepresentation;
        }
    }

    public static class ParserVariant {
        public ActiveStacksRepresentation activeStacksRepresentation;
        public ForActorStacksRepresentation forActorStacksRepresentation;
        public ParseForestRepresentation parseForestRepresentation;
        public ParseForestConstruction parseForestConstruction;
        public StackRepresentation stackRepresentation;
        public Reducing reducing;

        public ParserVariant(ActiveStacksRepresentation activeStacksRepresentation,
            ForActorStacksRepresentation forActorStacksRepresentation,
            ParseForestRepresentation parseForestRepresentation, ParseForestConstruction parseForestConstruction,
            StackRepresentation stackRepresentation, Reducing reducing) {
            this.activeStacksRepresentation = activeStacksRepresentation;
            this.forActorStacksRepresentation = forActorStacksRepresentation;
            this.parseForestRepresentation = parseForestRepresentation;
            this.parseForestConstruction = parseForestConstruction;
            this.stackRepresentation = stackRepresentation;
            this.reducing = reducing;
        }

        public boolean isValid() {
            boolean validElkhound =
                reducing != Reducing.Elkhound || (stackRepresentation == StackRepresentation.BasicElkhound
                    || stackRepresentation == StackRepresentation.HybridElkhound);
            boolean validParseForest = parseForestRepresentation != ParseForestRepresentation.Null
                || parseForestConstruction == ParseForestConstruction.Full;

            return validElkhound && validParseForest;
        }

        public String name() {
            return "ActiveStacksRepresentation:" + activeStacksRepresentation + "/ForActorStacksRepresentation:"
                + forActorStacksRepresentation + "/ParseForestRepresentation:" + parseForestRepresentation
                + "/ParseForestConstruction:" + parseForestConstruction + "/StackRepresentation:" + stackRepresentation
                + "/Reducing:" + reducing;
        }

        @Override public boolean equals(Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }

            ParserVariant that = (ParserVariant) o;

            return activeStacksRepresentation == that.activeStacksRepresentation
                && forActorStacksRepresentation == that.forActorStacksRepresentation
                && parseForestRepresentation == that.parseForestRepresentation
                && parseForestConstruction == that.parseForestConstruction
                && stackRepresentation == that.stackRepresentation && reducing == that.reducing;
        }
    }

    public static List<ParserVariant> allVariants() {
        List<ParserVariant> variants = new ArrayList<>();

        for(ActiveStacksRepresentation activeStacksRepresentation : ActiveStacksRepresentation.values()) {
            for(ForActorStacksRepresentation forActorStacksRepresentation : ForActorStacksRepresentation.values()) {
                for(ParseForestRepresentation parseForestRepresentation : ParseForestRepresentation.values()) {
                    if(parseForestRepresentation != ParseForestRepresentation.Null)
                        for(ParseForestConstruction parseForestConstruction : ParseForestConstruction.values()) {
                            for(StackRepresentation stackRepresentation : StackRepresentation.values()) {
                                for(Reducing reducing : Reducing.values()) {
                                    ParserVariant variant = new ParserVariant(activeStacksRepresentation,
                                        forActorStacksRepresentation, parseForestRepresentation,
                                        parseForestConstruction, stackRepresentation, reducing);

                                    if(variant.isValid())
                                        variants.add(variant);
                                }
                            }
                        }
                }
            }
        }

        return variants;
    }

    public static List<Variant> testVariants() {
        //@formatter:off
        return Arrays.asList(
            new Variant(new ParseTableVariant(ActionsForCharacterRepresentation.Separated,      ProductionToGotoRepresentation.ForLoop),     new ParserVariant(ActiveStacksRepresentation.ArrayList,     ForActorStacksRepresentation.ArrayDeque,    ParseForestRepresentation.Basic,  ParseForestConstruction.Full,      StackRepresentation.Basic,          Reducing.Basic)),
            new Variant(new ParseTableVariant(ActionsForCharacterRepresentation.DisjointSorted, ProductionToGotoRepresentation.JavaHashMap), new ParserVariant(ActiveStacksRepresentation.ArrayList,     ForActorStacksRepresentation.ArrayDeque,    ParseForestRepresentation.Basic,  ParseForestConstruction.Full,      StackRepresentation.Basic,          Reducing.Basic))/*,
            new Variant(new ParseTableVariant(ActionsForCharacterRepresentation.Separated,      ProductionToGotoRepresentation.ForLoop),     new ParserVariant(ActiveStacksRepresentation.LinkedHashMap, ForActorStacksRepresentation.LinkedHashMap, ParseForestRepresentation.Hybrid, ParseForestConstruction.Optimized, StackRepresentation.Hybrid,         Reducing.Basic)),
            new Variant(new ParseTableVariant(ActionsForCharacterRepresentation.Separated,      ProductionToGotoRepresentation.ForLoop),     new ParserVariant(ActiveStacksRepresentation.LinkedHashMap, ForActorStacksRepresentation.LinkedHashMap, ParseForestRepresentation.Hybrid, ParseForestConstruction.Optimized, StackRepresentation.HybridElkhound, Reducing.Elkhound))*/
        );
        //@formatter:on
    }

    public static Parser<?, ?, ?, ?, ?> getParser(IParseTable parseTable, ParserVariant variant) {
        IActiveStacksFactory activeStacksFactory = new ActiveStacksFactory(variant.activeStacksRepresentation);
        IForActorStacksFactory forActorStacksFactory = new ForActorStacksFactory(variant.forActorStacksRepresentation);

        return getParser(parseTable, activeStacksFactory, forActorStacksFactory, variant);
    }


    public static Parser<?, ?, ?, ?, ?> getParser(IParseTable parseTable, IActiveStacksFactory activeStacksFactory,
        IForActorStacksFactory forActorStacksFactory, ParserVariant variant) {
        if(!variant.isValid())
            throw new IllegalStateException("Invalid parser variant");

        switch(variant.parseForestRepresentation) {
            default:
            case Basic:
                BasicParseForestManager basicParseForestManager = new BasicParseForestManager();

                if(variant.reducing == Reducing.Elkhound) {
                    if(variant.stackRepresentation == StackRepresentation.HybridElkhound) {
                        ElkhoundStackManager<BasicParseForest, HybridElkhoundStackNode<BasicParseForest>> elkhoundStackManager =
                            new HybridElkhoundStackManager<>();

                        ElkhoundReduceManager<BasicParseForest, BasicParseNode, BasicDerivation, HybridElkhoundStackNode<BasicParseForest>> elkhoundReducer =
                            new ElkhoundReduceManager<>(parseTable, elkhoundStackManager, basicParseForestManager,
                                variant.parseForestConstruction);

                        return new ElkhoundParser<>(Parse.factory(), parseTable, activeStacksFactory,
                            forActorStacksFactory, elkhoundStackManager, basicParseForestManager, elkhoundReducer);
                    } else {
                        ElkhoundStackManager<BasicParseForest, BasicElkhoundStackNode<BasicParseForest>> elkhoundStackManager =
                            new BasicElkhoundStackManager<>();

                        ElkhoundReduceManager<BasicParseForest, BasicParseNode, BasicDerivation, BasicElkhoundStackNode<BasicParseForest>> elkhoundReducer =
                            new ElkhoundReduceManager<>(parseTable, elkhoundStackManager, basicParseForestManager,
                                variant.parseForestConstruction);

                        return new ElkhoundParser<>(Parse.factory(), parseTable, activeStacksFactory,
                            forActorStacksFactory, elkhoundStackManager, basicParseForestManager, elkhoundReducer);
                    }
                } else {
                    if(variant.stackRepresentation == StackRepresentation.Basic
                        || variant.stackRepresentation == StackRepresentation.Hybrid) {
                        AbstractStackManager<BasicParseForest, StackNode<BasicParseForest>> basicStackManager;

                        if(variant.stackRepresentation == StackRepresentation.Basic)
                            basicStackManager = new BasicStackManager<>();
                        else
                            basicStackManager = new HybridStackManager<>();

                        ReduceManager<BasicParseForest, BasicParseNode, BasicDerivation, StackNode<BasicParseForest>> basicReducer =
                            new ReduceManager<>(parseTable, basicStackManager, basicParseForestManager,
                                variant.parseForestConstruction);

                        return new Parser<>(Parse.factory(), parseTable, activeStacksFactory, forActorStacksFactory,
                            basicStackManager, basicParseForestManager, basicReducer);
                    } else {
                        if(variant.stackRepresentation == StackRepresentation.HybridElkhound) {
                            ElkhoundStackManager<BasicParseForest, HybridElkhoundStackNode<BasicParseForest>> elkhoundStackManager =
                                new HybridElkhoundStackManager<>();

                            ReduceManager<BasicParseForest, BasicParseNode, BasicDerivation, HybridElkhoundStackNode<BasicParseForest>> basicReducer =
                                new ReduceManager<>(parseTable, elkhoundStackManager, basicParseForestManager,
                                    variant.parseForestConstruction);

                            return new Parser<>(Parse.factory(), parseTable, activeStacksFactory, forActorStacksFactory,
                                elkhoundStackManager, basicParseForestManager, basicReducer);
                        } else {
                            ElkhoundStackManager<BasicParseForest, BasicElkhoundStackNode<BasicParseForest>> elkhoundStackManager =
                                new BasicElkhoundStackManager<>();

                            ReduceManager<BasicParseForest, BasicParseNode, BasicDerivation, BasicElkhoundStackNode<BasicParseForest>> basicReducer =
                                new ReduceManager<>(parseTable, elkhoundStackManager, basicParseForestManager,
                                    variant.parseForestConstruction);

                            return new Parser<>(Parse.factory(), parseTable, activeStacksFactory, forActorStacksFactory,
                                elkhoundStackManager, basicParseForestManager, basicReducer);
                        }
                    }
                }
            case Null:
            case Hybrid:
                ParseForestManager<HybridParseForest, HybridParseNode, HybridDerivation> hybridParseForestManager;

                if(variant.parseForestRepresentation == ParseForestRepresentation.Null)
                    hybridParseForestManager = new NullParseForestManager();
                else
                    hybridParseForestManager = new HybridParseForestManager();

                if(variant.reducing == Reducing.Elkhound) {
                    if(variant.stackRepresentation == StackRepresentation.HybridElkhound) {
                        ElkhoundStackManager<HybridParseForest, HybridElkhoundStackNode<HybridParseForest>> elkhoundStackManager =
                            new HybridElkhoundStackManager<>();

                        ElkhoundReduceManager<HybridParseForest, HybridParseNode, HybridDerivation, HybridElkhoundStackNode<HybridParseForest>> elkhoundReducer =
                            new ElkhoundReduceManager<>(parseTable, elkhoundStackManager, hybridParseForestManager,
                                variant.parseForestConstruction);

                        return new ElkhoundParser<>(Parse.factory(), parseTable, activeStacksFactory,
                            forActorStacksFactory, elkhoundStackManager, hybridParseForestManager, elkhoundReducer);
                    } else {
                        ElkhoundStackManager<HybridParseForest, BasicElkhoundStackNode<HybridParseForest>> elkhoundStackManager =
                            new BasicElkhoundStackManager<>();

                        ElkhoundReduceManager<HybridParseForest, HybridParseNode, HybridDerivation, BasicElkhoundStackNode<HybridParseForest>> elkhoundReducer =
                            new ElkhoundReduceManager<>(parseTable, elkhoundStackManager, hybridParseForestManager,
                                variant.parseForestConstruction);

                        return new ElkhoundParser<>(Parse.factory(), parseTable, activeStacksFactory,
                            forActorStacksFactory, elkhoundStackManager, hybridParseForestManager, elkhoundReducer);
                    }
                } else {
                    if(variant.stackRepresentation == StackRepresentation.Basic
                        || variant.stackRepresentation == StackRepresentation.Hybrid) {
                        AbstractStackManager<HybridParseForest, StackNode<HybridParseForest>> basicStackManager;

                        if(variant.stackRepresentation == StackRepresentation.Basic)
                            basicStackManager = new BasicStackManager<>();
                        else
                            basicStackManager = new HybridStackManager<>();

                        ReduceManager<HybridParseForest, HybridParseNode, HybridDerivation, StackNode<HybridParseForest>> hybridReducer =
                            new ReduceManager<>(parseTable, basicStackManager, hybridParseForestManager,
                                variant.parseForestConstruction);

                        return new Parser<>(Parse.factory(), parseTable, activeStacksFactory, forActorStacksFactory,
                            basicStackManager, hybridParseForestManager, hybridReducer);
                    } else {
                        if(variant.stackRepresentation == StackRepresentation.HybridElkhound) {
                            ElkhoundStackManager<HybridParseForest, HybridElkhoundStackNode<HybridParseForest>> elkhoundStackManager =
                                new HybridElkhoundStackManager<>();

                            ReduceManager<HybridParseForest, HybridParseNode, HybridDerivation, HybridElkhoundStackNode<HybridParseForest>> basicReducer =
                                new ReduceManager<>(parseTable, elkhoundStackManager, hybridParseForestManager,
                                    variant.parseForestConstruction);

                            return new Parser<>(Parse.factory(), parseTable, activeStacksFactory, forActorStacksFactory,
                                elkhoundStackManager, hybridParseForestManager, basicReducer);
                        } else {
                            ElkhoundStackManager<HybridParseForest, BasicElkhoundStackNode<HybridParseForest>> elkhoundStackManager =
                                new BasicElkhoundStackManager<>();

                            ReduceManager<HybridParseForest, HybridParseNode, HybridDerivation, BasicElkhoundStackNode<HybridParseForest>> basicReducer =
                                new ReduceManager<>(parseTable, elkhoundStackManager, hybridParseForestManager,
                                    variant.parseForestConstruction);

                            return new Parser<>(Parse.factory(), parseTable, activeStacksFactory, forActorStacksFactory,
                                elkhoundStackManager, hybridParseForestManager, basicReducer);
                        }
                    }
                }
            case DataDependent:
                AbstractStackManager<DataDependentParseForest, StackNode<DataDependentParseForest>> basicStackManager;
                DataDependentParseForestManager ddParseForestManager = new DataDependentParseForestManager();

                if(variant.stackRepresentation == StackRepresentation.Basic)
                    basicStackManager = new BasicStackManager<>();
                else
                    basicStackManager = new HybridStackManager<>();

                DataDependentReduceManager<DataDependentParseForest, DataDependentParseNode, DataDependentDerivation, StackNode<DataDependentParseForest>> ddReducer =
                    new DataDependentReduceManager<>(parseTable, basicStackManager, ddParseForestManager,
                        variant.parseForestConstruction);

                return new Parser<>(Parse.factory(), parseTable, activeStacksFactory, forActorStacksFactory,
                    basicStackManager, ddParseForestManager, ddReducer);

            case LayoutSensitive:
                AbstractStackManager<LayoutSensitiveParseForest, StackNode<LayoutSensitiveParseForest>> basicStackManagerLayoutSensitive;
                LayoutSensitiveParseForestManager lsParseForestManager = new LayoutSensitiveParseForestManager();

                if(variant.stackRepresentation == StackRepresentation.Basic)
                    basicStackManagerLayoutSensitive = new BasicStackManager<>();
                else
                    basicStackManagerLayoutSensitive = new HybridStackManager<>();

                LayoutSensitiveReduceManager<LayoutSensitiveParseForest, LayoutSensitiveParseNode, LayoutSensitiveDerivation, StackNode<LayoutSensitiveParseForest>> lsReducer =
                    new LayoutSensitiveReduceManager<>(parseTable, basicStackManagerLayoutSensitive,
                        lsParseForestManager, variant.parseForestConstruction);

                return new Parser<>(Parse.factory(), parseTable, activeStacksFactory, forActorStacksFactory,
                    basicStackManagerLayoutSensitive, lsParseForestManager, lsReducer);
        }
    }

    public static List<Parser<?, ?, ?, ?, ?>> allParsers(IParseTable parseTable) {
        List<Parser<?, ?, ?, ?, ?>> parsers = new ArrayList<>();

        for(ParserVariant variant : allVariants()) {
            Parser<?, ?, ?, ?, ?> parser = getParser(parseTable,
                new ParserVariant(variant.activeStacksRepresentation, variant.forActorStacksRepresentation,
                    variant.parseForestRepresentation, variant.parseForestConstruction, variant.stackRepresentation,
                    variant.reducing));

            parsers.add(parser);
        }

        return parsers;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) public static JSGLR2<?, IStrategoTerm>
        getJSGLR2(IParseTable parseTable, ParserVariant variant) {
        IParser<?, ?> parser = getParser(parseTable, variant);
        IImploder<?, IStrategoTerm> imploder;

        if(variant.parseForestRepresentation == ParseForestRepresentation.Null)
            imploder = new NullParseForestStrategoImploder();
        else
            imploder = new StrategoTermImploder();

        return new JSGLR2(parser, imploder);
    }

    public static List<JSGLR2<?, IStrategoTerm>> allJSGLR2(IParseTable parseTable) {
        List<JSGLR2<?, IStrategoTerm>> jsglr2s = new ArrayList<>();

        for(ParserVariant variant : allVariants()) {
            JSGLR2<?, IStrategoTerm> jsglr2 = getJSGLR2(parseTable, variant);

            jsglr2s.add(jsglr2);
        }

        return jsglr2s;
    }

}

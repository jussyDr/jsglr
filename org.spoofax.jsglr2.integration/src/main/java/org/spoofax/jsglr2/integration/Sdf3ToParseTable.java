package org.spoofax.jsglr2.integration;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.project.ISimpleProjectService;
import org.metaborg.core.project.SimpleProjectService;
import org.metaborg.meta.core.project.ILanguageSpec;
import org.metaborg.parsetable.IParseTable;
import org.metaborg.sdf2table.grammar.NormGrammar;
import org.metaborg.sdf2table.io.NormGrammarReader;
import org.metaborg.sdf2table.io.ParseTableIO;
import org.metaborg.sdf2table.parsetable.ParseTable;
import org.metaborg.sdf2table.parsetable.ParseTableConfiguration;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.meta.core.SpoofaxExtensionModule;
import org.metaborg.spoofax.meta.core.SpoofaxMeta;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.iterators.Iterables2;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.HybridInterpreter;

import com.google.common.collect.Iterables;
import com.google.inject.Singleton;

public class Sdf3ToParseTable {

    public static class SpoofaxSimpleProjectModule extends SpoofaxModule {
        @Override protected void bindProject() {
            bind(SimpleProjectService.class).in(Singleton.class);
            bind(IProjectService.class).to(SimpleProjectService.class);
        }

        @Override protected void bindEditor() {
            bind(IEditorRegistry.class).to(NullEditorRegistry.class).in(Singleton.class);
        }
    }

    private final Spoofax spoofax;

    private final ILanguageImpl sdf3Impl;
    private final ILanguageComponent sdf3Component;

    private final IContext context;

    private final Function<String, String> getResourcePath;

    public Sdf3ToParseTable(Function<String, String> getResourcePath) throws MetaborgException {
        this.getResourcePath = getResourcePath;

        spoofax = new Spoofax(new SpoofaxSimpleProjectModule(), new SpoofaxExtensionModule());
        SpoofaxMeta spoofaxMeta = new SpoofaxMeta(spoofax);

        final FileObject sdf3Location = spoofax.resolve("zip:" + getResourcePath("sdf3.spoofax-language"));

        final Set<ILanguageImpl> languageImpls = spoofax.scanLanguagesInDirectory(sdf3Location);

        sdf3Impl = Iterables.get(languageImpls, 0);
        sdf3Component = Iterables.get(sdf3Impl.components(), 0);

        final FileObject testDirectory = spoofax.resourceService.resolve(getResourcePath("grammars"));
        final IProject testProject = ((ISimpleProjectService) spoofax.projectService).create(testDirectory);
        final ILanguageSpec testLanguageSpec = spoofaxMeta.languageSpecService.get(testProject);

        context = spoofax.contextService.get(testDirectory, testLanguageSpec, sdf3Impl);

        spoofaxMeta.close();
    }

    public IParseTable getParseTable(ParseTableVariant variant, String sdf3Resource) throws Exception {
        return getParseTable(variant, sdf3Resource, false);
    }

    public IParseTable getParseTable(ParseTableVariant variant, String sdf3Resource, boolean permissive)
        throws Exception {
        NormGrammar normalizedGrammar = normalizedGrammarFromSDF3("grammars/" + sdf3Resource, permissive);

        // TODO: use the parse table variant in the parse table generator
        return new ParseTable(normalizedGrammar, new ParseTableConfiguration(false, false, true, false, false, false));
    }

    public IParseTable getLayoutSensitiveParseTable(ParseTableVariant variant, String sdf3Resource) throws Exception {
        NormGrammar normalizedGrammar = normalizedGrammarFromSDF3("grammars/" + sdf3Resource, false);

        // TODO: use the parse table variant in the parse table generator
        return new ParseTable(normalizedGrammar, new ParseTableConfiguration(false, false, false, false, false, true));
    }

    public IStrategoTerm getParseTableTerm(String sdf3Resource) throws Exception {
        return getParseTableTerm(sdf3Resource, false);
    }

    public IStrategoTerm getParseTableTerm(String sdf3Resource, boolean permissive) throws Exception {
        NormGrammar normalizedGrammar = normalizedGrammarFromSDF3("grammars/" + sdf3Resource, permissive);

        ParseTable parseTable =
            new ParseTable(normalizedGrammar, new ParseTableConfiguration(false, false, true, false, false, false));

        return ParseTableIO.generateATerm(parseTable);
    }

    private NormGrammar normalizedGrammarFromSDF3(String sdf3Resource, boolean permissive) throws Exception {
        final FileObject sdf3File = spoofax.resourceService.resolve(getResourcePath(sdf3Resource));
        final String sdf3Text = spoofax.sourceTextService.text(sdf3File);

        ISpoofaxInputUnit inputUnit = spoofax.unitService.inputUnit(sdf3File, sdf3Text, sdf3Impl, null);

        final ISpoofaxParseUnit parseResult = spoofax.syntaxService.parse(inputUnit);
        if(!parseResult.success())
            throw new RuntimeException("Parsing of " + sdf3Resource + " failed: "
                + Iterables2.stream(parseResult.messages()).map(IMessage::message).collect(Collectors.joining(", ")));

        final IStrategoTerm sdf3Module = parseResult.ast();
        final IStrategoTerm sdf3ModuleNormalized = normalize(permissive ? makePermissive(sdf3Module) : sdf3Module);

        return new NormGrammarReader().readGrammar(sdf3ModuleNormalized);
    }

    private IStrategoTerm normalize(IStrategoTerm sdf3Module) throws MetaborgException {
        return executeStratego(sdf3Module, "module-to-normal-form");
    }

    private IStrategoTerm makePermissive(IStrategoTerm sdf3Module) throws MetaborgException {
        return executeStratego(sdf3Module, "module-to-permissive");
    }

    private IStrategoTerm executeStratego(IStrategoTerm input, String strategy) throws MetaborgException {
        try(IClosableLock ignored = context.read()) {
            final HybridInterpreter runtime = spoofax.strategoRuntimeService.runtime(sdf3Component, context);

            return spoofax.strategoCommon.invoke(runtime, input, strategy);
        }
    }

    private String getResourcePath(String resource) {
        return getResourcePath.apply(resource);
    }

}

/*
 * Created on 04.des.2005
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk near strategoxt.org>
 *
 * Licensed under the GNU Lesser General Public License, v2.1
 */
package org.spoofax.jsglr.client;

import static java.util.Arrays.asList;
import static org.spoofax.interpreter.terms.IStrategoTerm.APPL;
import static org.spoofax.interpreter.terms.IStrategoTerm.LIST;
import static org.spoofax.terms.Term.intAt;
import static org.spoofax.terms.Term.isTermInt;
import static org.spoofax.terms.Term.javaInt;
import static org.spoofax.terms.Term.termAt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.sdf2table.grammar.CharacterClass;
import org.metaborg.sdf2table.io.IncrementalParseTableGenerator;
import org.metaborg.sdf2table.io.ParseTableIO;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoNamed;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.TreeBuilder;
import org.spoofax.jsglr.io.ParseTableManager;
import org.spoofax.jsglr.io.SGLR;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.terms.ParseError;
import org.spoofax.terms.Term;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.util.NotImplementedException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A parse table.
 * 
 * Can (should!) be shared by multiple parser instances.
 */
public class ParseTable implements Serializable {

    /**
     * Number of possible characters to expect (0x10FFFF would be all chars of UTF-8, but is not yet supported by the
     * parse table format.)
     */
    public static final int NUM_CHARS = 256;
    public static final int LABEL_BASE = NUM_CHARS + 1;

    private static final long serialVersionUID = -3372429249660900093L;

    private final ParseTableIO pt_generator;

    private static SGLR layoutParser;

    private State[] states;

    private Map<org.metaborg.sdf2table.parsetable.State, State> states_cache = Maps.newHashMap();

    private int startState;

    private Label[] labels;

    private Priority[] priorities;

    private Associativity[] associativities;

    private boolean hasRejects;

    private boolean hasAvoids;

    private boolean hasPrefers;

    private boolean hasRecovers;

    transient private ITermFactory factory;

    transient public IStrategoConstructor applIStrategoConstructor;

    transient public IStrategoConstructor ambIStrategoConstructor;

    private Label[] injections;

    // TODO: allocate prototypes to avoid measurable GC overhead in ParseTable construction
    // (especially when using the CMS garbage collector, those gotos and stuff
    // introduce a lot of overhead)

    private transient HashMap<Goto, Goto> gotoCache = new HashMap<Goto, Goto>();

    private transient HashMap<Shift, Shift> shiftCache = new HashMap<Shift, Shift>();

    private transient HashMap<Reduce, Reduce> reduceCache = new HashMap<Reduce, Reduce>();

    private transient HashMap<RangeList, RangeList> rangesCache = new HashMap<RangeList, RangeList>();

    private transient Map<Label, List<Priority>> priorityCache;

    private transient IKeywordRecognizer keywords;

    private boolean dynamicPTgeneration = false;

    public ParseTable(IStrategoTerm pt, ITermFactory factory) throws InvalidParseTableException {
        initTransientData(factory);
        parse(pt);
        pt_generator = null;

        if(states.length == 0) {
            throw new InvalidParseTableException(
                "Parse table does not contain any state and normalized grammar is null");
        }
    }

    public ParseTable(IStrategoTerm pt, ITermFactory factory, FileObject persistedTable) throws Exception {
        initTransientData(factory);
        parse(pt);
        if(states.length == 0) {
            dynamicPTgeneration = true;
        }

        if(dynamicPTgeneration && persistedTable != null) {
            pt_generator = new ParseTableIO(persistedTable);
            gotoCache = new HashMap<Goto, Goto>();
            shiftCache = new HashMap<Shift, Shift>();
            reduceCache = new HashMap<Reduce, Reduce>();
            rangesCache = new HashMap<RangeList, RangeList>();
        } else {
            pt_generator = null;
        }

        if(dynamicPTgeneration && persistedTable == null) {
            throw new InvalidParseTableException(
                "Parse table does not contain any state and normalized grammar is null");
        }
    }

    public ParseTable(IStrategoTerm pt, ITermFactory factory, FileObject persistedTable,
        org.metaborg.sdf2table.parsetable.ParseTable referencePt) throws Exception {
        initTransientData(factory);

        dynamicPTgeneration = checkDynamicGeneration(pt);


        if(dynamicPTgeneration && persistedTable != null) {
            pt_generator = new IncrementalParseTableGenerator(persistedTable, referencePt);
        } else {
            pt_generator = null;
        }

        parse(pt_generator.generateATerm(pt_generator.getParseTable()));
        gotoCache = new HashMap<Goto, Goto>();
        shiftCache = new HashMap<Shift, Shift>();
        reduceCache = new HashMap<Reduce, Reduce>();
        rangesCache = new HashMap<RangeList, RangeList>();

        if(dynamicPTgeneration && persistedTable == null) {
            throw new InvalidParseTableException(
                "Parse table does not contain any state and normalized grammar is null");
        }
    }

    private boolean checkDynamicGeneration(IStrategoTerm pt) {
        // check if list of states in the aterm is empty
        // TODO may need a different checking method
        return true;
    }

    @Deprecated public ParseTable(IStrategoTerm pt) throws InvalidParseTableException {
        this(pt, new TermFactory());
    }

    public void initTransientData(ITermFactory factory) {
        this.factory = factory;
        applIStrategoConstructor = factory.makeConstructor("appl", 2);
        ambIStrategoConstructor = factory.makeConstructor("amb", 1);
    }

    public ITermFactory getFactory() {
        return factory;
    }

    private boolean parse(IStrategoTerm pt) throws InvalidParseTableException {
        int version = intAt(pt, 0);
        if(pt.getSubtermCount() == 1) // Seen with ParseTable(0)
            throw new InvalidParseTableException("Invalid parse table (possibly wrong start symbol specified)\n" + pt);
        startState = intAt(pt, 1);
        IStrategoList labelsTerm = termAt(pt, 2);
        IStrategoNamed statesTerm = termAt(pt, 3);
        IStrategoNamed prioritiesTerm = termAt(pt, 4);

        if(version != 4 && version != 6) {
            throw new InvalidParseTableException("Only supports version 4 and 6 tables.");
        }

        labels = parseLabels(labelsTerm);
        states = parseStates(statesTerm);
        priorities = parsePriorities(prioritiesTerm);
        associativities = parseAssociativities(prioritiesTerm);

        injections = new Label[labels.length];
        for(int i = 0; i < labels.length; i++)
            if(labels[i] != null && labels[i].isInjection())
                injections[i] = labels[i];


        gotoCache = null;
        shiftCache = null;
        reduceCache = null;
        rangesCache = null;

        return true;
    }

    private Priority[] parsePriorities(IStrategoNamed prioritiesTerm) throws InvalidParseTableException {

        IStrategoList prods = termAt(prioritiesTerm, 0);
        List<Priority> ret = new ArrayList<Priority>();

        while(!prods.isEmpty()) {
            IStrategoNamed a = (IStrategoNamed) prods.head();
            prods = prods.tail();

            int left = intAt(a, 0);
            int right = intAt(a, 1);
            if(a.getName().equals("left-prio")) {
                // handled by parseAssociativities
            } else if(a.getName().equals("right-prio")) {
                // handled by parseAssociativities
            } else if(a.getName().equals("non-assoc")) {
                // handled by parseAssociativities
            } else if(a.getName().equals("gtr-prio")) {
                if(left != right)
                    ret.add(new Priority(Priority.GTR, left, right));
            } else if(a.getName().equals("arg-gtr-prio")) {
                int arg = right;
                right = intAt(a, 2);
                if(left != right)
                    ret.add(new Priority(Priority.GTR, left, right, arg));
            } else {
                throw new InvalidParseTableException("Unknown priority : " + a.getName());
            }
        }
        return ret.toArray(new Priority[0]);
    }

    private Associativity[] parseAssociativities(IStrategoNamed prioritiesTerm) throws InvalidParseTableException {

        IStrategoList prods = termAt(prioritiesTerm, 0);
        List<Associativity> ret = new ArrayList<Associativity>();

        for(IStrategoNamed a = (IStrategoNamed) prods.head(); !prods.tail().isEmpty(); prods = prods.tail()) {
            int left = intAt(a, 0);
            int right = intAt(a, 1);
            if(a.getName().equals("left-prio")) {
                if(left == right)
                    ret.add(new Associativity(Priority.LEFT, left));
            } else if(a.getName().equals("right-prio")) {
                if(left == right)
                    ret.add(new Associativity(Priority.RIGHT, left));
            } else if(a.getName().equals("non-assoc")) {
                if(left == right)
                    ret.add(new Associativity(Priority.NONASSOC, left));
            } else if(a.getName().equals("gtr-prio")) {
                // handled by parsePriorities
            } else if(a.getName().equals("arg-gtr-prio")) {
                // handled by parsePriorities
            } else {
                throw new InvalidParseTableException("Unknown priority : " + a.getName());
            }
        }
        return ret.toArray(new Associativity[0]);
    }

    private Label[] parseLabels(IStrategoList labelsTerm) throws InvalidParseTableException {

        final Label[] ret = new Label[labelsTerm.getSubtermCount() + LABEL_BASE];

        while(!labelsTerm.isEmpty()) {

            final IStrategoNamed a = (IStrategoNamed) labelsTerm.head();
            final IStrategoAppl prod = termAt(a, 0);
            final int labelNumber = intAt(a, 1);
            final boolean injection = isInjection(prod);
            IStrategoAppl attrs = termAt(prod, 2);
            final ProductionAttributes pa = parseProductionAttributes(attrs);

            ret[labelNumber] = new Label(labelNumber, prod, pa, injection);

            labelsTerm = labelsTerm.tail();
        }

        return ret;
    }

    private boolean isInjection(IStrategoNamed prod) {

        // Injections are terms on the following form:
        // . prod([<term>],cf(<term>),<term>)
        // . prod([<term>],lex(sort(<str>)),<term>)
        // . lit(<str>)

        // TODO: optimize - use constants for these constructors (a la parseproductionreader)

        if(!prod.getName().equals("prod"))
            return false;


        if(prod.getSubterm(1).getTermType() != APPL)
            return false;

        final String nm = ((IStrategoNamed) prod.getSubterm(1)).getName();

        if(!(nm.equals("cf") || nm.equals("lex")))
            return false;

        if(prod.getSubterm(0).getTermType() != LIST)
            return false;

        IStrategoList ls = ((IStrategoList) prod.getSubterm(0));

        if(ls.getSubtermCount() != 1)
            return false;

        if(ls.head().getTermType() != APPL)
            return false;

        final IStrategoConstructor fun = ((IStrategoAppl) ls.head()).getConstructor();
        return !(fun.getName().equals("lit") && fun.getArity() == 1);
    }


    private ProductionAttributes parseProductionAttributes(IStrategoAppl attr) throws InvalidParseTableException {
        if(attr.getName().equals("attrs")) {
            int type = 0;
            boolean isRecover = false;
            boolean isIgnoreLayout = false;
            IStrategoTerm layoutConstraint = null;
            boolean isNewlineEnforced = false;
            boolean isLongestMatch = false;
            boolean isCompletion = false;
            boolean isPlaceholderInsertion = false;
            boolean isLiteralCompletion = false;
            boolean isBracket = false;
            IStrategoTerm term = null;

            for(IStrategoList ls = (IStrategoList) attr.getSubterm(0); !ls.isEmpty(); ls = ls.tail()) {
                IStrategoNamed t = (IStrategoNamed) ls.head();
                String ctor = t.getName();
                if(ctor.equals("reject")) {
                    type = ProductionType.REJECT;
                    hasRejects = true;
                } else if(ctor.equals("prefer")) {
                    type = ProductionType.PREFER;
                    hasPrefers = true;
                } else if(ctor.equals("avoid")) {
                    type = ProductionType.AVOID;
                    hasAvoids = true;
                } else if(ctor.equals("bracket")) {
                    type = ProductionType.BRACKET;
                    isBracket = true;
                } else {
                    if(ctor.equals("assoc")) {
                        IStrategoNamed a = (IStrategoNamed) t.getSubterm(0);
                        if(a.getName().equals("left") || a.getName().equals("assoc")) {
                            // ('assoc' is identical to 'left' for the parser)
                            type = ProductionType.LEFT_ASSOCIATIVE;
                        } else if(a.getName().equals("right")) {
                            type = ProductionType.RIGHT_ASSOCIATIVE;
                        } else if(a.getName().equals("non-assoc")) {
                            // FIXME: complete and test the non-assoc implementation
                            // (it currently already seems to work at least for direct cases)
                            // the current SDF manual and some tests seem to indicate that non-assoc
                            // has the same effects the same as having a priority P > P
                        } else {
                            throw new InvalidParseTableException("Unknown assocativity: " + a.getName());
                        }
                    } else if(ctor.equals("term") && t.getSubtermCount() == 1) {
                        // Term needs to be shaped as term(cons(Constructor)) to be a constructor
                        if(t.getSubterm(0) instanceof IStrategoNamed) {
                            IStrategoNamed child = (IStrategoNamed) t.getSubterm(0);
                            if(child.getSubtermCount() == 1 && child.getName().equals("cons")) {
                                term = t.getSubterm(0).getSubterm(0);
                            } else if(child.getSubtermCount() == 0 && child.getName().equals("recover")) {
                                hasRecovers = isRecover = true;
                            } else if(child.getSubtermCount() == 0 && child.getName().equals("completion")) {
                                isCompletion = true;
                            } else if(child.getSubtermCount() == 0 && child.getName().equals("placeholder-insertion")) {
                                isPlaceholderInsertion = true;
                            } else if(child.getSubtermCount() == 0 && child.getName().equals("literal-completion")) {
                                isLiteralCompletion = true;
                            } else if(child.getSubtermCount() == 0 && (child.getName().equals("ignore-layout")
                                || child.getName().equals("ignore-indent"))) {
                                isIgnoreLayout = true;
                            } else if(child.getSubtermCount() == 1 && child.getName().equals("layout")) {
                                layoutConstraint = child.getSubterm(0);
                                if(Term.isTermString(layoutConstraint))
                                    try {
                                        if(layoutParser == null) {
                                            try {
                                                InputStream in =
                                                    getClass().getResourceAsStream("indentation/LayoutConstraint.tbl");
                                                ParseTable pt = new ParseTableManager(factory).loadFromStream(in);
                                                layoutParser = new SGLR(new TreeBuilder(), pt);
                                            } catch(ParseError e) {
                                                e.printStackTrace();
                                            } catch(IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        layoutConstraint = (IStrategoTerm) layoutParser
                                            .parse(Term.asJavaString(layoutConstraint), "", "Constraint").output;
                                    } catch(SGLRException e) {
                                        throw new InvalidParseTableException("invalid layout constraint "
                                            + Term.asJavaString(layoutConstraint) + ": " + e.getMessage());
                                    } catch(InterruptedException e) {
                                        e.printStackTrace();
                                    }
                            } else if(child.getSubtermCount() == 0 && child.getName().equals("enforce-newline")) {
                                isNewlineEnforced = true;
                            } else if(child.getSubtermCount() == 0 && child.getName().equals("longest-match")) {
                                isLongestMatch = true;
                            }
                            // TODO Support other terms that are not a constructor (custom annotations)
                        } else if(ctor.equals("id")) {
                            // FIXME not certain about this
                            term = t.getSubterm(0);
                        } else {
                            throw new InvalidParseTableException("Unknown attribute: " + t);
                        }
                    }
                }
            }
            return new ProductionAttributes(term, type, isRecover, isBracket, isCompletion, isPlaceholderInsertion,
                isLiteralCompletion, isIgnoreLayout, layoutConstraint, isNewlineEnforced, isLongestMatch);
        } else if(attr.getName().equals("no-attrs")) {
            return new ProductionAttributes(null, ProductionType.NO_TYPE, false, false, false, false, false, false,
                null, false, false);
        }
        throw new InvalidParseTableException("Unknown attribute type: " + attr);
    }

    private State[] parseStates(IStrategoNamed statesTerm) throws InvalidParseTableException {
        IStrategoList states = termAt(statesTerm, 0);
        State[] ret = new State[states.getSubtermCount()];

        for(int i = 0; i < ret.length; i++) {
            IStrategoNamed stateRec = (IStrategoNamed) states.head();
            states = states.tail();

            int stateNumber = intAt(stateRec, 0);

            Goto[] gotos = parseGotos((IStrategoList) termAt(stateRec, 1));
            Action[] actions = parseActions((IStrategoList) termAt(stateRec, 2));

            ret[i] = new State(stateNumber, gotos, actions);
        }

        return ret;
    }

    private Goto makeGoto(int newStateNumber, RangeList ranges) {
        Goto g = new Goto(ranges, newStateNumber);
        Goto cached = gotoCache.get(g);
        if(cached == null) {
            gotoCache.put(g, g);
            return g;
        } else {
            return cached;
        }
    }

    private Action[] parseActions(IStrategoList actionList) throws InvalidParseTableException {
        Action[] ret = new Action[actionList.getSubtermCount()];

        for(int i = 0; i < ret.length; i++) {
            IStrategoNamed action = (IStrategoNamed) actionList.head();
            actionList = actionList.tail();
            RangeList ranges = parseRanges((IStrategoList) termAt(action, 0));
            ActionItem[] items = parseActionItems((IStrategoList) termAt(action, 1));
            ret[i] = new Action(ranges, items);
        }
        return ret;
    }

    private ActionItem[] parseActionItems(IStrategoList items) throws InvalidParseTableException {

        ActionItem[] ret = new ActionItem[items.getSubtermCount()];

        for(int i = 0; i < ret.length; i++) {
            ActionItem item = null;
            IStrategoAppl a = (IStrategoAppl) items.head();
            items = items.tail();

            if(a.getName().equals("reduce") && a.getConstructor().getArity() == 3) {
                int productionArity = intAt(a, 0);
                int label = intAt(a, 1);
                int status = intAt(a, 2);
                boolean isRecoverAction = getLabel(label).getAttributes().isRecoverProduction();
                boolean isCompletionAction = getLabel(label).getAttributes().isCompletionProduction();
                boolean isPlaceholderInsertionAction =
                    getLabel(label).getAttributes().isPlaceholderInsertionProduction();
                boolean isLiteralCompletionAction = getLabel(label).getAttributes().isLiteralCompletionProduction();
                boolean isBracketAction = getLabel(label).getAttributes().isBracket();
                item = makeReduce(productionArity, label, status, isRecoverAction, isBracketAction, isCompletionAction,
                    isPlaceholderInsertionAction, isLiteralCompletionAction);
            } else if(a.getName().equals("reduce") && a.getConstructor().getArity() == 4) {
                int productionArity = intAt(a, 0);
                int label = intAt(a, 1);
                int status = intAt(a, 2);
                RangeList[] charClasses = parseCharRanges((IStrategoList) termAt(a, 3));
                item = makeReduceLookahead(productionArity, label, status, charClasses);

            } else if(a.getName().equals("accept")) {
                item = new Accept();
            } else if(a.getName().equals("shift")) {
                int nextState = intAt(a, 0);
                item = makeShift(nextState);
            } else {
                throw new InvalidParseTableException("Unknown action " + a.getName());
            }
            ret[i] = item;
        }
        return ret;
    }

    private RangeList[] parseCharRanges(IStrategoList list) throws InvalidParseTableException {
        List<RangeList> ret = new LinkedList<RangeList>();
        for(int i = 0; i < list.getSubtermCount(); i++) {
            IStrategoNamed t = (IStrategoNamed) list.head();
            list = list.tail();
            IStrategoList l, n;
            if(t.getName().equals("look")) { // sdf2bundle 2.4
                l = termAt(termAt(t, 0), 0);
                n = termAt(t, 1);
            } else { // sdf2bundle 2.6
                assert t.getName().equals("follow-restriction");
                l = termAt(Term.termAt(termAt(t, 0), 0), 0);
                n = ((IStrategoList) termAt(t, 0)).tail();
            }

            // FIXME: multiple lookahead are not fully supported or tested
            // (and should work for both 2.4 and 2.6 tables)

            ret.add(parseRanges(l));

            if(n.getSubtermCount() > 0)
                throw new InvalidParseTableException("Multiple lookahead not fully supported");
            for(IStrategoTerm nt : n.getAllSubterms())
                ret.add(parseRanges((IStrategoList) nt.getSubterm(0)));
        }
        return ret.toArray(new RangeList[ret.size()]);
    }

    private ActionItem makeReduceLookahead(int productionArity, int label, int status, RangeList[] charClasses) {
        return new ReduceLookahead(productionArity, label, status, charClasses);
    }

    private Reduce makeReduce(int arity, int label, int status, boolean isRecoverAction, boolean isBracketAction,
        boolean isCompletionAction, boolean isPlaceholderInsertionAction, boolean isLiteralCompletionAction) {
        Reduce r = new Reduce(arity, label, status, isRecoverAction, isBracketAction, isCompletionAction,
            isPlaceholderInsertionAction, isLiteralCompletionAction);
        Reduce cached = reduceCache.get(r);
        if(cached == null) {
            reduceCache.put(r, r);
            return r;
        } else {
            return cached;
        }
    }

    private Shift makeShift(int nextState) {
        Shift s = new Shift(nextState);
        Shift cached = shiftCache.get(s);
        if(cached == null) {
            shiftCache.put(s, s);
            return s;
        } else {
            return cached;
        }
    }

    private Goto[] parseGotos(IStrategoList gotos) throws InvalidParseTableException {
        Goto[] ret = new Goto[gotos.getSubtermCount()];
        for(int i = 0; i < ret.length; i++) {
            IStrategoNamed go = (IStrategoNamed) gotos.head();
            gotos = gotos.tail();

            IStrategoList rangeList = termAt(go, 0);
            int newStateNumber = intAt(go, 1);
            RangeList ranges = parseRanges(rangeList);
            // int[] productionLabels = parseProductionLabels(rangeList);
            ret[i] = makeGoto(newStateNumber, ranges);
        }

        return ret;
    }

    // private int[] parseProductionLabels(IStrategoList ranges) throws InvalidParseTableException {
    //
    // int[] ret = new int[ranges.getChildCount()];
    //
    // for (int i = 0; i < ranges.getChildCount(); i++) {
    // IStrategoTerm t = Term.termAt(ranges, i);
    // if (isTermInt(t)) {
    // ret[i] = javaInt(t);
    // } else {
    //// else if(Term.isAppl(t) && ((IStrategoNamed)t).getName().equals("range")) {
    //// int s = intAt(t, 0);
    //// int e = intAt(t, 1);
    // Tools.debug(t);
    // throw new InvalidParseTableException("");
    // }
    // }
    // return ret;
    // }

    private RangeList parseRanges(IStrategoList ranges) throws InvalidParseTableException {
        int size = ranges.getSubtermCount();
        int[] ret = new int[size * 2];

        int idx = 0;

        for(int i = 0; i < size; i++) {
            IStrategoTerm t = ranges.head();
            ranges = ranges.tail();
            if(isTermInt(t)) {
                int value = javaInt(t);
                ret[idx++] = value;
                ret[idx++] = value;
            } else {
                ret[idx++] = intAt(t, 0);
                ret[idx++] = intAt(t, 1);
            }
        }

        return makeRangeList(ret);
    }

    private RangeList makeRangeList(int[] ranges) throws InvalidParseTableException {
        RangeList r = new RangeList(ranges);
        RangeList cached = rangesCache.get(r);
        if(cached == null) {
            rangesCache.put(r, r);
            return r;
        } else {
            return cached;
        }
    }

    private Goto parseGoto(IStrategoTerm term) {
        IStrategoNamed go = (IStrategoNamed) term;

        IStrategoList rangeList = termAt(go, 0);
        int newStateNumber = intAt(go, 1);
        RangeList ranges = null;
        try {
            ranges = parseRanges(rangeList);
        } catch(InvalidParseTableException e) {
            System.err.println("Could not generate Goto.");
            e.printStackTrace();
        }

        return makeGoto(newStateNumber, ranges);
    }

    private State parseDynamicState(org.metaborg.sdf2table.parsetable.State s_orig) {
        if(states_cache.containsKey(s_orig)) {
            return states_cache.get(s_orig);
        }
        List<Goto> gotos = Lists.newArrayList();

        for(org.metaborg.sdf2table.parsetable.Goto g : s_orig.gotos()) {
            // TODO create the structure directly without generating an ATerm
            gotos.add(parseGoto(g.toAterm(factory)));
        }

        List<IStrategoTerm> action_terms = Lists.newArrayList();

        for(CharacterClass cc : s_orig.actionsMapping().keySet()) {

            List<IStrategoTerm> actions = Lists.newArrayList();
            for(org.metaborg.sdf2table.parsetable.Action a : s_orig.actionsMapping().get(cc)) {
                actions.add(a.toAterm(factory, pt_generator.getParseTable()));
            }
            action_terms.add(factory.makeAppl(factory.makeConstructor("action", 2), cc.toStateAterm(factory),
                factory.makeList(actions)));
            // action_terms.add(action.toAterm(termFactory, this));
        }
        Action[] new_actions = null;

        try {
            new_actions = parseActions(factory.makeList(action_terms));
        } catch(InvalidParseTableException e) {
            System.err.println("Could not generate Actions.");
            e.printStackTrace();
        }

        State s_new = new State(s_orig.getLabel(), gotos.toArray(new Goto[gotos.size()]), new_actions);
        states_cache.put(s_orig, s_new);
        return s_new;
    }

    public State getInitialState() {
        if(dynamicPTgeneration) {
            org.metaborg.sdf2table.parsetable.State s0 = (org.metaborg.sdf2table.parsetable.State) pt_generator.getParseTable().startState();

            State s = parseDynamicState(s0);
            // System.out.println(s0.getLabel() + "->");
            return s;
        }
        return states[startState];
    }

    public State go(State s, int label) {
        if(dynamicPTgeneration) {
            org.metaborg.sdf2table.parsetable.State s0 = (org.metaborg.sdf2table.parsetable.State) pt_generator.getParseTable().getState(s.go(label));
            State s_new = parseDynamicState(s0);
            // System.out.println(s0.getLabel() + "->");
            return s_new;
        }
        return states[s.go(label)];
    }

    public Label getLabel(int label) {
        return labels[label];
    }

    public State getState(int s) {
        if(dynamicPTgeneration) {
            org.metaborg.sdf2table.parsetable.State s0 = (org.metaborg.sdf2table.parsetable.State) pt_generator.getParseTable().getState(s);
            State s_new = parseDynamicState(s0);
            // System.out.println(s0.getLabel() + "->");
            return s_new;
        }
        return states[s];
    }

    public int getStateCount() {
        return states.length;
    }

    public int getProductionCount() {
        return labels.length - NUM_CHARS;
    }

    public int getActionEntryCount() {
        int total = 0;
        for(State s : states) {
            total += s.getActionItemCount();
        }
        return total;
    }

    public int getGotoCount() {
        int total = 0;
        for(State s : states) {
            total += s.getGotoCount();
        }
        return total;
    }

    public int getActionCount() {
        int total = 0;
        for(State s : states) {
            total += s.getActionCount();
        }
        return total;
    }

    public ParseTableIO getPTgenerator() {
        return pt_generator;
    }

    public boolean hasPriorities() {
        return priorities.length > 0 || associativities.length > 0;
    }

    public boolean hasRejects() {
        return hasRejects;
    }

    public boolean hasPrefers() {
        return hasPrefers;
    }

    public boolean hasAvoids() {
        return hasAvoids;
    }

    public boolean hasRecovers() {
        return hasRecovers;
    }

    public boolean hasPrefersOrAvoids() {
        return hasAvoids() || hasPrefers();
    }

    public IStrategoTerm getProduction(int prod) {
        if(prod < NUM_CHARS) {
            return factory.makeInt(prod);
        }
        return labels[prod].prod;
    }

    public List<Priority> getPriorities(Label prodLabel) {
        if(priorityCache == null)
            priorityCache = new HashMap<Label, List<Priority>>();
        List<Priority> results = priorityCache.get(prodLabel);
        if(results != null)
            return results;

        results = new ArrayList<Priority>();
        for(Priority p : priorities) {
            if(p.left == prodLabel.labelNumber && p.type == Priority.GTR) {
                results.add(p);
            }
        }

        priorityCache.put(prodLabel, results);

        return results;
    }

    public Label lookupInjection(int prod) {
        return injections[prod];
    }

    public void lookupAction(int stateNumber, int peekNextToken) {
        throw new NotImplementedException();
    }

    public List<Label> getLabels() {
        return Collections.unmodifiableList(asList(labels));
    }

    public void initializeTreeBuilder(ITreeBuilder treeBuilder) {
        treeBuilder.initializeTable(this, NUM_CHARS, LABEL_BASE, labels.length);
        for(int i = 0; i < labels.length; i++) {
            if(labels[i] == null)
                continue;
            treeBuilder.initializeLabel(i, labels[i].getProduction());
        }
    }

    public IKeywordRecognizer getKeywordRecognizer() {
        if(keywords == null)
            keywords = new KeywordRecognizer(this);
        return keywords;
    }
}

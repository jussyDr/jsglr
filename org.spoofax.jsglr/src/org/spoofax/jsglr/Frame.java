/*
 * Created on 04.des.2005
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk@ii.uib.no>
 * 
 * Licensed under the GNU Lesser General Public License, v2.1
 */
package org.spoofax.jsglr;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.io.Serializable;

public class Frame implements Serializable {

    static final long serialVersionUID = -4757644376472129935L;

    public final State state;

    // Using a Vector and regular iteration takes parsing of file-test from 1100ms (min) to 2020ms (max)!
    private Link[] steps;
    private int stepsCount;

    // FIXME: All frames except the root must have a step with a label
    // that goes to the parent frame. Should we enforce this in this
    // constructor?
    public Frame(State s) {
        state = s;
        steps = new Link[80]; //todo: what is the max size?
        stepsCount = 0;
    }

    public boolean allLinksRejected() {

        if (stepsCount == 0)
            return false;

        for (int i = 0; i < stepsCount; i++) {
            if (!steps[i].isRejected())
                return false;
        }

        return true;
    }

    public State peek() {
        return state;
    }

    public List<Path> computePathsToRoot(int arity) {

        if (arity == 0) {
            List<Path> ret = new Vector<Path>();
            ret.add(Path.valueOf(null, null, this));
            return ret;
        }

        List<Path> ret = new Vector<Path>();
        doComputePathsToRoot(ret, null, arity);
        Collections.reverse(ret);
        return ret;
    }

    private void doComputePathsToRoot(List<Path> collect, Path node, int arity) {

        if (arity == 0) {
            Path n = Path.valueOf(node, null, this);
            collect.add(n);
        }

        for (int i = 0; i < stepsCount; i++) {
            Link ln = steps[i];

            Path n = Path.valueOf(node, ln.label, this);
            ln.parent.doComputePathsToRoot(collect, n, arity - 1);
        }
    }

    public Frame getRoot() {
        // FIXME: I'm iffy about the contract here. The assumption is
        // that the user applies addStep correctly.
        if (stepsCount == 0)
            return this;
        return steps[0].parent.getRoot();
    }

    public Link findLink(Frame st0) {

        for (int i = 0; i < stepsCount; i++) {
            if (steps[i].parent == st0)
                return steps[i];
        }
        return null;
    }

    public Link addLink(Frame st0, IParseNode n, int length) {
        Link s = new Link(st0, n, length);
        steps[stepsCount] = s;
        stepsCount++;
        return s;
    }

    public String dumpStack() {
        StringBuffer sb = new StringBuffer();

        sb.append("GSS [\n").append(doDumpStack(2)).append("\n  ]");
        return sb.toString();
    }

    public String doDumpStack(int indent) {
        StringBuffer sb = new StringBuffer();
        sb.append(" ").append(state.stateNumber);
        if (stepsCount > 1) {
            sb.append(" ( ");
            for (Link s : steps) {
                if(s == null) {
                    break;
                }
                sb.append(s.parent.doDumpStack(indent + 1));
                sb.append(" | ");
            }
            sb.append(")");

        } else {
            for (Link s : steps) {
                if(s == null) {
                    break;
                }
                sb.append(" <").append(s.label).append("> ").append(s.parent.state.stateNumber).append("\n");
                sb.append(s.parent.doDumpStack(indent));
            }
        }
        return sb.toString();
    }

    public String dumpStackCompact() {
        StringBuffer sb = new StringBuffer();

        sb.append("GSS [").append(doDumpStackCompact()).append(" ]");
        return sb.toString();
    }

    public String doDumpStackCompact() {
        StringBuffer sb = new StringBuffer();
        sb.append(" ").append(state.stateNumber);
        if (stepsCount > 1) {
            sb.append(" ( ");
            for (Link s : steps) {
                if(s == null) {
                    break;
                }
                sb.append(s.parent.doDumpStackCompact());
                sb.append(" | ");
            }
            sb.append(")");

        } else {
            for (Link s : steps) {
                if(s == null) {
                    break;
                }
                sb.append(", ").append(s.parent.doDumpStackCompact());
            }
        }
        return sb.toString();
    }

//    public List<Frame> computeFramesAtDepth(int depth) {
//        List<Frame> frames = new LinkedList<Frame>();
//
//        if (depth == 0) {
//            frames.add(this);
//        } else {
//            for (Link s : steps) {
//                Frame st = s.parent;
//                frames.addAll(st.computeFramesAtDepth(depth - 1));
//            }
//        }
//        return frames;
//    }

    public List<Path> computePathsToRoot(int arity, Link l) {

        if (arity == 0) {
            List<Path> ret = new Vector<Path>();
            ret.add(Path.valueOf(null, null, this));
            return ret;
        }

        List<Path> ret = new Vector<Path>();
        doComputePathsToRoot(ret, null, l, false, arity);
        // FIXME: Necessary?
        Collections.reverse(ret);
        return ret;
    }

    private void doComputePathsToRoot(List<Path> collect, Path node, Link l,
      boolean seen, int arity) {

        if (arity == 0 && seen) {
            Path n = Path.valueOf(node, null, this);
            collect.add(n);
        }

        for (int i = 0; i < stepsCount; i++) {
            Link ln = steps[i];
            boolean seenIt = seen || (ln == l);
            Path n = Path.valueOf(node, ln.label, this);
            ln.parent.doComputePathsToRoot(collect, n, l, seenIt, arity - 1);
        }
    }

    public void clear() {

        if (this.steps != null) {
            for (int i = 0; i < stepsCount; i++) {
                steps[i].clear();
                steps[i] = null;
            }
            this.steps = null;
            this.stepsCount = 0;
        }
    }

}

/*
 * Created on 30.mar.2006
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk near strategoxt.org>
 *
 * Licensed under the GNU General Public License, v2
 */
package org.spoofax.jsglr.client;

import java.util.List;

import org.spoofax.jsglr.shared.terms.ATerm;
import org.spoofax.jsglr.shared.terms.ATermFactory;
import org.spoofax.jsglr.shared.terms.ATermList;

public class ParseNode extends AbstractParseNode {

    public final int label;

    protected final AbstractParseNode[] kids;

    private int cachedHashCode;

    public ParseNode(int label, AbstractParseNode[] kids) {
        this.label = label;
        this.kids = kids;
    }

    @Override
	public Object toParseTree(ParseTable pt) {
        TreeBuilder tb = pt.getTreeBuilder();

        Object[] subtrees = new Object[kids.length];
        for (int i = 0; i < kids.length; i++) {
        	subtrees[i] = kids[i].toParseTree(pt);
        }

        return tb.buildNode(label - ParseTable.LABEL_BASE, subtrees);
    }

    /**
     * todo: stolen from TAFReader; move elsewhere
     */
    public static ATermList makeList(ATermFactory factory, List<ATerm> terms) {
        ATermList result = factory.makeList();
        for (int i = terms.size() - 1; i >= 0; i--) {
            result = result.prepend(terms.get(i));
        }
        return result;
    }

    @Override
    public String toString() {
        return "regular(aprod(" + label + ")," + kids + ")";
    }

    public int getLabel() { return label; }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ParseNode))
            return false;
        if (obj == this)
            return true;
        ParseNode o = (ParseNode)obj;
        if(label != o.label || kids.length != o.kids.length
                || hashCode() != o.hashCode())
            return false;
        for(int i = 0; i < kids.length; i++) {
            if(!kids[i].equals(o.kids[i]))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (cachedHashCode != NO_HASH_CODE)
            return cachedHashCode;
        final int prime = 31;
        int result = prime * label;
        for(AbstractParseNode n : kids)
            result += (prime * n.hashCode());
        cachedHashCode = result;
        return result;
    }

    @Override
    public String toStringShallow() {
        return "regular*(" + label + ", {" +  kids.length + "})";
    }
}
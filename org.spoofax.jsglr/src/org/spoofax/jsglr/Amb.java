/*
 * Created on 30.mar.2006
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk@ii.uib.no>
 * 
 * Licensed under the GNU General Public License, v2
 */
package org.spoofax.jsglr;

import java.util.ArrayList;
import java.util.List;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import aterm.ATerm;

public class Amb extends IParseNode {

    public final List<IParseNode> alternatives;
    
    Amb(IParseNode left, IParseNode right) {
        alternatives = new ArrayList<IParseNode>();
        alternatives.add(left);
        alternatives.add(right);
    }
    
    public Amb(List<IParseNode> alternatives) {
        this.alternatives = alternatives;
    }

    public boolean isLiteral() {
        return false;
    }
    
    public ATerm toParseTree(ParseTable pt) {
        List<ATerm> r = new ArrayList<ATerm>();
        for(IParseNode pn : alternatives)
            r.add(pn.toParseTree(pt));
        return pt.getFactory().parse("amb(" + r + ")");
    }
    
    @Override
    public String toString() {
        return "amb(" + alternatives + ")";
    }

    public boolean hasAmbiguity(IParseNode newNode) {
        throw new NotImplementedException();
    }

    public List<IParseNode> getAlternatives() {
        return alternatives;
    }
}
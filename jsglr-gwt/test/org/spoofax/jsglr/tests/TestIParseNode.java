/*
 * Created on 21.apr.2006
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk near strategoxt.org>
 * 
 * Licensed under the GNU General Public License, v2
 */
package org.spoofax.jsglr.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.spoofax.jsglr.client.AbstractParseNode;
import org.spoofax.jsglr.client.ParseNode;
import org.spoofax.jsglr.client.ParseProductionNode;

public class TestIParseNode extends TestCase {

    private AbstractParseNode pn0; 
    private AbstractParseNode pn1;
    
    @Override
    protected void setUp() throws Exception {
        List<AbstractParseNode> r0 = new ArrayList<AbstractParseNode>();
        r0.add(new ParseProductionNode(123));
        pn0 = new ParseNode(233, r0);

        List<AbstractParseNode> r1 = new ArrayList<AbstractParseNode>();
        r1.add(new ParseProductionNode(123));
        pn1 = new ParseNode(233, r1);
    }

    public void testHashCode() {
        assertTrue(pn0.hashCode() == pn1.hashCode());
    }

    public void testEquals() {
        assertTrue(pn0.equals(pn1));
    }


}

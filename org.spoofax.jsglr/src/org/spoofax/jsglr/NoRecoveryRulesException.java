package org.spoofax.jsglr;

/**
 * An exception thrown when recovery is enabled for a parse table with
 * no recovery rules.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
@Deprecated
public class NoRecoveryRulesException extends RuntimeException {

    private static final long serialVersionUID = 442099735579027095L;

    public NoRecoveryRulesException(SGLR parser) {
        super("Parse table has no recovery rules");
    }

}

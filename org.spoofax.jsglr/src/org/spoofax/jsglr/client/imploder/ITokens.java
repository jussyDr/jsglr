package org.spoofax.jsglr.client.imploder;

import java.io.Serializable;

public interface ITokens extends Iterable<IToken>, Serializable {

    String ERROR_SKIPPED_REGION = "Syntax error, unexpected construct(s)";

    String ERROR_UNEXPECTED_EOF = "Syntax error, unexpected end of input";

    String ERROR_WATER_PREFIX = "Syntax error, not expected here";

    String ERROR_INSERT_PREFIX = "Syntax error, expected";

    String ERROR_INSERT_END_PREFIX = "Syntax error, unterminated construct";

    String ERROR_INCOMPLETE_PREFIX = "Syntax error, incomplete construct";

    String ERROR_GENERIC_PREFIX = "Syntax error";

    String ERROR_WARNING_PREFIX = "Warning";

    String getInput();

    int getTokenCount();

    IToken getTokenAt(int index);

    IToken getTokenAtOffset(int offset);

    String getFilename();

    String toString(IToken left, IToken right);

    String toString(int startOffset, int endOffset);

    /**
     * Determines if the tokenizer is ambiguous. If it is, tokens with subsequent indices may not always have matching
     * start/end offsets.
     * 
     * @see Tokenizer#getTokenAfter(IToken) Gets the next token with a matching offset.
     * @see Tokenizer#getTokenBefore(IToken) Gets the previous token with a matching offset.
     */
    boolean isAmbiguous();

}

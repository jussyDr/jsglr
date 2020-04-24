package org.spoofax.jsglr.client;

public class TokenOffset {

    public static final int NONE = -2;

    private int token;
    private int offset;

    public TokenOffset() {
        token = NONE;
        offset = -1;
    }

    public TokenOffset(int token, int offset) {
        this.token = token;
        this.offset = offset;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;

    }

    @Override public String toString() {
        // TODO Auto-generated method stub
        return "token: " + token + "\ntokenchar: " + (char) token + "\noffset: " + offset;
    }



}

package org.spoofax.jsglr2.stack.collections;

public enum ActiveStacksRepresentation {
    ArrayList;

    public static ActiveStacksRepresentation standard() {
        return ArrayList;
    }
}

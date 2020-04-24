package org.spoofax.jsglr2.testset;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.spoofax.jsglr2.testset.testinput.StringInput;
import org.spoofax.jsglr2.testset.testinput.TestInput;

public abstract class TestSetInput<ContentType, Input extends TestInput<ContentType>> {

    public enum Type {
        SINGLE, // A single file from the org.spoofax.jsglr2.integration samples resources directory
        INCREMENTAL, // Multiple revisions of a single file, from a folder in the same samples resources directory
        MULTIPLE, // All files with a certain extension from an absolute path
        SIZED // Custom input string of dynamic size based on given argument
    }

    public final Type type;

    public final boolean internal;

    public static TestSetInput<String, StringInput> fromArgs(Map<String, String> args) {
        String type = args.get("type");
        String sourcePath = args.get("sourcePath");
        String extension = args.get("extension");

        if("multiple".equals(type) && sourcePath != null && extension != null) {
            return new TestSetMultipleInputs.StringInputSet(sourcePath, extension);
        } else if("single".equals(type) && sourcePath != null) {
            return new TestSetSingleInput.StringInputSet(sourcePath, false);
        }

        throw new IllegalStateException("invalid arguments");
    }

    protected abstract Input getInput(String filename, ContentType input);

    public abstract List<Input> getInputs() throws IOException;

    protected TestSetInput(Type type, boolean internal) {
        this.type = type;
        this.internal = internal;
    }

    protected String getFileAsString(String filename) throws IOException {
        InputStream inputStream;

        if(internal)
            inputStream = getClass().getResourceAsStream("/samples/" + filename);
        else
            inputStream = new FileInputStream(filename);

        return inputStreamAsString(inputStream);
    }

    protected String inputStreamAsString(InputStream inputStream) throws IOException {
        try(Scanner s = new Scanner(inputStream)) {
            s.useDelimiter("\\A");

            return s.hasNext() ? s.next() : "";
        }
    }

}

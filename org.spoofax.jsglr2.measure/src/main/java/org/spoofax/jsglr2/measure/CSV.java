package org.spoofax.jsglr2.measure;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CSV<T> {

    private final List<T> columns;
    private final List<Map<T, String>> rows = new ArrayList<>();

    public CSV(T[] columns) {
        this.columns = Arrays.asList(columns);
    }

    public void addRow(Map<T, String> row) {
        rows.add(row);
    }

    public void write(String path) throws FileNotFoundException {
        write(new PrintWriter(path));
    }

    private void write(PrintWriter out) {
        writeHeader(out);
        rows.stream().forEach(row -> writeRow(out, row));

        out.close();
    }

    private void writeHeader(PrintWriter out) {
        writeLine(out, columns.stream().map(T::toString).collect(Collectors.toList()));
    }

    private void writeRow(PrintWriter out, Map<T, String> row) {
        writeLine(out, columns.stream().map(column -> row.getOrDefault(column, "")).collect(Collectors.toList()));
    }

    private void writeLine(PrintWriter out, List<String> row) {
        out.println(String.join(",", row));
    }

}

package com.mvpief.export;

import com.mvpief.LogEntry;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Writes {@link LogEntry} rows to a CSV file (RFC 4180-ish: comma-separated, quoted only when needed).
 * <p>
 * The writer is streamed and buffered, so the whole log is never assembled into one big String in
 * memory — exporting thousands of rows stays cheap. Call {@link #export} off the EDT.
 */
@Slf4j
@Singleton
public class CsvExporter
{
    private static final String HEADER = "date,course,laps,marks";

    /**
     * Writes every entry to {@code file}, overwriting any existing file at that path.
     *
     * @throws IOException if the file cannot be written
     */
    public void export(File file, List<LogEntry> entries) throws IOException
    {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))
        {
            writer.write(HEADER);
            writer.newLine();

            for (LogEntry entry : entries)
            {
                writer.write(escape(entry.getDate()));
                writer.write(',');
                writer.write(escape(entry.getCourse()));
                writer.write(',');
                writer.write(Integer.toString(entry.getLaps()));
                writer.write(',');
                writer.write(Integer.toString(entry.getMarks()));
                writer.newLine();
            }
        }
    }

    /**
     * Quotes a field only when it contains a comma, quote, or line break, doubling any inner quotes.
     */
    private static String escape(String field)
    {
        if (field == null)
        {
            return "";
        }

        if (field.indexOf(',') >= 0 || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0)
        {
            return '"' + field.replace("\"", "\"\"") + '"';
        }

        return field;
    }
}

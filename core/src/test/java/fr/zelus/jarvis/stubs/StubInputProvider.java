package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.io.InputProvider;
import org.apache.commons.configuration2.Configuration;

import java.io.PrintWriter;

public class StubInputProvider extends InputProvider {

    private PrintWriter writer;

    public StubInputProvider() {
        super();
        writer = new PrintWriter(outputStream, true);
    }

    public StubInputProvider(Configuration configuration) {
        this();
    }

    public void write(String message) {
        writer.println(message);
    }

    public void close() {
        writer.close();
    }
}

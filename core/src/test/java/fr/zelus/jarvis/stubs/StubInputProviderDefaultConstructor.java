package fr.zelus.jarvis.stubs;

import fr.zelus.jarvis.io.InputProvider;

import java.io.PrintWriter;

public class StubInputProviderDefaultConstructor extends InputProvider {

    private PrintWriter writer;

    public StubInputProviderDefaultConstructor() {
        super();
        writer = new PrintWriter(outputStream, true);
    }

}

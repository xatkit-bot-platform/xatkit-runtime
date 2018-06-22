package fr.zelus.jarvis.io;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisException;

import java.io.*;

import static fr.inria.atlanmod.commons.Preconditions.checkNotNull;

/**
 * Consumes textual lines from a provided {@link InputProvider} and delegate them to {@link JarvisCore}.
 * <p>
 * This class embeds a {@link PipedInputStream} and attempts to connect it to the provided
 * {@link InputProvider#getOutputStream()} to retrieve input lines.
 * <p>
 * Calling {@link #run()} is a blocking operation that will not return until the provided {@link InputProvider}'s
 * stream is closed.
 * <p>
 * <b>Note:</b> this class expects that the associated {@link InputProvider} writes lines in its output stream (e.g.
 * using a line separator or {@link PrintWriter#println()}).
 *
 * @see InputProvider
 * @see JarvisCore
 */
public class LineInputConsumer implements Runnable {

    /**
     * The {@link JarvisCore} component used to handle the retrieved line inputs.
     */
    private JarvisCore jarvisCore;

    /**
     * The {@link PipedInputStream} used to read inputs from the provided {@link InputProvider}.
     * <p>
     * This stream is bound to the provided {@link InputProvider#getOutputStream()}.
     */
    private PipedInputStream inputStream;

    /**
     * The {@link Reader} used to retrieve input lines from the {@link #inputStream}.
     */
    private BufferedReader reader;

    /**
     * The {@link InputProvider} bound to this instance.
     */
    private InputProvider inputProvider;


    /**
     * Constructs a new {@link LineInputConsumer} for the provided {@code inputProvider}, delegating computation to
     * the given {@code jarvisCore}.
     * <p>
     * This method attempts to create and bind its {@link #inputStream} to the provided
     * {@link InputProvider#getOutputStream()}, and wraps it in a {@link BufferedReader} to retrieve input lines.
     *
     * @param jarvisCore    the {@link JarvisCore} component used to handle the retrieved line inputs
     * @param inputProvider the {@link InputProvider} to bind
     * @throws NullPointerException if the provided {@code jarvisCore} or {@code inputProvider} is {@code null}
     * @throws JarvisException      if the {@link #inputStream} cannot be bound to the provided {@link InputProvider}
     */
    public LineInputConsumer(JarvisCore jarvisCore, InputProvider inputProvider) throws JarvisException {
        checkNotNull(jarvisCore, "Cannot construct a LineInputConsumer from a null JarvisCore");
        checkNotNull(inputProvider, "Cannot construct a LineInputConsumer from a null InputProvider");
        this.jarvisCore = jarvisCore;
        this.inputProvider = inputProvider;
        try {
            this.inputStream = new PipedInputStream(inputProvider.getOutputStream());
        } catch (IOException e) {
            String errorMessage = "Cannot create jarvis LineInputConsumer";
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        }
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    /**
     * Returns the {@link JarvisCore} instance.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the {@link JarvisCore} instance
     */
    protected JarvisCore getJarvisCore() {
        return jarvisCore;
    }

    /**
     * Returns the underlying {@link PipedInputStream}.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the underlying {@link PipedInputStream}
     */
    protected PipedInputStream getInputStream() {
        return inputStream;
    }

    /**
     * Returns the underlying {@link BufferedReader}.
     * <p>
     * <b>Note:</b> this method is protected for testing purposes, and should not be called by client code.
     *
     * @return the underlying {@link BufferedReader}
     */
    protected BufferedReader getReader() {
        return reader;
    }

    /**
     * Returns the {@link InputProvider} bound to this instance.
     *
     * @return the {@link InputProvider} bound this this instance
     */
    public InputProvider getInputProvider() {
        return inputProvider;
    }

    /**
     * Starts the {@link LineInputConsumer} and delegate input processing to the {@link JarvisCore} component.
     * <p>
     * This method is blocking, and will not return as long as the {@link #inputStream} is open.
     * <p>
     * <b>Note:</b> calling {@link Thread#interrupt()} on the the {@link Thread} running this class will close the
     * underlying stream and reader.
     *
     * @throws JarvisException if an exception occurs when reading the {@link #inputStream}, or if the
     *                         {@link #inputStream} cannot be closed properly
     */
    @Override
    public void run() throws JarvisException {
        Log.info("Starting input listening");
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                jarvisCore.handleMessage(line);
            }
        } catch (InterruptedIOException e) {
            Log.info("Input listening interrupted");
        } catch (IOException e) {
            String errorMessage = "Cannot read from the input stream";
            Log.error(errorMessage);
            throw new JarvisException(errorMessage, e);
        } finally {
            try {
                Log.info("Closing input stream");
                reader.close();
                inputStream.close();
            } catch (IOException e1) {
                String errorMessage = "Cannot close the input stream";
                Log.error(errorMessage);
                throw new JarvisException(errorMessage, e1);
            }
        }
        Log.info("Stopping input listening");
    }
}

package fr.zelus.jarvis.core;

public class JarvisException extends RuntimeException {

    public JarvisException() {
        super();
    }

    public JarvisException(String message) {
        super(message);
    }

    public JarvisException(String message, Throwable cause) {
        super(message, cause);
    }

    public JarvisException(Throwable cause) {
        super(cause);
    }

    protected JarvisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

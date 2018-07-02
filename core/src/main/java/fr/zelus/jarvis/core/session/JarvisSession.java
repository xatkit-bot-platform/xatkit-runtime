package fr.zelus.jarvis.core.session;

public class JarvisSession {

    private String sessionId;

    private JarvisContext jarvisContext;

    public JarvisSession(String sessionId) {
        this.sessionId = sessionId;
        this.jarvisContext = new JarvisContext();
    }

    public String getSessionId() {
        return sessionId;
    }

    public JarvisContext getJarvisContext() {
        return jarvisContext;
    }
}

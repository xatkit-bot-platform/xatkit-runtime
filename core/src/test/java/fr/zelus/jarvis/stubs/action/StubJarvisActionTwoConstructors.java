package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.core.session.JarvisSession;

import java.util.List;

public class StubJarvisActionTwoConstructors extends JarvisAction {

    private String param;

    private List<String> listParam;

    public StubJarvisActionTwoConstructors(JarvisModule containingModule, JarvisSession session, String param) {
        super(containingModule, session);
        this.param = param;
    }

    public StubJarvisActionTwoConstructors(JarvisModule containingModule, JarvisSession session, List<String>
            listParam) {
        super(containingModule, session);
        this.listParam = listParam;
    }

    public String getParam() {
        return this.param;
    }

    public List<String> getListParam() {
        return this.listParam;
    }

    @Override
    public Object compute() {
        return null;
    }
}

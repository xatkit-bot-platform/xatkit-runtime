package fr.zelus.jarvis.stubs.action;

import fr.zelus.jarvis.core.platform.action.RuntimeAction;
import fr.zelus.jarvis.core.platform.RuntimePlatform;
import fr.zelus.jarvis.core.session.JarvisSession;

import java.util.List;

public class StubRuntimeActionTwoConstructors extends RuntimeAction {

    private String param;

    private List<String> listParam;

    public StubRuntimeActionTwoConstructors(RuntimePlatform runtimePlatform, JarvisSession session, String param) {
        super(runtimePlatform, session);
        this.param = param;
    }

    public StubRuntimeActionTwoConstructors(RuntimePlatform runtimePlatform, JarvisSession session, List<String>
            listParam) {
        super(runtimePlatform, session);
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

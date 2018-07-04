package fr.zelus.jarvis.plugins.core.module.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.plugins.core.module.CoreModule;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class GetTime extends JarvisAction<CoreModule> {

    public GetTime(CoreModule containingModule, JarvisContext context) {
        super(containingModule, context);
    }

    @Override
    public Object call() {
        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }

}

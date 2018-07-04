package fr.zelus.jarvis.plugins.core.module.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisContext;
import fr.zelus.jarvis.plugins.core.module.CoreModule;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class GetDate extends JarvisAction<CoreModule> {

    public GetDate(CoreModule containingModule, JarvisContext context) {
        super(containingModule, context);
    }

    @Override
    public Object call() {
        return new SimpleDateFormat("dd/MM/yyyy").format(Calendar.getInstance().getTime());
    }
}

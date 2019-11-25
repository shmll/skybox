package org.swdc.skybox;

import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.swdc.skybox.ui.Splash;
import org.swdc.skybox.ui.view.StartView;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

@SpringBootApplication
public class SkyboxApplication extends AbstractJavaFxApplicationSupport {

    @Autowired
    private ExecutorService asyncExecutor;

    public static void main(String[] args) {
        launch(SkyboxApplication.class, StartView.class, new Splash(), args);
    }

    @PreDestroy
    public void shutdown() {
        asyncExecutor.shutdown();
    }

}

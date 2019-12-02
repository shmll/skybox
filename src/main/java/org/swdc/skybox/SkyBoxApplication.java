package org.swdc.skybox;

import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import de.felixroske.jfxsupport.GUIState;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.events.RestartEvent;
import org.swdc.skybox.ui.AwsomeIconData;
import org.swdc.skybox.ui.Splash;
import org.swdc.skybox.ui.view.StartView;

import javax.annotation.PreDestroy;
import java.io.File;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;

@CommonsLog
@SpringBootApplication
public class SkyBoxApplication extends AbstractJavaFxApplicationSupport {

    @Autowired
    private ExecutorService asyncExecutor;

    public static void main(String[] args) {
        launch(SkyBoxApplication.class, StartView.class, new Splash(), args);
    }

    @Override
    public void beforeShowingSplash(Stage splashStage) {
        splashStage.getIcons().addAll(AwsomeIconData.getImageIcons());
    }

    @Override
    public void beforeInitialView(Stage stage, ConfigurableApplicationContext ctx) {
        stage.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue && GUIState.getScene().getStylesheets().size() == 0) {
                ApplicationConfig config = ctx.getBean(ApplicationConfig.class);
                try {
                    GUIState.getScene().getStylesheets().add(new File("./config/theme/" + config.getTheme() + "/stage.css").toURI().toURL().toExternalForm());
                } catch (MalformedURLException e) {
                    log.error(e);
                }
            }
        });
    }

    @EventListener(RestartEvent.class)
    public void onRestart() {
        Platform.runLater(() -> {
            try {
                GUIState.getStage().close();
                this.stop();
                this.init();
                this.start(new Stage());
            } catch (Exception e){
                log.error(e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        asyncExecutor.shutdown();
    }

}

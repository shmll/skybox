package org.swdc.skybox.ui.view;

import de.felixroske.jfxsupport.AbstractFxmlView;
import de.felixroske.jfxsupport.FXMLView;
import de.felixroske.jfxsupport.GUIState;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.utils.UIUtils;

import javax.annotation.PostConstruct;

@FXMLView("/views/ProgressView.fxml")
public class ProgressView extends AbstractFxmlView {

    @Autowired
    private ApplicationConfig config;

    private Stage stage;

    @PostConstruct
    private void initUI() throws Exception {
        BorderPane panel = (BorderPane)getView();
        UIUtils.configUI(panel,config);
        Platform.runLater(() -> {
            stage = new Stage();
            Scene scene = new Scene(panel);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initOwner(GUIState.getStage());
            stage.initModality(Modality.APPLICATION_MODAL);
        });
    }

    public void show() {
        if (stage.isShowing()) {
            stage.requestFocus();
        } else {
            stage.show();
        }
    }

    public void hide() {
        if (stage.isShowing()) {
            stage.close();
        }
    }

}

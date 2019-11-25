package org.swdc.skybox.ui.controller;

import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.springframework.context.event.EventListener;
import org.swdc.skybox.events.ProgressEvent;

import java.net.URL;
import java.util.ResourceBundle;

@FXMLController
public class ProgressController implements Initializable {

    private SimpleDoubleProperty progress = new SimpleDoubleProperty();

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label lblOpt;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        progressBar.progressProperty().bind(progress);
    }

    @EventListener
    public void onProgress(ProgressEvent event) {
        Platform.runLater(() -> {
            lblOpt.setText(event.getLabel());
            progress.set(event.getVal());
        });
    }

}

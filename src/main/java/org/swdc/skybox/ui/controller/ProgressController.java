package org.swdc.skybox.ui.controller;

import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.core.exception.EncryptException;
import org.swdc.skybox.core.exception.InvalidPasswordException;
import org.swdc.skybox.core.exception.NotSupportException;
import org.swdc.skybox.core.exception.ResolveExceptionEvent;
import org.swdc.skybox.events.ProgressEvent;
import org.swdc.skybox.ui.view.ProgressView;
import org.swdc.skybox.utils.UIUtils;

import java.net.URL;
import java.util.ResourceBundle;

@FXMLController
public class ProgressController implements Initializable {

    private SimpleDoubleProperty progress = new SimpleDoubleProperty();

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label lblOpt;

    @Autowired
    private ApplicationConfig config;

    @Autowired
    private ProgressView view;

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

    @EventListener
    public void onException(ResolveExceptionEvent exceptionEvent) {
        Platform.runLater(() -> {
            if (exceptionEvent.getException() instanceof EncryptException) {
                UIUtils.showAlertDialog("异常：" + exceptionEvent.getSource(),"出现了问题", Alert.AlertType.ERROR,config);
            } else if (exceptionEvent.getException() instanceof NotSupportException) {
                UIUtils.showAlertDialog("正在试图使用不支持的算法进行数据处理，操作已经终止。","暂不支持", Alert.AlertType.WARNING,config);
            } else if (exceptionEvent.getException() instanceof InvalidPasswordException) {
                UIUtils.showAlertDialog("无法进行进一步解密，可能是密码不正确。","密钥不正确", Alert.AlertType.WARNING,config);
            }
            view.hide();
        });
    }

}

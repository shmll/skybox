package org.swdc.skybox.ui.controller;

import de.felixroske.jfxsupport.FXMLController;
import de.felixroske.jfxsupport.GUIState;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.core.exception.EncryptException;
import org.swdc.skybox.events.RestartEvent;
import org.swdc.skybox.utils.DataUtils;
import org.swdc.skybox.utils.UIUtils;

import java.net.URL;
import java.util.ResourceBundle;

@FXMLController
public class ConfigController implements Initializable {

    @Autowired
    private ApplicationConfig config;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    public void onConfigSave() {
        try {
            DataUtils.saveConfigFile(config);
            UIUtils.showAlertDialog("主题设置需要重新启动应用才会生效.是否要重启应用？"
                    ,"提示", Alert.AlertType.CONFIRMATION,config).ifPresent(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    publisher.publishEvent(new RestartEvent());
                } else {
                    Notifications.create()
                            .owner(GUIState.getStage())
                            .hideCloseButton()
                            .hideAfter(Duration.millis(2000))
                            .text("配置已经保存。")
                            .position(Pos.CENTER)
                            .show();
                }
            });
        } catch (Exception e) {
            EncryptException exception = new EncryptException(e);
            UIUtils.showAlertDialog("存储配置文件遇到了问题：" + exception.toString(),"存储失败", Alert.AlertType.ERROR, config);
        }
    }

}

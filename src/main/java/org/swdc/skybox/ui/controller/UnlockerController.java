package org.swdc.skybox.ui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.felixroske.jfxsupport.FXMLController;
import de.felixroske.jfxsupport.GUIState;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.core.DataResolver;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.utils.UIUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@FXMLController
public class UnlockerController implements Initializable {

    @FXML
    private CheckBox chkDelSource;

    @FXML
    private TextField txtPath;

    @FXML
    private PasswordField txtPassword;

    @Autowired
    private List<DataResolver> resolvers;

    @Autowired
    private ApplicationConfig config;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    protected void executeProcess(){
        if (txtPassword.getText() == null || txtPassword.getText().trim().equals("")) {
            UIUtils.showAlertDialog("请输入密钥。","提示", Alert.AlertType.WARNING, config);
            return;
        }
        if (txtPath.getText() == null || txtPath.getText().trim().equals("")) {
            UIUtils.showAlertDialog("请选择要操作的文件。","提示", Alert.AlertType.WARNING, config);
            return;
        }
        try {
            File input = new File(txtPath.getText());
            FileInputStream fileInputStream = new FileInputStream(input);
            ObjectMapper mapper = new ObjectMapper();
            EncryptedHeader header = mapper.readValue(fileInputStream,EncryptedHeader.class);
            fileInputStream.close();

            Class<?> resolverClazz =  Class.forName(header.getResolverName());
            for (DataResolver resolver : resolvers) {
                if (resolver.getClass() == resolverClazz) {
                    resolver.decode(input,txtPassword.getText(),header);
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    protected void chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择文件");
        File file = fileChooser.showOpenDialog(GUIState.getStage());
        if (file!= null) {
            txtPath.setText(file.getPath());
        }
    }

}

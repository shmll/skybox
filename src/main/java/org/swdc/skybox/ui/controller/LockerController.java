package org.swdc.skybox.ui.controller;

import de.felixroske.jfxsupport.FXMLController;
import de.felixroske.jfxsupport.GUIState;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.core.DataResolver;
import org.swdc.skybox.core.dataobject.EncodeLevel;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.ui.view.LockerView;
import org.swdc.skybox.ui.view.ProgressView;
import org.swdc.skybox.utils.UIUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

@FXMLController
public class LockerController implements Initializable {

    @Autowired
    private List<DataResolver> resolvers;

    @Autowired
    private LockerView lockerView;

    @FXML
    private ComboBox<DataResolver> combResolver;

    @FXML
    private ComboBox<EncodeLevel> combLevel;

    @FXML
    private CheckBox cbxDelSource;

    @FXML
    private TextField txtPath;

    @FXML
    private PasswordField txtPassword;

    @Autowired
    private ApplicationConfig config;

    @Autowired
    private ExecutorService asyncExecutor;

    @Autowired
    private ProgressView progressView;

    private ObservableList<DataResolver> resolversList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        combResolver.setItems(resolversList);
        combResolver.getSelectionModel().selectedItemProperty().addListener(this::onResolverChange);
    }

    private void onResolverChange(ObservableValue observableValue, DataResolver itemOld, DataResolver itemNew) {
        if (itemNew != null) {
            combLevel.getItems().clear();
            combLevel.getItems().addAll(itemNew.getLevels());
        }
    }

    @PostConstruct
    private void initData() {
        resolversList.clear();
        resolversList.addAll(resolvers);
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

    @FXML
    protected void chooseFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择文件夹");
        File folder = directoryChooser.showDialog(GUIState.getStage());
        if (folder != null) {
            txtPath.setText(folder.getPath());
        }
    }

    @FXML
    protected void executeProcess() {

        DataResolver resolver = combResolver.getValue();
        if (resolver == null) {
            UIUtils.showAlertDialog("请选择加密算法。","提示", Alert.AlertType.WARNING,config);
            return;
        }
        EncodeLevel level = combLevel.getValue();
        if (level == null) {
            UIUtils.showAlertDialog("请选择运算强度。","提示", Alert.AlertType.WARNING,config);
            return;
        }
        if(txtPath.getText().trim().equals("")) {
            UIUtils.showAlertDialog("请选择加密的文件或目录。","提示", Alert.AlertType.WARNING,config);
            return;
        }
        if (txtPassword.getText().trim().equals("")) {
            UIUtils.showAlertDialog("请输入密钥。","提示", Alert.AlertType.WARNING,config);
            return;
        }

        RadioButton folderOpt = (RadioButton)lockerView.getFolderOptionGroup().getSelectedToggle();

        EncodeOptions options = new EncodeOptions();
        options.setDeleteSource(cbxDelSource.isSelected());
        options.setLevel(level);
        options.setDictionaryMode(EncodeOptions.DictionaryMode.valueOf(folderOpt.getId()));
        options.setResolverClazzName(resolver.getClass().getName());
        File target = new File(txtPath.getText());
        if (!target.exists()) {
            UIUtils.showAlertDialog("文件/目录不存在。","提示", Alert.AlertType.ERROR,config);
            return;
        }

        asyncExecutor.execute(() -> {
            Platform.runLater(() -> {
                progressView.show();
            });

            resolver.encode(target,txtPassword.getText(),options);

            Platform.runLater(() -> {
                progressView.hide();
                UIUtils.showAlertDialog("加密完毕。","提示", Alert.AlertType.WARNING,config);
            });
        });


    }

}

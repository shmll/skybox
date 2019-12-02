package org.swdc.skybox.utils;

import de.felixroske.jfxsupport.GUIState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.PropertyEditor;
import org.springframework.core.io.ClassPathResource;
import org.swdc.skybox.anno.ConfigProp;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.core.dataobject.ConfigProperty;
import org.swdc.skybox.ui.view.PropertyEditors;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Created by lenovo on 2019/5/19.
 */
public class UIUtils {

    /**
     * 初始化node的CSS样式
     * @param pane 被初始化的界面
     * @param config 配置
     * @throws Exception
     */
    public static void configUI(Pane pane, ApplicationConfig config) throws Exception {
        if(config.getTheme().equals("")||config.getTheme().equals("default")){
            pane.getStylesheets().add(new ClassPathResource("style/default.css").getURL().toExternalForm());
        }else{
            pane.getStylesheets().add("file:configs/theme/"+config.getTheme()+"/"+config.getTheme()+".css");
        }
    }

    public static <T> T findById(String id, ObservableList<Node> list){
        for (Node node:list) {
            if(node.getId().equals(id)){
                return (T)node;
            }
        }
        return null;
    }

    public static Optional<ButtonType> showAlertDialog(String content, String title, Alert.AlertType type, ApplicationConfig config) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.initOwner(GUIState.getStage());
        try {
            UIUtils.configUI(alert.getDialogPane(), config);
            return alert.showAndWait();
        }catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ObservableList<PropertySheet.Item> getProperties(Object object) throws Exception {
        ObservableList<PropertySheet.Item> list = FXCollections.observableArrayList();
        Field[] fields = object.getClass().getDeclaredFields();
        for(Field field: fields) {
            if (field.getAnnotation(ConfigProp.class) == null){
                continue;
            }
            ConfigProp propDefinition = field.getAnnotation(ConfigProp.class);
            ConfigProperty property = new ConfigProperty(object,new PropertyDescriptor(field.getName(),object.getClass()),propDefinition);
            list.add(property);
        }
        return list;
    }

    public static PropertyEditor<?> getEditor(PropertySheet.Item prop, ApplicationConfig config) {
        if (!(prop instanceof ConfigProperty)) {
            return null;
        }
        ConfigProperty property = (ConfigProperty) prop;
        ConfigProp propData = property.getPropData();
        switch (propData.type()) {
            case FILE_SELECT_IMPORTABLE:
                return PropertyEditors.createFileImportableEditor(property, config);
            case FOLDER_SELECT_IMPORTABLE:
                return PropertyEditors.createFolderImportableEditor(property);
            case CHECK:
                return PropertyEditors.createCheckedEditor(property);
            case COLOR:
                return PropertyEditors.createColorEditor(property);
            case NUMBER_SELECTABLE:
                return PropertyEditors.createNumberRangeEditor(property);
            case NUMBER:
                return PropertyEditors.createNumberEditor(property);
        }
        return null;
    }

}

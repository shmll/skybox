package org.swdc.skybox.ui.view;

import de.felixroske.jfxsupport.AbstractFxmlView;
import de.felixroske.jfxsupport.FXMLView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.PropertySheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.ui.SubView;
import org.swdc.skybox.utils.UIUtils;

import javax.annotation.PostConstruct;

@FXMLView("/views/ConfigView.fxml")
public class ConfigView extends AbstractFxmlView implements SubView {

    @Autowired
    private ApplicationConfig config;

    @PostConstruct
    public void initUI() throws Exception{
        UIUtils.configUI((BorderPane)getView(), config);
        BorderPane pane = (BorderPane)getView();
        TabPane configTabs = (TabPane) pane.getCenter();

        PropertySheet configSheet = new PropertySheet(UIUtils.getProperties(config));
        configSheet.getStyleClass().add("config-sheet");
        configSheet.setPropertyEditorFactory(item -> UIUtils.getEditor(item, config));
        configSheet.setModeSwitcherVisible(false);
        configTabs.getTabs().add(new Tab("通用配置", configSheet));
    }

    @Override
    public AbstractFxmlView get() {
        return this;
    }

    @Override
    public String getName() {
        return "setting";
    }
}

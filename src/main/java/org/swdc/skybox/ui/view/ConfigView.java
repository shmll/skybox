package org.swdc.skybox.ui.view;

import de.felixroske.jfxsupport.AbstractFxmlView;
import de.felixroske.jfxsupport.FXMLView;
import javafx.scene.layout.BorderPane;
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
        UIUtils.configUI((BorderPane)getView(),config);
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

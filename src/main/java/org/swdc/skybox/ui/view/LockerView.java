package org.swdc.skybox.ui.view;

import de.felixroske.jfxsupport.AbstractFxmlView;
import de.felixroske.jfxsupport.FXMLView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.ui.SubView;
import org.swdc.skybox.utils.UIUtils;

import javax.annotation.PostConstruct;
import static org.swdc.skybox.utils.UIUtils.findById;

@FXMLView("/views/LockerView.fxml")
public class LockerView extends AbstractFxmlView implements SubView {

    @Autowired
    private ApplicationConfig config;

    @Getter
    private ToggleGroup folderOptionGroup = new ToggleGroup();

    @PostConstruct
    public void initUI() throws Exception {
        BorderPane pane = (BorderPane) this.getView();
        UIUtils.configUI(pane, config);
        VBox vBox = (VBox) pane.getCenter();
        HBox hFolders = findById("hbxSec", vBox.getChildren());
        GridPane gridFolder = findById("gradFolders", hFolders.getChildren());
        HBox radiosHbox = (HBox)gridFolder.lookup("#folderRadios");
        radiosHbox.getChildren().forEach(item -> folderOptionGroup.getToggles().add((RadioButton)item));
    }

    @Override
    public AbstractFxmlView get() {
        return this;
    }

    @Override
    public String getName() {
        return "lock";
    }
}

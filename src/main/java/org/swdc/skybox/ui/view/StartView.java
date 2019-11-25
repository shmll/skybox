package org.swdc.skybox.ui.view;

import de.felixroske.jfxsupport.AbstractFxmlView;
import de.felixroske.jfxsupport.FXMLView;
import de.felixroske.jfxsupport.GUIState;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.swdc.skybox.config.ApplicationConfig;
import org.swdc.skybox.events.ViewChangeEvent;
import org.swdc.skybox.ui.AwsomeIconData;
import org.swdc.skybox.ui.SubView;
import org.swdc.skybox.utils.UIUtils;

import javax.annotation.PostConstruct;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.swdc.skybox.utils.UIUtils.findById;

@FXMLView("/views/MainView.fxml")
public class StartView extends AbstractFxmlView {

    @Autowired
    private ApplicationConfig config;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private List<SubView> subViews;

    private ToggleGroup group = new ToggleGroup();

    @PostConstruct
    public void initUI() throws Exception {
        BorderPane pane = (BorderPane) this.getView();
        UIUtils.configUI(pane ,config);
        pane.setStyle(pane.getStyle() + "-fx-background-image:url("  + ApplicationConfig.getConfigLocation() + "images/" + config.getBackground() + ");");

        ToolBar toolBar = (ToolBar)pane.getBottom();
        configButton("setting", toolBar, "cog");
        configButton("lock", toolBar, "lock");
        configButton("unlock", toolBar, "unlock");

        group.selectedToggleProperty().addListener(this::onToggleChange);

        ToggleButton lockToggle = findById("lock", toolBar.getItems());
        lockToggle.setSelected(true);

        subViews.stream().map(AbstractFxmlView.class::cast)
                .map(AbstractFxmlView::getView)
                .map(BorderPane.class::cast)
                .map(BorderPane::prefWidthProperty)
                .forEach(width -> width.bind(pane.widthProperty()));

        subViews.stream().map(AbstractFxmlView.class::cast)
                .map(AbstractFxmlView::getView)
                .map(BorderPane.class::cast)
                .map(BorderPane::prefHeightProperty)
                .forEach(height -> height.bind(pane.widthProperty()));

        Stage stage = GUIState.getStage();
        stage.setMinHeight(460);
        stage.setMinWidth(706);
        stage.setWidth(706);
        stage.setHeight(460);
        stage.setTitle("天空盒");
    }

    private void onToggleChange(Observable observable, Toggle oldTog, Toggle newTog) {
        if (newTog == null) {
            oldTog.setSelected(true);
            return;
        }
        ViewChangeEvent event = new ViewChangeEvent(((ToggleButton)newTog).getId());
        publisher.publishEvent(event);
    }

    private void configButton(String id, ToolBar toolBar, String name) {
        ToggleButton btn = findById(id, toolBar.getItems());
        btn.setFont(AwsomeIconData.getFontIcon());
        btn.setText("" + AwsomeIconData.getAwesomeMap().get(name));
        group.getToggles().add(btn);
    }

    public void changeView(AbstractFxmlView view) {
        Parent parent = view.getView();
        BorderPane pane = (BorderPane) this.getView();
        pane.setCenter(parent);
    }

}

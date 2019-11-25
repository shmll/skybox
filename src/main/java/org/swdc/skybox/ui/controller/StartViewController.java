package org.swdc.skybox.ui.controller;

import de.felixroske.jfxsupport.AbstractFxmlView;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.swdc.skybox.events.ViewChangeEvent;
import org.swdc.skybox.ui.SubView;
import org.swdc.skybox.ui.view.StartView;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@FXMLController
public class StartViewController implements Initializable {

    @Autowired
    private List<SubView> views;

    @Autowired
    private StartView startView;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @PostConstruct
    public void init() {
        views.stream().filter(view->view.getName().equals("lock")).findFirst().ifPresent(view -> {
            Platform.runLater(() -> startView.changeView(view.get()));
        });

    }

    @EventListener(ViewChangeEvent.class)
    public void onViewChange(ViewChangeEvent event) {
        String name = event.getSource();
        Optional<SubView> targetView = views.stream().filter(view->view.getName().equals(name)).findFirst();
        if (targetView.isPresent()) {
            Platform.runLater(() -> startView.changeView((AbstractFxmlView) targetView.get()));
        }
    }

}

package org.swdc.skybox.events;

import org.springframework.context.ApplicationEvent;

public class ViewChangeEvent extends ApplicationEvent {

    public ViewChangeEvent(String viewName) {
        super(viewName);
    }

    @Override
    public String getSource() {
        return source.toString();
    }
}

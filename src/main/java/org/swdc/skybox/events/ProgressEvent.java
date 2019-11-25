package org.swdc.skybox.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

public class ProgressEvent extends ApplicationEvent {

    @Getter
    @Setter
    private String label;

    @Getter
    @Setter
    private Double val;

    public ProgressEvent(String message, int value) {
        super("");
        this.label = message;
        this.val = value / 100.0;
    }
    public ProgressEvent(String message, double value) {
        super("");
        this.label = message;
        this.val = value;
    }

}

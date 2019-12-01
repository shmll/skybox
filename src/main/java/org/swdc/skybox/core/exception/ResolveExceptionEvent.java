package org.swdc.skybox.core.exception;

import org.springframework.context.ApplicationEvent;

public class ResolveExceptionEvent extends ApplicationEvent {


    public ResolveExceptionEvent(Exception source) {
        super(source);
    }

    @Override
    public String getSource() {
        return source.toString();
    }

    public Exception getException() {
        return Exception.class.cast(super.getSource());
    }

}

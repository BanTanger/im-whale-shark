package com.bantanger;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PackApplicationEvent extends ApplicationEvent {

    private final String message;

    public PackApplicationEvent(Object source, String message) {
        super(source);
        this.message = message;
    }

}

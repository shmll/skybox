package org.swdc.skybox.core.dataobject;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EncodeLevel {

    private String name;
    private int value;

    public String toString() {
        return name;
    }

}

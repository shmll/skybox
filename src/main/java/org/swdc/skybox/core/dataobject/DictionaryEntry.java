package org.swdc.skybox.core.dataobject;

import lombok.Data;

@Data
public class DictionaryEntry {
    private String name;
    private String path;
    private long start;
    private long end;
}

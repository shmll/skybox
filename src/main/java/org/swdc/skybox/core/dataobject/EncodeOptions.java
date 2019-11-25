package org.swdc.skybox.core.dataobject;

import lombok.Getter;
import lombok.Setter;
import org.swdc.skybox.core.dataobject.EncodeLevel;

public class EncodeOptions {

    /**
     * 此枚举名字为LockerView中RadioButton的id
     */
    public enum DictionaryMode {
        SINGLE_FILE, MULTI_ARCHIVE, MULTI
    }

    @Getter
    @Setter
    private EncodeLevel level;

    @Getter
    @Setter
    private DictionaryMode dictionaryMode;

    @Getter
    @Setter
    private Boolean deleteSource;

    @Getter
    @Setter
    private String resolverClazzName;

}

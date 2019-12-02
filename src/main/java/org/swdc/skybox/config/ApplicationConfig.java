package org.swdc.skybox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.swdc.skybox.anno.ConfigProp;
import org.swdc.skybox.anno.PropType;

@Component
@ConfigurationProperties(prefix = "app")
@PropertySource("file:config/config.properties")
public class ApplicationConfig {

    @Getter
    @Setter
    @ConfigProp(name = "主题",type = PropType.FOLDER_SELECT_IMPORTABLE,
            value = "config/theme", tooltip = "应用的主题", propName = "theme")
    private String theme;

    @Getter
    @Setter
    @ConfigProp(name = "背景图片", type = PropType.FILE_SELECT_IMPORTABLE,
            value = "config/images/background", tooltip = "背景图片，一般是配合主题使用。", propName = "background")
    private String background;

    public static String getConfigLocation() {
        return "file:config/";
    }

}

package org.swdc.skybox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@PropertySource("file:config/config.properties")
public class ApplicationConfig {

    @Getter
    @Setter
    private String theme;

    @Getter
    @Setter
    private String background;

    public static String getConfigLocation() {
        return "file:config/";
    }

}

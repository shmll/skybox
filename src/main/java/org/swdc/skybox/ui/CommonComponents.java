package org.swdc.skybox.ui;

import javafx.scene.text.Font;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lenovo on 2019/5/19.
 */
@Configuration
public class CommonComponents {

    @Getter
    private static Map<String, Font> fontMap;

    static {
        fontMap = new LinkedHashMap<>();
        File fonts = new File("config/fonts");
        if (!fonts.exists()) {
            fonts.mkdir();
        }
        try {
            FileInputStream fConfig = new FileInputStream("config/config.properties");
            Properties properties = new Properties();
            properties.load(fConfig);
            Font.loadFont(new File("config/theme/" + properties.getProperty("app.theme") + "/system.ttf").toURI().toURL().toExternalForm(), 18);
            fConfig.close();
            for(File fontFile : fonts.listFiles()){
                Font font = Font.loadFont(fontFile.toURI().toURL().toExternalForm(), 18);
                fontMap.put(fontFile.getName(), font);
            }
        } catch (Exception ex) {

        }
    }

    @Bean
    public ExecutorService asyncExecutor() {
        return Executors.newFixedThreadPool(5);
    }

}

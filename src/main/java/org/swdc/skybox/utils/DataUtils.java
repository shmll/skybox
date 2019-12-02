package org.swdc.skybox.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.swdc.skybox.anno.ConfigProp;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Properties;

public class DataUtils {

    public static int countFiles(File file) {
        if (file.isFile() && file.exists()) {
            return 1;
        } else {
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                return 0;
            } else {
                int count = 0;
                for (File item : files) {
                    if (item.isDirectory()) {
                        count = count + countFiles(item);
                    } else {
                        count ++;
                    }
                }
                return count;
            }
        }
    }

    public static void deleteFile(File item) {
        if (!item.exists()) {
            return;
        }
        if (item.isDirectory()) {
            File[] files = item.listFiles();
            if (files == null || files.length == 0) {
                if(!item.delete()) {
                    item.deleteOnExit();
                }
                return;
            }
            for (File file: files) {
                deleteFile(file);
            }
            if(!item.delete()){
                item.deleteOnExit();
            }
        } else {
            if(!item.delete()) {
                item.deleteOnExit();
            }
        }
    }

    public static void saveConfigFile(Object config) throws Exception {
        Field[] fields = config.getClass().getDeclaredFields();
        ConfigurationProperties prop = config.getClass().getAnnotation(ConfigurationProperties.class);
        PropertySource propSource = config.getClass().getAnnotation(PropertySource.class);
        String name = propSource.value()[0].substring(propSource.value()[0].lastIndexOf("/"));
        String prefix = prop.prefix();
        Properties props = new Properties();
        props.load(new FileInputStream("./config/" + name));
        for(Field field: fields) {
            if (field.getAnnotation(ConfigProp.class) == null) {
                continue;
            }
            PropertyDescriptor desc = new PropertyDescriptor(field.getName(),config.getClass());
            props.setProperty(prefix +"."+ field.getAnnotation(ConfigProp.class).propName(), desc.getReadMethod().invoke(config).toString());
        }
        props.store(new FileOutputStream("./config/" + name), "");
    }

}

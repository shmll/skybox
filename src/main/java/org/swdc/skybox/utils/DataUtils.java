package org.swdc.skybox.utils;

import java.io.File;

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

}

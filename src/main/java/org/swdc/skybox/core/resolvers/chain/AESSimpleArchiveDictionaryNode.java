package org.swdc.skybox.core.resolvers.chain;

import org.swdc.skybox.core.DataResolver;
import org.swdc.skybox.core.exception.EncryptException;
import org.swdc.skybox.core.exception.InvalidPasswordException;
import org.swdc.skybox.core.exception.NotSupportException;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.events.ProgressEvent;
import org.swdc.skybox.utils.DataUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AESSimpleArchiveDictionaryNode extends AESSimpleMultiDictionaryNode {

    public AESSimpleArchiveDictionaryNode(DataResolver resolver) {
        super(resolver);
    }


    @Override
    public boolean supportEnc(File input, String password, EncodeOptions options) {
        if (!input.exists()) {
            return false;
        }
        if (!input.isDirectory()) {
            return false;
        }
        if (options.getDictionaryMode() != EncodeOptions.DictionaryMode.MULTI_ARCHIVE) {
            return false;
        }
        return true;
    }

    @Override
    public boolean supportDec(File input, String password, EncryptedHeader header) {
        return super.supportDec(input, password, header);
    }

    @Override
    public void doEncrypt(File input, String password, EncodeOptions options) throws NotSupportException, EncryptException {
        super.doEncrypt(input,password,options);
        try {
            File parent = input.getAbsoluteFile().getParentFile();
            int count = DataUtils.countFiles(input);
            File archive = new File(parent.getPath() + File.separator + input.getName() + ".zip");
            FileOutputStream fileOutputStream = new FileOutputStream(archive);
            ZipOutputStream outputStream = new ZipOutputStream(fileOutputStream);
            getResolver().emitEvent(new ProgressEvent("正在压缩.", 0));
            archive(parent,input, outputStream, new ArrayList<>(), count);
            outputStream.flush();
            outputStream.close();
            fileOutputStream.close();
            if (options.getDeleteSource()) {
                DataUtils.deleteFile(input);
            }
        }catch (EncryptException|NotSupportException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void archive(File parent, File file, ZipOutputStream outputStream, List<String> paths, int count) throws Exception{
        String path = parent.toURI().relativize(file.toURI()).getPath();
        if (file.isDirectory()) {

            getResolver().emitEvent(new ProgressEvent("正在压缩：" + file.getName(), Double.valueOf(paths.size()) / count));

            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File item : files) {
                archive(parent,item,outputStream,paths,count);
            }
        } else {
            if (!file.getName().endsWith("skysecrity")) {
                return;
            }
            paths.add(path);
            getResolver().emitEvent(new ProgressEvent("正在压缩：" + file.getName(), Double.valueOf(paths.size()) / count));
            ZipEntry entry = new ZipEntry(path);
            outputStream.putNextEntry(entry);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024 * 1024];
            while (fileInputStream.read(buffer) > 0) {
                outputStream.write(buffer);
            }
            outputStream.closeEntry();
        }
    }

}

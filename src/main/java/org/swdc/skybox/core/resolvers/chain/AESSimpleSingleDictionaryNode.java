package org.swdc.skybox.core.resolvers.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.swdc.skybox.core.*;
import org.swdc.skybox.core.dataobject.DictionaryEntry;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.core.exception.InvalidPasswordException;
import org.swdc.skybox.core.exception.NotSupportException;
import org.swdc.skybox.core.resolvers.AESSimpleResolver;
import org.swdc.skybox.events.ProgressEvent;
import org.swdc.skybox.utils.DataUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.Key;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class AESSimpleSingleDictionaryNode implements ResolveChain {

    private ThreadLocal<Long> position = new ThreadLocal<>();

    private DataResolver resolver;

    @Getter
    @Setter
    private ResolveChain next;

    public AESSimpleSingleDictionaryNode(DataResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean supportEnc(File input, String password, EncodeOptions options) {
        if (input.isFile()) {
            return false;
        } else if (options.getDictionaryMode() == EncodeOptions.DictionaryMode.SINGLE_FILE) {
            if (input.isDirectory()) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean supportDec(File input, String password, EncryptedHeader header) {
        if (!header.getIsFolder()) {
            return false;
        }
        if (header.getFolderOffset() <= 0) {
            return false;
        }
        return true;
    }

    @Override
    public void doEncrypt(File input, String password, EncodeOptions options) throws NotSupportException {
        if (!supportEnc(input,password,options)) {
            if (next != null) {
                next.doEncrypt(input,password,options);
                return;
            } else {
                throw new NotSupportException();
            }
        }
        try {
            // 临时文件
            File encoded = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + input.getName() + ".temp");
            // 加密为一个文件的时候
            FileOutputStream outputStream = new FileOutputStream(encoded);
            // 用来描述文件夹的entry
            List<DictionaryEntry> entries = new ArrayList<>();
            resolver.emitEvent(new ProgressEvent("寻找文件", 10));
            // 文件总数
            int fileCount = DataUtils.countFiles(input);
            position.set(0L);
            // 执行加密
            resolver.emitEvent(new ProgressEvent("文件统计完毕，开始执行", 100));
            encodeFileEntry(input.getAbsoluteFile().getParentFile(),input,password,options,entries,outputStream,fileCount);

            outputStream.flush();
            outputStream.close();

            resolver.emitEvent(new ProgressEvent("数据写入完毕，正在计算header", 20));

            // 复制文件，借以写入header
            File target = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + input.getName() + ".skysecrity");
            if (target.exists()) {
                target.delete();
            }
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(target));

            ObjectMapper mapper = new ObjectMapper();
            EncryptedHeader header = new EncryptedHeader(options.getLevel().getValue()+"",resolver.getClass().getName(),true,entries);
            byte[] headerData = mapper.writeValueAsBytes(header);
            header.setFolderOffset(headerData.length);
            headerData = mapper.writeValueAsBytes(header);
            header.setFolderOffset(headerData.length);
            headerData = mapper.writeValueAsBytes(header);
            dataOutputStream.write(headerData);

            resolver.emitEvent(new ProgressEvent("正在结束",0));

            DataInputStream dataInputStream = new DataInputStream(new FileInputStream(encoded));
            byte[] buffer = new byte[AESSimpleResolver.AES_ENCODE_BLOCK];
            long filesize = 0;
            while ((dataInputStream.read(buffer) > 0)) {
                filesize = filesize + buffer.length;
                resolver.emitEvent(new ProgressEvent("正在结束", filesize / encoded.length()));
                dataOutputStream.write(buffer);
            }
            dataOutputStream.flush();

            dataInputStream.close();
            dataOutputStream.close();
            DataUtils.deleteFile(encoded);
            position.set(0L);
            if (options.getDeleteSource()) {
                DataUtils.deleteFile(input);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void doDecrypt(File input, String password, EncryptedHeader header) throws NotSupportException, InvalidPasswordException {
        if (!this.supportDec(input,password,header)) {
            if (this.next != null) {
                this.next.doDecrypt(input,password,header);
                return;
            } else {
                throw new NotSupportException();
            }
        }
        try {
            RandomAccessFile file = new RandomAccessFile(input, "r");

            for (DictionaryEntry entry : header.getEntries()) {
                decodeFileEntry(file,entry,null);
            }

        } catch (Exception ex) {

        }
    }

    private void decodeFileEntry(RandomAccessFile file, DictionaryEntry entry, Key key) {

    }

    private void encodeFileEntry(File parent, File file, String password, EncodeOptions options, List<DictionaryEntry> data, FileOutputStream outputStream, int count) throws Exception{
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0){
                return;
            }
            for (File item : files) {
                encodeFileEntry(parent, item, password, options, data, outputStream, count);
            }
            return;
        }

        if (file.getName().endsWith("skysecrity")) {
            return;
        }

        KeyGenerator generator = KeyGenerator.getInstance("AES");
        if (options.getLevel().getValue() == 0) {
            generator.init(128, new SecureRandom(password.getBytes("utf8")));
        } else {
            generator.init(256, new SecureRandom(password.getBytes("utf8")));
        }
        resolver.emitEvent(new ProgressEvent("正在准备密钥：" + file.getName(), Double.valueOf(data.size()) / count));
        SecretKey keyX = generator.generateKey();
        byte[] rawX = keyX.getEncoded();
        SecretKey keyY = new SecretKeySpec(rawX, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keyY);
        FileInputStream dataInputStream = new FileInputStream(file);
        byte[] dataBytes = new byte[1024 * 1024];
        long passed = 0;
        resolver.emitEvent(new ProgressEvent("写入数据：" + file.getName(), Double.valueOf(data.size()) / count));
        while ((dataInputStream.read(dataBytes)) > 0) {
            byte[] enc = cipher.doFinal(dataBytes);
            passed = passed + enc.length;
            outputStream.write(enc);
        }
        dataInputStream.close();
        outputStream.flush();
        DictionaryEntry entry = new DictionaryEntry();
        String path = parent.getAbsoluteFile().toURI().relativize(file.toURI()).getPath();
        entry.setPath(path);
        entry.setStart(position.get());
        entry.setEnd(position.get() + passed);
        entry.setName(file.getName());
        data.add(entry);
        position.set(position.get() + passed);
    }

}

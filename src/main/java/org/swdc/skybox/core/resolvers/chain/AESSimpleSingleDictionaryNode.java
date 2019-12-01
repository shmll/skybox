package org.swdc.skybox.core.resolvers.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.swdc.skybox.core.*;
import org.swdc.skybox.core.dataobject.DictionaryEntry;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.core.exception.EncryptException;
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
    public void doEncrypt(File input, String password, EncodeOptions options) throws NotSupportException, EncryptException {
        if (!supportEnc(input,password,options)) {
            if (next != null) {
                next.doEncrypt(input,password,options);
                return;
            } else {
                throw new NotSupportException();
            }
        }
        // 指向临时文件的stream
        FileOutputStream tempOutputStream = null;
        // 指向加密后文件的stream
        DataOutputStream encodeTargetOutputStream = null;
        // 执行临时文件的输入流
        DataInputStream tempInputStream = null;
        try {
            // 临时文件
            File encoded = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + input.getName() + ".temp");
            tempOutputStream = new FileOutputStream(encoded);
            // 用来描述文件夹的entry
            List<DictionaryEntry> entries = new ArrayList<>();
            resolver.emitEvent(new ProgressEvent("寻找文件", 10));
            // 文件总数
            int fileCount = DataUtils.countFiles(input);
            position.set(0L);
            // 执行加密
            resolver.emitEvent(new ProgressEvent("文件统计完毕，开始执行", 100));
            Key key = AESSimpleResolver.generateAESKey(password, options.getLevel().getValue());
            encodeFileEntry(input.getAbsoluteFile().getParentFile(),input,key,entries,tempOutputStream,fileCount);

            tempOutputStream.flush();
            tempOutputStream.close();

            resolver.emitEvent(new ProgressEvent("数据写入完毕，正在计算header", 20));

            // 复制文件，借以写入header
            File target = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + input.getName() + ".skysecrity");
            if (target.exists()) {
                target.delete();
            }
            encodeTargetOutputStream = new DataOutputStream(new FileOutputStream(target));

            ObjectMapper mapper = new ObjectMapper();
            EncryptedHeader header = new EncryptedHeader(options.getLevel().getValue()+"",resolver.getClass().getName(),true,entries);
            byte[] headerData = mapper.writeValueAsBytes(header);
            header.setFolderOffset(headerData.length);
            headerData = mapper.writeValueAsBytes(header);
            header.setFolderOffset(headerData.length);
            headerData = mapper.writeValueAsBytes(header);
            encodeTargetOutputStream.write(headerData);

            resolver.emitEvent(new ProgressEvent("正在结束",0));

            tempInputStream = new DataInputStream(new FileInputStream(encoded));
            byte[] buffer = new byte[AESSimpleResolver.AES_ENCODE_BLOCK];
            long filesize = 0;
            while ((tempInputStream.read(buffer) > 0)) {
                filesize = filesize + buffer.length;
                resolver.emitEvent(new ProgressEvent("正在结束", filesize / encoded.length()));
                encodeTargetOutputStream.write(buffer);
            }
            encodeTargetOutputStream.flush();
            DataUtils.deleteFile(encoded);
            position.set(0L);
            if (options.getDeleteSource()) {
                DataUtils.deleteFile(input);
            }
        } catch (EncryptException|NotSupportException ex) {
            throw ex;
        }catch (Exception ex) {
            throw new EncryptException(ex);
        } finally {
            try {
                tempInputStream.close();
                tempOutputStream.close();
                encodeTargetOutputStream.close();
            } catch (Exception ex) {
                throw new EncryptException(ex);
            }
        }
    }

    @Override
    public void doDecrypt(File input, String password, EncryptedHeader header) throws NotSupportException, InvalidPasswordException, EncryptException {
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
            resolver.emitEvent(new ProgressEvent("正在准备密钥", 10));
            Key key = AESSimpleResolver.generateAESKey(password,Integer.valueOf(header.getEncodeLevel()));
            for (DictionaryEntry entry : header.getEntries()) {
                resolver.emitEvent(new ProgressEvent("正在解密：" + entry.getName(), header.getEntries().indexOf(entry)/Double.valueOf(header.getEntries().size())));
                decodeFileEntry(input.getAbsoluteFile().getParentFile(),header,file,entry,key);
            }

            resolver.emitEvent(new ProgressEvent("完成",100));
            file.close();
        }catch (InvalidPasswordException|EncryptException|NotSupportException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EncryptException(ex);
        }
    }

    private void decodeFileEntry(File parent, EncryptedHeader header, RandomAccessFile file, DictionaryEntry entry, Key key) throws Exception {
        String filePath = entry.getPath();
        File dictionary = new File(resolvePath(filePath, parent.getPath()));
        if (!dictionary.exists()) {
            resolver.emitEvent(new ProgressEvent("创建目录", 10));
            dictionary.mkdirs();
        }
        File decode = new File(dictionary.getAbsolutePath() + File.separator + entry.getName());
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(decode);
            file.seek(header.getFolderOffset() + entry.getStart());

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] dataBuffer = new byte[AESSimpleResolver.AES_DECODE_BLOCK];
            long encodeLength = entry.getEnd() - entry.getStart();
            while (encodeLength > 0) {
                if (encodeLength >= AESSimpleResolver.AES_DECODE_BLOCK) {
                    file.read(dataBuffer);
                    encodeLength = encodeLength - AESSimpleResolver.AES_DECODE_BLOCK;
                } else {
                    dataBuffer = new byte[(int) encodeLength];
                    file.read(dataBuffer);
                    encodeLength = 0;
                }
                byte[] data = cipher.doFinal(dataBuffer);
                outputStream.write(data);
                resolver.emitEvent(new ProgressEvent("正在写入数据：" + entry.getName(), 1 - Double.valueOf(encodeLength)/(entry.getEnd() - entry.getStart())));
            }
            outputStream.flush();

        } catch (Exception ex) {
            throw new EncryptException(ex);
        } finally {
            try {
                outputStream.close();
            } catch (Exception ex) {
                throw new EncryptException(ex);
            }
        }

    }

    private String resolvePath(String path, String base) {
        String[] pathItems = path.split("/");
        StringBuilder stringBuilder = new StringBuilder(base);
        for (String item : pathItems) {
            if (item.split("[.]").length > 1) {
                break;
            }
            stringBuilder.append(File.separator).append(item);
        }
        return stringBuilder.toString();
    }

    private void encodeFileEntry(File parent, File file, Key key, List<DictionaryEntry> data, FileOutputStream outputStream, int count) throws Exception{
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0){
                return;
            }
            for (File item : files) {
                encodeFileEntry(parent, item, key, data, outputStream, count);
            }
            return;
        }

        if (file.getName().endsWith("skysecrity")) {
            return;
        }

        resolver.emitEvent(new ProgressEvent("正在准备密钥：" + file.getName(), Double.valueOf(data.size()) / count));
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        FileInputStream dataInputStream = new FileInputStream(file);
        byte[] dataBytes = new byte[AESSimpleResolver.AES_ENCODE_BLOCK];
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

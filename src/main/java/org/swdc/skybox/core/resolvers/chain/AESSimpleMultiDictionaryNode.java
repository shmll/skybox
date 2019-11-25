package org.swdc.skybox.core.resolvers.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.swdc.skybox.core.DataResolver;
import org.swdc.skybox.core.NotSupportException;
import org.swdc.skybox.core.ResolveChain;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.events.ProgressEvent;
import org.swdc.skybox.utils.DataUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;

public class AESSimpleMultiDictionaryNode implements ResolveChain {

    private DataResolver resolver;

    private ThreadLocal<Integer> counter = new ThreadLocal<>();

    @Getter
    @Setter
    private ResolveChain next;

    public AESSimpleMultiDictionaryNode(DataResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean supportEnc(File input, String password, EncodeOptions options) {
        if(!input.exists()) {
            return false;
        }
        if (!input.isDirectory()) {
            return false;
        }
        if (options.getDictionaryMode() != EncodeOptions.DictionaryMode.MULTI) {
            return false;
        }
        return true;
    }

    @Override
    public boolean supportDec(File input, String password, EncryptedHeader header) {
        return false;
    }

    protected DataResolver getResolver() {
        return resolver;
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
        counter.set(0);
        try {
            resolver.emitEvent(new ProgressEvent("正在寻找文件", 10));
            File[] files = input.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            int counts = DataUtils.countFiles(input);
            resolver.emitEvent(new ProgressEvent("开始处理", 100));
            for (File item : files) {
                encodeFile(item, password,options,counts);
            }
            resolver.emitEvent(new ProgressEvent("完毕", 100));
        } catch (Exception ex) {

        }
    }

    private void encodeFile(File input, String password, EncodeOptions options, int counts) {
        if (input.isDirectory()) {
            File[] files = input.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File item : files) {
                encodeFile(item,password,options, counts);
            }
        }
        counter.set(counter.get() + 1);
        try {
            if (input.getName().endsWith("skysecrity")) {
                return;
            } else if (input.isDirectory()) {
                return;
            }
            resolver.emitEvent(new ProgressEvent("正在计算密钥：" + input.getName(), counter.get() / counts));
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            if (options.getLevel().getValue() == 0) {
                generator.init(128, new SecureRandom(password.getBytes("utf8")));
            } else {
                generator.init(256, new SecureRandom(password.getBytes("utf8")));
            }
            SecretKey keyX = generator.generateKey();
            byte[] rawX = keyX.getEncoded();
            SecretKey keyY = new SecretKeySpec(rawX, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keyY);

            String fileName = input.getName().split("[.]")[0];
            File encoded = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + fileName + ".skysecrity");
            if(encoded.exists()) {
                encoded.delete();
            }

            FileInputStream fileInputStream = new FileInputStream(input);
            DataInputStream dataInputStream = new DataInputStream(fileInputStream);
            ObjectMapper mapper = new ObjectMapper();
            EncryptedHeader header = new EncryptedHeader(options.getLevel().getValue()+"",resolver.getClass().getName(),false,null);
            header.setName(input.getName());
            byte[] headerData = mapper.writeValueAsBytes(header);
            FileOutputStream outputStream = new FileOutputStream(encoded);
            outputStream.write(headerData);
            resolver.emitEvent(new ProgressEvent("正在写入数据：" + input.getName(), Double.valueOf(counter.get()) / counts));
            byte[] data = new byte[1024 * 1024];
            long counter = 0;
            while ((dataInputStream.read(data)) > 0) {
                counter = counter + data.length;
                resolver.emitEvent(new ProgressEvent("正在写入数据：" + input.getName(), Double.valueOf(counter) / input.length()));
                outputStream.write(cipher.doFinal(data));
            }
            outputStream.flush();
            outputStream.close();
            dataInputStream.close();
            fileInputStream.close();
            if (options.getDeleteSource()) {
                DataUtils.deleteFile(input);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void doDecrypt(File input, String password, EncryptedHeader header) {

    }

}

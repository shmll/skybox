package org.swdc.skybox.core.resolvers.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.swdc.skybox.core.*;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.core.exception.InvalidPasswordException;
import org.swdc.skybox.core.exception.NotSupportException;
import org.swdc.skybox.core.resolvers.AESSimpleResolver;
import org.swdc.skybox.events.ProgressEvent;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class AESSimpleFileNode implements ResolveChain {

    private DataResolver resolver;

    @Setter
    private ResolveChain next;

    public AESSimpleFileNode(DataResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean supportEnc(File input, String password, EncodeOptions options) {
        if (input.isDirectory()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean supportDec(File input, String password, EncryptedHeader header) {
        if (!header.getResolverName().equals(this.resolver.getClass().getName())) {
            return false;
        }
        if (header.getIsFolder()) {
            return false;
        }
        return true;
    }

    @Override
    public void doEncrypt(File input, String password, EncodeOptions options) throws NotSupportException {
        if (!supportEnc(input,password,options)) {
            if (next != null){
                next.doEncrypt(input,password,options);
                return;
            } else {
                throw new NotSupportException();
            }
        }
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG") ;
            secureRandom.setSeed(password.getBytes(StandardCharsets.UTF_8));
            if (options.getLevel().getValue() == 0) {
                generator.init(128, secureRandom);
            } else {
                generator.init(256, secureRandom);
            }

            resolver.emitEvent(new ProgressEvent("正在准备密钥", 4));

            SecretKey keyX = generator.generateKey();
            byte[] rawX = keyX.getEncoded();
            SecretKey keyY = new SecretKeySpec(rawX, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keyY);

            String name = input.getName().split("[.]")[0];

            File encoded = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + name + ".skysecrity");

            if (encoded.exists()) {
                encoded.delete();
            }

            FileInputStream fileInputStream = new FileInputStream(input);
            DataInputStream dataInputStream = new DataInputStream(fileInputStream);

            ObjectMapper mapper = new ObjectMapper();
            EncryptedHeader header = new EncryptedHeader(options.getLevel().getValue()+"",resolver.getClass().getName(),false,null);
            header.setName(input.getName());
            byte[] headerData = mapper.writeValueAsBytes(header);
            resolver.emitEvent(new ProgressEvent("正在写入header", 8));

            FileOutputStream outputStream = new FileOutputStream(encoded);
            outputStream.write(headerData);
            resolver.emitEvent(new ProgressEvent("写入数据", 10));
            byte[] data = new byte[AESSimpleResolver.AES_ENCODE_BLOCK];
            long passed = 0;
            while ((dataInputStream.read(data)) > 0) {
                passed = passed + data.length;
                double prog = ((double) passed / input.length());
                resolver.emitEvent(new ProgressEvent("正在写入数据", prog));
                outputStream.write(cipher.doFinal(data));
            }
            resolver.emitEvent(new ProgressEvent("完成", 100));
            outputStream.flush();
            outputStream.close();
            dataInputStream.close();
            fileInputStream.close();
            if (options.getDeleteSource()) {
                input.delete();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void doDecrypt(File input, String password, EncryptedHeader header) throws InvalidPasswordException, NotSupportException {
        if (!supportDec(input,password,header)) {
            if (this.next != null) {
                this.next.doDecrypt(input,password,header);
                return;
            } else {
                throw new NotSupportException();
            }
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            byte[] data = mapper.writeValueAsBytes(header);

            FileInputStream fileInputStream = new FileInputStream(input);
            fileInputStream.skip(data.length);

            File decode = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + header.getName());

            KeyGenerator generator = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG") ;
            secureRandom.setSeed(password.getBytes(StandardCharsets.UTF_8));

            if (Integer.valueOf(header.getEncodeLevel()) == 0) {
                generator.init(128, secureRandom);
            } else {
                generator.init(256, secureRandom);
            }
            SecretKey keyX = generator.generateKey();
            byte[] rawX = keyX.getEncoded();
            SecretKey keyY = new SecretKeySpec(rawX, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keyY);

            FileOutputStream outputStream = new FileOutputStream(decode);

            try {
                byte[] buffer = new byte[AESSimpleResolver.AES_DECODE_BLOCK];
                while (fileInputStream.read(buffer) > 0) {
                    byte[] decodeData = cipher.doFinal(buffer);
                    outputStream.write(decodeData);
                }
                outputStream.flush();
                outputStream.close();
                fileInputStream.close();
            } catch (Exception ex) {
                throw new InvalidPasswordException(ex);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public ResolveChain getNext() {
        return this.next;
    }
}

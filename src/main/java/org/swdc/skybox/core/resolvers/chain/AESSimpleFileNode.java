package org.swdc.skybox.core.resolvers.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.swdc.skybox.core.*;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.core.exception.EncryptException;
import org.swdc.skybox.core.exception.InvalidPasswordException;
import org.swdc.skybox.core.exception.NotSupportException;
import org.swdc.skybox.core.resolvers.AESSimpleResolver;
import org.swdc.skybox.events.ProgressEvent;

import javax.crypto.Cipher;
import java.io.*;
import java.security.Key;

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
    public void doEncrypt(File input, String password, EncodeOptions options) throws NotSupportException, EncryptException {
        if (!supportEnc(input,password,options)) {
            if (next != null){
                next.doEncrypt(input,password,options);
                return;
            } else {
                throw new NotSupportException();
            }
        }
        FileInputStream inputFileStream = null;
        DataInputStream inputWrapperStream = null;
        FileOutputStream outputStream = null;
        try {
            resolver.emitEvent(new ProgressEvent("正在准备密钥", 4));

            Key key = AESSimpleResolver.generateAESKey(password,options.getLevel().getValue());
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            String name = input.getName().split("[.]")[0];
            File encoded = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + name + ".skysecrity");
            if (encoded.exists()) {
                encoded.delete();
            }
            inputFileStream = new FileInputStream(input);
            inputWrapperStream = new DataInputStream(inputFileStream);

            ObjectMapper mapper = new ObjectMapper();
            EncryptedHeader header = new EncryptedHeader(options.getLevel().getValue()+"",resolver.getClass().getName(),false,null);
            header.setName(input.getName());
            byte[] headerData = mapper.writeValueAsBytes(header);
            resolver.emitEvent(new ProgressEvent("正在写入header", 8));

            outputStream = new FileOutputStream(encoded);
            outputStream.write(headerData);
            resolver.emitEvent(new ProgressEvent("写入数据", 10));
            byte[] data = new byte[AESSimpleResolver.AES_ENCODE_BLOCK];
            long passed = 0;
            while ((inputWrapperStream.read(data)) > 0) {
                passed = passed + data.length;
                double prog = ((double) passed / input.length());
                resolver.emitEvent(new ProgressEvent("正在写入数据", prog));
                outputStream.write(cipher.doFinal(data));
            }
            resolver.emitEvent(new ProgressEvent("完成", 100));
            outputStream.flush();

            if (options.getDeleteSource()) {
                input.delete();
            }
        } catch (EncryptException|NotSupportException ex) {
            throw ex;
        }catch (Exception ex) {
            throw new EncryptException(ex);
        } finally {
            try {
                outputStream.close();
                inputWrapperStream.close();
                inputFileStream.close();
            } catch (Exception ex) {
                throw new EncryptException(ex);
            }
        }
    }

    @Override
    public void doDecrypt(File input, String password, EncryptedHeader header) throws InvalidPasswordException, NotSupportException, EncryptException {
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

            resolver.emitEvent(new ProgressEvent("正在准备密钥", 10));
            Key key = AESSimpleResolver.generateAESKey(password,Integer.valueOf(header.getEncodeLevel()));
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            FileOutputStream outputStream = new FileOutputStream(decode);

            try {
                byte[] buffer = new byte[AESSimpleResolver.AES_DECODE_BLOCK];
                long passed = 0;
                while (fileInputStream.read(buffer) > 0) {
                    byte[] decodeData = cipher.doFinal(buffer);
                    passed = passed + decodeData.length;
                    resolver.emitEvent(new ProgressEvent("正在写入数据", Double.valueOf(passed)/input.length()));
                    outputStream.write(decodeData);
                }
                resolver.emitEvent(new ProgressEvent("完成", 100));
            } catch (Exception ex) {
                throw new InvalidPasswordException(ex);
            } finally {
                outputStream.flush();
                outputStream.close();
                fileInputStream.close();
            }
        } catch (InvalidPasswordException|EncryptException|NotSupportException ex) {
           throw ex;
        } catch (Exception ex) {
            throw new EncryptException(ex);
        }

    }

    @Override
    public ResolveChain getNext() {
        return this.next;
    }
}

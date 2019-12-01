package org.swdc.skybox.core.resolvers.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.swdc.skybox.core.DataResolver;
import org.swdc.skybox.core.exception.EncryptException;
import org.swdc.skybox.core.exception.InvalidPasswordException;
import org.swdc.skybox.core.exception.NotSupportException;
import org.swdc.skybox.core.ResolveChain;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.core.resolvers.AESSimpleResolver;
import org.swdc.skybox.events.ProgressEvent;
import org.swdc.skybox.utils.DataUtils;

import javax.crypto.Cipher;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;

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
    public void doEncrypt(File input, String password, EncodeOptions options) throws NotSupportException, EncryptException {
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
            Key key = AESSimpleResolver.generateAESKey(password,options.getLevel().getValue());
            for (File item : files) {
                encodeFile(item,options,key,counts);
            }
            resolver.emitEvent(new ProgressEvent("完毕", 100));
        }catch (EncryptException|NotSupportException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EncryptException(ex);
        }
    }

    private void encodeFile(File input, EncodeOptions options, Key key, int counts) throws EncryptException {
        if (input.isDirectory()) {
            File[] files = input.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File item : files) {
                encodeFile(item,options, key, counts);
            }
        }
        counter.set(counter.get() + 1);

        FileInputStream encodeInputStream = null;
        DataInputStream encodeWrapperStrame = null;
        FileOutputStream outputStream = null;
        try {
            if (input.getName().endsWith("skysecrity")) {
                return;
            } else if (input.isDirectory()) {
                return;
            }
            resolver.emitEvent(new ProgressEvent("正在计算密钥：" + input.getName(), counter.get() / counts));
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            String fileName = input.getName().split("[.]")[0];
            File encoded = new File(input.getAbsoluteFile().getParentFile().getPath() + File.separator + fileName + ".skysecrity");
            if(encoded.exists()) {
                encoded.delete();
            }

            encodeInputStream = new FileInputStream(input);
            encodeWrapperStrame = new DataInputStream(encodeInputStream);
            ObjectMapper mapper = new ObjectMapper();
            EncryptedHeader header = new EncryptedHeader(options.getLevel().getValue()+"",resolver.getClass().getName(),false,null);
            header.setName(input.getName());
            byte[] headerData = mapper.writeValueAsBytes(header);
            outputStream = new FileOutputStream(encoded);
            outputStream.write(headerData);
            resolver.emitEvent(new ProgressEvent("正在写入数据：" + input.getName(), Double.valueOf(counter.get()) / counts));
            byte[] data = new byte[1024 * 1024];
            long counter = 0;
            while ((encodeWrapperStrame.read(data)) > 0) {
                counter = counter + data.length;
                resolver.emitEvent(new ProgressEvent("正在写入数据：" + input.getName(), Double.valueOf(counter) / input.length()));
                outputStream.write(cipher.doFinal(data));
            }
            outputStream.flush();
            if (options.getDeleteSource()) {
                DataUtils.deleteFile(input);
            }
        } catch (Exception ex) {
            throw new EncryptException(ex);
        } finally {
            try {
                encodeInputStream.close();
                encodeWrapperStrame.close();
                outputStream.close();
            } catch (Exception ex) {
                throw new EncryptException(ex);
            }
        }
    }

    @Override
    public void doDecrypt(File input, String password, EncryptedHeader header) {

    }

}

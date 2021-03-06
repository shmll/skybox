package org.swdc.skybox.core.resolvers;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.swdc.skybox.core.*;
import org.swdc.skybox.core.dataobject.EncodeLevel;
import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.resolvers.chain.AESSimpleArchiveDictionaryNode;
import org.swdc.skybox.core.resolvers.chain.AESSimpleFileNode;
import org.swdc.skybox.core.resolvers.chain.AESSimpleMultiDictionaryNode;
import org.swdc.skybox.core.resolvers.chain.AESSimpleSingleDictionaryNode;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
public class AESSimpleResolver implements DataResolver {

    private List<EncodeLevel> encodeLevels;

    public static final int AES_DECODE_BLOCK = 1048592;
    public static final int AES_ENCODE_BLOCK = 1024 * 1024;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Getter
    private ResolveChain chain;

    public AESSimpleResolver () {
        encodeLevels = new ArrayList<>();
        encodeLevels.add(new EncodeLevel("普通AES",0));
        encodeLevels.add(new EncodeLevel("强AES", 1));

        ResolveChain chain = new AESSimpleFileNode(this);
        ResolveChain chainDictSingle = new AESSimpleSingleDictionaryNode(this);
        ResolveChain chainDictMulti = new AESSimpleMultiDictionaryNode(this);
        ResolveChain chainDictArchive = new AESSimpleArchiveDictionaryNode(this);

        chain.setNext(chainDictSingle);
        chainDictSingle.setNext(chainDictMulti);
        chainDictMulti.setNext(chainDictArchive);

        this.chain = chain;
    }

    public static Key generateAESKey(String password, int level) throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        if (level == 0) {
            generator.init(128, new SecureRandom(password.getBytes("utf8")));
        } else {
            generator.init(256, new SecureRandom(password.getBytes("utf8")));
        }
        SecretKey keyX = generator.generateKey();
        byte[] rawX = keyX.getEncoded();
        SecretKey keyY = new SecretKeySpec(rawX, "AES");
        return keyY;
    }

    @Override
    public String getName() {
        return "AES算法";
    }

    @Override
    public List<EncodeLevel> getLevels() {
        return encodeLevels;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void emitEvent(ApplicationEvent event) {
        publisher.publishEvent(event);
    }
}

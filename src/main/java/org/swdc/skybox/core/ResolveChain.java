package org.swdc.skybox.core;

import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;

import java.io.File;

public interface ResolveChain {

    boolean supportEnc(File input, String password, EncodeOptions options);
    boolean supportDec(File input, String password, EncryptedHeader header);
    void doEncrypt(File input, String password, EncodeOptions options) throws NotSupportException;
    void doDecrypt(File input, String password, EncryptedHeader header);
    ResolveChain getNext();
    void setNext(ResolveChain chain);

}

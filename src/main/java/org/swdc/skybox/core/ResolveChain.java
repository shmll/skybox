package org.swdc.skybox.core;

import org.swdc.skybox.core.dataobject.EncodeOptions;
import org.swdc.skybox.core.dataobject.EncryptedHeader;
import org.swdc.skybox.core.exception.EncryptException;
import org.swdc.skybox.core.exception.InvalidPasswordException;
import org.swdc.skybox.core.exception.NotSupportException;

import java.io.File;

/**
 * 链式加密接口
 * 责任链的实践，依次检查自身是否支持当前选项，不支持委托给下一个。
 */
public interface ResolveChain {

    /**
     * 是否支持当前选项的加密
     * @param input 文件
     * @param password 密码
     * @param options 加密选项
     * @return 是否支持
     */
    boolean supportEnc(File input, String password, EncodeOptions options);

    /**
     * 是否支持当前选项的解密
     * @param input 文件
     * @param password 密钥
     * @param header 加密header
     * @return 是否支持
     */
    boolean supportDec(File input, String password, EncryptedHeader header);

    /**
     * 加密执行
     *
     * 注意：解密的时候需要知道加密的时候doFinal后的BlockSize，按照这个size读取
     * 才能正确解密
     * @param input 文件
     * @param password 密钥
     * @param options 加密选项
     * @throws NotSupportException 不支持此选项的时候
     */
    void doEncrypt(File input, String password, EncodeOptions options) throws NotSupportException, EncryptException;

    /**
     * 解密执行
     *
     * 注意：解密的时候，应该读取加密的doFinal返回的数组的size大小，这样才能正确解密
     * @param input 文件
     * @param password 密钥
     * @param header 加密头
     * @throws InvalidPasswordException 密码错误的时候
     * @throws NotSupportException 不支持此header的文件的时候
     */
    void doDecrypt(File input, String password, EncryptedHeader header) throws InvalidPasswordException, NotSupportException, EncryptException;

    /**
     * 返回本链条的下一个加密节点
     * @return 节点
     */
    ResolveChain getNext();

    /**
     * 设置下一个加密节点
     * @param chain 加密链节点
     */
    void setNext(ResolveChain chain);

}

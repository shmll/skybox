package org.swdc.skybox.core;

import org.springframework.context.ApplicationEvent;
import org.swdc.skybox.core.dataobject.EncodeLevel;
import org.swdc.skybox.core.dataobject.EncodeOptions;

import java.io.File;
import java.util.List;

/**
 * 接口：数据解析器
 * 加密和解密使用的核心接口。
 *
 * 这里是责任链模式的实践，Resolver不执行加密和解密操作
 * 这些操作在ResolveChain里面，每一个chain都有next，
 * 依次查找支持当前操作的节点，没有就下放到下一个节点。
 *
 * 数据解析器接口本身也是使用Spring环境的批量注入，属于
 * 工厂模式的实践。
 */
public interface DataResolver {

    default void encode(File input, String password, EncodeOptions options) {
        try {
            getChain().doEncrypt(input,password,options);
        } catch (NotSupportException ex) {
            ex.printStackTrace();
        }
    }

    default void decode(File input,String password){

    }

    String getName();

    List<EncodeLevel> getLevels();

    String toString();

    ResolveChain getChain();

    void emitEvent(ApplicationEvent event);

}

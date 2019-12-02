package org.swdc.skybox.core.dataobject;

import lombok.Getter;
import org.controlsfx.property.BeanProperty;
import org.swdc.skybox.anno.ConfigProp;

import java.beans.PropertyDescriptor;

/**
 * Created by lenovo on 2019/6/8.
 */
public class ConfigProperty extends BeanProperty {

    @Getter
    private ConfigProp propData;

    public ConfigProperty(Object bean, PropertyDescriptor propertyDescriptor, ConfigProp prop) {
        super(bean, propertyDescriptor);
        propData = prop;
    }

    @Override
    public String getName() {
        return propData.name();
    }

    @Override
    public String getDescription() {
        return propData.tooltip();
    }
}

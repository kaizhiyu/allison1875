package com.spldeolin.allison1875.si;

import com.spldeolin.allison1875.base.BaseConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 【standard-inspector】的全局配置
 *
 * @author Deolin 2020-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class StandardInspectorConfig extends BaseConfig {

    public static final StandardInspectorConfig CONFIG = new StandardInspectorConfig();

    private StandardInspectorConfig() {
        super();
        this.initLoad();
    }

    private void initLoad() {
    }

}
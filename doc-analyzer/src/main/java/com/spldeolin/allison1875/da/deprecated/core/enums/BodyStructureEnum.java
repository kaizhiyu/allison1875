package com.spldeolin.allison1875.da.deprecated.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Deolin 2019-12-03
 */
@AllArgsConstructor
@Getter
public enum BodyStructureEnum {

    none("none"),

    va1ue("value"),

    valueArray("valueArray"),

    keyValue("keyValue"),

    keyValueArray("keyValueArray"),

    keyValuePage("keyValuePage"),

    chaos("chaos");

    private String value;

}

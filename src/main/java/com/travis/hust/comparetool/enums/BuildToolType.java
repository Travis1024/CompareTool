package com.travis.hust.comparetool.enums;

import lombok.Getter;

/**
 * @ClassName BuildToolType
 * @Description 编译工具类型
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/7/26
 */
@Getter
public enum BuildToolType {

    MAVEN (1, "pom.xml"),
    GRADLE (2, "build.gradle");

    private final int code;
    private final String fileName;

    BuildToolType(int code, String fileName) {
        this.code = code;
        this.fileName = fileName;
    }
}

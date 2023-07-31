package com.travis.hust.comparetool.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * @ClassName CompareResult
 * @Description 比较结果封装
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/7/25
 */
@Data
public class CompareResult implements Serializable {
    private Integer targetClassSum;
    private Integer jarClassSum;
    private Integer missingClassNumber;
    private Integer differentClassNumber;
    private Integer sameClassNumber;

    private Set<String> missingClassSet;
    private Set<String> differentClassSet;
}

package com.travis.hust.comparetool.pojo.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName CompareRequestParam
 * @Description TODO
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/8/4
 */
@Data
public class CompareRequestParam implements Serializable {
    private String rootPath;
    private List<String> jarPathList;
    private String uuid;
}

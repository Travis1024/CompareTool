package com.travis.hust.comparetool.service;

import com.travis.hust.comparetool.enums.BuildToolType;
import com.travis.hust.comparetool.utils.R;

import java.io.IOException;
import java.util.List;

/**
 * @ClassName CompareService
 * @Description 对比服务接口
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/7/25
 */
public interface CompareService {
    R<?> compareClassFile(String rootPath, List<String> jarPathList, String uuid, BuildToolType buildToolType) throws IOException;
}

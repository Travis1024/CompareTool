package com.travis.hust.comparetool.controller;

import cn.hutool.core.io.FileUtil;
import com.travis.hust.comparetool.enums.BizCodeEnum;
import com.travis.hust.comparetool.enums.BuildToolType;
import com.travis.hust.comparetool.service.CompareService;
import com.travis.hust.comparetool.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.File;
import java.util.List;

/**
 * @ClassName CompareController
 * @Description Controller
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/7/25
 */
@RestController
@Slf4j
public class CompareController {

    // @Resource(name = "compareMavenService")
    // private CompareService compareMavenService;
    // @Resource(name = "compareGradleService")
    // private CompareService compareGradleService;

    @Autowired
    private CompareService compareService;

    @PostMapping("/compare")
    public R<?> compareClassFile(@RequestParam("rootPath") String rootPath, @RequestParam("jarPathList") List<String> jarPathList, @RequestParam("uuid") String uuid) {
        try {
            // 一、处理 项目 根路径地址
            rootPath = rootPath.trim();
            if (rootPath.indexOf(rootPath.length() - 1) == File.separatorChar) {
                rootPath = rootPath.substring(0, rootPath.length() - 1);
            }

            // 二、判断 项目 使用的编译工具
            if (FileUtil.exist(rootPath + File.separator + BuildToolType.MAVEN.getFileName())) {
                return compareService.compareClassFile(rootPath, jarPathList, uuid, BuildToolType.MAVEN);
            } else if (FileUtil.exist(rootPath + File.separator + BuildToolType.GRADLE.getFileName())) {
                return compareService.compareClassFile(rootPath, jarPathList, uuid, BuildToolType.GRADLE);
            }

            return R.error(BizCodeEnum.BAD_REQUEST, "未检测到项目源代码的编译配置文件！");

        } catch (Exception e) {
            log.error(e.toString());
            return R.error(BizCodeEnum.UNKNOW, e.getMessage());
        }
    }
}

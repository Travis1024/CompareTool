package com.travis.hust.comparetool.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.travis.hust.comparetool.enums.BizCodeEnum;
import com.travis.hust.comparetool.service.CompareService;
import com.travis.hust.comparetool.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * @ClassName CompareServiceImpl
 * @Description TODO
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/7/25
 */

@Service("compareMavenService")
@Slf4j
public class CompareMavenServiceImpl implements CompareService {

    @Override
    public R<?> compareClassFile(String rootPath, List<String> jarPathList) throws IOException {
        /**
         * 一、发送编译命令（起 websocket 实时返回编译结果）
         */
        Process execked = RuntimeUtil.exec("mvn compile -f " + rootPath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(execked.getInputStream()));

        // 记录当前行内容
        String line = null;
        // 记录最后 10行编译输出
        Deque<String> deque = new LinkedList<>();

        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            // 保留最后 10 行输出
            if (deque.size() > 10) {
                deque.removeFirst();
            }
            deque.add(line);
        }

        // 判断是否编译成功
        boolean flag = false;
        for (String temp : deque) {
            if (temp.contains("BUILD SUCCESS")) {
                log.info("[编译成功！]");
                flag = true;
                break;
            }
        }
        if (!flag) return R.error(BizCodeEnum.UNKNOW, "工程源代码编译失败！");


        /**
         * 二、进 target 文件计算 class 文件 md5 值
         */
        String classPath = rootPath + File.separator + "target" + File.separator + "classes";

        if (!FileUtil.isDirectory(classPath)) {
            return R.error(BizCodeEnum.UNKNOW, "未找到编译后的 target -> classes文件夹！");
        }

        // 获取所有的 class 文件
        List<File> fileList = FileUtil.loopFiles(new File(classPath), pathname -> pathname.isFile() && pathname.getName().endsWith(".class"));
        Map<String, List<String>> targetClassMap = new HashMap<>();

        for (File tempFile : fileList) {
            // 获取class文件名
            String fileName = tempFile.getName();
            // 计算文件 md5 值
            String md5Hex = DigestUtil.md5Hex(tempFile);

            if (targetClassMap.containsKey(fileName)) {
                targetClassMap.get(fileName).add(md5Hex);
            } else {
                List<String> arrayList = new ArrayList<>();
                arrayList.add(md5Hex);
                targetClassMap.put(fileName, arrayList);
            }
        }

        // 解压 所有 jar 包，计算 jar 包下的 class 文件md5值


        // 对比 所有文件的md5值


        return null;
    }


}

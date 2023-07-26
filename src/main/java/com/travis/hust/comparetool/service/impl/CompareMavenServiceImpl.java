package com.travis.hust.comparetool.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.travis.hust.comparetool.enums.BizCodeEnum;
import com.travis.hust.comparetool.pojo.dto.CompareResult;
import com.travis.hust.comparetool.service.CompareService;
import com.travis.hust.comparetool.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
         * 准备工作：mvn clean
         */
        Process execkedClean = RuntimeUtil.exec("mvn clean -f " + rootPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(execkedClean.getInputStream()));

        // 记录当前行内容
        String line = null;
        // 记录最后 10 行编译输出
        Deque<String> deque = new LinkedList<>();

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            // 保留最后 10 行输出
            if (deque.size() > 10) {
                deque.removeFirst();
            }
            deque.add(line);
        }

        // 判断是否 clean 成功
        boolean flag = false;
        for (String temp : deque) {
            if (temp.contains("BUILD SUCCESS")) {
                log.info("[ Clean 成功！]");
                flag = true;
                break;
            }
        }
        if (!flag) return R.error(BizCodeEnum.UNKNOW, "工程源代码 Clean 失败！");


        /**
         * 一、发送编译命令（起 websocket 实时返回编译结果）
         */
        Process execked = RuntimeUtil.exec("mvn compile -f " + rootPath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(execked.getInputStream()));

        // 记录最后 10 行编译输出
        deque.clear();

        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            // 保留最后 10 行输出
            if (deque.size() > 10) {
                deque.removeFirst();
            }
            deque.add(line);
        }

        // 判断是否编译成功
        flag = false;
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


        /**
         * 三、解压 所有 jar 包，计算 jar 包下的 class 文件 md5 值
         */
        Map<String, List<String>> jarClassMap = new HashMap<>();
        int jarClassSum = 0;

        String prefix = "BOOT-INF" + File.separator + "classes";

        for (String oneJarPath : jarPathList) {
            // 判断传入的 jar 包是否存在
            if (!FileUtil.exist(oneJarPath)) continue;

            JarFile jarFile = new JarFile(oneJarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (!jarEntry.isDirectory() && jarEntry.getName().startsWith(prefix) && jarEntry.getName().endsWith(".class")) {
                    // 计数+1
                    jarClassSum++;

                    // 获取 class 文件名, 需要处理前缀
                    String jarEntryName = jarEntry.getName();
                    int lastFlagIndex = jarEntryName.lastIndexOf(File.separator);
                    if (lastFlagIndex != -1) {
                        jarEntryName = jarEntryName.substring(lastFlagIndex + 1);
                    }

                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    // 计算文件 md5 值
                    String md5Hex = DigestUtil.md5Hex(inputStream);

                    if (jarClassMap.containsKey(jarEntryName)) {
                        jarClassMap.get(jarEntryName).add(md5Hex);
                    } else {
                        List<String> arrayList = new ArrayList<>();
                        arrayList.add(md5Hex);
                        jarClassMap.put(jarEntryName, arrayList);
                    }
                }
            }
        }


        /**
         * 四、对比两个 map 所有文件的 md5 值
         */
        Set<String> missingClassSet = new HashSet<>();
        Set<String> differentClassSet = new HashSet<>();
        int sameNumber = 0;

        for (String key : jarClassMap.keySet()) {
            if (targetClassMap.containsKey(key)) {
                List<String> jarClassMd5List = jarClassMap.get(key);
                List<String> targetClassMd5List = targetClassMap.get(key);
                for (String oneMd5 : jarClassMd5List) {
                    if (targetClassMd5List.contains(oneMd5)) {
                        sameNumber++;
                    } else {
                        differentClassSet.add(key);
                        break;
                    }
                }
            } else {
                missingClassSet.add(key);
            }
        }

        /**
         * 封装对比结果
         */
        CompareResult compareResult = new CompareResult();
        compareResult.setMissingClassSet(missingClassSet);
        compareResult.setDifferentClassSet(differentClassSet);

        compareResult.setJarClassSum(jarClassSum);
        compareResult.setTargetClassSum(fileList.size());

        compareResult.setSameClassNumber(sameNumber);
        compareResult.setMissingClassNumber(missingClassSet.size());
        compareResult.setDifferentClassNumber(jarClassSum - sameNumber - missingClassSet.size());

        return R.success(compareResult);
    }
}

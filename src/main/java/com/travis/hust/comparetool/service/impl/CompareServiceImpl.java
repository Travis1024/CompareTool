package com.travis.hust.comparetool.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.travis.hust.comparetool.enums.BizCodeEnum;
import com.travis.hust.comparetool.enums.BuildToolType;
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
 * @Description 业务实现类
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/7/26
 */
@Service
@Slf4j
public class CompareServiceImpl implements CompareService {

    @Override
    public R<?> compareClassFile(String rootPath, List<String> jarPathList, BuildToolType buildToolType) throws IOException {
        /**
         * 0: 准备工作：mvn clean
         */
        if (BuildToolType.MAVEN.equals(buildToolType)) clean("mvn clean -f " + rootPath);
        else if (BuildToolType.GRADLE.equals(buildToolType)) clean("gradle clean -p " + rootPath);
        log.info("-------------- [ Clean Successful! ]");

        /**
         * 一、发送编译命令（起 websocket 实时返回编译结果）
         */
        if (BuildToolType.MAVEN.equals(buildToolType)) build("mvn compile -f " + rootPath);
        else if (BuildToolType.GRADLE.equals(buildToolType)) build("gradle build -p " + rootPath);
        log.info("-------------- [ Compile Successful! ]");

        /**
         * 二、进 target 文件计算 class 文件 md5 值
         */
        Map<String, List<String>> targetClassMap = new HashMap<>();
        String classPath = null;
        if (BuildToolType.MAVEN.equals(buildToolType)) {
            classPath = rootPath + File.separator + "target" + File.separator + "classes";
        } else if (BuildToolType.GRADLE.equals(buildToolType)) {
            classPath = rootPath + File.separator + "build" + File.separator + "classes";
        }
        int targetClassSum = calcTargetClassMD5(classPath, targetClassMap);
        log.info("-------------- [ Calc Target Class Files MD5 Successful! ]");

        /**
         * 三、解压 所有 jar 包，计算 jar 包下的 class 文件 md5 值
         */
        Map<String, List<String>> jarClassMap = new HashMap<>();
        int jarClassSum = calcJarClassMD5(jarPathList, jarClassMap);
        log.info("-------------- [ Calc Jar Class Files MD5 Successful! ]");

        /**
         * 四、对比两个 map 所有文件的 md5 值
         */
        Set<String> missingClassSet = new HashSet<>();
        Set<String> differentClassSet = new HashSet<>();
        int sameNumber = compare(jarClassMap, targetClassMap, missingClassSet, differentClassSet);
        log.info("-------------- [ Compare MD5 Successful! ]");

        /**
         * 五、封装对比结果
         */
        CompareResult compareResult = new CompareResult();
        compareResult.setMissingClassSet(missingClassSet);
        compareResult.setDifferentClassSet(differentClassSet);

        compareResult.setJarClassSum(jarClassSum);
        compareResult.setTargetClassSum(targetClassSum);

        compareResult.setSameClassNumber(sameNumber);
        compareResult.setMissingClassNumber(missingClassSet.size());
        compareResult.setDifferentClassNumber(jarClassSum - sameNumber - missingClassSet.size());
        log.info("-------------- [ Package Result Successful! ]");

        return R.success(compareResult);
    }

    /**
     * @MethodName clean
     * @Description 准备工作：mvn clean
     * @Author travis-wei
     * @Data 2023/7/26
     * @param command
     * @Return void
     **/
    private void clean (String command) throws IOException {
        Process execkedClean = RuntimeUtil.exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(execkedClean.getInputStream()));

        // 记录当前行内容
        String line = null;
        // 记录最后 10 行编译输出
        Deque<String> deque = new LinkedList<>();

        while ((line = reader.readLine()) != null) {
            // TODO 向页面 WebSocket 发送数据
            System.out.println(line);
            // 保留最后 10 行输出
            if (deque.size() >= 10) {
                deque.removeFirst();
            }
            deque.add(line);
        }

        // 判断是否 clean 成功
        boolean flag = false;
        for (String temp : deque) {
            if (temp.contains("BUILD SUCCESS")) {
                flag = true;
                break;
            }
        }
        if (!flag) throw new RuntimeException("工程源代码 Clean 失败！");
    }

    /**
     * @MethodName build
     * @Description 发送编译命令
     * @Author travis-wei
     * @Data 2023/7/26
     * @param command
     * @Return void
     **/
    private void build (String command) throws IOException {
        Process execked = RuntimeUtil.exec(command);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(execked.getInputStream()));

        // 记录当前行内容
        String line = null;
        // 记录最后 10 行编译输出
        Deque<String> deque = new LinkedList<>();

        while ((line = bufferedReader.readLine()) != null) {
            // TODO 向页面 WebSocket 发送数据
            System.out.println(line);
            // 保留最后 10 行输出
            if (deque.size() >= 10) {
                deque.removeFirst();
            }
            deque.add(line);
        }

        // 判断是否编译成功
        boolean flag = false;
        for (String temp : deque) {
            if (temp.contains("BUILD SUCCESS")) {
                flag = true;
                break;
            }
        }
        if (!flag) throw new RuntimeException("工程源代码编译失败！");
    }

    /**
     * @MethodName calcTargetClassMD5
     * @Description 进 target 文件计算 class 文件 md5 值
     * @Author travis-wei
     * @Data 2023/7/26
     * @param classPath
     * @param targetClassMap
     * @Return int
     **/
    private int calcTargetClassMD5(String classPath, Map<String, List<String>> targetClassMap) {

        if (!FileUtil.isDirectory(classPath)) {
            throw new RuntimeException("未找到编译后的 target -> classes文件夹！");
        }

        // 获取所有的 class 文件
        List<File> fileList = FileUtil.loopFiles(new File(classPath), pathname -> pathname.isFile() && pathname.getName().endsWith(".class"));

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
        return fileList.size();
    }

    /**
     * @MethodName calcJarClassMD5
     * @Description 解压 所有 jar 包，计算 jar 包下的 class 文件 md5 值
     * @Author travis-wei
     * @Data 2023/7/26
     * @param jarPathList
     * @param jarClassMap
     * @Return int
     **/
    private int calcJarClassMD5(List<String> jarPathList, Map<String, List<String>> jarClassMap) throws IOException {
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
        return jarClassSum;
    }

    /**
     * @MethodName compare
     * @Description 对比两个 map 所有文件的 md5 值
     * @Author travis-wei
     * @Data 2023/7/26
     * @param jarClassMap
     * @param targetClassMap
     * @param missingClassSet
     * @param differentClassSet
     * @Return int
     **/
    private int compare(Map<String, List<String>> jarClassMap, Map<String, List<String>> targetClassMap, Set<String> missingClassSet, Set<String> differentClassSet) {
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
        return sameNumber;
    }


}

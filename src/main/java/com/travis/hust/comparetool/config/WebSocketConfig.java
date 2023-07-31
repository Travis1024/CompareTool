package com.travis.hust.comparetool.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

/**
 * @ClassName WebSocketConfig
 * @Description WebSocket配置类
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/7/31
 */
@Configuration
public class WebSocketConfig extends ServerEndpointConfig.Configurator {

    /**
     * 注入ServerEndpointExporter
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 建立握手时，连接前的操作
     */
    // @Deprecated
    // @Override
    // public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
    //     Map<String, Object> userProperties = sec.getUserProperties();
    //     Map<String, List<String>> headers = request.getHeaders();
    //     List<String> userIdList = headers.get("userId");
    //     List<String> usernameList = headers.get("username");
    //     String userId = null, username = null;
    //     if (userIdList.size() > 0) userId = userIdList.get(0);
    //     if (usernameList.size() > 0) username = usernameList.get(0);
    //
    //     if (!StrUtil.isEmpty(userId)) userProperties.put("userId", userId);
    //     if (!StrUtil.isEmpty(username)) userProperties.put("username", username);
    // }

    /**
     * 初始化端点对象（被@ServerEndpoint 标注的对象）
     */
    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return super.getEndpointInstance(clazz);
    }
}

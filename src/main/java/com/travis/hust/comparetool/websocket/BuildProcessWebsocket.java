package com.travis.hust.comparetool.websocket;

import com.travis.hust.comparetool.config.WebSocketConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName BuildProcessWebsocket
 * @Description 页面连接 WebSocket 处理类
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/7/25
 */

@Slf4j
@Component
@ServerEndpoint(value = "/websocket/process/{wbUuid}", configurator = WebSocketConfig.class)
public class BuildProcessWebsocket {

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    // 页面 wbUuid
    private String wbUuid;
    /**
     * 用来存 - (key: uuid) 对应的 Session 连接
     */
    private static ConcurrentHashMap<String, Session> sessionPool = new ConcurrentHashMap<>();


    @OnOpen
    public void onOpen(Session session, @PathParam(value = "wbUuid") String wbUuid) {
        this.session = session;
        this.wbUuid = wbUuid;

        // 把当前 session 添加到 session pool 中
        sessionPool.put(wbUuid, session);
        log.info("[WebSocket Connect] 页面 " + wbUuid + " 连接成功!");
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("[WebSocket Receive-Message] 接收到页面 " + this.wbUuid + " 消息: " + message);
    }

    @OnClose
    public void onClose() {
        try {
            sessionPool.remove(this.wbUuid);
            // this.session.close();
            log.info("[WebSocket Close] 关闭页面 " + this.wbUuid + " 连接");
        } catch (Exception e) {
            log.error("[WebSocket Error] " + e);
        }
    }

    @OnError
    public void onError(Throwable throwable) {
        log.error(throwable.toString());
    }

    public static Session getSession(String wbUuid) {
        try {
            Session sessionNow = sessionPool.get(wbUuid);
            if (sessionNow != null && sessionNow.isOpen()) return sessionNow;
        } catch (Exception e) {
            log.error("[WebSocket Error] " + e);
        }
        return null;
    }

    public static void sendMessage(String wbUuid, String message) {
        Session sessionNow = sessionPool.get(wbUuid);
        if (sessionNow != null && sessionNow.isOpen()) {
            try {
                log.info("[WebSocket Send-Message] - " + wbUuid + " : " + message);
                sessionNow.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.error("[WebSocket Error] " + e);
            }
        }
    }

}

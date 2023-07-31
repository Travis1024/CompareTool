// package com.travis.hust.comparetool.websocket;
//
// import cn.hutool.core.lang.Pair;
// import cn.hutool.core.util.StrUtil;
// import com.hust.vmproject.config.WebSocketConfig;
// import com.hust.vmproject.utils.LockUtil;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Component;
//
// import javax.websocket.*;
// import javax.websocket.server.PathParam;
// import javax.websocket.server.ServerEndpoint;
// import java.util.LinkedList;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.CopyOnWriteArraySet;
// import java.util.concurrent.locks.ReentrantReadWriteLock;
//
// /**
//  * @ClassName WebSocketHostHandler
//  * @Description 宿主机操作 WebSocket
//  * @Author travis-wei
//  * @Version v1.0
//  * @Data 2023/5/31
//  */
// @Slf4j
// @Component
// @ServerEndpoint(value = "/websocket/host/{hostId}", configurator = WebSocketConfig.class)
// public class WebSocketHostHandler {
//
//     // 与某个客户端的连接会话，需要通过它来给客户端发送数据
//     private Session session;
//     // 宿主机 ID
//     private String hostId;
//     // 当前 session 连接的用户 ID
//     private String userId;
//     // 当前 session 连接的用户名字
//     private String username;
//
//     /**
//      * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
//      * 虽然@Component默认是单例模式的，但springboot还是会为每个websocket连接初始化一个bean，所以可以用一个静态set保存起来。
//      */
//     private static CopyOnWriteArraySet<WebSocketHostHandler> webSockets = new CopyOnWriteArraySet<>();
//     /**
//      * 用来存 - (key: hostId) 正在进行宿主机配置修改的 Session 连接
//      */
//     public static ConcurrentHashMap<String, LinkedList<Session>> sessionPool = new ConcurrentHashMap<>();
//     /**
//      * 用来存 - 当前进行宿主机配置修改的 (key: hostId) (value) 用户 ID 和 Session
//      */
//     public static ConcurrentHashMap<String, Pair<String, Session>> hostUserPool = new ConcurrentHashMap<>();
//
//     public static void addHostInstance(String hostId) {
//         if (sessionPool.containsKey(hostId)) return;
//         LinkedList<Session> sessionList = new LinkedList<>();
//         sessionPool.put(hostId, sessionList);
//     }
//
//     public static void deleteHostInstance(String hostId) {
//         if (sessionPool.containsKey(hostId)) sessionPool.remove(hostId);
//         // 理论上，hostUserPool<hostId> 肯定是为空的，没有人在修改配置
//         if (hostUserPool.containsKey(hostId)) hostUserPool.remove(hostId);
//     }
//
//     @OnOpen
//     public void onOpen(Session session, @PathParam(value = "hostId") String hostId) {
//         this.session = session;
//         this.hostId = hostId;
//         /**
//          * 获取请求头中的当前连接用户的 ID 和名字
//          */
//         String userId = (String) this.session.getUserProperties().get("userId");
//         String username = (String) this.session.getUserProperties().get("username");
//         this.userId = userId;
//         this.username = username;
//
//         ReentrantReadWriteLock hostLockKV = LockUtil.getHostLockKV(this.hostId);
//         boolean tryLock = hostLockKV.writeLock().tryLock();
//         // 如果加锁失败
//         if (!tryLock) {
//             sendHostConflictMessage("[UNKNOW] 获取锁失败!");
//             return;
//         }
//
//         try {
//             // 把当前 session 添加到 session pool 中
//             sessionPool.get(this.hostId).add(this.session);
//             webSockets.add(this);
//             // 已经有用户正在修改宿主机的相关信息
//             if (hostUserPool.containsKey(this.hostId)) {
//                 // 获取当前正在进行宿主机修改的用户信息
//                 Pair<String, Session> sessionPair = hostUserPool.get(this.hostId);
//                 String curUserId = sessionPair.getKey();
//                 Session sessionPairValue = sessionPair.getValue();
//                 // 向客户端发送冲突信息
//                 sendHostConflictMessage("[" + curUserId + "] 正在进行宿主机修改!");
//             } else {
//                 // 当前没有用户修改虚拟机
//                 Pair<String, Session> pair = new Pair<>(this.userId, this.session);
//                 hostUserPool.put(this.hostId, pair);
//                 log.info("[WebSocket Message] 当前宿主机 " + hostId + " 开始进行配置");
//             }
//         } catch (Exception e) {
//             log.error("[WebSocket Message Error] " + e.toString());
//         } finally {
//             hostLockKV.writeLock().unlock();
//         }
//     }
//
//     @OnMessage
//     public void onMessage(String message) {
//         log.info("[WebSocket Message] 接收到客户端消息：" + message);
//         log.info("SessionId:" + this.session.getId());
//     }
//
//     @OnClose
//     public void onClose() {
//         try {
//             webSockets.remove(this);
//             sessionPool.get(this.hostId).remove(this.session);
//             // this.session.close();
//             Pair<String, Session> sessionPair = hostUserPool.get(this.hostId);
//             if (sessionPair == null) return;
//             String userId = hostUserPool.get(this.hostId).getKey();
//             if (!StrUtil.isEmpty(userId)) {
//                 if (userId.equals(this.userId)) {
//                     // 当前用户为正在操作宿主机的唯一用户
//                     hostUserPool.remove(this.hostId);
//                     log.info("[WebSocket Message] 当前宿主机 " + this.hostId + " 结束配置");
//                 }
//             } else {
//                 hostUserPool.remove(this.hostId);
//             }
//         } catch (Exception e) {
//             log.error("[WebSocket Message Error] " + e.toString());
//         }
//     }
//
//     @OnError
//     public void onError(Throwable throwable) {
//         log.error(throwable.toString());
//     }
//
//
//     /**
//      * @MethodName sendHostConflictMessage
//      * @Description 向当前 session 发送冲突消息
//      * @Author travis-wei
//      * @Data 2023/6/3
//      * @param message
//      * @Return void
//      **/
//     private void sendHostConflictMessage(String message) {
//         if (this.session != null && this.session.isOpen()) {
//             try {
//                 log.info("[WebSocket Message] 向宿主机 " + this.hostId + " 用户:" + this.userId + " | 推送冲突消息：" + message);
//                 this.session.getAsyncRemote().sendText(message);
//             } catch (Exception e) {
//                 log.error("[WebSocket Message Error] " + e);
//             }
//         }
//     }
// }

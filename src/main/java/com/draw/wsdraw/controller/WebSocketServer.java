package com.draw.wsdraw.controller;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
@ServerEndpoint("/webSocket/{userId}")
@Component
public class WebSocketServer {

    private static ConcurrentHashMap<String, List<WebSocketServer>> webSocketMap = new ConcurrentHashMap<>(3);

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session=null;

    /**
     * 接收userId
     */
    private String userId = null;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        if (StrUtil.isBlank(userId)) {
            return;
        }
        this.session = session;
        this.userId = userId;
        addSocketServerToMap(this);
        sendMessageBySession(session,"连接成功");
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        List<WebSocketServer> wssList = webSocketMap.get(userId);
        if (wssList != null) {
            for (WebSocketServer item : wssList) {
                if (item.session.getId().equals(session.getId())) {
                    wssList.remove(item);
                    if (wssList.isEmpty()) {
                        webSocketMap.remove(userId);
                    }
                    break;
                }
            }
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        sendMessageBySession(session,message);
    }

    /**
     * 服务异常处理
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
       log.error("websocket服务异常",error);
    }

    private static synchronized void addSocketServerToMap(WebSocketServer wss) {
        if (wss != null) {
            List<WebSocketServer> wssList = webSocketMap.get(wss.userId);
            if (wssList == null) {
                wssList = new ArrayList<>(6);
                webSocketMap.put(wss.userId, wssList);
            }
            wssList.add(wss);
        }
    }

    /**
     * 实现服务器主动发送消息
     */
    public static void sendMessage(String userId,String message) {
        if (StrUtil.isBlank(userId)) return;
        List<WebSocketServer> wssList = webSocketMap.get(userId);
        for (WebSocketServer item : wssList) {
            sendMessageBySession(item.session,message);
        }
    }

    private static void sendMessageBySession(Session session,String message){
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
           log.error("消息发送失败",e);
        }
    }
}

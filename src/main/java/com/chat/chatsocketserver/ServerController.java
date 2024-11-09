/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chat.chatsocketserver;

import com.chat.tcpcommons.ConnectionTemplate;
import com.chat.tcpcommons.IConnection;
import com.chat.tcpcommons.IObserver;
import com.chat.tcpcommons.InboxChat;
import com.chat.tcpcommons.Message;
import com.chat.tcpcommons.User;
import com.chat.tcpcommons.logging.IChatLogger;
import com.chat.tcpcommons.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author felix
 */
public class ServerController implements IConnection, IObserver, ConnectionTemplate {

    private ServerFrame serverFrame;
    private User admin;
    private List<InboxChat> privateChats;
    private ChatServer server;
    private IChatLogger logger = LoggerFactory.getLogger(ServerController.class);

    ReentrantLock lock = new ReentrantLock();

    public ServerController() {
        privateChats = new ArrayList<>();
        admin = new User("admin", "admin@example.com");
        serverFrame = new ServerFrame(this, admin);
        serverFrame.setVisible(true);
    }

    @Override
    public void sendMessage(Message message) {
        server.proccessMessage(message);
    }

    @Override
    public void closeInbox(InboxChat inbox) {
        privateChats.remove(inbox);
        inbox.dispose();
    }

    @Override
    public void openInbox(User friend) {
        var inbox = privateChats.stream().filter(i -> i.getUser().equals(friend)).findFirst().orElse(null);
        if (inbox == null) {
            inbox = new InboxChat(serverFrame, friend, admin, this);
            privateChats.add(inbox);
            inbox.setVisible(true);
        }
        inbox.requestFocus();
    }

    @Override
    public void init() {
        try {
            serverFrame.log("Iniciando el servidor...");
            server = new ChatServer(serverFrame.getPort());
            server.init();
            server.subscribe(this);

            serverFrame.log("Servidor iniciado");
        } catch (Exception ex) {
            serverFrame.log(String.format("Error al iniciar el servidor: ", ex.getMessage()));
            logger.error(String.format("Error al iniciar el servidor: ", ex.getMessage()));
        }
    }

    @Override
    public synchronized void onUpdate(Object obj) {
        lock.lock();
        try {
            proccessMessage((Message) obj);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onUsersUpdate(Message message) {
        serverFrame.setUsers((List<User>) message.getBody());
    }

    @Override
    public void onInboxMessage(Message message) {
        var inbox = privateChats.stream().filter(i -> i.getUser().equals(message.getSender())).findFirst().orElse(null);
        if (inbox != null) {
            inbox.receiveMessage(message);
            return;
        }
        openInbox(message.getSender());
        onInboxMessage(message);
    }

    @Override
    public void onGeneralMessage(Message message) {
        serverFrame.receiveMessage(message);
    }
}

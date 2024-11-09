/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chat.chatsocketserver;

import com.chat.tcpcommons.ConnectionTemplate;
import com.chat.tcpcommons.IObserver;
import com.chat.tcpcommons.IConnection;
import com.chat.tcpcommons.InboxChat;
import com.chat.tcpcommons.Message;
import com.chat.tcpcommons.MessageType;
import com.chat.tcpcommons.Observable;
import com.chat.tcpcommons.User;
import com.chat.tcpcommons.logging.IChatLogger;
import com.chat.tcpcommons.logging.LoggerFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

/**
 *
 * @author felix
 */
public class Server extends Observable implements ConnectionTemplate, IObserver, Runnable, IConnection {

    private ServerSocket server;
    private List<ClientConnection> connections;
    private List<InboxChat> privateChats = new ArrayList<>();
    private int port;
    private ServerFrame serverFrame;
    private Thread serverThread;
    private User admin;

    private IChatLogger logger = LoggerFactory.getLogger(Server.class);

    public Server() {
        connections = new ArrayList<>();
        this.admin = new User("admin", "admin@example.com");
        serverFrame = new ServerFrame(this, admin);
        serverFrame.setVisible(true);
        serverThread = new Thread(this);
    }

    public void init(int port) {
        try {
            this.port = port;
            serverFrame.log("Iniciando servidor...");
            server = new ServerSocket(port);
            System.out.println("server started...");
            serverFrame.log("Servidor iniciado");
            serverThread.start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(serverFrame, "Error al intentar usar el puerto " + port, "Error al iniciar", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = server.accept();
                proccessConnection(socket);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private List<User> getUsers() {
        return this.connections.stream().map(c -> c.getUser()).collect(Collectors.toList());
    }

    private void proccessConnection(Socket socket) {
        var client = new ClientConnection(socket);
        Thread t = new Thread(client);
        t.start();
        this.serverFrame.log("usuario conectado");
        this.connections.add(client);
        client.subscribe(this);
        // this.serverFrame.setUsers(getUsers());
    }

    private void receiveInboxMessage(Message message) {
        var inbox = this.privateChats.stream().filter(i -> i.getUser().equals(message.getSender())).findFirst().orElse(null);
        if (inbox != null) {
            inbox.receiveMessage(message);
        }
    }

    @Override
    public void onUpdate(Object obj) {
        proccessMessage((Message) obj);
    }

    @Override
    public void sendMessage(Message message) {
        proccessMessage(message);
    }

    @Override
    public void closeInbox(InboxChat inbox) {
        this.privateChats.remove(inbox);
        inbox.dispose();
    }

    @Override
    public void openInbox(User friend) {
        var inbox = new InboxChat(serverFrame, friend, admin, this);
        privateChats.add(inbox);
        inbox.setVisible(true);
    }

    @Override
    public void onUsersUpdate(Message message) {
                        Message users = new Message.Builder()
                        .body(getUsers())
                        .messageType(MessageType.USERS_UPDATE)
                        .build();
                this.serverFrame.setUsers(getUsers());
                this.connections.forEach(c -> c.sendMessage(users));
    }

    @Override
    public void onInboxMessage(Message message) {
        var connection = this.connections.stream().filter(c -> c.getUser().equals(message.getReceiver())).findFirst().get();
        if (message.getReceiver().equals(this.admin)) {
            receiveInboxMessage(message);
            return;
        }
        connection.sendMessage(message);
    }

    @Override
    public void onGeneralMessage(Message message) {
        this.connections.forEach(c -> c.sendMessage(message));
    }

    @Override
    public void onDisconnect(Message message) {
        var connection = this.connections.stream().filter(c -> c.getUser().equals(message.getSender())).findFirst().get();
        this.connections.remove(connection);
        this.serverFrame.setUsers(getUsers());
        var m = new Message.Builder().body(getUsers()).messageType(MessageType.USERS_UPDATE).build();
        this.connections.forEach(c -> c.sendMessage(m));
    }

    @Override
    public void init() {
        
    }

    @Override
    public void notifyObservers(Object obj) {
        observers.forEach(o -> o.onUpdate(obj));
    }
}

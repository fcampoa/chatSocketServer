/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chat.chatsocketserver;

import com.chat.tcpcommons.ClientThread;
import com.chat.tcpcommons.ConnectionTemplate;
import com.chat.tcpcommons.IObserver;
import com.chat.tcpcommons.Message;
import com.chat.tcpcommons.Observable;
import com.chat.tcpcommons.User;
import com.chat.tcpcommons.logging.IChatLogger;
import com.chat.tcpcommons.logging.LoggerFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 *
 * @author felix
 */
public class ChatServer extends Observable implements ConnectionTemplate, Runnable, IObserver {

    private ServerSocket server;
    private final IChatLogger logger = LoggerFactory.getLogger(ChatServer.class);
    private List<ClientThread> connections;
    private int port;
    private Thread serverThread;

    private ReentrantLock lock = new ReentrantLock();

    public ChatServer(int port) {
        this.port = port;
        connections = new ArrayList<>();
        serverThread = new Thread(this);
    }

    @Override
    public synchronized void onUsersUpdate(Message message) {
        lock.lock();
        try {
            message.setBody(getUsers());
            connections.forEach(c -> c.sendMessage(message));
            notifyObservers(message);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onInboxMessage(Message message) {
        var friend = connections.stream().filter(c -> c.getUser().equals(message.getReceiver())).findFirst().orElse(null);
        if (friend != null) {
            friend.sendMessage(message);
            notifyObservers(message);
        }
    }

    @Override
    public void onGeneralMessage(Message message) {
        connections.forEach(c -> {
            if (!c.getUser().equals(message.getSender())) {
                c.sendMessage(message);
            }
        });
        notifyObservers(message);
    }

    @Override
    public void onDisconnect(Message message) {
        var user = connections.stream().filter(c -> c.getUser().equals(message.getSender())).findFirst().orElse(null);
        if (user != null) {
            connections.remove(user);
            notifyObservers(message);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket clientSocket = server.accept();
                ClientThread client = new ClientThread(clientSocket);
                Thread connectionThread = new Thread(client);
                connectionThread.start();
                connections.add(client);
                client.subscribe(this);
                // var message = new Message.Builder().body(getUsers()).messageType(MessageType.CONNECTION_MESSAGE).build();
                //client.sendMessage(message);
            }
        } catch (Exception ex) {
            logger.error(String.format("Error al iniciar la conexion al servidor: %s", ex.getMessage()));
        }
    }

    @Override
    public void onUpdate(Object obj) {
        proccessMessage((Message) obj);
    }

    public void init() {
        try {
            server = new ServerSocket(port);
            serverThread.start();
        } catch (Exception ex) {
            logger.error(String.format("Error al iniciar el servidor: %s", ex.getMessage()));
        }
    }

    private List<User> getUsers() {
        return connections.stream().map(c -> c.getUser()).collect(Collectors.toList());
    }

    @Override
    public void notifyObservers(Object obj) {
        observers.forEach(o -> o.onUpdate(obj));
    }
}

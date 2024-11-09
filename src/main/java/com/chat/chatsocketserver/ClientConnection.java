/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.chat.chatsocketserver;

import com.chat.tcpcommons.Observable;
import com.chat.tcpcommons.User;
import com.chat.tcpcommons.Message;
import com.chat.tcpcommons.MessageType;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author felix
 */
public class ClientConnection extends Observable implements Runnable {

    private User user;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Message message;
    private boolean connected;
    
    Logger logger = Logger.getLogger(ClientConnection.class.getName());

    public ClientConnection(Socket socket) {
        this.socket = socket;
        this.connected = true;
        initConnection();
    }

    private void initConnection() {
        try {
            this.out = new ObjectOutputStream(this.socket.getOutputStream());
            this.in = new ObjectInputStream(this.socket.getInputStream());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void sendMessage(Message message) {
        try {
            this.out.writeObject(message);
            this.out.flush();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        proccessMessage();
    }

    public User getUser() {
        return this.user;
    }

    @Override
    public void notifyObservers(Object obj) {
        this.observers.forEach(o -> o.onUpdate(this.message));
    }

    private void proccessMessage() {
        try {
            while (connected) {
                this.message = (Message) this.in.readObject();

                if (message.getMessageType() == MessageType.CONNECTION_MESSAGE) {
                    this.user = message.getSender();
                    this.message.setMessageType(MessageType.USERS_UPDATE);                    
                    logger.info("usuario conectado: " + this.user.getName());
                }
                if (this.message.getMessageType() == MessageType.DISCONNECT) {
                    this.connected = false;
                    this.message = message;
                    this.disconnect();
                }
                this.notifyObservers(null);
            }
        } catch (Exception e) {
            connected = false;
            System.out.println(e.getMessage());
        }
    }

    private void disconnect() {
        try {
            this.in.close();
            this.out.close();
            this.socket.close();
            logger.log(Level.WARNING, "Usuario desconetado: " + this.user.getName());
        } catch (Exception e) {
            this.connected = false;
            System.out.println(e.getMessage());
        }
    }
}

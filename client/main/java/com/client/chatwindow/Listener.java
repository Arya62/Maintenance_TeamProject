package com.client.chatwindow;

import com.client.login.LoginController;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

import static com.messages.MessageType.CONNECTED;

public class Listener implements Runnable{

    private static final String HASCONNECTED = "has connected";

    private static String picture;
    private Socket socket;
    public String hostname;
    public int port;
    public static String username;
    public static String password;
    public ChatController controller;
    private static ObjectOutputStream oos;
    private InputStream is;
    private ObjectInputStream input;
    private OutputStream outputStream;
    Logger logger = LoggerFactory.getLogger(Listener.class);

    public Listener(String hostname, int port, String username, String password, ChatController controller) {
        this.hostname = hostname;
        this.port = port;
        Listener.username = username;
        Listener.password = password;
        this.controller = controller;
    }

    public void run() {
        try {
            socket = new Socket(hostname, port);
            outputStream = socket.getOutputStream();
            oos = new ObjectOutputStream(outputStream);
            is = socket.getInputStream();
            input = new ObjectInputStream(is);
        } catch (IOException e) {
            LoginController.getInstance().showErrorDialog("Could not connect to server");
            logger.error("Could not Connect");
        }
        logger.info("Connection accepted " + socket.getInetAddress() + ":" + socket.getPort());

        try {
            connect();
            boolean connect=true;
            LoginController.getInstance().showScene();
            logger.info("Sockets in and out ready!");
            while (socket.isConnected()&&connect) {
                Message message = null;
                message = (Message) input.readObject();

                if (message != null) {
                    logger.debug("Message recieved:" + message.getMsg() + " MessageType:" + message.getType() + "Name:" + message.getName());
                    switch (message.getType()) {
                        case USER:
                            controller.addToChat(message, username);
                            break;
                        case VOICE:
                            controller.addToChat(message, username);
                            break;
                        case NOTIFICATION:
                            controller.newUserNotification(message);
                            break;
                        case SERVER:
                            controller.addAsServer(message);
                            break;
                        case CONNECTED:
                            controller.setUserList(message);
                            break;
                        case DISCONNECTED:
                            controller.setUserList(message);
                            break;
                        case STATUS:
                            controller.setUserList(message);
                            break;
                        case DUPLICATION:
                        	controller.duplicationUser();
                        	connect=false;
                        	break;
                        case LOGOUT :
                        	connect=false;
                        	break;
                        case FAIL :
                        	controller.showFailDialog("Login Failed!");
                        	connect=false;
                        	break;
                        case CHANGE :
                        	controller.showSuccessChangeDialog("패스워드 변경 완료!");
                        	break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            controller.logoutScene();
	        if (input != null){
	        	try {
	        		input.close();
	        	} catch (IOException e) {
	        		e.printStackTrace();
	        	}
	        }
	        if (is != null){
	        	try {
	        		is.close();
	        	} catch (IOException e) {
	        		e.printStackTrace();
	        	}
	        }
	        if (oos != null){
	        	try {
	        		oos.close();
	        	} catch (IOException e) {
	        		e.printStackTrace();
	        	}
	        }
	        if (outputStream != null) {
	        	try {
	        		outputStream.close();
	        	} catch(IOException e) {
	        		e.printStackTrace();
	        	}
	        }
		}
    }
    
    /* This method is used for sending a normal Message
     * @param msg - The message which the user generates
     */
    public static void send(String msg) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.USER);
        createMessage.setStatus(Status.AWAY);
        createMessage.setMsg(msg);
        createMessage.setPicture(picture);
        oos.writeObject(createMessage);
        oos.flush();
    }

    /* This method is used for sending a voice Message
 * @param msg - The message which the user generates
 */
    public static void sendVoiceMessage(byte[] audio) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.VOICE);
        createMessage.setStatus(Status.AWAY);
        createMessage.setVoiceMsg(audio);
        createMessage.setPicture(picture);
        oos.writeObject(createMessage);
        oos.flush();
    }

    /* This method is used for sending a normal Message
 * @param msg - The message which the user generates
 */
    public static void sendStatusUpdate(Status status) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setType(MessageType.STATUS);
        createMessage.setStatus(status);
        createMessage.setPicture(picture);
        oos.writeObject(createMessage);
        oos.flush();
    }
    
    public static void sendChangePw(String prpw, String chpw) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        if(password.equals(prpw)) {
        	createMessage.setPassword(chpw);
        	createMessage.setType(MessageType.CHANGE);
        	oos.writeObject(createMessage);
        	oos.flush();
        	password=chpw;
        }else {
        	showFailDialog("입력한 기존 비밀번호 값이 현재 비빌번호와 일치하지 않습니다!");
        }
    }
    
    public static void logout() throws IOException{
    	Message createMessage = new Message();
    	createMessage.setType(MessageType.LOGOUT);
    	oos.writeObject(createMessage);
    }
    /* This method is used to send a connecting message */
    public static void connect() throws IOException {
        Message createMessage = new Message();
        createMessage.setName(username);
        createMessage.setPassword(password);
        createMessage.setType(CONNECTED);
        createMessage.setMsg(HASCONNECTED);
        createMessage.setPicture(picture);
        oos.writeObject(createMessage);
    }

    public static void showFailDialog(String message) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Failed!");
            alert.setHeaderText(message);
            alert.setContentText("The password you entered is different from the current password.");
            alert.showAndWait();
        });
    }
}

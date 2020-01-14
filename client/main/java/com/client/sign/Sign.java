package com.client.sign;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.client.login.LoginController;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;

import javafx.stage.Stage;

public class Sign implements Runnable{
	public SignController controller;
    private Socket socket;
    public String hostname;
    public int port;
    public static String username;
    private static ObjectOutputStream oos;
    private InputStream is;
    private ObjectInputStream input;
    private OutputStream outputStream;
    Logger logger = LoggerFactory.getLogger(Sign.class);
    
	public Sign(SignController controller) {
		this.controller = controller;
	}
	
	public void run() {
		try {
			socket = new Socket("localhost", 9001);
            LoginController.getInstance().showScene2();
            outputStream = socket.getOutputStream();
            oos = new ObjectOutputStream(outputStream);
            is = socket.getInputStream();
            input = new ObjectInputStream(is);
			
		}catch(IOException e) {
            LoginController.getInstance().showErrorDialog("Could not connect to server");
            logger.error("Could not Connect");
		}
		try {
			connect();
			logger.info("회원가입으로 접속 중 ");
			boolean stop=false;
            while (socket.isConnected()&&!stop) {
                Message message = null;
                message = (Message) input.readObject();
                
                if (message != null) {
                    logger.debug("Message recieved:" + message.getMsg() + " MessageType:" + message.getType() + "Name:" + message.getName());
                    
                    switch (message.getType()) {
                        case SIGN :
                        	SignController.getInstance().showSignupDialog("회원가입 완료!");
                        	stop=true;
                            break;
                        case ALLOW :
                            SignController.getInstance().showDuplicationDialog("사용가능한 아이디 입니다.");
                            break;
                        case DENY :
                        	SignController.getInstance().showErrorDialog("아이디가 중복되었습니다.");
                            break;
                        case HOME :
                        	stop=true;
                        	break;
                        default :
                        	break;
                    }
                    
                }
            }
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		controller.logoutScene();
	}
    public static void send(String msg,String msg2) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(msg);
        createMessage.setPassword(msg2);
        createMessage.setType(MessageType.SIGN);
        oos.writeObject(createMessage);
        oos.flush();
    }
    public static void sendCheck(String id) throws IOException {
        Message createMessage = new Message();
        createMessage.setName(id);
        createMessage.setType(MessageType.CHECK);
        oos.writeObject(createMessage);
        oos.flush();
    }
    public static void disconnect() throws IOException{
    	Message createMessage = new Message();
    	createMessage.setType(MessageType.HOME);
    	oos.writeObject(createMessage);
    }
    
	
    public static void connect() throws IOException {
        Message createMessage = new Message();
        createMessage.setType(MessageType.SIGN);
        oos.writeObject(createMessage);
    }
}



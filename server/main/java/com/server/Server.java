package com.server;

import com.exception.DuplicateUsernameException;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;
import com.messages.User;
import oracle.jdbc.OracleDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Server {

    /* Setting up variables */
    private static final int PORT = 9001;
    private static final String TABLE = "SIGN";
    private static final HashMap<String, User> names = new HashMap<>();
    private static HashSet<ObjectOutputStream> writers = new HashSet<>();
    private static ArrayList<User> users = new ArrayList<>();
    static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws Exception {
        logger.info("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);

        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            listener.close();
        }
    }


    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private Logger logger = LoggerFactory.getLogger(Handler.class);
        private User user;
        private ObjectInputStream input;
        private OutputStream os;
        private ObjectOutputStream output;
        private InputStream is;
        
        private final String USER = "miner";
        private final String PASSWD="wjsansrk";
        public static final String URL = "jdbc:oracle:thin:@127.0.0.1:1521:orcl";

        public Handler(Socket socket) throws IOException {
            this.socket = socket;
        }

        public void run() {
            logger.info("Attempting to connect a user...");
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            
            boolean stopped=false;
            int test = 1;
            try {
            	DriverManager.deregisterDriver(new OracleDriver());
            	conn=DriverManager.getConnection(URL,USER,PASSWD);
            	
                is = socket.getInputStream();
                input = new ObjectInputStream(is);
                os = socket.getOutputStream();
                output = new ObjectOutputStream(os);
                Message firstMessage = (Message) input.readObject();
                if(firstMessage.getType()==MessageType.SIGN) {
                	test=2;
                	while(socket.isConnected()&&!stopped) {
                		Message signmsg=(Message) input.readObject();
                		if(signmsg!=null) {
                			String sql=null;
                			switch(signmsg.getType()) {
                				case SIGN :
                					sql="INSERT INTO "+TABLE+" VALUES (?,?)";
                        			pstmt=conn.prepareStatement(sql);
                        			pstmt.setString(1, signmsg.getName());
                        			pstmt.setString(2, signmsg.getPassword());
                        			int result = pstmt.executeUpdate();
                        			Message remsg = new Message();
                        			remsg.setType(MessageType.SIGN);
                        			output.writeObject(remsg);
                        			output.flush();
                        			stopped=true;
                        			break;
                				case CHECK :
                					sql="SELECT * FROM "+TABLE+" WHERE ID = ?";
                					pstmt=conn.prepareStatement(sql);
                        			pstmt.setString(1, signmsg.getName());
                        			if(pstmt.executeUpdate()==0) {
                        				sendMsg(output,MessageType.ALLOW);
                        			}else {
                        				sendMsg(output,MessageType.DENY);
                        			}
                        			break;
                				case HOME :
                					sendMsg(output,MessageType.HOME);
                					stopped=true;
                					break;
                        		default :
                        			break;
                			}
                		}
                	}
                }else {
                	LoginCheck(conn,firstMessage,output);
                	checkDuplicateUsername(firstMessage,output);
                	writers.add(output);
                	sendNotification(firstMessage);
                	addToList();

                	while (socket.isConnected()) {
                		Message inputmsg = (Message) input.readObject();
                		if (inputmsg != null) {
                			logger.info(inputmsg.getType() + " - " + inputmsg.getName() + ": " + inputmsg.getMsg());
                			switch (inputmsg.getType()) {
                				case USER:
                					write(inputmsg);
                					break;
                				case VOICE:
                					write(inputmsg);
                					break;
                				case CONNECTED:
                					addToList();
                					break;
                				case STATUS:
                					changeStatus(inputmsg);
                					break;
                				case LOGOUT :
                					sendMsg(output,MessageType.LOGOUT);
                					stopped=true;
                					break;
                				case CHANGE :
                					ChangePw(conn,inputmsg,output);
                					break;
                			}
                		}
                	}
                }
            } catch (SocketException socketException) {
                logger.error("Socket Exception for user " + name);
            } catch (DuplicateUsernameException duplicateException){
                logger.error("Duplicate Username : " + name);
            } catch (Exception e){
                logger.error("Exception in run() method for user: " + name, e);
            } finally {
            	closeConnections();
            	if(test==2) {
                    if (pstmt != null){
                        try {
                            pstmt.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (conn != null){
                        try {
                            conn.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            	}
            	
            }
        }

        private Message changeStatus(Message inputmsg) throws IOException {
            logger.debug(inputmsg.getName() + " has changed status to  " + inputmsg.getStatus());
            Message msg = new Message();
            msg.setName(user.getName());
            msg.setType(MessageType.STATUS);
            msg.setMsg("");
            User userObj = names.get(name);
            userObj.setStatus(inputmsg.getStatus());
            write(msg);
            return msg;
        }
        
        private void LoginCheck(Connection conn,Message firstMessage, ObjectOutputStream output) throws DuplicateUsernameException, SQLException, IOException {
        	String id = firstMessage.getName();
        	String pw = firstMessage.getPassword();
            PreparedStatement pstmt = null;
        	String sql="SELECT ID FROM "+TABLE+" WHERE id = ? and password = ?";
			pstmt=conn.prepareStatement(sql);
			pstmt.setString(1, id);
			pstmt.setString(2, pw);
			pstmt.executeUpdate();
			Message msg = new Message();
			if(pstmt.executeUpdate()==0) {
				//½ÇÆÐ
				msg.setType(MessageType.FAIL);
				output.writeObject(msg);
				output.flush();
				throw new DuplicateUsernameException(firstMessage.getName() + " failed to login.");
			}
			pstmt.close();
        }
        
        private void ChangePw(Connection conn,Message msg ,ObjectOutputStream output) throws SQLException, IOException{
        	PreparedStatement pstmt = null;
        	String sql = "UPDATE "+TABLE+" SET PASSWORD = ? WHERE ID = ?";
        	pstmt=conn.prepareStatement(sql);
        	pstmt.setString(1,msg.getPassword());
        	pstmt.setString(2,msg.getName());
        	pstmt.executeUpdate();
        	Message result = new Message();
        	result.setType(MessageType.CHANGE);
        	output.writeObject(result);
        	output.flush();
        }
        
        private synchronized void checkDuplicateUsername(Message firstMessage, ObjectOutputStream output) throws DuplicateUsernameException, IOException {
            logger.info(firstMessage.getName() + " is trying to connect");
            if (!names.containsKey(firstMessage.getName())) {
                this.name = firstMessage.getName();
                user = new User();
                user.setName(firstMessage.getName());
                user.setStatus(Status.ONLINE);
                user.setPicture(firstMessage.getPicture());

                users.add(user);
                names.put(name, user);

                logger.info(name + " has been added to the list");
            } else {
            	logger.error(firstMessage.getName() + " is already connected");
                Message msg = new Message();
                msg.setMsg("has joined the chat.");
                msg.setType(MessageType.DUPLICATION);
                //ObjectOutputStream writer = null;
                output.writeObject(msg);
                throw new DuplicateUsernameException(firstMessage.getName() + " is already connected");
            }
        }

        private Message sendNotification(Message firstMessage) throws IOException {
            Message msg = new Message();
            msg.setMsg("has joined the chat.");
            msg.setType(MessageType.NOTIFICATION);
            msg.setName(firstMessage.getName());
            msg.setPicture(firstMessage.getPicture());
            write(msg);
            return msg;
        }


        private Message removeFromList() throws IOException {
            logger.debug("removeFromList() method Enter");
            Message msg = new Message();
            msg.setMsg("has left the chat.");
            msg.setType(MessageType.DISCONNECTED);
            msg.setName("SERVER");
            msg.setUserlist(names);
            write(msg);
            logger.debug("removeFromList() method Exit");
            return msg;
        }

        /*
         * For displaying that a user has joined the server
         */
        private Message addToList() throws IOException {
            Message msg = new Message();
            msg.setMsg("Welcome, You have now joined the server! Enjoy chatting!");
            msg.setType(MessageType.CONNECTED);
            msg.setName("SERVER");
            write(msg);
            return msg;
        }

        private void sendMsg(ObjectOutputStream output,MessageType type) throws IOException{
        	Message msg = new Message();
        	msg.setType(type);
        	output.writeObject(msg);
        	output.flush();
        }
        /*
         * Creates and sends a Message type to the listeners.
         */
        private void write(Message msg) throws IOException {
            for (ObjectOutputStream writer : writers) {
                msg.setUserlist(names);
                msg.setUsers(users);
                msg.setOnlineCount(names.size());
                writer.writeObject(msg);
                writer.reset();
            }
        }

        /*
         * Once a user has been disconnected, we close the open connections and remove the writers
         */
        private synchronized void closeConnections()  {
            logger.debug("closeConnections() method Enter");
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size() + " usersList size:" + users.size());
            if (name != null) {
                names.remove(name);
                logger.info("User: " + name + " has been removed!");
            }
            if (user != null){
                users.remove(user);
                logger.info("User object: " + user + " has been removed!");
            }
            if (output != null){
                writers.remove(output);
                logger.info("Writer object: " + user + " has been removed!");
            }
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (input != null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                removeFromList();
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("HashMap names:" + names.size() + " writers:" + writers.size() + " usersList size:" + users.size());
            logger.debug("closeConnections() method Exit");
        }
    }
}

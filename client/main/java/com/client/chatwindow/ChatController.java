package com.client.chatwindow;

import com.client.login.MainLauncher;
import com.client.util.VoicePlayback;
import com.client.util.VoiceRecorder;
import com.client.util.VoiceUtil;
import com.messages.Message;
import com.messages.MessageType;
import com.messages.Status;
import com.messages.User;
import com.messages.bubble.BubbleSpec;
import com.messages.bubble.BubbledLabel;
import com.traynotifications.animations.AnimationType;
import com.traynotifications.notification.TrayNotification;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Insets;

import java.awt.Desktop;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatController implements Initializable {

    @FXML private TextArea messageBox;
    @FXML private Label usernameLabel;
    @FXML private Label onlineCountLabel;
    @FXML private ListView userList;
    @FXML private Button recordBtn;
    @FXML private Button fileBtn;
    @FXML Pane settingPane;
    @FXML ListView chatPane;
    @FXML ListView statusList;
    @FXML BorderPane borderPane;
    @FXML ComboBox statusComboBox;
    @FXML ImageView microphoneImageView;
    @FXML PasswordField currentPW;
    @FXML PasswordField changePW;

    Image microphoneActiveImage = new Image(getClass().getClassLoader().getResource("images/microphone-active.png").toString());
    Image microphoneInactiveImage = new Image(getClass().getClassLoader().getResource("images/microphone.png").toString());

    private double xOffset;
    private double yOffset;
    Logger logger = LoggerFactory.getLogger(ChatController.class);


    public void sendButtonAction() throws IOException {
        String msg = messageBox.getText();
        if (!messageBox.getText().isEmpty()) {
            Listener.send(msg);
            messageBox.clear();
        }
    }
    
    public void recordVoiceMessage() throws IOException {
        if (VoiceUtil.isRecording()) {
            Platform.runLater(() -> {
                microphoneImageView.setImage(microphoneInactiveImage);
                }
            );
            VoiceUtil.setRecording(false);
        } else {
            Platform.runLater(() -> {
                microphoneImageView.setImage(microphoneActiveImage);
                }
            );
            VoiceRecorder.captureAudio();
        }
    }

    public synchronized void addToChat(Message msg, String username) {
        Task<HBox> othersMessages = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {
                BubbledLabel bl6 = new BubbledLabel();
                bl6.setBackground(new Background(new BackgroundFill(Color.SKYBLUE,null, null)));
                HBox x = new HBox();
                bl6.setBubbleSpec(BubbleSpec.FACE_LEFT_CENTER);
                if (msg.getType() == MessageType.VOICE){
                    ImageView imageview = new ImageView(new Image(getClass().getClassLoader().getResource("images/sound.png").toString()));
                    bl6.setGraphic(imageview);
                    bl6.setText(msg.getName() + ": " + "Sent a voice message!");
                    VoicePlayback.playAudio(msg.getVoiceMsg());
                    x.getChildren().add(bl6);
                }else {
                	if(UrlFind(msg.getMsg())==true) {
                		ArrayList<String> msgdiv=UrlHtml(msg.getMsg());
                		int index=0;
                		int whomsg=1;
                		bl6.setText("");
                		x.getChildren().add(bl6);
                		for(int i=0;i<msgdiv.size();i++) {
                			Label dlabel = new Label();
                            dlabel.setBackground(new Background(new BackgroundFill(Color.SKYBLUE, null, null)));
                            dlabel.setPadding(new Insets(5,0,5,0));
                			dlabel.setText(msg.getName() + ": " + msg.getMsg().substring(index,msg.getMsg().indexOf(msgdiv.get(i))));
                			x.getChildren().add(dlabel);
                			x.getChildren().add(HyperlinkCreate(msgdiv.get(i),whomsg));
                			index=msg.getMsg().indexOf(msgdiv.get(i))+msgdiv.get(i).length();
                		}
                		Label rlabel = new Label();
                		rlabel.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
                        rlabel.setPadding(new Insets(5,0,5,0));
                        rlabel.setText(msg.getMsg().substring(index));
                        x.getChildren().add(rlabel);
                	}else {
                		bl6.setText(msg.getName() + ": " + msg.getMsg());
                		x.getChildren().add(bl6);
                	}
                }
                
                logger.debug("ONLINE USERS: " + Integer.toString(msg.getUserlist().size()));
                setOnlineLabel(Integer.toString(msg.getOnlineCount()));
                return x;
            }
        };

        othersMessages.setOnSucceeded(event -> {
            chatPane.getItems().add(othersMessages.getValue());
        });

        Task<HBox> yourMessages = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {
                BubbledLabel bl6 = new BubbledLabel();
                bl6.setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN, null, null)));
                HBox x = new HBox();
                if (msg.getType() == MessageType.VOICE){
                    bl6.setGraphic(new ImageView(new Image(getClass().getClassLoader().getResource("images/sound.png").toString())));
                    bl6.setText("Sent a voice message!");
                    VoicePlayback.playAudio(msg.getVoiceMsg());
                }else {
                	if(UrlFind(msg.getMsg())==true) {
                		ArrayList<String> msgdiv=UrlHtml(msg.getMsg());
                		int index=0;
                		int whomsg=2;
                		for(int i=0;i<msgdiv.size();i++) {
                			Label dlabel = new Label();
                            dlabel.setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN, null, null)));
                            dlabel.setPadding(new Insets(5,0,5,0));
                			dlabel.setText(msg.getMsg().substring(index,msg.getMsg().indexOf(msgdiv.get(i))));
                			x.getChildren().add(dlabel);
                			x.getChildren().add(HyperlinkCreate(msgdiv.get(i),whomsg));
                			index=msg.getMsg().indexOf(msgdiv.get(i))+msgdiv.get(i).length();
                		}
                		bl6.setText(msg.getMsg().substring(index));
                	}else {
                		bl6.setText(msg.getMsg());
                	}
                }

                x.setMaxWidth(chatPane.getWidth() - 20);
                x.setAlignment(Pos.TOP_RIGHT);
                bl6.setBubbleSpec(BubbleSpec.FACE_RIGHT_CENTER);
                x.getChildren().addAll(bl6);

                setOnlineLabel(Integer.toString(msg.getOnlineCount()));
                return x;
            }
        };
        yourMessages.setOnSucceeded(event -> chatPane.getItems().add(yourMessages.getValue()));

        if (msg.getName().equals(usernameLabel.getText())) {
            Thread t2 = new Thread(yourMessages);
            t2.setDaemon(true);
            t2.start();
        } else {
            Thread t = new Thread(othersMessages);
            t.setDaemon(true);
            t.start();
        }
    }
    public void setUsernameLabel(String username) {
        this.usernameLabel.setText(username);
    }

    public void setOnlineLabel(String usercount) {
        Platform.runLater(() -> onlineCountLabel.setText(usercount));
    }

    public void setUserList(Message msg) {
        logger.info("setUserList() method Enter");
        Platform.runLater(() -> {
            ObservableList<User> users = FXCollections.observableList(msg.getUsers());
            userList.setItems(users);
            userList.setCellFactory(new CellRenderer());
            setOnlineLabel(String.valueOf(msg.getUserlist().size()));
        });
        logger.info("setUserList() method Exit");
    }

    /* Displays Notification when a user joins */
    public void newUserNotification(Message msg) {
        Platform.runLater(() -> {
        	TrayNotification tray = new TrayNotification();
            tray.setTitle("A new user has joined!");
            tray.setMessage(msg.getName() + " has joined the JavaFX Chatroom!");
            tray.setRectangleFill(Paint.valueOf("#2C3E50"));
            tray.setAnimationType(AnimationType.POPUP);
            tray.showAndDismiss(Duration.seconds(5));
            try {
                Media hit = new Media(getClass().getClassLoader().getResource("sounds/notification.wav").toString());
                MediaPlayer mediaPlayer = new MediaPlayer(hit);
                mediaPlayer.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }
    /* username 중복시 알림 */
    public void duplicationUser() {
    	Platform.runLater(() ->{
    		TrayNotification duplication = new TrayNotification();
    		duplication.setTitle("ERROR_MESSAGE");
    		duplication.setMessage("This username is connected.");
    		duplication.showAndDismiss(Duration.seconds(5));  //5초후 사라짐
    	});
    }
    
    public void sendMethod(KeyEvent event) throws IOException {
        if (event.getCode() == KeyCode.ENTER) {
            sendButtonAction();
        }
    }

    @FXML
    public void closeApplication() {
        Platform.exit();
        System.exit(0);
    }
    
    @FXML
    public void minimizeWindow() {
    	MainLauncher.getPrimaryStage().setIconified(true);
    }
    
    @FXML
    public void setting() {
    	settingPane.setVisible(true); 	
    }

    /* Method to display server messages */
    public synchronized void addAsServer(Message msg) {
        Task<HBox> task = new Task<HBox>() {
            @Override
            public HBox call() throws Exception {
                BubbledLabel bl6 = new BubbledLabel();
                bl6.setText(msg.getMsg());
                bl6.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, null, null)));
                HBox x = new HBox();
                bl6.setBubbleSpec(BubbleSpec.FACE_BOTTOM);
                x.setAlignment(Pos.CENTER);
                x.getChildren().addAll(bl6);
                return x;
            }
        };
        task.setOnSucceeded(event -> {
            chatPane.getItems().add(task.getValue());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
    
    public void logoutButtonAction() throws IOException {
    	Listener.logout();
    }
   
    public void changeButtonAction() throws IOException {
    	String curpw = currentPW.getText();
    	String chapw = changePW.getText();
    	if(!currentPW.getText().isEmpty()&&!changePW.getText().isEmpty())
    		Listener.sendChangePw(curpw,chapw);
    	else
    		showEmptyDialog("값이 모두 입력되지않았습니다.");
    		
    }
    
    public void closeButtonAction() throws IOException {
    	settingPane.setVisible(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	settingPane.setVisible(false);
    	
    	/* Drag and Drop */
        borderPane.setOnMousePressed(event -> {
            xOffset = MainLauncher.getPrimaryStage().getX() - event.getScreenX();
            yOffset = MainLauncher.getPrimaryStage().getY() - event.getScreenY();
            borderPane.setCursor(Cursor.CLOSED_HAND);
        });

        borderPane.setOnMouseDragged(event -> {
            MainLauncher.getPrimaryStage().setX(event.getScreenX() + xOffset);
            MainLauncher.getPrimaryStage().setY(event.getScreenY() + yOffset);

        });

        borderPane.setOnMouseReleased(event -> {
            borderPane.setCursor(Cursor.DEFAULT);
        });

        statusComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                try {
                    Listener.sendStatusUpdate(Status.valueOf(newValue.toUpperCase()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        /* Added to prevent the enter from adding a new line to inputMessageBox */
        messageBox.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                try {
                    sendButtonAction();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ke.consume();
            }
        });

    }

    public void logoutScene() {
        Platform.runLater(() -> {
        	FXMLLoader fmxlLoader=null;
        	fmxlLoader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent window = null;
            try {
                window = (Pane) fmxlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Stage stage = MainLauncher.getPrimaryStage();
            Scene scene = new Scene(window);
            stage.setWidth(350);
            stage.setHeight(420);
            stage.setMaxWidth(350);
            stage.setMaxHeight(420);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.centerOnScreen();
        });
    }
    
    public boolean UrlFind(String str) {
    	String regex ="[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";
    	Pattern p = Pattern.compile(regex);
    	Matcher m = p.matcher(str);
    	if(m.find()) return true;
    	else return false;
    }
    
	public ArrayList<String> UrlHtml(String str){
	    String regex ="[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";
	    Pattern p = Pattern.compile(regex);
	    Matcher m=p.matcher(str);        
	    ArrayList<String> result = new ArrayList<>();
	    while(m.find()) result.add(m.group());
	    return result;
	}

	public static void openWebpage(URI uri) {
	    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
	    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
	        try {
	            desktop.browse(uri);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}

	public static void openWebpage(URL url) {
		try {
			openWebpage(url.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public Hyperlink HyperlinkCreate(String str,int who) {
		String url;
    	Hyperlink hyperlink= new Hyperlink(str);
    	if(who==1) hyperlink.setBackground(new Background(new BackgroundFill(Color.SKYBLUE, null, null)));
    	else hyperlink.setBackground(new Background(new BackgroundFill(Color.LIGHTGREEN, null, null)));
        hyperlink.setVisited(true);
        hyperlink.setUnderline(true);
        hyperlink.setBorder(Border.EMPTY);
        hyperlink.setPadding(new Insets(5,0,5,0));
        String protocal = "http://";
        if(str.substring(0,7).matches(protocal)) url=str;
        else url=protocal.concat(str);
        
        hyperlink.setOnAction(new EventHandler<ActionEvent>() { 
        	@Override
            public void handle(ActionEvent e) 
            { 
        		try {
					URL testURL=new URL(url);
					openWebpage(testURL);
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
            }
            
        });
		return hyperlink;
	}
	
    public void showFailDialog(String message) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Failed!");
            alert.setHeaderText(message);
            alert.setContentText("Please check for your id and password.");
            alert.showAndWait();
        });
    }
    
    public void showSuccessChangeDialog(String message) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success!");
            alert.setHeaderText(message);
            alert.setContentText("비밀번호가 성공적으로 변경되었습니다.");
            alert.showAndWait();
        });
    }
    
    public void showEmptyDialog(String message) {
        Platform.runLater(()-> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Empty!");
            alert.setHeaderText(message);
            alert.showAndWait();
        });

    }
}
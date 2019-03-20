
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.Scanner;

public class LobbyView extends View {
	@FXML JFXButton enterBtn;
	@FXML JFXTextField usernameField, IPField;

	public LobbyView(ClientController controller, Stage primaryStage) throws Exception {
		super(controller);
		this.primaryStage = primaryStage;

		System.out.println("You are in lobby view");

		FXMLLoader loader = new FXMLLoader(getClass().getResource("lobbyTemplate.fxml"));
		loader.setController(this);
		
		StageManager sm = new StageManager(primaryStage);
		sm.loadScene(loader);
		sm.setWindowName("Lobby");

		init();
	}


	public void joinGame (ActionEvent actionEvent) throws Exception {
		//System.out.print("Enter server ip: ");
		//Scanner sc = new Scanner (System.in);
		//String hostname = sc.nextLine();
		String hostname = IPField.getText();

		//System.out.print("Enter a username: ");
		//sc = new Scanner (System.in);
		//String username = sc.nextLine();
		String username = usernameField.getText();

		if (!hostname.isEmpty() && !username.isEmpty()) {
			controller.submitUsername(hostname, username);
		}

	}

	public void init(){
		Image enter = new Image("resources/enter.png");
		ImageView enterView = new ImageView(enter);
		enterView.setFitHeight(35);
		enterView.setFitWidth(85);
		enterBtn.setGraphic(enterView);
	}


	@Override
	public void Update() {

	}
}
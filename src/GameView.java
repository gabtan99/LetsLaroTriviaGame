import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Scanner;

public class GameView extends View{

	@FXML Text playersText;
	@FXML Label questionLabel;
	@FXML Label scoreLabel;
	@FXML Label ans1Label, ans2Label, ans3Label, ans4Label;
	@FXML JFXButton ansBtn1, ansBtn2, ansBtn3, ansBtn4;

	public GameView(ClientController controller, Stage primaryStage) {
		super(controller);
		this.primaryStage = primaryStage;

		System.out.println("You are in game view");

		FXMLLoader loader = new FXMLLoader(getClass().getResource("gameTemplate.fxml"));
		loader.setController(this);

		StageManager sm = new StageManager(primaryStage);
		sm.loadScene(loader);
		sm.setWindowName("Trivia");

		init();
	}

	public void init(){
		questionLabel.setText("Waiting for enough players to connect.");
		ans1Label.setText("");
		ans2Label.setText("");
		ans3Label.setText("");
		ans4Label.setText("");

		DropShadow dropShadow = new DropShadow();
		dropShadow.setRadius(7.0);
		dropShadow.setColor(Color.color(0, 0, 0.10));
		playersText.setEffect(dropShadow);
		questionLabel.setEffect(dropShadow);
		scoreLabel.setEffect(dropShadow);
		ans1Label.setEffect(dropShadow);
		ans2Label.setEffect(dropShadow);
		ans3Label.setEffect(dropShadow);
		ans4Label.setEffect(dropShadow);

		Image ansTri = new Image("resources/ansButton.png");
		Image ansTri2 = new Image("resources/ansButton2.png");
		ImageView ansView = new ImageView(ansTri);
		ImageView ansView2 = new ImageView(ansTri2);
		ImageView ansView3 = new ImageView(ansTri);
		ImageView ansView4 = new ImageView(ansTri2);
		ansBtn1.setGraphic(ansView2);
		ansBtn2.setGraphic(ansView);
		ansBtn3.setGraphic(ansView4);
		ansBtn4.setGraphic(ansView3);
		ansView.setFitWidth(70);
		ansView.setFitHeight(30);
		ansView2.setFitWidth(70);
		ansView2.setFitHeight(30);
		ansView3.setFitWidth(70);
		ansView3.setFitHeight(30);
		ansView4.setFitWidth(70);
		ansView4.setFitHeight(30);
	}

	@Override
	public void Update()  {
		this.state = controller.getMystate();
		printQuestion(this.state);
		updateButtons();
		questionLabel.setText(state.getCurrentQuestion().getQuestion());
	}

	private void updateButtons() {

		if (state.getCurrentQuestion().getAnswersList().size() > 0) {
			ans1Label.setText(state.getCurrentQuestion().getAnswersList().get(0).getAnswer());
			ans1Label.setOnMouseClicked(e -> {
				try {
					selectAnswer(0);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
		}

		if (state.getCurrentQuestion().getAnswersList().size() > 1) {
			ans2Label.setText(state.getCurrentQuestion().getAnswersList().get(1).getAnswer());
			ans2Label.setOnMouseClicked(e -> {
				try {
					selectAnswer(1);

				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
		}

		if (state.getCurrentQuestion().getAnswersList().size() > 2) {
			ans3Label.setText(state.getCurrentQuestion().getAnswersList().get(2).getAnswer());
			ans3Label.setOnMouseClicked(e -> {
				try {
					selectAnswer(2);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
		}

		if (state.getCurrentQuestion().getAnswersList().size() > 3) {
			ans4Label.setText(state.getCurrentQuestion().getAnswersList().get(3).getAnswer());
			ans4Label.setOnMouseClicked(e -> {
				try {
					selectAnswer(3);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
		}
	}

	public void selectAnswer(int n) throws Exception {
		controller.selectAnswer(n);
	}

	private void printQuestion (GameState state) {

		System.out.println("Player: " + state.getCurrentPlayer().getName() + " - " + state.getCurrentPlayer().getScore());
		System.out.println(state.getQuestionNumber() + ": " + state.getCurrentQuestion().getQuestion());

		for (Answer a: state.getCurrentQuestion().getAnswersList()) {
			System.out.println(a.getAnswer());
		}

	}
}
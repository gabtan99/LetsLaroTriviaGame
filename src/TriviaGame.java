import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TriviaGame {

	private List<Question> questionsList;
	private List<Player> playersList;
	private boolean isDone;
	private Connection connection;
	private int nQuestions;

	public TriviaGame (int nQuestions) {

		this.nQuestions = nQuestions;
		playersList = new ArrayList<>();
		isDone = false;

		dbConnection connector = new dbConnection();
		connection = connector.getConnection();
	}

	private List<Question> buildQuestions (List selectedNums) {

		QuestionDAODB questionsDAO = new QuestionDAODB(connection);
		AnswersDAODB answersDAO = new AnswersDAODB(connection);

		List<Question> readyQs = new ArrayList<>();

		for (Object n: selectedNums) {
			int selected = ((Integer)n);

			Question q = questionsDAO.getQuestion(selected);
			q.setAnswersList(answersDAO.getAnswers(selected));

			readyQs.add(q);
		}

		return readyQs;
	}

	private List getSelectedQuestionIDs (int nSelected){
		QuestionDAODB test = new QuestionDAODB(connection);
		int totalQuestions = test.getNQuestions();

		if (nSelected > totalQuestions) {
			nSelected = totalQuestions;
		}

		// get all songs and shuffle
		ArrayList randompool = new ArrayList();
		for (int i=0; i<totalQuestions; i++) {
			randompool.add(i+1);
		}
		Collections.shuffle(randompool);

		// select first n IDs
		ArrayList selectedpool = new ArrayList();
		for(int i=0; i<nSelected; i++) {
			selectedpool.add(randompool.get(i));
		}

		return selectedpool;
	}

	public boolean startGame () {

		System.out.println("\n\nWELCOME TO TRIVIA GAME\n\n");
		questionsList = buildQuestions(getSelectedQuestionIDs(nQuestions));
		return true;
	}

	public boolean connectPlayer(Player dude) {
		playersList.add(dude);
		System.out.println(dude.getName() + " has joined the game");
		return true;
	}

	public boolean disconnectPlayer(Player dude) {
		playersList.remove(dude);
		System.out.println(dude.getName() + " has left the game");
		return true;
	}

	public boolean askQuestion () {

		if(questionsList.isEmpty()) {
			return false;
		}

		System.out.println(questionsList.get(0).getQuestion() + "(" + questionsList.get(0).getPoints() + " Points)");
		System.out.println("--------CHOICES--------");

		for (Answer a: questionsList.get(0).getAnswersList()) {
			System.out.println(a.getAnswer());
		}

		return true;
	}

	public boolean checkAnswer (String answer, Player dude) {

		Question q = questionsList.get(0);

		for (Answer a: q.getAnswersList()) {
			if (answer.equals(a.getAnswer()) && a.isCorrect()) {
				addPoints(dude, q.getPoints());
				questionsList.remove(0);
				return true;
			}
		}

		questionsList.remove(0);
		return false;
	}

	public void addPoints (Player dude, int points) {
		int i = playersList.indexOf(dude);
		int score = playersList.get(i).getScore();
		playersList.get(i).setScore(score+points);
	}

	public List<Player> getPlayersList () {
		return playersList;
	}

}

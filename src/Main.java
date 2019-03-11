import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        TriviaGame game = new TriviaGame(3);
        Player me = new Player("Gab");

        game.connectPlayer(me);
        game.startGame();

        while (game.askQuestion()) {


            Scanner sc = new Scanner(System.in);
            String myanswer = sc.nextLine();

            GameState state = game.getGameState(me);
            System.out.println("////////////////////////////");
            System.out.println(state.getCurrentPlayer().getName() + "Points: " + state.getCurrentPlayer().getScore());
            System.out.println("Question " + state.getQuestionNumber()+ state.getCurrentQuestion().getQuestion());
            System.out.println("Done State: " + state.isDone());
            System.out.println("Player Quitting " + state.isQuitting());
            System.out.println("////////////////////////////");


            if (game.checkAnswer(myanswer, me)) {
                System.out.println("correct!");
            } else
                System.out.println("wrong!");

        }

        for(Player p: game.getPlayersList()) {
            System.out.println(p.getName() + ": " + p.getScore() + " points");
        }

        game.disconnectPlayer(me);

    }

}

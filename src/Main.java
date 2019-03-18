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


        }

        for(Player p: game.getPlayersList()) {
            System.out.println(p.getName() + ": " + p.getScore() + " points");
        }

        game.disconnectPlayer(me);

    }

}

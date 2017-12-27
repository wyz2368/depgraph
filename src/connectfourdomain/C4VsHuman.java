package connectfourdomain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import connectfourdomain.C4Board.Winner;

/**
 * This main class runs a game between the baseline
 * C4Player (depth-limited minimax searcher) and
 * the human, giving input over standard input.
 */
public final class C4VsHuman {

	/**
	 * Main method for playing the game.
	 * @param args not used
	 */
	public static void main(final String[] args) {
		final int searchDepth = 1;
		C4Player.setSearchDepth(searchDepth);
		playGame();
	}
	
	/**
	 * Private constructor for utility class.
	 */
	private C4VsHuman() {
		// not called
	}
	
	/**
	 * Prompts AI and human for moves until the game
	 * is over, then declares the winner.
	 */
	public static void playGame() {
		final C4Board board = new C4Board();
		while (board.getWinner() == Winner.NONE) {
			System.out.println("\n" + board + "\n");
			if (board.isBlackTurn()) {
				int col = getHumanMove();
				while (!board.isLegalMove(col)) {
					System.out.println("Invalid move.");
					col = getHumanMove();
				}
				board.makeMove(col);
			} else {
				board.makeMove(C4Player.getRedMove(board));
			}
		}
		
		System.out.println(board);
		System.out.println("Winner: " + board.getWinner());
		if (board.getWinner() == Winner.RED) {
			System.out.println("Computer wins, puny human.");
		} else if (board.getWinner() == Winner.BLACK) {
			System.out.println("You have defeated the computer!");
		}
	}
	
	/**
	 * Prompts human for move over standard input,
	 * asking again if invalid.
	 * @return the integer column where the human would move.
	 */
	public static int getHumanMove() {
		System.out.println("Enter your move, as column 0-6: ");
		
		final BufferedReader reader =
			new BufferedReader(new InputStreamReader(System.in));
		try {
			final String input = reader.readLine();
			try {
			    return Integer.parseInt(input);
			} catch (NumberFormatException e) {
			    System.out.println("That is not an integer.");
			    return getHumanMove();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		System.out.println("Failed to get input. Try again.");
		return getHumanMove();
	}
}

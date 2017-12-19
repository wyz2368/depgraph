package connectfourdomain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import connectfourdomain.C4Board.Winner;

public final class C4VsHuman {

	public static void main(final String[] args) {
		playGame();
	}
	
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
	}
	
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

package connectfourdomain;

import connectfourdomain.C4Board.Winner;

// always plays as red and goes first
public final class C4Player {

	public static final int SEARCH_DEPTH = 4;
	
	public static final int MAX_VALUE = 100;
	
	public static int getRedMove(final C4Board board) {
		assert !board.isBlackTurn();
		assert board.getWinner() == Winner.NONE;
		final boolean[][] redPieces = board.getRedPieces();
		final boolean[][] blackPieces = board.getBlackPieces();
		
		// move in center column if empty board
		if (isEmpty(redPieces, blackPieces)) {
			return C4Board.WIDTH / 2;
		}
		
		
		return 0;
	}
	
	private static int getMinimaxMove(final C4Board board) {
		final C4Board boardCopy = new C4Board(board);
		
		
		return 0;
	}
	
	private static boolean isEmpty(
		final boolean[][] redPieces,
		final boolean[][] blackPieces) {
		for (int col = 0; col < C4Board.WIDTH; col++) {
			if (redPieces[0][col] && !blackPieces[0][col]) {
				return false;
			}
		}
		return true;
	}
	
	private static int boardValue(
		final C4Board board
	) {
		if (board.getWinner() == Winner.RED) {
			return MAX_VALUE;
		}
		if (board.getWinner() == Winner.BLACK) {
			return -1 * MAX_VALUE;
		}
		final int redLength = maxColorLength(board.getRedPieces());
		final int blackLength = maxColorLength(board.getBlackPieces());
		return redLength - blackLength;
	}
	
	private static int maxColorLength(
		final boolean[][] colorPieces
	) {
		int result = 0;
		
		// check vertical lines
		for (int col = 0; col < C4Board.WIDTH; col++) {
			int colorLength = 0;
			for (int row = 0; row < C4Board.HEIGHT; row++) {
				if (colorPieces[row][col]) {
					colorLength++;
					if (colorLength > result) {
						result = colorLength;
					}
				} else {
					colorLength = 0;
				}
			}
		}
		
		// check for horizontal line
		for (int row = 0; row < C4Board.HEIGHT; row++) {
			int colorLength = 0;
			for (int col = 0; col < C4Board.WIDTH; col++) {
				if (colorPieces[row][col]) {
					colorLength++;
					if (colorLength > result) {
						result = colorLength;
					}
				} else {
					colorLength = 0;
				}
			}
		}
		
		// check for right diagonal line
		final int diagonals = 6;
		for (int i = 0; i < diagonals; i++) {
			final int top = Math.min(C4Board.GOAL + i - 1, C4Board.HEIGHT - 1);
			final int left = Math.max(i - 2, 0);
			int row = top;
			int col = left;
			int colorLength = 0;
			while (row >= 0 && col < C4Board.WIDTH) {
				if (colorPieces[row][col]) {
					colorLength++;
					if (colorLength > result) {
						result = colorLength;
					}
				} else {
					colorLength = 0;
				}
				
				row--;
				col++;
			}
		}

		// check for left diagonal line
		for (int i = 0; i < diagonals; i++) {
			final int top = Math.min(C4Board.GOAL + i - 1, C4Board.HEIGHT - 1);
			final int right =
				Math.min(C4Board.WIDTH - i + 1, C4Board.WIDTH - 1);
			int row = top;
			int col = right;
			int colorLength = 0;
			while (row >= 0 && col < C4Board.WIDTH) {
				if (colorPieces[row][col]) {
					colorLength++;
					if (colorLength > result) {
						result = colorLength;
					}
				} else {
					colorLength = 0;
				}
				
				row++;
				col--;
			}
		}
		
		return result;
	}
}

package connectfourdomain;

/**
 * Represents a game of Connect Four, including
 * the red and black pieces on the board,
 * and whether it is red's turn to play (first player is red).
 */
public final class C4Board {

	/**
	 * Width of board.
	 */
	public static final int WIDTH = 7;
	/**
	 * Height of board.
	 */
	public static final int HEIGHT = 6;
	/**
	 * Number in a row needed to win.
	 */
	public static final int GOAL = 4;
	
	/**
	 * Indicates who won, if anyone.
	 */
	public enum Winner {
			/** Black won. */
			BLACK, 
			/** Red won. */
			RED,
			/** Board is full, but no one won. */
			DRAW,
			/** Board is not full, and no one won yet. */
			NONE
	}

	/**
	 * True if a red piece is there.
	 * Outer array per row, from bottom to top.
	 * Items from left to right.
	 */
	private final boolean[][] redPieces;
	/**
	 * True if a black piece is there.
	 * Outer array per row, from bottom to top.
	 * Items from left to right.
	 */
	private final boolean[][] blackPieces;
	/**
	 * True if it's black's turn.
	 * Red moves first, so initially false.
	 */
	private boolean blackTurn;
	
	/**
	 * Constructor of an empty board, with
	 * first move for red.
	 */
	public C4Board() {
		this.redPieces = new boolean[HEIGHT][WIDTH];
		this.blackPieces = new boolean[HEIGHT][WIDTH];
		this.blackTurn = false;
	}
	
	/**
	 * Copy constructor.
	 * @param toCopy the board to copy
	 */
	public C4Board(final C4Board toCopy) {
		this.redPieces = new boolean[HEIGHT][WIDTH];
		this.blackPieces = new boolean[HEIGHT][WIDTH];
		for (int row = 0; row < HEIGHT; row++) {
			for (int col = 0; col < WIDTH; col++) {
				this.redPieces[row][col] = toCopy.redPieces[row][col];
				this.blackPieces[row][col] = toCopy.blackPieces[row][col];
			}
		}
		this.blackTurn = toCopy.isBlackTurn();
	}
	
	/**
	 * @return format: from left to right in each row, from
	 * bottom to top. First for red, then for black.
	 */
	public float[] getAsFloatArray() {
		final int size = HEIGHT * WIDTH * 2;
		final float[] result = new float[size];
		int i = 0;
		for (int row = 0; row < HEIGHT; row++) {
			for (int col = 0; col < WIDTH; col++) {
				if (this.redPieces[row][col]) {
					result[i] = 1;
				} else {
					result[i] = 0;
				}
				i++;
			}
		}
		for (int row = 0; row < HEIGHT; row++) {
			for (int col = 0; col < WIDTH; col++) {
				if (this.blackPieces[row][col]) {
					result[i] = 1;
				} else {
					result[i] = 0;
				}
				i++;
			}
		}
		return result;
	}
	
	/**
	 * @param col Column in which to move.
	 * @return True if the column is not full.
	 */
	public boolean isLegalMove(final int col) {
		if (col < 0 || col >= WIDTH) {
			return false;
		}
		return !this.redPieces[HEIGHT - 1][col]
			&& !this.blackPieces[HEIGHT - 1][col];
	}
	
	/**
	 * @return true if black should be next to move.
	 * Assumes red plays first.
	 * If true, red should have one more piece on the
	 * board than black.
	 */
	private boolean shouldBeBlackTurn() {
		int blackCount = 0;
		int redCount = 0;
		for (int col = 0; col < WIDTH; col++) {
			for (int row = 0; row < HEIGHT; row++) {
				if (this.redPieces[row][col]) {
					redCount++;
				}
				if (this.blackPieces[row][col]) {
					blackCount++;
				}
			}
		}
		
		assert redCount == blackCount || redCount == blackCount + 1;
		return redCount > blackCount;
	}
	
	/**
	 * Take back the most recent move in column col.
	 * @param col the column where the most recent piece
	 * was added.
	 * This action also switches whose turn it is to
	 * play, in this.blackTurn.
	 */
	public void undoMove(final int col) {
		assert col >= 0 && col < WIDTH;
		for (int row = HEIGHT - 1; row >= 0; row--) {
			if (this.redPieces[row][col]) {
				this.redPieces[row][col] = false;
				break;
			}
			if (this.blackPieces[row][col]) {
				this.blackPieces[row][col] = false;
				break;
			}
		}
		this.blackTurn = !this.blackTurn;
		assert shouldBeBlackTurn() == this.blackTurn;
	}
	
	/**
	 * Adds a token in column col and swaps whose
	 * turn it is in this.blackTurn.
	 * The color of the piece added is black is this.blackTurn.
	 * @param col the column to play in.
	 */
	public void makeMove(final int col) {
		if (!isLegalMove(col)) {
			throw new IllegalArgumentException();
		}
		if (this.blackTurn) {
			assert shouldBeBlackTurn();
		} else {
			assert !shouldBeBlackTurn();
		}
		for (int row = 0; row < HEIGHT; row++) {
			if (!this.blackPieces[row][col] && !this.redPieces[row][col]) {
				if (this.blackTurn) {
					this.blackPieces[row][col] = true;
				} else {
					this.redPieces[row][col] = true;
				}
				this.blackTurn = !this.blackTurn;
				return;
			}
		}
		throw new IllegalStateException();
	}	
	
	/**
	 * @return true if it's black's move.
	 */
	public boolean isBlackTurn() {
		return this.blackTurn;
	}

	/**
	 * @return has true for each cell with a red
	 * piece.
	 */
	public boolean[][] getRedPieces() {
		return this.redPieces;
	}

	/**
	 * @return has true for each cell with a black piece.
	 */
	public boolean[][] getBlackPieces() {
		return this.blackPieces;
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		for (int col = 0; col < WIDTH; col++) {
			builder.append(col).append('\t');
		}
		builder.append('\n');
		for (int row = HEIGHT - 1; row >= 0; row--) {
			for (int col = 0; col < WIDTH; col++) {
				if (this.redPieces[row][col]) {
					builder.append('O');
				} else if (this.blackPieces[row][col]) {
					builder.append('X');
				} else {
					builder.append('_');
				}
				builder.append('\t');
			}
			builder.append('\n');
		}
		
		return builder.toString();
	}

	/**
	 * @return who won the game, or NONE if not over yet.
	 */
	public Winner getWinner() {
		// check for vertical line
		for (int col = 0; col < WIDTH; col++) {
			int redLength = 0;
			int blackLength = 0;
			for (int row = 0; row < HEIGHT; row++) {
				if (this.redPieces[row][col]) {
					redLength++;
					if (redLength == GOAL) {
						return Winner.RED;
					}
					blackLength = 0;
				} else if (this.blackPieces[row][col]) {
					redLength = 0;
					blackLength++;
					if (blackLength == GOAL) {
						return Winner.BLACK;
					}
				} else {
					redLength = 0;
					blackLength = 0;
				}
			}
		}
		
		// check for horizontal line
		for (int row = 0; row < HEIGHT; row++) {
			int redLength = 0;
			int blackLength = 0;
			for (int col = 0; col < WIDTH; col++) {
				if (this.redPieces[row][col]) {
					redLength++;
					if (redLength == GOAL) {
						return Winner.RED;
					}
					blackLength = 0;
				} else if (this.blackPieces[row][col]) {
					redLength = 0;
					blackLength++;
					if (blackLength == GOAL) {
						return Winner.BLACK;
					}
				} else {
					redLength = 0;
					blackLength = 0;
				}
			}
		}
		
		// check for right diagonal line
		final int diagonals = 6;
		for (int i = 0; i < diagonals; i++) {
			final int top = Math.min(GOAL + i - 1, HEIGHT - 1);
			final int left = Math.max(i - 2, 0);
			int row = top;
			int col = left;
			int redLength = 0;
			int blackLength = 0;
			while (row >= 0 && col < WIDTH) {
				if (this.redPieces[row][col]) {
					redLength++;
					if (redLength == GOAL) {
						return Winner.RED;
					}
					blackLength = 0;
				} else if (this.blackPieces[row][col]) {
					redLength = 0;
					blackLength++;
					if (blackLength == GOAL) {
						return Winner.BLACK;
					}
				} else {
					redLength = 0;
					blackLength = 0;
				}
				
				row--;
				col++;
			}
		}

		// check for left diagonal line
		for (int i = 0; i < diagonals; i++) {
			final int top = Math.min(GOAL + i - 1, HEIGHT - 1);
			final int right = Math.min(WIDTH - i + 1, WIDTH - 1);
			int row = top;
			int col = right;
			int redLength = 0;
			int blackLength = 0;
			while (row >= 0 && col >= 0) {
				if (this.redPieces[row][col]) {
					redLength++;
					if (redLength == GOAL) {
						return Winner.RED;
					}
					blackLength = 0;
				} else if (this.blackPieces[row][col]) {
					redLength = 0;
					blackLength++;
					if (blackLength == GOAL) {
						return Winner.BLACK;
					}
				} else {
					redLength = 0;
					blackLength = 0;
				}
				
				row--;
				col--;
			}
		}
		
		for (int i = 0; i < WIDTH; i++) {
			if (isLegalMove(i)) {
				return Winner.NONE;
			}
		}
		
		return Winner.DRAW;
	}
}

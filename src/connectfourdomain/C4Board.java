package connectfourdomain;

public final class C4Board {

	public static final int WIDTH = 7;
	public static final int HEIGHT = 6;
	public static final int GOAL = 4;
	
	public enum Winner {
			BLACK,
			RED,
			DRAW,
			NONE
	}
	
	// red moves first
	// each inner array is a row from left to right.
	// outer arrays include rows from bottom to top.
	private final boolean[][] redPieces;
	private final boolean[][] blackPieces;
	private boolean blackTurn;
	
	public C4Board() {
		this.redPieces = new boolean[HEIGHT][WIDTH];
		this.blackPieces = new boolean[HEIGHT][WIDTH];
		this.blackTurn = false;
	}
	
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
	
	public boolean isLegalMove(final int col) {
		assert col >= 0 && col < WIDTH;
		return !this.redPieces[HEIGHT - 1][col]
			&& !this.blackPieces[HEIGHT - 1][col];
	}
	
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
	
	public boolean isBlackTurn() {
		return this.blackTurn;
	}

	public boolean[][] getRedPieces() {
		return this.redPieces;
	}

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
						return Winner.RED;
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
						return Winner.RED;
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
						return Winner.RED;
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
						return Winner.RED;
					}
				} else {
					redLength = 0;
					blackLength = 0;
				}
				
				row++;
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

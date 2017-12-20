package connectfourdomain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import connectfourdomain.C4Board.Winner;

/**
 * A simple AI player for Connect Four.
 * Always plays as red (first player).
 * Uses a depth-limited minimax search, with the
 * heuristic of always moving first in the center column
 * when the game begins.
 */
public final class C4Player {
	
	/**
	 * Maximum allowable search depth, based on
	 * apparent time budget.
	 */
	public static final int MAX_SEARCH_DEPTH = 6;

	/**
	 * Default depth to search.
	 */
	private static final int DEFAULT_SEARCH_DEPTH = 5;
	
	/**
	 * Depth in plies to search.
	 */
	private static int searchDepth = DEFAULT_SEARCH_DEPTH;
	
	/**
	 * Value for winning the game.
	 */
	public static final int MAX_VALUE = 100;
	
	/**
	 * Private constructor of utility class.
	 */
	private C4Player() {
		// not called
	}
	
	/**
	 * @return the current search depth
	 */
	public static int getSearchDepth() {
		return searchDepth;
	}
	
	/**
	 * @param aDepth the new search depth to use. Must be
	 * in {1, . . ., MAX_SEARCH_DEPTH}.
	 */
	public static void setSearchDepth(final int aDepth) {
		if (aDepth < 0 || aDepth > MAX_SEARCH_DEPTH) {
			throw new IllegalArgumentException("" + aDepth);
		}
		searchDepth = aDepth;
	}
	
	/**
	 * @param board current board state.
	 * @return The index of the column to move in.
	 */
	public static int getRedMove(final C4Board board) {
		assert !board.isBlackTurn();
		assert board.getWinner() == Winner.NONE;
		final boolean[][] redPieces = board.getRedPieces();
		final boolean[][] blackPieces = board.getBlackPieces();
		
		// move in center column if empty board
		if (isEmpty(redPieces, blackPieces)) {
			return C4Board.WIDTH / 2;
		}
		
		return minimaxMove(board);
	}
	
	/**
	 * Use minimax search to find a good move.
	 * @param board the current board state.
	 * @return the index of the best column to move in.
	 */
	private static int minimaxMove(final C4Board board) {
		final C4Board boardCopy = new C4Board(board);
		
		int bestMove = -1;
		int maxValue = -1 * MAX_VALUE - 1;
		
		// randomize order columns are considered,
		// so random choice will be made in case of equal values.
		final List<Integer> cols = new ArrayList<Integer>();
		for (int i = 0; i < C4Board.WIDTH; i++) {
			cols.add(i);
		}
		Collections.shuffle(cols);
		
		for (final int col: cols) {
			if (boardCopy.isLegalMove(col)) {
				if (searchDepth == 0) {
					// make first valid move in random order
					return col;
				}
				
				boardCopy.makeMove(col);
				final int curValue =
					minimaxMoveRecurse(boardCopy, searchDepth - 1);
				boardCopy.undoMove(col);
				if (curValue > maxValue) {
					maxValue = curValue;
					bestMove = col;
				}
			}
		}
		
		return bestMove;
	}
	
	/**
	 * Recursively find the minimax-optimal value of this
	 * search subtree, from the perspective of the current
	 * player to move at this point in the search tree.
	 * @param board the board state at this point in the
	 * search tree
	 * @param depthLeft the remaining depth to which search
	 * is allowed in this path of the search tree
	 * @return the best value, from the current player's
	 * perspective in the search tree, based on minimax
	 * search. 
	 */
	private static int minimaxMoveRecurse(
		final C4Board board,
		final int depthLeft
	) {
		final Winner winner = board.getWinner();
		if (winner == Winner.RED) {
			// game is over, red (self) wins
			return MAX_VALUE;
		}
		if (winner == Winner.BLACK) {
			// game is over, red (self) loses
			return -1 * MAX_VALUE;
		}
		if (winner == Winner.DRAW) {
			// game is over, tie
			return 0;
		}
		if (depthLeft == 0) {
			// max search depth reached.
			// return evaluation benefit for red (self)
			return boardValue(board);
		}
		if (board.isBlackTurn()) {
			// minimizing player (opponent)
			int minValue = MAX_VALUE;
			for (int col = 0; col < C4Board.WIDTH; col++) {
				if (board.isLegalMove(col)) {
					board.makeMove(col);
					final int curValue =
						minimaxMoveRecurse(board, depthLeft - 1);
					board.undoMove(col);
					if (curValue < minValue) {
						minValue = curValue;
					}
				}
			}
			return minValue;
		}

		// maximizing player (self)
		
		int maxValue = -1 * MAX_VALUE;
		for (int col = 0; col < C4Board.WIDTH; col++) {
			if (board.isLegalMove(col)) {
				board.makeMove(col);
				final int curValue =
					minimaxMoveRecurse(board, depthLeft - 1);
				board.undoMove(col);
				if (curValue > maxValue) {
					maxValue = curValue;
				}
			}
		}
		return maxValue;
	}
	
	/**
	 * @param redPieces true at red piece locations
	 * @param blackPieces true at black piece locations
	 * @return true if all values are false (no pieces)
	 */
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
	
	/**
	 * @param board the current board state, somewhere in the
	 * minimax search tree
	 * @return the heuristic value of the board to the red player
	 * (self). This will be MAX_VALUE if red won, -1 * MAX_VALUE
	 * if black won, or the difference in length of longest runs
	 * between red and black otherwise.
	 */
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
	
	/**
	 * @param colorPieces true at the locations of a certain color's
	 * pieces (either red or black)
	 * @return the length of the longest run of that color's pieces
	 * in a row, which will be in {0, . . ., C4Board.GOAL}.
	 */
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
			while (row >= 0 && col >= 0) {
				if (colorPieces[row][col]) {
					colorLength++;
					if (colorLength > result) {
						result = colorLength;
					}
				} else {
					colorLength = 0;
				}
				
				row--;
				col--;
			}
		}
		
		return result;
	}
}

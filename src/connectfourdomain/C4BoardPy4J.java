package connectfourdomain;

import java.util.ArrayList;
import java.util.List;

import connectfourdomain.C4Board.Winner;
import py4j.GatewayServer;

/**
 * Wrapper for a game of Connect Four, 
 * to be used by Py4J.
 * 
 * Requirements: Py4J,
 * https://www.py4j.org/install.html
 * https://www.py4j.org/download.html
 */
public final class C4BoardPy4J {

	/**
	 * Inner object that represents the board state.
	 */
	private C4Board innerBoard;

	/**
	 * Public constructor.
	 */
	public C4BoardPy4J() {
		this.innerBoard = new C4Board();
	}
	
	/**
	 * Entry method, used to set up the Py4J server.
	 * @param args not used
	 */
	public static void main(final String[] args) {
		// opponent (RED) will search to depth 1.
		final int searchDepth = 1;
		C4Player.setSearchDepth(searchDepth);
		
		// set up Py4J server
		final GatewayServer gatewayServer =
			new GatewayServer(new C4BoardPy4J());
		gatewayServer.start();
		System.out.println("Gateway Server Started");
	}
	
	/**
	 * Get a new C4BoardPy4J object for Py4J.
	 * @return the C4BoardPy4J for Py4J to use.
	 */
	public static C4BoardPy4J getBoard() {
		return new C4BoardPy4J();
	}
	
	/**
	 * Reset the board (clear all moves except first opponent move),
	 * have opponent (RED) make its first move,
	 * and return the board state.
	 * 
	 * @return the state of the board after clearing and opponent's
	 * (RED's) first move,
	 * as a flat list of 0 and 1.
	 */
	public List<Double> reset() {
		// clear the board state.
		this.innerBoard = new C4Board();
		
		// opponent (RED) will move first.
		final int redMove = C4Player.getRedMove(this.innerBoard);
		this.innerBoard.makeMove(redMove);
		
		return getBoardAsListDouble();
	}
	
	/**
	 * Take a step based on the given action, represented as
	 * a list of integers (with one item only).
	 * 
	 * Return a flat list representing, in order:
	 * the updated board state,
	 * the reward of the step for player taking action (in {-1, 0, 1}),
	 * whether the game is done (in {0, 1}).
	 * 
	 * If the action is illegal, do not update the board state,
	 * but consider the game as lost (i.e., -1) and thus done (i.e., 1).
	 * 
	 * If the action ends the game, the opponent (RED) will not move.
	 * Otherwise, the opponent (RED) will play a move in response before
	 * this method returns.
	 * 
	 * @param action the list representing the action to take.
	 * The action list will have exactly 1 item, a column index to move.
	 * @return the list representing the new game state,
	 * including the board state, reward, and whether the game is over,
	 * as one flat list.
	 */
	public List<Double> step(final List<Integer> action) {
		if (action == null || action.size() != 1) {
			// action list must have one item
			throw new IllegalArgumentException("" + action);
		}
		final int actionCol = action.get(0);
		if (actionCol < 0 || actionCol >= C4Board.WIDTH) {
			// action must be a valid column number.
			throw new IllegalArgumentException("" + actionCol);
		}
		
		final List<Double> result = new ArrayList<Double>();
		if (!this.innerBoard.isLegalMove(actionCol)) {
			// illegal move. game is lost.
			final List<Double> board = getBoardAsListDouble();
			// self player (BLACK) loses for illegal move.
			final double reward = -1.0;
			// game is over.
			final double isOver = 1.0;
			result.addAll(board);
			result.add(reward);
			result.add(isOver);
			return result;
		}
		
		// move is legal. make the move.
		this.innerBoard.makeMove(actionCol);
		
		final Winner blackMoveWinner = this.innerBoard.getWinner();
		if (blackMoveWinner != Winner.NONE) {
			// game is over. opponent (RED) does not move.
			final List<Double> board = getBoardAsListDouble();
			// reward would be 0.0 if game is a DRAW.
			double reward = 0.0;
			if (blackMoveWinner == Winner.BLACK) {
				// BLACK (self) won.
				reward = 1.0;
			}
			double isOver = 1.0;
			result.addAll(board);
			result.add(reward);
			result.add(isOver);
			return result;
		}
		
		// game is ongoing. opponent (RED) will move.
		final int redMove = C4Player.getRedMove(this.innerBoard);
		this.innerBoard.makeMove(redMove);
		
		final Winner redMoveWinner = this.innerBoard.getWinner();
		final List<Double> board = getBoardAsListDouble();
		// reward would be 0.0 if game is a DRAW or NONE (ongoing).
		double reward = 0.0;
		if (redMoveWinner == Winner.RED) {
			// RED won, so self player (BLACK) lost.
			reward = -1.0;
		}
		// game would not be over if winner is still NONE.
		double isOver = 0.0;
		if (redMoveWinner == Winner.RED || redMoveWinner == Winner.DRAW) {
			// RED won or drew the game, so game is over.
			isOver = 1.0;
		}
		result.addAll(board);
		result.add(reward);
		result.add(isOver);
		return result;
	}
	
	
	/**
	 * Get a human-readable board state string.
	 * @return the string representing the human-readable board state.
	 */
	public String render() {
		return this.innerBoard.toString();
	}
	
	/**
	 * @return get the board state as a list of Double
	 */
	private List<Double> getBoardAsListDouble() {
		final float[] boardAsFloats = this.innerBoard.getAsFloatArray();
		final List<Double> result = new ArrayList<Double>();
		for (final float f: boardAsFloats) {
			result.add((double) f);
		}
		return result;
	}
}

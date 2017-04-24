package game;

import graph.Edge;
import graph.Node;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;

import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;

import model.AttackerAction;
import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;
import model.GameSample;
import model.GameState;
import agent.Attacker;
import agent.Defender;

public final class GameSimulation {
	private int numTimeStep;
	private DependencyGraph depGraph;
	
	private Attacker attacker;
	private Defender defender;
	
	private final double discFact;
	
	private RandomDataGenerator rng;
	
	public static final boolean DEBUG_PRINT = false;
	
	//Outcome
	private GameSimulationResult simResult;
	
	public GameSimulation(final DependencyGraph depGraph, final Attacker attacker,
		final Defender defender, final RandomDataGenerator rng,
		final int numTimeStep, final double discFact) {
		if (depGraph == null || attacker == null || defender == null
			|| rng == null || numTimeStep < 1 || discFact <= 0.0 || discFact > 1.0) {
			throw new IllegalArgumentException();
		}
		if (!depGraph.isValid()) {
			throw new IllegalArgumentException();
		}
		this.depGraph = depGraph;
		this.numTimeStep = numTimeStep;
		this.discFact = discFact;
		
		this.attacker = attacker;
		this.defender = defender;
		
		this.rng = rng;
		
		this.simResult = new GameSimulationResult();
	}
	
	public void setRandomSeed(final long seed) {
		this.rng.reSeed(seed);
	}
	
	public void setDefender(final Defender aDefender) {
		assert aDefender != null;
		this.defender = aDefender;
	}
	
	public void setAttacker(final Attacker aAttacker) {
		assert aAttacker != null;
		this.attacker = aAttacker;
	}
	
	private boolean isAllInactive() {
		for (Node node : this.depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				return false;
			}
		}
		return true;
	}
	
	public void runSimulation() {
		if (!isAllInactive()) {
			throw new IllegalStateException();
		}

		// Start simulation
		DefenderObservation dObservation = new DefenderObservation();
		GameState gameState = new GameState();
		gameState.createID();
		DefenderBelief dBelief = new DefenderBelief(); 
		dBelief.addState(gameState, 1.0); // initial belief of the defender
		long start, end;
		final double thousand = 1000.0;
		for (int t = 1; t <= this.numTimeStep; t++) {
			printIfDebug("Time step: " + t);
			start = System.currentTimeMillis();
			printIfDebug("Sample attacker action...");
			AttackerAction attAction = this.attacker.sampleAction(
				this.depGraph, 
				t, 
				this.numTimeStep,
				this.rng.getRandomGenerator()
			);
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			printIfDebug("Sample defender action...");
			start = System.currentTimeMillis();
			DefenderAction defAction = this.defender.sampleAction(
				this.depGraph,
				t,
				this.numTimeStep,
				dBelief,
				this.rng.getRandomGenerator()
			);
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			printIfDebug("Sample game state...");
			start = System.currentTimeMillis();
			gameState = GameOracle.generateStateSample(gameState, attAction, defAction, this.rng); // new game state
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			printIfDebug("Sample observation...");
			start = System.currentTimeMillis();
			// observation based on game state
			dObservation = GameOracle.generateDefObservation(this.depGraph, gameState, this.rng); 
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			printIfDebug("Update defender belief...");
			start = System.currentTimeMillis();
			dBelief = this.defender.updateBelief(this.depGraph, dBelief, defAction,
				dObservation, t, this.numTimeStep, this.rng.getRandomGenerator());
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			//Update states
			printIfDebug("Update game state...");
			start = System.currentTimeMillis();
			this.depGraph.setState(gameState);
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			GameSample gameSample = new GameSample(t, gameState, dObservation, defAction, attAction);
			this.simResult.addGameSample(gameSample);
		}
		if (this.simResult.getGameSampleList().size() != this.numTimeStep) {
			throw new IllegalStateException(
				this.simResult.getGameSampleList().size() + "\t" + this.numTimeStep);
		}
		this.computePayoff();
	}
	
	public void computePayoff() {
		double defPayoff = 0.0;
		double attPayoff = 0.0;
		for (final GameSample gameSample : this.simResult.getGameSampleList()) {
			final int timeStep = gameSample.getTimeStep();
			final GameState gameState = gameSample.getGameState();
			final DefenderAction defAction = gameSample.getDefAction();
			final AttackerAction attAction = gameSample.getAttAction();
			final double discFactPow = Math.pow(this.discFact, timeStep - 1);
			for (final Node node : gameState.getEnabledNodeSet()) {
				defPayoff += discFactPow * node.getDPenalty();
				attPayoff += discFactPow * node.getAReward();
			}
			// omit the final round's action cost, because action has no effect
			if (timeStep <= this.numTimeStep) {
				for (final Node node : defAction.getAction()) {
					defPayoff += discFactPow * node.getDCost();
				}
				for (final Entry<Node, Set<Edge>> entry : attAction.getActionCopy().entrySet()) {
					final Node node = entry.getKey();
					if (node.getActivationType() == NodeActivationType.AND) {
						attPayoff += discFactPow * node.getACost();
					} else {
						final Set<Edge> edgeSet = entry.getValue();
						for (final Edge edge : edgeSet) {
							attPayoff += discFactPow * edge.getACost();
						}
					}
				}
			}
		}
		this.simResult.setAttPayoff(attPayoff);
		this.simResult.setDefPayoff(defPayoff);
	}
	
	public GameSimulationResult getSimulationResult() {
		return this.simResult;
	}

	public static void saveResult() {
		throw new UnsupportedOperationException();
	}
	
	public void printPayoff() {
		this.simResult.printPayoff();
	}
	
	public static void end() {
		throw new UnsupportedOperationException();
	}
	
	public void reset() {
		//Reset node states
		for (Node node : this.depGraph.vertexSet()) {
			node.setState(NodeState.INACTIVE);
		}
		this.simResult.clear();
	}
	
	public static void printIfDebug(final String toPrint) {
		if (DEBUG_PRINT) {
			System.out.println(toPrint);
		}
	}

	@Override
	public String toString() {
		return "GameSimulation [numTimeStep=" + this.numTimeStep + ", attacker="
			+ this.attacker + ", defender=" + this.defender + ", discFact=" + this.discFact
			+ ", rng=" + this.rng + "]";
	}
}

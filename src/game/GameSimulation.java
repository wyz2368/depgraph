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
	// private GameOracle gameOracle;
	
	private double discFact = 1.0;
	
	private RandomDataGenerator rng;
	
	//Outcome
	private GameSimulationResult simResult;
	
	public GameSimulation(final DependencyGraph depGraph, final Attacker attacker,
		final Defender defender, final RandomDataGenerator rng,
		final int numTimeStep, final double discFact) {
		if (depGraph == null || attacker == null || defender == null
			|| rng == null || numTimeStep < 1 || discFact <= 0.0 || discFact > 1.0) {
			throw new IllegalArgumentException();
		}
		this.depGraph = depGraph;
		this.numTimeStep = numTimeStep;
		this.discFact = discFact;
		
		this.attacker = attacker;
		this.defender = defender;
		
		this.rng = rng;
		
		//Outcome
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
	
	public void runSimulation() {
		// Get initial state
		for (Node node : this.depGraph.vertexSet()) {
			if (node.getState() == NodeState.ACTIVE) {
				this.simResult.addEnabledNodetoInitialState(node);
			}
		}
		// Start simulation
		DefenderObservation dObservation = new DefenderObservation();
		GameState gameState = new GameState();
		gameState.createID();
		DefenderBelief dBelief = new DefenderBelief(); 
		dBelief.addState(gameState, 1.0); // initial belief of the defender
		long start, end;
		final double thousand = 1000.0;
		for (int t = 0; t <= this.numTimeStep; t++) {
			System.out.println("Time step: " + t);
			start = System.currentTimeMillis();
			System.out.println("Sample attacker action...");
			AttackerAction attAction = this.attacker.sampleAction(
				this.depGraph, 
				t, 
				this.numTimeStep,
				this.rng.getRandomGenerator()
			);
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / thousand);
			// attAction.print();
			
			System.out.println("Sample defender action...");
			start = System.currentTimeMillis();
			DefenderAction defAction = this.defender.sampleAction(
				this.depGraph, t, this.numTimeStep, dBelief, this.rng.getRandomGenerator());
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / thousand);
			// defAction.print();
			
			System.out.println("Sample game state...");
			start = System.currentTimeMillis();
			gameState = GameOracle.generateStateSample(gameState, attAction, defAction, this.rng); // new game state
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / thousand);
			// gameState.print();
			
			System.out.println("Sample observation...");
			start = System.currentTimeMillis();
			// observation based on game state
			dObservation = GameOracle.generateDefObservation(this.depGraph, gameState, this.rng); 
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / thousand);
			
			System.out.println("Update defender belief...");
			start = System.currentTimeMillis();
			dBelief = this.defender.updateBelief(this.depGraph, dBelief, defAction,
				dObservation, t, this.numTimeStep, this.rng.getRandomGenerator());
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / thousand);
			
			//Update states
			System.out.println("Update game state...");
			start = System.currentTimeMillis();
			this.depGraph.setState(gameState);
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / thousand);
			
			GameSample gameSample = new GameSample(t, gameState, dObservation, defAction, attAction);
			this.simResult.addGameSample(gameSample);
		}
		this.computePayoff();
	}
	
	public void computePayoff() {
		double defPayoff = 0.0;
		double attPayoff = 0.0;
		for (GameSample gameSample : this.simResult.getGameSampleList()) {
			int timeStep = gameSample.getTimeStep();
			GameState gameState = gameSample.getGameState();
			DefenderAction defAction = gameSample.getDefAction();
			AttackerAction attAction = gameSample.getAttAction();
			for (Node node : gameState.getEnabledNodeSet()) {
				defPayoff += Math.pow(this.discFact, timeStep) * node.getDPenalty();
				attPayoff += Math.pow(this.discFact, timeStep) * node.getAReward();
			}
			if (timeStep <= this.numTimeStep) {
				for (Node node : defAction.getAction()) {
					defPayoff += node.getDCost();
				}
				for (Entry<Node, Set<Edge>> entry : attAction.getAction().entrySet()) {
					Node node = entry.getKey();
					if (node.getActivationType() == NodeActivationType.AND) {
						attPayoff += Math.pow(this.discFact, timeStep) * node.getACost();
					} else {
						Set<Edge> edgeSet = entry.getValue();
						for (Edge edge : edgeSet) {
							attPayoff += Math.pow(this.discFact, timeStep) * edge.getACost();
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
}

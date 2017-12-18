package game;

import graph.Edge;
import graph.INode.NodeType;
import graph.Node;
import graph.INode.NodeActivationType;
import graph.INode.NodeState;

import java.util.ArrayList;
import java.util.List;
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
import rl.RLDefenderAction;
import rl.RLDefenderEpisode;
import rl.RLDefenderRawObservation;
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
	
	private final List<RLDefenderEpisode> rlDefenderEpisodes;
	
	public GameSimulation(
		final DependencyGraph aDepGraph, final Attacker aAttacker,
		final Defender aDefender, final RandomDataGenerator aRng,
		final int aNumTimeStep, final double aDiscFact) {
		if (aDepGraph == null || aAttacker == null || aDefender == null
			|| aRng == null || aNumTimeStep < 1
			|| aDiscFact <= 0.0 || aDiscFact > 1.0) {
			throw new IllegalArgumentException();
		}
		if (!aDepGraph.isValid()) {
			throw new IllegalArgumentException();
		}
		this.depGraph = aDepGraph;
		this.numTimeStep = aNumTimeStep;
		this.discFact = aDiscFact;
		
		this.attacker = aAttacker;
		this.defender = aDefender;
		
		this.rng = aRng;
		
		this.simResult = new GameSimulationResult();
		this.rlDefenderEpisodes = new ArrayList<RLDefenderEpisode>();
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
			gameState = GameOracle.generateStateSample(
				gameState, attAction, defAction, this.rng); // new game state
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			printIfDebug("Sample observation...");
			start = System.currentTimeMillis();
			// observation based on game state
			dObservation = GameOracle.generateDefObservation(
				this.depGraph, gameState, this.rng); 
			
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			printIfDebug("Update defender belief...");
			start = System.currentTimeMillis();
			dBelief = this.defender.updateBelief(
				this.depGraph, dBelief, defAction,
				dObservation, t, this.numTimeStep,
				this.rng.getRandomGenerator());
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			//Update states
			printIfDebug("Update game state...");
			start = System.currentTimeMillis();
			this.depGraph.setState(gameState);
			end = System.currentTimeMillis();
			printIfDebug("Elapsed time: " + (end - start) / thousand);
			
			GameSample gameSample = new GameSample(
				t, gameState, dObservation, defAction, attAction);
			this.simResult.addGameSample(gameSample);
		}
		if (this.simResult.getGameSampleList().size() != this.numTimeStep) {
			throw new IllegalStateException(
				this.simResult.getGameSampleList().size()
				+ "\t" + this.numTimeStep);
		}
		this.computePayoff();
		this.computeRLDefenderEpisodes();
		for (final RLDefenderEpisode episode: this.rlDefenderEpisodes) {
			System.out.println(episode);
			System.out.println("\n\n");
		}
	}
	
	private void computeRLDefenderEpisodes() {		
		final List<RLDefenderRawObservation> defRawObs = 
			new ArrayList<RLDefenderRawObservation>();
		final List<RLDefenderAction> defActionList =
			new ArrayList<RLDefenderAction>();
		// payoffs will include this.discFact discounting,
		// but not the additional discount factor for RL.
		final List<Double> payoffs = new ArrayList<Double>();

		for (int time = 1; time <= this.numTimeStep; time++) {
			for (final GameSample gameSample
				: this.simResult.getGameSampleList()) {
				if (gameSample.getTimeStep() == time) {
					final GameState gameState = gameSample.getGameState();
					final DefenderAction defAction =
						gameSample.getDefAction();
					
					double defPayoff = 0.0;
					final double discFactPow =
						Math.pow(this.discFact, time - 1);
					for (final Node node : gameState.getEnabledNodeSet()) {
						if (node.getType() == NodeType.TARGET) {
							defPayoff += discFactPow * node.getDPenalty();
						}
					}
					for (final Node node : defAction.getAction()) {
						defPayoff += discFactPow * node.getDCost();
					}
					
					defRawObs.add(new RLDefenderRawObservation(
						gameSample.getDefObservation()));
					defActionList.add(new RLDefenderAction(defAction));
					payoffs.add(defPayoff);
				}				
			}
		}
		
		final List<List<RLDefenderRawObservation>> rawObsLists =
			new ArrayList<List<RLDefenderRawObservation>>();
		final List<List<RLDefenderAction>> actionLists =
			new ArrayList<List<RLDefenderAction>>();
		final List<Double> rlDiscountedPayoffs = new ArrayList<Double>();

		
		for (int time = 1; time <= this.numTimeStep; time++) {
			final List<RLDefenderRawObservation> curRawObsList =
				new ArrayList<RLDefenderRawObservation>();
			final List<RLDefenderAction> curActionList =
				new ArrayList<RLDefenderAction>();
			for (int i = time - 1;
				i >= 0
					&& curRawObsList.size()
						< RLDefenderEpisode.RL_MEMORY_LENGTH;
				i--) {
				curRawObsList.add(defRawObs.get(i));
				curActionList.add(defActionList.get(i));
			}
			rawObsLists.add(curRawObsList);
			actionLists.add(curActionList);
			
			double discDefPayoff = 0.0;
			double discFactor = 1.0;
			for (int i = time - 1; i < this.numTimeStep; i++) {
				discDefPayoff += payoffs.get(i) * discFactor;
				discFactor *= RLDefenderEpisode.RL_DISCOUNT_FACTOR;
			}
			rlDiscountedPayoffs.add(discDefPayoff);
		}
		
		final List<RLDefenderEpisode> result =
			new ArrayList<RLDefenderEpisode>();
		for (int i = 0; i < defRawObs.size(); i++) {
			final int timeStepsLeft = this.numTimeStep - i;
			result.add(new RLDefenderEpisode(
				rawObsLists.get(i), 
				actionLists.get(i), 
				rlDiscountedPayoffs.get(i),
				timeStepsLeft,
				this.depGraph.vertexSet().size()
			));
		}
		
		this.rlDefenderEpisodes.clear();
		this.rlDefenderEpisodes.addAll(result);
	}
	
	public void computePayoff() {
		double defPayoff = 0.0;
		double attPayoff = 0.0;

		// for each state in the game's history
		for (final GameSample gameSample : this.simResult.getGameSampleList()) {
			final int timeStep = gameSample.getTimeStep();
			final GameState gameState = gameSample.getGameState();
			final DefenderAction defAction = gameSample.getDefAction();
			final AttackerAction attAction = gameSample.getAttAction();
			// this.discFact is applied to all time steps exponentially
			// (first time step has no discounting)
			final double discFactPow = Math.pow(this.discFact, timeStep - 1);
			for (final Node node : gameState.getEnabledNodeSet()) {
				if (node.getType() == NodeType.TARGET) {
					defPayoff += discFactPow * node.getDPenalty();
					attPayoff += discFactPow * node.getAReward();
				}
			}
			for (final Node node : defAction.getAction()) {
				defPayoff += discFactPow * node.getDCost();
			}
			for (final Entry<Node, Set<Edge>> entry
				: attAction.getActionCopy().entrySet()) {
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
		this.simResult.setAttPayoff(attPayoff);
		this.simResult.setDefPayoff(defPayoff);
	}
	
	public List<RLDefenderEpisode> getRLDefenderEpisodes() {
		return this.rlDefenderEpisodes;
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
			+ this.attacker + ", defender="
			+ this.defender + ", discFact=" + this.discFact
			+ ", rng=" + this.rng + "]";
	}
}

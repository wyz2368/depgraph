package game;

import graph.Edge;
import graph.Node;
import graph.INode.NODE_ACTIVATION_TYPE;
import graph.INode.NODE_STATE;

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

public class GameSimulation {
	int numTimeStep;
	DependencyGraph depGraph;
	
	Attacker attacker;
	Defender defender;
//	GameOracle gameOracle;
	
	double discFact = 1.0;
	
	RandomDataGenerator rng;
	
	//Outcome
	GameSimulationResult simResult;
	
	public GameSimulation(DependencyGraph depGraph, Attacker attacker, Defender defender, RandomDataGenerator rng,
			int numTimeStep, double discFact)
	{
		this.depGraph = depGraph;
		this.numTimeStep = numTimeStep;
		this.discFact = discFact;
		
		this.attacker = attacker;
		this.defender = defender;
		
		this.rng = rng;
		
		//Outcome
		this.simResult = new GameSimulationResult();
	}
	public void setRandomSeed(long seed)
	{
		rng.reSeed(seed);
	}
	public void setDefender(Defender defender)
	{
		this.defender = defender;
	}
	public void setAttacker(Attacker attacker)
	{
		this.attacker = attacker;
	}
	
	public void runSimulation()
	{
		// Get initial state
		for(Node node : this.depGraph.vertexSet())
		{
			if(node.getState() == NODE_STATE.ACTIVE)
				this.simResult.addEnabledNodetoInitialState(node);
		}
		// Start simulation
		DefenderObservation dObservation = new DefenderObservation();
		GameState gameState = new GameState();
		gameState.createID();
		DefenderBelief dBelief = new DefenderBelief(); 
		dBelief.addState(gameState, 1.0); // initial belief of the defender
		long start, end;
		for(int t = 0; t <= this.numTimeStep; t++)
		{
			System.out.println("Time step: " + t);
			start = System.currentTimeMillis();
			System.out.println("Sample attacker action...");
			AttackerAction attAction = this.attacker.sampleAction(this.depGraph, t, this.numTimeStep, this.rng.getRandomGenerator());
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / 1000.0);
//			attAction.print();
			
			System.out.println("Sample defender action...");
			start = System.currentTimeMillis();
			DefenderAction defAction = this.defender.sampleAction(this.depGraph, t, this.numTimeStep, dBelief, this.rng.getRandomGenerator());
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / 1000.0);
//			defAction.print();
			
			System.out.println("Sample game state...");
			start = System.currentTimeMillis();
			gameState = GameOracle.generateStateSample(gameState, attAction, defAction, rng); // new game state
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / 1000.0);
//			gameState.print();
			
			System.out.println("Sample observation...");
			start = System.currentTimeMillis();
			dObservation = GameOracle.generateDefObservation(this.depGraph, gameState, rng); // observation based on game state
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / 1000.0);
			
			System.out.println("Update defender belief...");
			start = System.currentTimeMillis();
			dBelief = this.defender.updateBelief(this.depGraph, dBelief, defAction, dObservation, t, this.numTimeStep, rng.getRandomGenerator());
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / 1000.0);
			
			//Update states
			System.out.println("Update game state...");
			start = System.currentTimeMillis();
			this.depGraph.setState(gameState);
			end = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (end - start) / 1000.0);
			
			GameSample gameSample = new GameSample(t, gameState, dObservation, defAction, attAction);
			this.simResult.addGameSample(gameSample);
		}
		this.computePayoff();
	}
	public void computePayoff()
	{
		double defPayoff = 0.0;
		double attPayoff = 0.0;
		for(GameSample gameSample : this.simResult.getGameSampleList())
		{
			int timeStep = gameSample.getTimeStep();
			GameState gameState = gameSample.getGameState();
			DefenderAction defAction = gameSample.getDefAction();
			AttackerAction attAction = gameSample.getAttAction();
			for(Node node : gameState.getEnabledNodeSet())
			{
				defPayoff += Math.pow(this.discFact, timeStep) * node.getDPenalty();
				attPayoff += Math.pow(this.discFact, timeStep) * node.getAReward();
			}
			if(timeStep <= this.numTimeStep)
			{
				for(Node node : defAction.getAction())
					defPayoff += node.getDCost();
				for(Entry<Node, Set<Edge>> entry : attAction.getAction().entrySet())
				{
					Node node = entry.getKey();
					if(node.getActivationType() == NODE_ACTIVATION_TYPE.AND)
						attPayoff += Math.pow(this.discFact, timeStep) * node.getACost();
					else
					{
						Set<Edge> edgeSet = entry.getValue();
						for(Edge edge : edgeSet)
						{
							attPayoff += Math.pow(this.discFact, timeStep) * edge.getACost();
						}
					}
				}
			}
			
		}
		this.simResult.setAttPayoff(attPayoff);
		this.simResult.setDefPayoff(defPayoff);
	}
	public GameSimulationResult getSimulationResult()
	{
		return this.simResult;
	}
	public void saveResult()
	{
		
	}
	public void printPayoff()
	{
		this.simResult.printPayoff();
	}
	public void end()
	{
		
	}
	public void reset()
	{
		//Reset node states
		for(Node node : this.depGraph.vertexSet())
			node.setState(NODE_STATE.INACTIVE);
		this.simResult.clear();
	}
}

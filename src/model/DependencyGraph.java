package model;

import graph.Edge;
import graph.INode.NODE_STATE;
import graph.Node;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

// There is a dummy node connecting to entry nodes.
public class DependencyGraph extends DirectedAcyclicGraph<Node, Edge>{
	private static final long serialVersionUID = 1L; // I dont know what this is for :)))
	private Set<Node> targetSet;
	private Set<Node> minCut;
	
	public DependencyGraph(){
		super(Edge.class);
		this.targetSet = new HashSet<Node>();
		this.minCut = new HashSet<Node>();
	}
	
	public boolean addTarget(Node node)
	{
		return this.targetSet.add(node);
	}
	
	public Set<Node> getTargetSet()
	{
		return this.targetSet;
	}
	public Set<Node> getMinCut()
	{
		return this.minCut;
	}
	public void setMinCut(Set<Node> minCut)
	{
		this.minCut = minCut;
	}
	public void addMinCut(Node node)
	{
		this.minCut.add(node);
	}
	public void resetState()
	{
		for(Node node : this.vertexSet())
		{
			node.setState(NODE_STATE.INACTIVE);
		}
	}
	public void setState(GameState gameState)
	{
		this.resetState();
		for(Node node : gameState.getEnabledNodeSet())
			node.setState(NODE_STATE.ACTIVE);
	}
	public GameState getGameState()
	{
		GameState gameState = new GameState();
		for(Node node : this.vertexSet())
			if(node.getState() == NODE_STATE.ACTIVE)
				gameState.addEnabledNode(node);
		return gameState;
	}
	
	public void print()
	{
		for(Node node : this.vertexSet())
			node.print();
		for(Edge edge: this.edgeSet())
			edge.print();
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Target set: ");
		for(Node target : this.targetSet)
		{
			System.out.print(target.getId() + "\t");
		}
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Mincut set: ");
		for(Node node : this.minCut)
		{
			System.out.print(node.getId() + "\t");
		}
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
	}
	public void clear(){
		targetSet.clear();
	}
}

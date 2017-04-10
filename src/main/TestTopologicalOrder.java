package main;

import graph.DagGenerator;
import graph.Edge;
import graph.Node;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class TestTopologicalOrder {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int numNode = 10;
		int numEdge = 20;
		
		RandomDataGenerator rng = new RandomDataGenerator();
		rng.reSeed(System.currentTimeMillis());
		DependencyGraph depGraph = DagGenerator.genRandomDAG(numNode, numEdge, rng);
		depGraph.print();
		TopologicalOrderIterator<Node, Edge> topoOrderIter = new TopologicalOrderIterator<Node, Edge>(depGraph);
		System.out.println("Topological order: ");
		while(topoOrderIter.hasNext())
		{
			Node node = (Node) topoOrderIter.next();
			System.out.println(node.getId());
		}
	}
}

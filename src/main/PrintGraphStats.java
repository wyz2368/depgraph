package main;

import java.io.File;

import graph.Node;
import graph.Edge;
import graph.INode.NodeActivationType;
import model.DependencyGraph;
import utils.DGraphUtils;

public class PrintGraphStats {

	public static void main(final String[] args) {
		if (args == null || args.length != 1) {
			throw new IllegalStateException(
				"Need 1 argument: graphName"
			);
		}
		// RandomGraph30N100E2T1.json
		// SepLayerGraph0.json
		
		final String graphName = args[0];
		final DependencyGraph depGraph = getGraph(graphName);
		final int andNodeCount = countAndNodes(depGraph);
		final int edgeToOrNodeCount = countEdgesToOrNodes(depGraph);
		System.out.println("Graph name: " + graphName);
		System.out.println("AND nodes: " + andNodeCount);
		System.out.println("Edges to OR nodes: " + edgeToOrNodeCount);
	}
	
	private static int countEdgesToOrNodes(final DependencyGraph depGraph) {
		int result = 0;
		for (final Edge edge: depGraph.edgeSet()) {
			if (edge.gettarget().getActivationType() == NodeActivationType.OR) {
				result++;
			}
		}
		return result;
	}

	
	private static int countAndNodes(final DependencyGraph depGraph) {
		int result = 0;
		for (final Node node: depGraph.vertexSet()) {
			if (node.getActivationType() == NodeActivationType.AND) {
				result++;
			}
		}
		return result;
	}
	
	private static DependencyGraph getGraph(final String graphName) {
		final String filePathName = "graphs" + File.separator + graphName;
		return DGraphUtils.loadGraph(filePathName);
	}
}

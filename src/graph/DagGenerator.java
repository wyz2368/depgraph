package graph;
import java.util.ArrayList;
import java.util.List;

import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
// import org.apache.commons.math3.random.RandomGenerator;

/******************************************************************************
 *  Compilation:  javac DigraphGenerator.java
 *  Execution:	java DigraphGenerator V E
 *  Dependencies: Digraph.java
 *
 *  A digraph generator.
 *  
 ******************************************************************************/

/**
 *  The {@code DigraphGenerator} class provides static methods for creating
 *  various digraphs, including Erdos-Renyi random digraphs, random DAGs,
 *  random rooted trees, random rooted DAGs, random tournaments, path digraphs,
 *  cycle digraphs, and the complete digraph.
 *  <p>
 *  For additional documentation, see <a href="http://algs4.cs.princeton.edu/42digraph">Section 4.2</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public final class DagGenerator {

	// this class cannot be instantiated
	private DagGenerator() { }
	
	/**
	 * Returns a random simple DAG containing {@code numNode} vertices and {@code numEdge} edges.
	 * Note: it is not uniformly selected at random among all such DAGs.
	 * @param numNode the number of vertices
	 * @param numEdge the number of edges
	 * @param rand a random number generator
	 * @return a random simple DAG on {@code numNode} vertices, containing a total
	 *	 of {@code numEdge} edges
	 * @throws IllegalArgumentException if no such simple DAG exists
	 */
	public static DependencyGraph genRandomDAG(
		final int numNode,
		final int numEdge,
		final RandomDataGenerator rand
	) {
		if (numNode < 1 || rand == null) {
			throw new IllegalArgumentException();
		}
		if (numEdge > (long) numNode * (numNode - 1) / 2) {
			throw new IllegalArgumentException("Too many edges");
		}
		if (numEdge < 0) {
			throw new IllegalArgumentException("Too few edges");
		}
		DependencyGraph dag = new DependencyGraph();
	
		boolean[][] isExist = new boolean[numNode][numNode];
		for (int i = 0; i < numNode; i++) {
			for (int j = 0; j < numNode; j++) {
				isExist[i][j] = false;
			}
		}
		for (int i = 0; i < numNode; i++) {
			dag.addVertex(new Node());
		}
		List<Node> nodeList = new ArrayList<Node>(dag.vertexSet());
		while (dag.edgeSet().size() < numEdge) {
			int srcNodeIdx = rand.nextInt(0, numNode - 1);
			int desNodeIdx = rand.nextInt(0, numNode - 1);
			if (srcNodeIdx != desNodeIdx) {
				if ((srcNodeIdx < desNodeIdx) && !isExist[srcNodeIdx][desNodeIdx]) {
					dag.addEdge(nodeList.get(srcNodeIdx), nodeList.get(desNodeIdx));
					isExist[srcNodeIdx][desNodeIdx] = true;
				}
			}
		}
		nodeList.clear();
		return dag;
	}
	
	public static DependencyGraph genRandomSepLayDAG(
			final int numLayer,
			final int numNode1Layer, // first layer, assuming largest number of nodes
			final double numNodeRatio, // decreased number of nodes in deeper layers
			final double numEdgeRatio, // number of edges between consecutive layers, with ratio w.r.t. #nodes of these layers
			final RandomDataGenerator rand
		) {
		if(numLayer < 1) {
			throw new IllegalArgumentException("Invalid numLayer");
		}
		if (numNode1Layer < 1 || rand == null) {
			throw new IllegalArgumentException();
		}
		if(numNodeRatio < 1.0) {
			throw new IllegalArgumentException("Invalid numNodeRatio");
		}
		if (numEdgeRatio > 1.0 || numEdgeRatio < 0.0) {
			throw new IllegalArgumentException("Invalid numEdgeRatio");
		}
		DependencyGraph depGraph = new DependencyGraph();
			
		Node[][] nodeList = new Node[numLayer][];
		for(int i = 0; i < numLayer; i++) {
			int curNumNode = (int) (numNode1Layer * Math.pow(numNodeRatio, i));
			nodeList[i] = new Node[curNumNode];
			for(int j = 0; j < curNumNode; j++) {
				Node node = new Node();
				depGraph.addVertex(node);
				nodeList[i][j] = node;
			}
		}
		for(int i = 1; i < numLayer; i++) {
			int prevNumNode = (int) (numNode1Layer * Math.pow(numNodeRatio, i - 1));
			int nextNumNode = (int) (numNode1Layer * Math.pow(numNodeRatio, i));
			int numEdge = (int) (prevNumNode * nextNumNode * numEdgeRatio);
			
			// Start randomly generating
			boolean[][] isExist = new boolean[prevNumNode][nextNumNode];
			for(int prevIdx = 0; prevIdx < prevNumNode; prevIdx++) {
				for(int nextIdx = 0; nextIdx < nextNumNode; nextIdx++) {
					isExist[prevIdx][nextIdx] = false;
				}
			}
			int count = 0;
			while(count < numEdge) {
				int prevIdx = rand.nextInt(0, prevNumNode - 1);
				int nextIdx = rand.nextInt(0, nextNumNode - 1);
				if (!isExist[prevIdx][nextIdx]) {
					isExist[prevIdx][nextIdx] = true;
					count++;
					Node prevNode = nodeList[i - 1][prevIdx];
					Node nextNode = nodeList[i][nextIdx];
					try {
						depGraph.addDagEdge(prevNode, nextNode);
					} catch (CycleFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return depGraph;
	}

	// tournament
	/**
	 * Returns a random tournament digraph on {@code numNode} vertices. A tournament digraph
	 * is a DAG in which for every two vertices, there is one directed edge.
	 * A tournament is an oriented complete graph.
	 * @param numNode the number of vertices
	 * @param rand a random number generator
	 * @return a random tournament digraph on {@code numNode} vertices
	 */
	/*
	private static DependencyGraph genTournamentDAG(
		final int numNode,
		final RandomDataGenerator rand
	) {
		if (numNode < 1 || rand == null) {
			throw new IllegalArgumentException();
		}
		DependencyGraph dag = new DependencyGraph();
		for (int i = 0; i < numNode; i++) {
			dag.addVertex(new Node());
		}
		List<Node> nodeList = new ArrayList<Node>(dag.vertexSet());
		final double half = 0.5;
		for (int v = 0; v < numNode; v++) {
			for (int w = v + 1; w < numNode; w++) {
				double pivot = rand.nextUniform(0, 1, true);
				if (pivot <= half) {
					dag.addEdge(nodeList.get(v), nodeList.get(w));
				} else {
					dag.addEdge(nodeList.get(w), nodeList.get(v));
				}
			}
		}
		nodeList.clear();
		return dag;
	}
	*/

	/**
	 * Returns a random rooted-in DAG on {@code numNode} vertices and {@code numEdge} edges.
	 * A rooted in-tree is a DAG in which there is a single vertex
	 * reachable from every other vertex.
	 * The DAG returned is not chosen uniformly at random among all such DAGs.
	 * @param numNode the number of vertices
	 * @param numEdge the number of edges
	 * @param rand random number generator
	 * @return a random rooted-in DAG on {@code numNode} vertices and {@code numEdge} edges
	 */
	/*
	private static DependencyGraph genRootedInDAG(
		final int numNode,
		final int numEdge,
		final RandomDataGenerator rand
	) {
		if (numNode < 1 || numEdge < 0 || rand == null) {
			throw new IllegalArgumentException();
		}
		if (numEdge > (long) numNode * (numNode - 1) / 2) {
			throw new IllegalArgumentException("Too many edges");
		}
		if (numEdge < 0) {
			throw new IllegalArgumentException("Too few edges");
		}
		DependencyGraph dag = new DependencyGraph();
	
		boolean[][] isExist = new boolean[numNode][numNode];
		for (int i = 0; i < numNode; i++) {
			for (int j = 0; j < numNode; j++) {
				isExist[i][j] = false;
			}
		}
		for (int i = 0; i < numNode; i++) {
			dag.addVertex(new Node());
		}
		List<Node> nodeList = new ArrayList<Node>(dag.vertexSet());

		// one edge pointing from each vertex, other than the root = vertices[V-1]
		for (int v = 0; v < numNode - 1; v++) {
			int w = rand.nextInt(v + 1, numNode - 1);
			dag.addEdge(nodeList.get(v), nodeList.get(w));
			isExist[v][w] = true;
		}
		while (dag.edgeSet().size() < numEdge) {
			int v = rand.nextInt(0, numNode - 1);
			int w = rand.nextInt(0, numNode - 1);
			if ((v < w) && !isExist[v][w]) {
				dag.addEdge(nodeList.get(v), nodeList.get(w));
			}
		}
		nodeList.clear();
		return dag;
	}
	*/

	/**
	 * Returns a random rooted-out DAG on {@code numNode} vertices and {@code numEdge} edges.
	 * A rooted out-tree is a DAG in which every vertex is reachable from a
	 * single vertex.
	 * The DAG returned is not chosen uniformly at random among all such DAGs.
	 * @param numNode the number of vertices
	 * @param numEdge the number of edges
	 * @param rand a random number generator
	 * @return a random rooted-out DAG on {@code numNode} vertices and {@code numEdge} edges
	 */
	/*
	private static DependencyGraph genRootedOutDAG(
		final int numNode,
		final int numEdge,
		final RandomDataGenerator rand
	) {
		if (numNode < 1 || numEdge < 0 || rand == null) {
			throw new IllegalArgumentException();
		}
		if (numEdge > (long) numNode * (numNode - 1) / 2) {
			throw new IllegalArgumentException("Too many edges");
		}
		if (numEdge < 0) {
			throw new IllegalArgumentException("Too few edges");
		}
		DependencyGraph dag = new DependencyGraph();
	
		boolean[][] isExist = new boolean[numNode][numNode];
		for (int i = 0; i < numNode; i++) {
			for (int j = 0; j < numNode; j++) {
				isExist[i][j] = false;
			}
		}
		for (int i = 0; i < numNode; i++) {
			dag.addVertex(new Node());
		}
		List<Node> nodeList = new ArrayList<Node>(dag.vertexSet());

		// one edge pointing from each vertex, other than the root = vertices[V-1]
		for (int v = 0; v < numNode - 1; v++) {
			int w = rand.nextInt(v + 1, numNode - 1);
			dag.addEdge(nodeList.get(w), nodeList.get(v));
			isExist[w][v] = true;
		}

		while (dag.edgeSet().size() < numEdge) {
			int v = rand.nextInt(0, numNode - 1);
			int w = rand.nextInt(0, numNode - 1);
			if ((v < w) && !isExist[w][v]) {
				dag.addEdge(nodeList.get(w), nodeList.get(v));
			}
		}
		return dag;
	}
	*/

	/**
	 * Returns a random rooted-in tree on {@code numNode} vertices.
	 * A rooted in-tree is an oriented tree in which there is a single vertex
	 * reachable from every other vertex.
	 * The tree returned is not chosen uniformly at random among all such trees.
	 * @param numNode the number of vertices
	 * @param rand a random number generator
	 * @return a random rooted-in tree on {@code numNode} vertices
	 */
	/*
	private static DependencyGraph rootedInTree(final int numNode, final RandomDataGenerator rand) {
		return genRootedInDAG(numNode, numNode - 1, rand);
	}
	*/

	/**
	 * Returns a random rooted-out tree on {@code numNode} vertices. A rooted out-tree
	 * is an oriented tree in which each vertex is reachable from a single vertex.
	 * It is also known as a <em>arborescence</em> or <em>branching</em>.
	 * The tree returned is not chosen uniformly at random among all such trees.
	 * @param numNode the number of vertices
	 * @param rand a random number generator
	 * @return a random rooted-out tree on {@code numNode} vertices
	 */
	/*
	private static DependencyGraph rootedOutTree(final int numNode, final RandomDataGenerator rand) {
		return genRootedOutDAG(numNode, numNode - 1, rand);
	}
	*/

	/**
	 * Returns a complete binary tree digraph on {@code numNode} vertices.
	 * @param numNode the number of vertices in the binary tree
	 * @return a digraph that is a complete binary tree on {@code numNode} vertices
	 */
	/*
	private static DependencyGraph binaryTree(final int numNode) {
		if (numNode < 1) {
			throw new IllegalArgumentException();
		}
		DependencyGraph dag = new DependencyGraph();
		for (int i = 0; i < numNode; i++) {
			dag.addVertex(new Node());
		}
		List<Node> nodeList = new ArrayList<Node>(dag.vertexSet());
		for (int i = 0; i < numNode; i++) {
			dag.addEdge(nodeList.get(i), nodeList.get((i - 1) / 2));
		}
		return dag;
	}
	*/
	
	/*
	private static DependencyGraph genLayerDAG(final int numEdgeLB, final int numEdgeUB,
		final int numNodeperLayerLB, final int numNodeperLayerUB
		, final int numLayer, final RandomGenerator rng) {
		if (numEdgeLB < 0 || numEdgeLB > numEdgeUB || numNodeperLayerLB < 1 || numNodeperLayerUB < numNodeperLayerLB) {
			throw new IllegalArgumentException();
		}
		if (numLayer < 1 || rng == null) {
			throw new IllegalArgumentException();
		}
		DependencyGraph dag = new DependencyGraph();
		RandomDataGenerator rand = new RandomDataGenerator(rng);
		List<Node> preLayerNodeList = null;
		for (int i = 0; i < numLayer; i++) {
			List<Node> curLayerNodeList = new ArrayList<Node>();
			int numNode = rand.nextInt(numNodeperLayerLB, numNodeperLayerUB);
			for (int j = 0; j < numNode; j++) {
				Node node = new Node();
				dag.addVertex(node);
				curLayerNodeList.add(node);
			}
			if (i > 0) { // Now add edges
				int numEdge = rand.nextInt(numEdgeLB, numEdgeUB);
				if (preLayerNodeList == null) {
					throw new IllegalStateException();
				}
				numEdge = Math.min(numEdge, curLayerNodeList.size() * preLayerNodeList.size());
				int curNumEdge = 0;
				boolean[][] isAdded = new boolean[preLayerNodeList.size()][curLayerNodeList.size()];
				for (int j = 0; j < preLayerNodeList.size(); j++) {
					for (int k = 0; k < curLayerNodeList.size(); k++) {
						isAdded[j][k] = false;
					}
				}
				while (curNumEdge < numEdge) {
					int startIdx = rand.nextInt(0, preLayerNodeList.size() - 1);
					int endIdx = rand.nextInt(0, curLayerNodeList.size() - 1);
					if (!isAdded[startIdx][endIdx]) {
						Node startNode = preLayerNodeList.get(startIdx);
						Node endNode = curLayerNodeList.get(endIdx);
						dag.addEdge(startNode, endNode);
						isAdded[startIdx][endIdx] = true;
						curNumEdge++;
					}
					
				}
			}
			if (preLayerNodeList != null) {
				preLayerNodeList.clear();
			}
			preLayerNodeList = curLayerNodeList;
		}
		if (preLayerNodeList == null) {
			throw new IllegalStateException();
		}
		preLayerNodeList.clear();
		return dag;
	}
	*/
}

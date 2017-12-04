package graph;
import java.util.ArrayList;
import java.util.List;

import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;

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
 *  For additional documentation, see 
 *  <a href="http://algs4.cs.princeton.edu/42digraph">Section 4.2</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public final class DagGenerator {

	// this class cannot be instantiated
	private DagGenerator() { }
	
	/**
	 * Returns a random simple DAG containing {@code numNode} 
	 * vertices and {@code numEdge} edges.
	 * Note: it is not uniformly selected at random among all such DAGs.
	 * @param numNode the number of vertices
	 * @param numEdge the number of edges
	 * @param rand a random number generator
	 * @return a random simple DAG on {@code numNode} vertices, 
	 * containing a total
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
				if ((srcNodeIdx < desNodeIdx)
					&& !isExist[srcNodeIdx][desNodeIdx]) {
					dag.addEdge(nodeList.get(srcNodeIdx),
						nodeList.get(desNodeIdx));
					isExist[srcNodeIdx][desNodeIdx] = true;
				}
			}
		}
		nodeList.clear();
		return dag;
	}
	
	public static DependencyGraph genRandomSepLayDAG(
		final int numLayer,
		// first layer, assuming largest number of nodes
		final int numNode1Layer,
		final double numNodeRatio, // decreased number of nodes in deeper layers
		// number of edges between consecutive layers,
		// with ratio w.r.t. #nodes of these layers
		final double numEdgeRatio,
		final RandomDataGenerator rand
		) {
		if (numLayer < 1) {
			throw new IllegalArgumentException("Invalid numLayer");
		}
		if (numNode1Layer < 1 || rand == null) {
			throw new IllegalArgumentException();
		}
		if (numNodeRatio > 1.0) {
			throw new IllegalArgumentException("Invalid numNodeRatio");
		}
		if (numEdgeRatio > 1.0 || numEdgeRatio < 0.0) {
			throw new IllegalArgumentException("Invalid numEdgeRatio");
		}
		DependencyGraph depGraph = new DependencyGraph();
			
		Node[][] nodeList = new Node[numLayer][];
		for (int i = 0; i < numLayer; i++) {
			int curNumNode = (int) (numNode1Layer * Math.pow(numNodeRatio, i));
			nodeList[i] = new Node[curNumNode];
			for (int j = 0; j < curNumNode; j++) {
				Node node = new Node();
				depGraph.addVertex(node);
				nodeList[i][j] = node;
			}
		}
		for (int i = 1; i < numLayer; i++) {
			int prevNumNode =
				(int) (numNode1Layer * Math.pow(numNodeRatio, i - 1));
			int nextNumNode = (int) (numNode1Layer * Math.pow(numNodeRatio, i));
			int numEdge = (int) (prevNumNode * nextNumNode * numEdgeRatio);
			
			// Start randomly generating
			boolean[][] isExist = new boolean[prevNumNode][nextNumNode];
			for (int prevIdx = 0; prevIdx < prevNumNode; prevIdx++) {
				for (int nextIdx = 0; nextIdx < nextNumNode; nextIdx++) {
					isExist[prevIdx][nextIdx] = false;
				}
			}
			int count = 0;
			while (count < numEdge) {
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
}

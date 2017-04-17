package graph;
import java.util.ArrayList;
import java.util.List;

import model.DependencyGraph;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/******************************************************************************
 *  Compilation:  javac DigraphGenerator.java
 *  Execution:    java DigraphGenerator V E
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
public class DagGenerator {

    // this class cannot be instantiated
    private DagGenerator() { }
    /**
     * Returns a random simple DAG containing {@code V} vertices and {@code E} edges.
     * Note: it is not uniformly selected at random among all such DAGs.
     * @param V the number of vertices
     * @param E the number of vertices
     * @return a random simple DAG on {@code V} vertices, containing a total
     *     of {@code E} edges
     * @throws IllegalArgumentException if no such simple DAG exists
     */
    public static DependencyGraph genRandomDAG(int numNode, int numEdge, RandomDataGenerator rand) {
        if (numEdge > (long) numNode * (numNode - 1) / 2) throw new IllegalArgumentException("Too many edges");
        if (numEdge < 0)                  throw new IllegalArgumentException("Too few edges");
        DependencyGraph dag = new DependencyGraph();
    
        boolean[][] isExist = new boolean[numNode][numNode];
        for(int i = 0; i < numNode; i++)
        	for(int j = 0; j < numNode; j++)
        		isExist[i][j] = false;
        for (int i = 0; i < numNode; i++)
            dag.addVertex(new Node());
        List<Node> nodeList = new ArrayList<Node>(dag.vertexSet());
        while (dag.edgeSet().size() < numEdge) {
        	int srcNodeIdx = rand.nextInt(0, numNode - 1);
            int desNodeIdx = rand.nextInt(0, numNode - 1);
            if(srcNodeIdx != desNodeIdx)
            {
	            if ((srcNodeIdx < desNodeIdx) && !isExist[srcNodeIdx][desNodeIdx]) {
	                dag.addEdge(nodeList.get(srcNodeIdx), nodeList.get(desNodeIdx));
	                isExist[srcNodeIdx][desNodeIdx] = true;
	            }
            }
        }
        nodeList.clear();
        return dag;
    }

    // tournament
    /**
     * Returns a random tournament digraph on {@code V} vertices. A tournament digraph
     * is a DAG in which for every two vertices, there is one directed edge.
     * A tournament is an oriented complete graph.
     * @param V the number of vertices
     * @return a random tournament digraph on {@code V} vertices
     */
    public static DependencyGraph genTournamentDAG(int numNode, RandomDataGenerator rand) {
    	DependencyGraph dag = new DependencyGraph();
        for (int i = 0; i < numNode; i++)
            dag.addVertex(new Node());
        List<Node> nodeList = new ArrayList<Node>(dag.vertexSet());
        for (int v = 0; v < numNode; v++) {
            for (int w = v + 1; w < numNode; w++) {
            	double pivot = rand.nextUniform(0, 1, true);
            	if(pivot <= 0.5)
            		dag.addEdge(nodeList.get(v), nodeList.get(w));
            	else
            		dag.addEdge(nodeList.get(w), nodeList.get(v));
            }
        }
        nodeList.clear();
        return dag;
    }

    /**
     * Returns a random rooted-in DAG on {@code V} vertices and {@code E} edges.
     * A rooted in-tree is a DAG in which there is a single vertex
     * reachable from every other vertex.
     * The DAG returned is not chosen uniformly at random among all such DAGs.
     * @param V the number of vertices
     * @param E the number of edges
     * @return a random rooted-in DAG on {@code V} vertices and {@code E} edges
     */
    public static DependencyGraph genRootedInDAG(int numNode, int numEdge, RandomDataGenerator rand) {
    	if (numEdge > (long) numNode * (numNode - 1) / 2) throw new IllegalArgumentException("Too many edges");
        if (numEdge < 0)                  throw new IllegalArgumentException("Too few edges");
        DependencyGraph dag = new DependencyGraph();
    
        boolean[][] isExist = new boolean[numNode][numNode];
        for(int i = 0; i < numNode; i++)
        	for(int j = 0; j < numNode; j++)
        		isExist[i][j] = false;
        for (int i = 0; i < numNode; i++)
            dag.addVertex(new Node());
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

    /**
     * Returns a random rooted-out DAG on {@code V} vertices and {@code E} edges.
     * A rooted out-tree is a DAG in which every vertex is reachable from a
     * single vertex.
     * The DAG returned is not chosen uniformly at random among all such DAGs.
     * @param V the number of vertices
     * @param E the number of edges
     * @return a random rooted-out DAG on {@code V} vertices and {@code E} edges
     */
    public static DependencyGraph genRootedOutDAG(int numNode, int numEdge, RandomDataGenerator rand) {
    	if (numEdge > (long) numNode * (numNode - 1) / 2) throw new IllegalArgumentException("Too many edges");
        if (numEdge < 0)                  throw new IllegalArgumentException("Too few edges");
        DependencyGraph dag = new DependencyGraph();
    
        boolean[][] isExist = new boolean[numNode][numNode];
        for(int i = 0; i < numNode; i++)
        	for(int j = 0; j < numNode; j++)
        		isExist[i][j] = false;
        for (int i = 0; i < numNode; i++)
            dag.addVertex(new Node());
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

    /**
     * Returns a random rooted-in tree on {@code V} vertices.
     * A rooted in-tree is an oriented tree in which there is a single vertex
     * reachable from every other vertex.
     * The tree returned is not chosen uniformly at random among all such trees.
     * @param V the number of vertices
     * @return a random rooted-in tree on {@code V} vertices
     */
    public static DependencyGraph rootedInTree(int numNode, RandomDataGenerator rand) {
        return genRootedInDAG(numNode, numNode - 1, rand);
    }

    /**
     * Returns a random rooted-out tree on {@code V} vertices. A rooted out-tree
     * is an oriented tree in which each vertex is reachable from a single vertex.
     * It is also known as a <em>arborescence</em> or <em>branching</em>.
     * The tree returned is not chosen uniformly at random among all such trees.
     * @param V the number of vertices
     * @return a random rooted-out tree on {@code V} vertices
     */
    public static DependencyGraph rootedOutTree(int numNode, RandomDataGenerator rand) {
        return genRootedOutDAG(numNode, numNode - 1, rand);
    }


    /**
     * Returns a complete binary tree digraph on {@code V} vertices.
     * @param V the number of vertices in the binary tree
     * @return a digraph that is a complete binary tree on {@code V} vertices
     */
    public static DependencyGraph binaryTree(int numNode) {
    	DependencyGraph dag = new DependencyGraph();
    	for (int i = 0; i < numNode; i++)
            dag.addVertex(new Node());
    	List<Node> nodeList = new ArrayList<Node>(dag.vertexSet());
    	for(int i = 0; i < numNode; i++)
    		dag.addEdge(nodeList.get(i), nodeList.get((i - 1) / 2));
        return dag;
    }
    public static DependencyGraph genLayerDAG(int numEdgeLB, int numEdgeUB, int numNodeperLayerLB, int numNodeperLayerUB
    		, int numLayer, RandomGenerator rng)
    {
    	DependencyGraph dag = new DependencyGraph();
    	RandomDataGenerator rand = new RandomDataGenerator(rng);
    	List<Node> preLayerNodeList = null;
    	for(int i = 0; i < numLayer; i++)
    	{
    		List<Node> curLayerNodeList = new ArrayList<Node>();
    		int numNode = rand.nextInt(numNodeperLayerLB, numNodeperLayerUB);
    		for(int j = 0; j < numNode; j++)
    		{
    			Node node = new Node();
	    		dag.addVertex(node);
	    		curLayerNodeList.add(node);
    		}
    		if(i > 0) // Now add edges
    		{
    			int numEdge = rand.nextInt(numEdgeLB, numEdgeUB);
    			if (preLayerNodeList == null) {
    				throw new IllegalStateException();
    			}
    			numEdge = Math.min(numEdge, curLayerNodeList.size() * preLayerNodeList.size());
    			int curNumEdge = 0;
    			boolean[][] isAdded = new boolean[preLayerNodeList.size()][curLayerNodeList.size()];
    			for(int j = 0; j < preLayerNodeList.size(); j++)
    				for(int k = 0; k < curLayerNodeList.size(); k++)
    					isAdded[j][k] = false;
    			while(curNumEdge < numEdge)
    			{
    				int startIdx = rand.nextInt(0, preLayerNodeList.size() - 1);
    				int endIdx = rand.nextInt(0, curLayerNodeList.size() - 1);
    				if(!isAdded[startIdx][endIdx])
    				{
	    				Node startNode = preLayerNodeList.get(startIdx);
	    				Node endNode = curLayerNodeList.get(endIdx);
	    				dag.addEdge(startNode, endNode);
	    				isAdded[startIdx][endIdx] = true;
	    				curNumEdge++;
    				}
    				
    			}
    		}
    		if(preLayerNodeList != null)
    			preLayerNodeList.clear();
    		preLayerNodeList = curLayerNodeList;
    	}
		if (preLayerNodeList == null) {
			throw new IllegalStateException();
		}
    	preLayerNodeList.clear();
    	return dag;
    }
}

package rl;

import java.util.Map;

import javax.swing.JFrame;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;

import model.DependencyGraph;
import utils.DGraphUtils;

import org.jgrapht.ext.JGraphXAdapter;

import graph.Node;
import graph.Edge;
import graph.INode.NodeActivationType;
import graph.INode.NodeType;


public final class GraphVisualize {	
	
	private static DirectedAcyclicGraph<Node, Edge> digraph =
		new DirectedAcyclicGraph<Node, Edge>(Edge.class);

	public static void main(final String[] args) {
		// final String graphName = "graphs/RandomGraph30N100E2T0.json";
		final String graphName = "graphs/RandomGraph100N300E15T0.json";
		digraph = getGraph(graphName);
		System.out.println(digraph.toString());
		showFrame();
	}
	
	private static DependencyGraph getGraph(final String fileName) {
		return DGraphUtils.loadGraph(fileName);
	}
	
	public static void showFrame() {
	    JFrame.setDefaultLookAndFeelDecorated(true);
	    final JFrame frame = new JFrame();
	    frame.setTitle("Dependency Graph");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.add(getComponent());
	    frame.pack();
	    frame.setVisible(true);
	}
	
	private static Object[] getOrCells(
		final JGraphXAdapter<Node, Edge> adapter,
		final boolean isOr) {
		int countGood = 0;
		for (Node n: digraph.vertexSet()) {
		    if ((n.getActivationType() == NodeActivationType.OR) == isOr) {
			    countGood++;
		    }
		}
		
		final Map<Node, com.mxgraph.model.mxICell> vertexToCellMap
		  = adapter.getVertexToCellMap();
		Object[] result = new Object[countGood];
		int i = 0;
		for (Node n: digraph.vertexSet()) {
			  if ((n.getActivationType() == NodeActivationType.OR) == isOr) {
				  result[i] = (Object) vertexToCellMap.get(n);
				  i++;
			  }
		}
		
		return result;
	}
	
	private static Object[] getTargetCells(
		final JGraphXAdapter<Node, Edge> adapter) {
		int countGood = 0;
		for (Node n: digraph.vertexSet()) {
		    if (n.getType() == NodeType.TARGET) {
			    countGood++;
		    }
		}
		
		final Map<Node, com.mxgraph.model.mxICell> vertexToCellMap
		  = adapter.getVertexToCellMap();
		Object[] result = new Object[countGood];
		int i = 0;
		for (Node n: digraph.vertexSet()) {
			  if (n.getType() == NodeType.TARGET) {
				  result[i] = (Object) vertexToCellMap.get(n);
				  i++;
			  }
		}
		
		return result;
	}
	
	public static mxGraphComponent getComponent() {
		final JGraphXAdapter<Node, Edge> jgxAdapter =
			new JGraphXAdapter<Node, Edge>(digraph);
		
		final mxGraphComponent result = new mxGraphComponent(jgxAdapter);

       //  final mxCircleLayout layout = new mxCircleLayout(jgxAdapter);
        final mxHierarchicalLayout layout =
    		new mxHierarchicalLayout(jgxAdapter);
        layout.execute(jgxAdapter.getDefaultParent());
        
        final String orColor = "#22AA22";
        final String andColor = "#8888CC";
        final String targetColor = "#DD5555";
		jgxAdapter.setCellStyle(
			"fillColor=" + orColor, getOrCells(jgxAdapter, true));
		jgxAdapter.setCellStyle(
			"fillColor=" + andColor, getOrCells(jgxAdapter, false));
		jgxAdapter.setCellStyle(
			"fillColor=" + targetColor, getTargetCells(jgxAdapter));
     
        return result;
	}
}

package utils;

import graph.Edge;
import graph.INode.NODE_ACTIVATION_TYPE;
import graph.Node;
import graph.Edge.EDGE_TYPE;
import graph.INode.NODE_STATE;
import graph.INode.NODE_TYPE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import model.DependencyGraph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DGraphUtils {
	static final String nodes = "nodes";
	static final String id = "id";
	static final String topoPosition = "topoPosition";
	static final String nodeType = "nodeType";
	static final String actType = "actType";
	static final String state = "state";
	static final String aReward = "aReward";
	static final String dPenalty = "dPenalty";
	static final String dCost = "dCost";
	
	static final String posActiveProb = "posActiveProb";
	static final String posInactiveProb = "posInactiveProb";
	
	static final String edges = "edges";
	static final String edgeType = "edgeType";
	static final String srcID = "srcID";
	static final String desID = "desID";
	
	static final String aActivationCost = "aActivationCost";
	static final String aActivationProb = "aActivationProb";
	
	static final String targets = "targets";
	static final String minCut = "minCut";

	public static DependencyGraph loadGraph(String filePathName)
	{
		DependencyGraph depGraph = new DependencyGraph();
		final String inputString = linesAsString(filePathName);
        final JsonObject inputJson = 
                new JsonParser().parse(inputString).getAsJsonObject();
        JsonArray nodeDataJson = inputJson.get(nodes).getAsJsonArray();
        JsonArray edgeDataJson = inputJson.get(edges).getAsJsonArray();
        JsonArray targetDataJson = inputJson.get(targets).getAsJsonArray();
        JsonArray mincutDataJson = inputJson.get(minCut).getAsJsonArray();
        
        /************************************************************************************/
        // Add node
        for(int i = 0; i < nodeDataJson.size(); i++)
        {
        	JsonObject nodeObject = nodeDataJson.get(i).getAsJsonObject();
        	int nID = nodeObject.get(id).getAsInt();
        	int nTopoPosition = nodeObject.get(topoPosition).getAsInt();
        	NODE_TYPE nType = NODE_TYPE.valueOf(nodeObject.get(nodeType).getAsString());
        	NODE_ACTIVATION_TYPE nActType = NODE_ACTIVATION_TYPE.valueOf(nodeObject.get(actType).getAsString());
        	NODE_STATE nState = NODE_STATE.valueOf(nodeObject.get(state).getAsString());
        	double nAReward = nodeObject.get(aReward).getAsDouble();
        	double nDPenalty = nodeObject.get(dPenalty).getAsDouble();
        	double nDCost = nodeObject.get(dCost).getAsDouble();
        	double nPosActiveProb = nodeObject.get(posActiveProb).getAsDouble();
        	double nPosInactiveProb = nodeObject.get(posInactiveProb).getAsDouble();
        	double nActivationCost = nodeObject.get(aActivationCost).getAsDouble();
        	double nActivationProb = nodeObject.get(aActivationProb).getAsDouble();
        	
        	Node node = new Node(nID, nType, nActType, nAReward, nDPenalty, nDCost
        			, nActivationCost, nPosActiveProb, nPosInactiveProb, nActivationProb);
        	node.setState(nState);
        	node.setTopoPosition(nTopoPosition);
        	
        	depGraph.addVertex(node);
        }
        
        // Distance
        Node[] nodeArray = new Node[depGraph.vertexSet().size()];
        for(Node node : depGraph.vertexSet())
        	nodeArray[node.getId() - 1] = node;
       
        /************************************************************************************/
        // Add edges
        for(int i = 0; i < edgeDataJson.size(); i++)
        {
        	JsonObject edgeObject = edgeDataJson.get(i).getAsJsonObject();
        	int edgeID = edgeObject.get(id).getAsInt();
        	int srcEdgeID = edgeObject.get(srcID).getAsInt();
        	int desEdgeID = edgeObject.get(desID).getAsInt();
        	EDGE_TYPE type = EDGE_TYPE.valueOf(edgeObject.get(edgeType).getAsString());
        	double aCost = edgeObject.get(aActivationCost).getAsDouble();
        	double aProb = edgeObject.get(aActivationProb).getAsDouble();
        	Edge edge = depGraph.addEdge(nodeArray[srcEdgeID - 1], nodeArray[desEdgeID - 1]);
        	edge.setId(edgeID);
        	edge.setType(type);
        	edge.setACost(aCost);
        	edge.setActProb(aProb);
        }
        /************************************************************************************/
        // Add target set
        for(int i = 0; i < targetDataJson.size(); i++)
        {
        	JsonObject targetObject = targetDataJson.get(i).getAsJsonObject();
        	int targetID = targetObject.get(id).getAsInt();
        	depGraph.addTarget(nodeArray[targetID - 1]);
        }
        /************************************************************************************/
        /************************************************************************************/
        // Add min cut
        for(int i = 0; i < mincutDataJson.size(); i++)
        {
        	JsonObject mincutObject = mincutDataJson.get(i).getAsJsonObject();
        	int nodeID = mincutObject.get(id).getAsInt();
        	depGraph.addMinCut(nodeArray[nodeID - 1]);
        }
        /************************************************************************************/
		return depGraph;
	}
	public static final String linesAsString(final String fileName) {
        assert fileName != null;
        final StringBuilder builder = new StringBuilder();
        try {
             final BufferedReader br =
                new BufferedReader(new FileReader(new File(fileName)));
            String line = null;
            while ((line = br.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
            br.close();
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
        return builder.toString();
    }
	public static void save(String filePathName, DependencyGraph depGraph)
	{
		
		/************************************************************************************/
		// Save nodes
		final JsonArray nodeArray = new JsonArray();
		for(Node node : depGraph.vertexSet())
		{
			final JsonObject nodeObject = new JsonObject();
			nodeObject.addProperty(id, node.getId());
			nodeObject.addProperty(topoPosition, node.getTopoPosition());
			nodeObject.addProperty(nodeType, node.getType().toString());
			nodeObject.addProperty(actType, node.getActivationType().toString());
			nodeObject.addProperty(state, node.getState().toString());
			nodeObject.addProperty(aReward, node.getAReward());
			nodeObject.addProperty(dPenalty, node.getDPenalty());
			nodeObject.addProperty(dCost, node.getDCost());
			nodeObject.addProperty(posActiveProb, node.getPosActiveProb());
			nodeObject.addProperty(posInactiveProb, node.getPosInactiveProb());
			nodeObject.addProperty(aActivationCost, node.getACost());
			nodeObject.addProperty(aActivationProb, node.getActProb());
			nodeArray.add(nodeObject);
		}
		/************************************************************************************/
		// Save edges
		final JsonArray edgeArray = new JsonArray();
		for(Edge edge : depGraph.edgeSet())
		{
			final JsonObject edgeObject = new JsonObject();
			edgeObject.addProperty(id, edge.getId());
			edgeObject.addProperty(srcID, edge.getsource().getId());
			edgeObject.addProperty(desID, edge.gettarget().getId());
			edgeObject.addProperty(edgeType, edge.getType().toString());
			edgeObject.addProperty(aActivationCost, edge.getACost());
			edgeObject.addProperty(aActivationProb, edge.getActProb());
			edgeArray.add(edgeObject);
		}
		/************************************************************************************/
		// Save target
		final JsonArray targetArray = new JsonArray();
		for(Node target : depGraph.getTargetSet())
		{
			final JsonObject targetObject = new JsonObject();
			targetObject.addProperty(id, target.getId());
			targetArray.add(targetObject);
		}
		/************************************************************************************/
		// Save mincut
		final JsonArray mincutArray = new JsonArray();
		for(Node node : depGraph.getMinCut())
		{
			final JsonObject mincutObject = new JsonObject();
			mincutObject.addProperty(id, node.getId());
			mincutArray.add(mincutObject);
		}
		/************************************************************************************/
        final JsonObject graphObject = new JsonObject();
        graphObject.add(nodes, nodeArray);
        graphObject.add(edges, edgeArray);
        graphObject.add(targets, targetArray);
        graphObject.add(minCut, mincutArray);

        final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
        String graphObjectString =  gson.toJson(graphObject).toString();
        final File file = new File(filePathName);
        try {
            final BufferedWriter output =
                new BufferedWriter(new FileWriter(file));
            output.write(graphObjectString);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}

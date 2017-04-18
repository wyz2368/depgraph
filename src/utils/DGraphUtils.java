package utils;

import graph.Edge;
import graph.INode.NodeActivationType;
import graph.Node;
import graph.Edge.EdgeType;
import graph.INode.NodeState;
import graph.INode.NodeType;

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

public final class DGraphUtils {
	static final String NODES = "nodes";
	static final String ID = "id";
	static final String TOPO_POSITION = "topoPosition";
	static final String NODE_TYPE = "nodeType";
	static final String ACT_TYPE = "actType";
	static final String STATE = "state";
	static final String A_REWARD = "aReward";
	static final String D_PENALTY = "dPenalty";
	static final String D_COST = "dCost";
	
	static final String POS_ACTIVE_PROB = "posActiveProb";
	static final String POS_INACTIVE_PROB = "posInactiveProb";
	
	static final String EDGES = "edges";
	static final String EDGE_TYPE = "edgeType";
	static final String SRC_ID = "srcID";
	static final String DES_ID = "desID";
	
	static final String A_ACTIVATION_COST = "aActivationCost";
	static final String A_ACTIVATION_PROB = "aActivationProb";
	
	static final String TARGETS = "targets";
	static final String MIN_CUT = "minCut";
	
	private DGraphUtils() {
		// private constructor
	}

	public static DependencyGraph loadGraph(final String filePathName) {
		DependencyGraph depGraph = new DependencyGraph();
		final String inputString = linesAsString(filePathName);
        final JsonObject inputJson = 
                new JsonParser().parse(inputString).getAsJsonObject();
        JsonArray nodeDataJson = inputJson.get(NODES).getAsJsonArray();
        JsonArray edgeDataJson = inputJson.get(EDGES).getAsJsonArray();
        JsonArray targetDataJson = inputJson.get(TARGETS).getAsJsonArray();
        JsonArray mincutDataJson = inputJson.get(MIN_CUT).getAsJsonArray();
        
        /************************************************************************************/
        // Add node
        for (int i = 0; i < nodeDataJson.size(); i++) {
        	JsonObject nodeObject = nodeDataJson.get(i).getAsJsonObject();
        	int nID = nodeObject.get(ID).getAsInt();
        	int nTopoPosition = nodeObject.get(TOPO_POSITION).getAsInt();
        	NodeType nType = NodeType.valueOf(nodeObject.get(NODE_TYPE).getAsString());
        	NodeActivationType nActType = NodeActivationType.valueOf(nodeObject.get(ACT_TYPE).getAsString());
        	NodeState nState = NodeState.valueOf(nodeObject.get(STATE).getAsString());
        	double nAReward = nodeObject.get(A_REWARD).getAsDouble();
        	double nDPenalty = nodeObject.get(D_PENALTY).getAsDouble();
        	double nDCost = nodeObject.get(D_COST).getAsDouble();
        	double nPosActiveProb = nodeObject.get(POS_ACTIVE_PROB).getAsDouble();
        	double nPosInactiveProb = nodeObject.get(POS_INACTIVE_PROB).getAsDouble();
        	double nActivationCost = nodeObject.get(A_ACTIVATION_COST).getAsDouble();
        	double nActivationProb = nodeObject.get(A_ACTIVATION_PROB).getAsDouble();
        	
        	Node node = new Node(nID, nType, nActType, nAReward, nDPenalty, nDCost
        			, nActivationCost, nPosActiveProb, nPosInactiveProb, nActivationProb);
        	node.setState(nState);
        	node.setTopoPosition(nTopoPosition);
        	
        	depGraph.addVertex(node);
        }
        
        // Distance
        Node[] nodeArray = new Node[depGraph.vertexSet().size()];
        for (Node node : depGraph.vertexSet()) {
        	nodeArray[node.getId() - 1] = node;
        }
       
        /************************************************************************************/
        // Add edges
        for (int i = 0; i < edgeDataJson.size(); i++) {
        	JsonObject edgeObject = edgeDataJson.get(i).getAsJsonObject();
        	int edgeID = edgeObject.get(ID).getAsInt();
        	int srcEdgeID = edgeObject.get(SRC_ID).getAsInt();
        	int desEdgeID = edgeObject.get(DES_ID).getAsInt();
        	EdgeType type = EdgeType.valueOf(edgeObject.get(EDGE_TYPE).getAsString());
        	double aCost = edgeObject.get(A_ACTIVATION_COST).getAsDouble();
        	double aProb = edgeObject.get(A_ACTIVATION_PROB).getAsDouble();
        	Edge edge = depGraph.addEdge(nodeArray[srcEdgeID - 1], nodeArray[desEdgeID - 1]);
        	edge.setId(edgeID);
        	edge.setType(type);
        	edge.setACost(aCost);
        	edge.setActProb(aProb);
        }
        /************************************************************************************/
        // Add target set
        for (int i = 0; i < targetDataJson.size(); i++) {
        	JsonObject targetObject = targetDataJson.get(i).getAsJsonObject();
        	int targetID = targetObject.get(ID).getAsInt();
        	depGraph.addTarget(nodeArray[targetID - 1]);
        }
        /************************************************************************************/
        /************************************************************************************/
        // Add min cut
        for (int i = 0; i < mincutDataJson.size(); i++) {
        	JsonObject mincutObject = mincutDataJson.get(i).getAsJsonObject();
        	int nodeID = mincutObject.get(ID).getAsInt();
        	depGraph.addMinCut(nodeArray[nodeID - 1]);
        }
        /************************************************************************************/
		return depGraph;
	}
	
	public static String linesAsString(final String fileName) {
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
	
	public static void save(final String filePathName, final DependencyGraph depGraph) {
		
		/************************************************************************************/
		// Save nodes
		final JsonArray nodeArray = new JsonArray();
		for (Node node : depGraph.vertexSet()) {
			final JsonObject nodeObject = new JsonObject();
			nodeObject.addProperty(ID, node.getId());
			nodeObject.addProperty(TOPO_POSITION, node.getTopoPosition());
			nodeObject.addProperty(NODE_TYPE, node.getType().toString());
			nodeObject.addProperty(ACT_TYPE, node.getActivationType().toString());
			nodeObject.addProperty(STATE, node.getState().toString());
			nodeObject.addProperty(A_REWARD, node.getAReward());
			nodeObject.addProperty(D_PENALTY, node.getDPenalty());
			nodeObject.addProperty(D_COST, node.getDCost());
			nodeObject.addProperty(POS_ACTIVE_PROB, node.getPosActiveProb());
			nodeObject.addProperty(POS_INACTIVE_PROB, node.getPosInactiveProb());
			nodeObject.addProperty(A_ACTIVATION_COST, node.getACost());
			nodeObject.addProperty(A_ACTIVATION_PROB, node.getActProb());
			nodeArray.add(nodeObject);
		}
		/************************************************************************************/
		// Save edges
		final JsonArray edgeArray = new JsonArray();
		for (Edge edge : depGraph.edgeSet()) {
			final JsonObject edgeObject = new JsonObject();
			edgeObject.addProperty(ID, edge.getId());
			edgeObject.addProperty(SRC_ID, edge.getsource().getId());
			edgeObject.addProperty(DES_ID, edge.gettarget().getId());
			edgeObject.addProperty(EDGE_TYPE, edge.getType().toString());
			edgeObject.addProperty(A_ACTIVATION_COST, edge.getACost());
			edgeObject.addProperty(A_ACTIVATION_PROB, edge.getActProb());
			edgeArray.add(edgeObject);
		}
		/************************************************************************************/
		// Save target
		final JsonArray targetArray = new JsonArray();
		for (Node target : depGraph.getTargetSet()) {
			final JsonObject targetObject = new JsonObject();
			targetObject.addProperty(ID, target.getId());
			targetArray.add(targetObject);
		}
		/************************************************************************************/
		// Save mincut
		final JsonArray mincutArray = new JsonArray();
		for (Node node : depGraph.getMinCut()) {
			final JsonObject mincutObject = new JsonObject();
			mincutObject.addProperty(ID, node.getId());
			mincutArray.add(mincutObject);
		}
		/************************************************************************************/
        final JsonObject graphObject = new JsonObject();
        graphObject.add(NODES, nodeArray);
        graphObject.add(EDGES, edgeArray);
        graphObject.add(TARGETS, targetArray);
        graphObject.add(MIN_CUT, mincutArray);

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

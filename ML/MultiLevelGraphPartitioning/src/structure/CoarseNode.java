package structure;

import java.util.HashMap;

public class CoarseNode extends Node{

	public CoarseNode(int nodeID, int nodeWeight) {
		// TODO Auto-generated constructor stub
		super(nodeID, nodeWeight);
	}
	public CoarseNode(int curNodeID, int curNodeWeight, Node[] neighbors,
			Edge[] neighborsEdges,
			HashMap<Integer, Tuple<Node, Edge>> neighborsMap) {
		super(curNodeID, curNodeWeight, neighbors, neighborsEdges, neighborsMap);
	}


}

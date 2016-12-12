package structure;
import java.util.HashMap;

/*
 * 
 * This class will contains information about Graph Node as follow
 * Node ID
 * Node Weight
 * Neighbors IDs
 * Edges to Neighbors
 * 
 * This class uses space O(V+E)
 * can be approximated to O(E)
 * */
public class Node {

	protected int nodeID, nodeWeight, numberOfNeighbors;
	protected Node[] neighbors;
	protected Edge[] neighborsEdges;
	protected HashMap<Integer, Tuple<Node, Edge>> neighborsMap;

	/* First Constructor */
	public Node(int nodeID) {
		this.nodeID = nodeID;
	}

	/* Second Constructor */
	public Node(int nodeID, int nodeWeight) {
		this.nodeID = nodeID;
		this.nodeWeight = nodeWeight;
	}

	/* Third Constructor */
	public Node(int nodeID, int nodeWeight, Node[] neighbors,
			Edge[] neighborsEdges,
			HashMap<Integer, Tuple<Node, Edge>> neighborsMap) {
		this.nodeID = nodeID;
		this.nodeWeight = nodeWeight;
		this.neighbors = neighbors;
		this.neighborsEdges = neighborsEdges;
		this.numberOfNeighbors = neighbors.length;
		this.neighborsMap = neighborsMap;
	}

	/* Setter & Getters */
	public int getNodeID() {
		return nodeID;
	}

	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}

	public int getNodeWeight() {
		return nodeWeight;
	}

	public void setNodeWeight(int nodeWeight) {
		this.nodeWeight = nodeWeight;
	}

	public int getNumberOfNeighbors() {
		return numberOfNeighbors;
	}

	public void setNumberOfNeighbors(int numberOfNeighbors) {
		this.numberOfNeighbors = numberOfNeighbors;
	}

	public Node[] getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(Node[] neighbors) {
		this.neighbors = neighbors;
		this.numberOfNeighbors = neighbors.length;
	}

	public Edge[] getNeighborsEdges() {
		return neighborsEdges;
	}

	public void setNeighborsEdges(Edge[] neighborsEdges) {
		this.neighborsEdges = neighborsEdges;
	}

	public HashMap<Integer, Tuple<Node, Edge>> getNeighborsMap() {
		return neighborsMap;
	}

	public void setNeighborsMap(HashMap<Integer, Tuple<Node, Edge>> neighborsMap) {
		this.neighborsMap = neighborsMap;
	}

	/* Class Methods */
	public void addNeighbors(Node[] neighbors, Edge[] neighborsEdges) {
		/*
		 * this.numberOfNeighbors = neighbors.length; if (this.neighbors ==
		 * null) { this.neighbors = neighbors; } if (this.neighborsEdges ==
		 * null) { this.neighborsEdges = neighborsEdges; }
		 */
	}

	public int getTotalEdgesWeight() {
		int totalEdgesWeight = 0;
		for (int i = 0; i < this.neighborsEdges.length; i++) {
			totalEdgesWeight += this.neighborsEdges[i].getWeight();
		}
		return totalEdgesWeight;
	}
	
	public void addEdge(Edge edge){
		int srcNodeID = edge.getSourceID();
		int dstNodeID = edge.getDestinationID();
		int otherNodeID = this.nodeID == srcNodeID ? dstNodeID : srcNodeID;

	}
}

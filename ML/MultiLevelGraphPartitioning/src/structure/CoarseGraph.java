package structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class CoarseGraph extends Graph {

	private Graph parentGraph;
	private ArrayList<RandomSet<Integer>> nodesTree;
	// reversed map
	// this map point from child node to parent node
	// it will be O(V) space
	// and will help to coarse the graph in linear time
	private HashMap<Integer, Integer> reversedMap;
	private ArrayList<Edge> edgesList;

	public CoarseGraph(Graph parentGraph, ArrayList<RandomSet<Integer>> nodesTree) {
		super();
		this.parentGraph = parentGraph;
		this.nodesTree = nodesTree;
		this.numberOfNodes = this.nodesTree.size();
		this.nodes = new CoarseNode[numberOfNodes];
		this.edgeIndex = 0;
		this.nodesEdgesMap = new HashMap<Tuple<Integer, Integer>, Edge>();
		this.edgesList = new ArrayList<Edge>();
		// create reversedMap
		this.createReversedMap();
		// create nodes
//		for (int i = 1; i <= this.numberOfNodes; i++) {
//			this.nodes[i-1] = new CoarseNode(i, this.getNodeWeight(i));
//		}
		// create edges
		// loop parent graph edges
		Edge[] parentGraphEdges = this.parentGraph.getEdges();
		for (int i = 0; i < parentGraphEdges.length; i++) {
			int childSrcNodeID = parentGraphEdges[i].getSourceID();
			int childDstNodeID = parentGraphEdges[i].getDestinationID();
			Edge childEdge = this.parentGraph.getEdge(childSrcNodeID, childDstNodeID);
			int childEdgeWeight = childEdge.getWeight();
			int parentSrcNodeID = this.reversedMap.get(childSrcNodeID);
			int parentDstNodeID = this.reversedMap.get(childDstNodeID);
			int tmpSrcNodeID = parentSrcNodeID;
			parentSrcNodeID = Math.min(parentSrcNodeID, parentDstNodeID);
			parentDstNodeID = Math.max(tmpSrcNodeID, parentDstNodeID);
			Tuple<Integer, Integer> edgeTuple = new Tuple<Integer, Integer> (parentSrcNodeID, parentDstNodeID);
			if (!this.nodesEdgesMap.containsKey(edgeTuple)) {
				Edge parentEdge = new CoarseEdge(parentSrcNodeID, parentDstNodeID, childEdgeWeight);
				this.nodesEdgesMap.put(edgeTuple, parentEdge);
			}
			else {
				Edge parentEdge = this.nodesEdgesMap.get(edgeTuple);
				parentEdge.setWeight(parentEdge.getWeight() + childEdgeWeight);
//				this.nodesEdgesMap.put(edgeTuple, parentEdge);
			}
		}
		// create nodes
		int curNodeID;
		for (int i = 0; i < this.numberOfNodes; i++) {
			curNodeID = i + 1;
			this.storeNode(curNodeID);
		}
		// initialize shuffled nodes
		this.shuffeledNodesIDs = new int[this.numberOfNodes];
		for (int i = 0; i < this.nodes.length; i++) {
			this.shuffeledNodesIDs[i] = i + 1;
		}
		this.edges = new Edge[this.edgesList.size()];
		for (int i = 0; i < this.edgesList.size(); i++) {
			this.edges[i] = this.edgesList.get(i);
		}
		this.numberOfEdges = this.edges.length;
		// Store Edges and sort it by weight
		Arrays.sort(this.edges);
	}

	private void storeNode(int curNodeID) {
		int curNodeWeight;
		int[] curNeighborsIDs;
		// calculate Node weight
		curNodeWeight = this.getNodeWeight(curNodeID);
		// find Node neighbors
		curNeighborsIDs = this.getNodeNeighbors(curNodeID);
		// create Neighbors Nodes
		Node[] neighbors = new CoarseNode[curNeighborsIDs.length];
		Edge[] neighborsEdges = new CoarseEdge[curNeighborsIDs.length];
		Node currentNode;
		HashMap<Integer, Tuple<Node, Edge>> neighborsMap = new HashMap<Integer, Tuple<Node, Edge>>(
				curNeighborsIDs.length);
		for (int j = 0; j < curNeighborsIDs.length; j++) {
			Node neighborNode;
			Edge neighborEdge;
			int neighborEdgeWeight;
			// check if Node was previously created
			if (isNodeCreated(curNeighborsIDs[j])) {
				neighborNode = this.nodes[curNeighborsIDs[j] - 1];
			} else {
				neighborNode = new CoarseNode(curNeighborsIDs[j], this.getNodeWeight(curNeighborsIDs[j]));
				this.nodes[curNeighborsIDs[j] - 1] = neighborNode;
			}

			neighbors[j] = neighborNode;
			if (isEdgeCreated(curNodeID, curNeighborsIDs[j])) {
				neighborEdge = getEdge(curNodeID, curNeighborsIDs[j]);
			} else {
				neighborEdgeWeight = this.calculateEdgeWeight(curNodeID, curNeighborsIDs[j]);
				neighborEdge = new CoarseEdge(curNodeID, curNeighborsIDs[j], neighborEdgeWeight);
			}
			neighborsEdges[j] = neighborEdge;
			neighborsMap.put(curNeighborsIDs[j], new Tuple<Node, Edge>(neighbors[j], neighborsEdges[j]));
			// Add edge to Graph
			// make sure each edge is added only once
			if (curNodeID < curNeighborsIDs[j]) {
				this.nodesEdgesMap.put(new Tuple<Integer, Integer>(curNodeID, curNeighborsIDs[j]), neighborEdge);
				this.edgesList.add(neighborEdge);
			}

		}
		// create current Node
		if (isNodeCreated(curNodeID)) {
			currentNode = this.nodes[curNodeID - 1];
			currentNode.setNeighbors(neighbors);
			currentNode.setNeighborsEdges(edges);
			currentNode.setNeighborsMap(neighborsMap);
		} else {
			currentNode = new CoarseNode(curNodeID, curNodeWeight, neighbors, neighborsEdges, neighborsMap);
			this.nodes[curNodeID - 1] = currentNode;
		}
	}

	private int[] getNodeNeighbors(int curNodeID) {
		HashSet<Integer> curNeighbors = new HashSet<Integer>();
		int[] curNeighborsArray;
		int curNodeIndex = curNodeID - 1;
		int curNeighbor = -1;
		int childNodeID;
		Node childNode;
		Node[] childNodeNeighbors;
		Iterator<Integer> childsIt = this.nodesTree.get(curNodeIndex).iterator();
		while (childsIt.hasNext()) {
			childNodeID = childsIt.next();
			childNode = this.parentGraph.getNode(childNodeID);

			// get child Node Neighbors
			childNodeNeighbors = childNode.getNeighbors();
			for (int j = 0; j < childNodeNeighbors.length; j++) {
				int neighborID = childNodeNeighbors[j].getNodeID();
				curNeighbor = this.reversedMap.get(neighborID);	
				
				if (curNeighbor != curNodeID) {
					curNeighbors.add(curNeighbor);
				}
			}
		}

		curNeighborsArray = new int[curNeighbors.size()];
		Iterator<Integer> it = curNeighbors.iterator();
		int arrIndex = 0;
		while (it.hasNext()) {
			curNeighborsArray[arrIndex] = it.next();
			arrIndex++;
		}

		return curNeighborsArray;
	}

	public ArrayList<RandomSet<Integer>> getNodesTree() {
		return nodesTree;
	}

	public void setNodesTree(ArrayList<RandomSet<Integer>> nodesTree) {
		this.nodesTree = nodesTree;
	}

	private int getNodeWeight(int curNodeID) {
		int weight = 0;
		int curNodeIndex = curNodeID - 1;
		int childNodeID;
		Iterator<Integer> childsIt = this.nodesTree.get(curNodeIndex).iterator();
		while (childsIt.hasNext()) {
			childNodeID = childsIt.next();
			weight += this.parentGraph.getNode(childNodeID).getNodeWeight();
		}
		return weight;
	}

	private void createReversedMap() {
		this.reversedMap = new HashMap<Integer, Integer>(this.parentGraph.numberOfNodes);
		// fill the reversed map
		int parentNodeID, childNodeID;
		for (int i = 0; i < this.nodesTree.size(); i++) {
			parentNodeID = i + 1;
			Iterator<Integer> childsIt = this.nodesTree.get(i).iterator();
			while (childsIt.hasNext()) {
				childNodeID = childsIt.next();
				reversedMap.put(childNodeID, parentNodeID);
			}
		}
	}


	public RandomSet<Integer> getNodeChilds(int nodeID) {
		return this.nodesTree.get(nodeID - 1);
	}

	private int calculateEdgeWeight(int sourceID, int destinationID) {
		int edgeWeight = 0;
		RandomSet<Integer> sourceNeighbors = this.nodesTree.get(sourceID - 1);
		RandomSet<Integer> destinationNeighbors = this.nodesTree.get(destinationID - 1);
		Iterator<Integer> sourceIt = sourceNeighbors.iterator();
		while (sourceIt.hasNext()) {
			int srcID = sourceIt.next();
			Iterator<Integer> destIt = destinationNeighbors.iterator();
			while (destIt.hasNext()) {
				Edge innerEdge = this.parentGraph.getEdge(srcID, destIt.next());
				if (innerEdge != null) {
					edgeWeight += innerEdge.getWeight();
				}
				
			}
			
		}
		return edgeWeight;
	}

	public long[][] getAdjacencyMatrix() {
		long[][] adjMatrix = new long[this.numberOfNodes][this.numberOfNodes];
		for (int i = 0; i < this.numberOfNodes - 1; i++) {
			int node1ID = i+1;
			for (int j = i+1; j < this.numberOfNodes; j++) {
				int node2ID = j+1;
				CoarseEdge edge = (CoarseEdge) getEdge(node1ID, node2ID);
				int edgeWeight = 0;
				if (edge != null) {
					edgeWeight = edge.getWeight();
				}
				adjMatrix[i][j] = edgeWeight;
				adjMatrix[j][i] = edgeWeight;
			}
		}
		return adjMatrix;
	}
	
	public void switchParentGraph(Graph parentGraph, ArrayList<RandomSet<Integer>> nodesTree){
		this.parentGraph = parentGraph;
		this.nodesTree = nodesTree;
		// create reversedMap
		this.createReversedMap();
	}
	/*
	 * Setters & Getters
	 */
	public Graph getParentGraph() {
		return parentGraph;
	}

	public void setParentGraph(Graph parentGraph) {
		this.parentGraph = parentGraph;
	}

	public int getParentNodeIDOf(int childNodeID) {
		return this.reversedMap.get(childNodeID);
	}

}

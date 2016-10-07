package structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class CoarseGraph extends Graph {

	private Graph parentGraph;
	private ArrayList<ArrayList<Integer>> nodesTree;
	// reversed map
	// this map point from child node to parent node
	// it will be O(V) space
	// and will help to coarse the graph in linear time
	private HashMap<Integer, Integer> reversedMap;
	private ArrayList<Edge> edgesList;

	public CoarseGraph(Graph parentGraph, ArrayList<ArrayList<Integer>> nodesTree) {
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
//		System.out.println("start");
//		for (int i = 0; i < nodesTree.size(); i++) {
//			for (int j = 0; j < nodesTree.get(i).size(); j++) {
//				System.out.print(nodesTree.get(i).get(j)+",");
//			}
//			System.out.println();
//		}

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
		int curNeighbor;
		int childNodeID;
		Node childNode;
		Node[] childNodeNeighbors;
		for (int i = 0; i < this.nodesTree.get(curNodeIndex).size(); i++) {
			childNodeID = this.nodesTree.get(curNodeIndex).get(i);
			childNode = this.parentGraph.getNode(childNodeID);

			// get child Node Neighbors
			childNodeNeighbors = childNode.getNeighbors();
			for (int j = 0; j < childNodeNeighbors.length; j++) {
				int neighborID = childNodeNeighbors[j].getNodeID();
				//System.out.println(neighborID);
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

	private int getNodeWeight(int curNodeID) {
		int weight = 0;
		int curNodeIndex = curNodeID - 1;
		int childNodeID;
		for (int i = 0; i < this.nodesTree.get(curNodeIndex).size(); i++) {
			childNodeID = this.nodesTree.get(curNodeIndex).get(i);
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
			for (int j = 0; j < this.nodesTree.get(i).size(); j++) {
				childNodeID = this.nodesTree.get(i).get(j);
				reversedMap.put(childNodeID, parentNodeID);
			}
		}
	}

	public ArrayList<Integer> getNodeChilds(int nodeID) {
		return this.nodesTree.get(nodeID - 1);
	}

	private int calculateEdgeWeight(int sourceID, int destinationID) {
		int edgeWeight = 0;
		ArrayList<Integer> sourceNeighbors = this.nodesTree.get(sourceID - 1);
		ArrayList<Integer> destinationNeighbors = this.nodesTree.get(destinationID - 1);
		for (int i = 0; i < sourceNeighbors.size(); i++) {
			for (int j = 0; j < destinationNeighbors.size(); j++) {
				Edge innerEdge = this.parentGraph.getEdge(sourceNeighbors.get(i), destinationNeighbors.get(j));
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
	/*
	 * Setters & Getters
	 */
	public Graph getParentGraph() {
		return parentGraph;
	}

	public void setParentGraph(Graph parentGraph) {
		this.parentGraph = parentGraph;
	}

}

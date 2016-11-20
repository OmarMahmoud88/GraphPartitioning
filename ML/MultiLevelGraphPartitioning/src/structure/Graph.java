package structure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

public class Graph {

	protected int numberOfNodes;
	protected long numberOfEdges;
	protected int edgeIndex;
	protected Node[] nodes;
	protected Edge[] edges;
	protected int[] shuffeledNodesIDs;
	//protected RealMatrix laplacianMatrix;
	//protected DenseMatrix64F laplacianMatrix;
	protected double[][] laplacianMatrix;
	protected double[] vertixWeightDiagonalMatrix;
	protected double[] vertixDegreeDiagonalMatrix;

	protected HashMap<Tuple<Integer, Integer>, Edge> nodesEdgesMap;

	public Graph() {
	}

	public Graph(String fileSrc) {
		FileReader in;
		BufferedReader br;
		String line;
		this.edgeIndex = 0;
		try {
			in = new FileReader(fileSrc);
			br = new BufferedReader(in);
			// read first line number of file
			// contains details about the graph
			// nodes number, and edges number
			// directed or undirected graph
			// weighted or non-weighted nodes, edges
			line = br.readLine();
			int[] hdr = translateFileLine(line);
			this.numberOfNodes = hdr[0];
			this.numberOfEdges = hdr[1];
			// hdr[2] graph is directed if 11
			this.nodes = new Node[this.numberOfNodes];
			this.edges = new Edge[(int) this.numberOfEdges];
			this.nodesEdgesMap = new HashMap<Tuple<Integer, Integer>, Edge>((int) this.numberOfEdges);
			// read nodes and edges
			int nodeID = 1;
			int lineCounter = 0;
			while (lineCounter < this.numberOfNodes) {
				line = br.readLine();
				if (line == null)
					break;
				storeNodeLine(nodeID, line);
				nodeID++;
				lineCounter++;
			}
			in.close();
			// initialize shuffled nodes
			// this array will be needed for randomization purposes
			this.shuffeledNodesIDs = new int[this.numberOfNodes];
			for (int i = 0; i < this.nodes.length; i++) {
				this.shuffeledNodesIDs[i] = i + 1;
			}
			// sort edges by weight
			Arrays.sort(this.edges);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * This Function receives Graph file line and return Integer array with
	 * nodes IDs
	 */
	private int[] translateFileLine(String line) {
		String trimmedLine = line.trim();
		if (trimmedLine.length() == 0) {
			return new int[0];
		}
		String[] neighborsString = trimmedLine.split(" ");
		int[] neighborsIDs = new int[neighborsString.length];
		for (int i = 0; i < neighborsIDs.length; i++) {
			neighborsIDs[i] = Integer.parseInt(neighborsString[i]);

		}
		return neighborsIDs;
	}

	/*
	 * This Function Checks if Node is already created
	 */
	protected boolean isNodeCreated(int NodeID) {
		// NodeID == NodeIndex + 1
		return (this.nodes[NodeID - 1] != null);
	}

	/*
	 * This Function Checks if Edge is already created
	 */
	protected boolean isEdgeCreated(int sourceID, int destinationID) {
		int min = Math.min(sourceID, destinationID);
		int max = Math.max(sourceID, destinationID);
		return (this.nodesEdgesMap.containsKey(new Tuple<Integer, Integer>(min, max)));
	}

	/*
	 * This function return edge with source and destination
	 */
	public Edge getEdge(int sourceID, int destinationID) {
		int min = Math.min(sourceID, destinationID);
		int max = Math.max(sourceID, destinationID);
		return (this.nodesEdgesMap.get(new Tuple<Integer, Integer>(min, max)));
	}

	/*
	 * This function store node file line into memory
	 */
	private void storeNodeLine(int nodeID, String line) {
		int[] nodeNeigborsIDs = translateFileLine(line);
		Node currentNode;
		Node[] neighbors = new Node[nodeNeigborsIDs.length];
		Edge[] edges = new Edge[nodeNeigborsIDs.length];
		HashMap<Integer, Tuple<Node, Edge>> neighborsMap = new HashMap<Integer, Tuple<Node, Edge>>(
				nodeNeigborsIDs.length);
		// create Neighbors Nodes
		for (int i = 0; i < nodeNeigborsIDs.length; i++) {
			Node neighborNode;
			Edge neighborEdge;
			// check if Node was previously created
			if (isNodeCreated(nodeNeigborsIDs[i])) {
				neighborNode = this.nodes[nodeNeigborsIDs[i] - 1];
			} else {
				neighborNode = new Node(nodeNeigborsIDs[i], 1);
				this.nodes[nodeNeigborsIDs[i] - 1] = neighborNode;
			}

			neighbors[i] = neighborNode;
			if (isEdgeCreated(nodeID, nodeNeigborsIDs[i])) {
				neighborEdge = getEdge(nodeID, nodeNeigborsIDs[i]);
			} else {
				neighborEdge = new Edge(nodeID, nodeNeigborsIDs[i], 1);
			}
			edges[i] = neighborEdge;
			neighborsMap.put(nodeNeigborsIDs[i], new Tuple<Node, Edge>(neighbors[i], edges[i]));
			// Add edge to Graph
			// make sure each edge is added only once
			if (nodeID <= nodeNeigborsIDs[i]) {
				this.nodesEdgesMap.put(new Tuple<Integer, Integer>(nodeID, nodeNeigborsIDs[i]), neighborEdge);
				this.edges[this.edgeIndex] = neighborEdge;
				this.edgeIndex++;
			}

		}
		// create current Node
		if (isNodeCreated(nodeID)) {
			currentNode = this.nodes[nodeID - 1];
			currentNode.setNeighbors(neighbors);
			currentNode.setNeighborsEdges(edges);
			currentNode.setNeighborsMap(neighborsMap);
		} else {
			currentNode = new Node(nodeID, 1, neighbors, edges, neighborsMap);
			this.nodes[nodeID - 1] = currentNode;
		}
	}

	public Node getNode(int nodeID) {
		return this.nodes[nodeID - 1];
	}

	/*
	 * This function return n random node ID using Durstenfeld's algorithm
	 * Durstenfeld, R. (July 1964). "Algorithm 235: Random permutation
	 */
	public int[] getNRandomNodesIDs(int n) {
		int[] randomIDs = new int[n];
		int index = 0;
		try {
			index = this.shuffeledNodesIDs.length - 1;
		} catch (Exception e) {
			System.out.println("this.shuffeledNodesIDs.length = " + this.shuffeledNodesIDs.length);
		}

		Random rnd = new Random();
		int randomIndex = -1;
		int tmp;
		// for n iteration do
		// n-1 swaps
		for (int i = 0; i < n && index > 0; i++, index--) {
			// generate random Index
			randomIndex = rnd.nextInt(index);
			// swap values
			tmp = this.shuffeledNodesIDs[randomIndex];
			this.shuffeledNodesIDs[randomIndex] = this.shuffeledNodesIDs[index];
			this.shuffeledNodesIDs[index] = tmp;
			// store randomvalue to array
			randomIDs[i] = tmp;
		}
		return randomIDs;
	}

	/*
	 * Developed By Omar Mahmoud 27 July 2016 Returns HashSet of Nodes IDs
	 * Developed for GGGP algorithm
	 */
	public HashSet<Integer> getCopyOfNodesIDs() {
		HashSet<Integer> nodesIDsHashSet = new HashSet<Integer>(this.numberOfNodes);
		for (int i = 0; i < this.nodes.length; i++) {
			nodesIDsHashSet.add(this.nodes[i].getNodeID());
		}
		return nodesIDsHashSet;
	}

	public int getTotalNodesWeights() {
		int totalWeight = 0;
		for (int i = 0; i < this.nodes.length; i++) {
			totalWeight += this.nodes[i].getNodeWeight();
		}
		return totalWeight;
	}

	public void printGraph() {
		for (Map.Entry<Tuple<Integer, Integer>, Edge> entry : this.nodesEdgesMap.entrySet()) {
			Tuple<Integer, Integer> key = entry.getKey();
			Edge value = entry.getValue();

			System.out.println(key.x + "(" + this.getNode(key.x).getNodeWeight() + ") " + key.y + "("
					+ this.getNode(key.y).getNodeWeight() + ") " + value.getWeight());
		}
	}

	public int getOrder() {
		return numberOfNodes;
	}

	public long getDegree() {
		return numberOfEdges;
	}

	public long[][] getAdjacencyMatrix() {
		long[][] adjMatrix = new long[this.numberOfNodes][this.numberOfNodes];
		for (int i = 0; i < this.numberOfNodes - 1; i++) {
			int node1ID = i + 1;
			for (int j = i + 1; j < this.numberOfNodes; j++) {
				int node2ID = j + 1;
				Edge edge = this.getEdge(node1ID, node2ID);
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
	 * setters & getters
	 */
	public int getNumberOfNodes() {
		return numberOfNodes;
	}

	public void setNumberOfNodes(int numberOfNodes) {
		this.numberOfNodes = numberOfNodes;
	}

	public long getNumberOfEdges() {
		return numberOfEdges;
	}

	public void setNumberOfEdges(long numberOfEdges) {
		this.numberOfEdges = numberOfEdges;
	}

	public Node[] getNodes() {
		return nodes;
	}

	public void setNodes(Node[] nodes) {
		this.nodes = nodes;
	}

	public HashMap<Tuple<Integer, Integer>, Edge> getNodesEdgesMap() {
		return nodesEdgesMap;
	}

	public void setNodesEdgesMap(HashMap<Tuple<Integer, Integer>, Edge> nodesEdgesMap) {
		this.nodesEdgesMap = nodesEdgesMap;
	}

	public Edge[] getEdges() {
		return edges;
	}

	public void setEdges(Edge[] edges) {
		this.edges = edges;
	}

	public double[][] getLaplacianMatrix() {
		
		if (this.laplacianMatrix == null) {
			this.laplacianMatrix = new double[this.numberOfNodes][this.numberOfNodes];
			for (int i = 0; i < laplacianMatrix.length; i++) {
				int curNodeID = i + 1;
				Node curNode = this.getNode(curNodeID);
				Node[] neighbors = curNode.getNeighbors();
				// node weighted degree
				int totalEdgesWeights = 0;
				for (int j = 0; j < neighbors.length; j++) {
					int neighborID = neighbors[j].getNodeID();
					Edge edge = this.getEdge(curNodeID, neighborID);
					totalEdgesWeights += edge.getWeight();
					laplacianMatrix[i][neighborID-1]= -edge.getWeight(); 
				}
				laplacianMatrix[i][i] = totalEdgesWeights;
			}
		}
		return laplacianMatrix;
	}
	
public double[] getVertixWeightsDiagonalMatrix() {
		
		if (this.vertixWeightDiagonalMatrix == null) {
			this.vertixWeightDiagonalMatrix = new double[this.numberOfNodes];
			for (int i = 0; i < vertixWeightDiagonalMatrix.length; i++) {
				int curNodeID = i + 1;
				Node curNode = this.getNode(curNodeID);
				vertixWeightDiagonalMatrix[i] = curNode.getNodeWeight();
			}
		}
		return vertixWeightDiagonalMatrix;
	}

	public ArrayList<Integer> getNodeChilds(int nodeID) {
		ArrayList<Integer> childs = new ArrayList<Integer>(1);
		childs.add(nodeID);
		return childs;
	}

	public double[] getVertixDegreeDiagonalMatrix() {
		if (this.vertixDegreeDiagonalMatrix == null) {
			this.vertixDegreeDiagonalMatrix = new double[this.numberOfNodes];
			for (int i = 0; i < vertixDegreeDiagonalMatrix.length; i++) {
				int curNodeID = i + 1;
				Node curNode = this.getNode(curNodeID);
				Node[] neighbors = curNode.getNeighbors();
				// node weighted degree
				int totalEdgesWeights = 0;
				for (int j = 0; j < neighbors.length; j++) {
					int neighborID = neighbors[j].getNodeID();
					Edge edge = this.getEdge(curNodeID, neighborID);
					totalEdgesWeights += edge.getWeight(); 
				}
				vertixDegreeDiagonalMatrix[i] = totalEdgesWeights;
			}
		}
		return vertixDegreeDiagonalMatrix;
	}

}

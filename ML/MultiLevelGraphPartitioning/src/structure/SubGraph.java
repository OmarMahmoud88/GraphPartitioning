package structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class SubGraph extends Graph {

	private HashMap<Integer, Integer> subOriginalNodesMapping;
	private HashMap<Integer, Integer> originalSubNodesMapping;
	private Graph parentGraph;

	public SubGraph(Graph parentGraph, HashSet<Integer> nodesSubset) {
		this.parentGraph = parentGraph;
		this.numberOfNodes = nodesSubset.size();
		this.nodes = new Node[this.numberOfNodes];
		this.nodesEdgesMap = new HashMap<Tuple<Integer, Integer>, Edge>();
		// create original sub nodes mapping
		subOriginalNodesMapping = new HashMap<>(this.numberOfNodes);
		originalSubNodesMapping = new HashMap<>(this.numberOfNodes);
		HashSet<Integer> orgNodes = new HashSet<>(nodesSubset);
		Iterator<Integer> nodesIt = nodesSubset.iterator();
		int subIndex = 1;
		while (nodesIt.hasNext()) {
			int nodeID = nodesIt.next();
			subOriginalNodesMapping.put(subIndex, nodeID);
			originalSubNodesMapping.put(nodeID, subIndex);
			subIndex ++;
		}
		// create subgraph nodes
		for (int i = 0; i < nodes.length; i++) {
			int nodeID = i + 1;
			int orgNodeID = subOriginalNodesMapping.get(nodeID);
			int nodeWeight = this.parentGraph.getNode(orgNodeID).getNodeWeight();
			this.nodes[i] = new Node(nodeID, nodeWeight);
		}
		// get subgraph nodes' neighbors & edges
		for (int i = 0; i < this.numberOfNodes; i++) {
			int subNodeID = i + 1;
			int orgNodeID = this.subOriginalNodesMapping.get(subNodeID);
			Node orgNode = this.parentGraph.getNode(orgNodeID);
			Node[] orgNeighbors = orgNode.getNeighbors();
			ArrayList<Node> subNeighbors = new ArrayList<Node>();
			ArrayList<Edge> subEdges = new ArrayList<Edge>();
			HashMap<Integer, Tuple<Node, Edge>> neighborsMap = new HashMap<Integer, Tuple<Node, Edge>>();
			for (int j = 0; j < orgNeighbors.length; j++) {
				int orgNeighborID = orgNeighbors[j].getNodeID();
				if (orgNodes.contains(orgNeighborID)) {
					int subNeighborID = originalSubNodesMapping.get(orgNeighborID);
					Node subNeighbor = this.nodes[subNeighborID - 1];
					subNeighbors.add(subNeighbor);
					int sourceID = Math.min(subNodeID, subNeighborID);
					int destinationID = Math.max(subNodeID, subNeighborID);
					Edge subEdge = null;
					if (this.nodesEdgesMap.containsKey(new Tuple<Integer, Integer>(sourceID, destinationID))) {
						subEdge = this.nodesEdgesMap.get(new Tuple<Integer, Integer>(sourceID, destinationID));
					} else {
						int edgeWeight = this.parentGraph.getEdge(orgNodeID, orgNeighborID).getWeight();
						subEdge = new Edge(sourceID, destinationID, edgeWeight);
						this.nodesEdgesMap.put(new Tuple<Integer, Integer>(sourceID, destinationID), subEdge);
					}
					subEdges.add(subEdge);
					neighborsMap.put(subNeighborID, new Tuple<Node, Edge>(subNeighbor, subEdge));
				}
			}
			// convert to arrays
			Node[] subNeighborsArr = new Node[subNeighbors.size()];
			Edge[] subEdgesArr = new Edge[subEdges.size()];
			for (int j = 0; j < subNeighbors.size(); j++) {
				subNeighborsArr[j] = subNeighbors.get(j);
				subEdgesArr[j] = subEdges.get(j);
			}
			this.nodes[i].setNeighbors(subNeighborsArr);
			this.nodes[i].setNeighborsEdges(subEdgesArr);
			this.nodes[i].setNeighborsMap(neighborsMap);
			// fill edges array
			this.edges = new Edge[this.nodesEdgesMap.size()];
			Collection<Edge> edges = this.nodesEdgesMap.values();
			int index = 0;
			for (Edge ed : edges) {
				this.edges[index] = ed;
				index++;
			}
		}
		// initialize shuffled nodes
		this.shuffeledNodesIDs = new int[this.numberOfNodes];
		for (int i = 0; i < this.nodes.length; i++) {
			this.shuffeledNodesIDs[i] = i + 1;
		}
		// Store Edges and sort it by weight
		Arrays.sort(this.edges);

	}

	public int getOriginalNodeID(int subNodeID) {
		// TODO Auto-generated method stub
		return this.subOriginalNodesMapping.get(subNodeID);
	}
}

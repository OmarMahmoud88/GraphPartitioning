package coarsening;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import structure.Edge;
import structure.Graph;
import structure.Node;
import structure.RandomAccessIntHashSet;

public class OrderedHeavyEdgeMatching extends Matching {
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		// list all unvisited nodes
		int numberOfNodes = graph.getNumberOfNodes();
		IntOpenHashSet unvisitedNodes = new IntOpenHashSet(numberOfNodes);
		for (int i = 1; i < numberOfNodes + 1; i++) {
			unvisitedNodes.add(i);
		}

		IntArrayList degreesAvailable = new IntArrayList();
		ArrayList<RandomAccessIntHashSet> nodesTree = new ArrayList<RandomAccessIntHashSet>();

		// Store Nodes sorted in buckets of degrees
		Int2ObjectOpenHashMap<IntArrayList> nodesBuckets = new Int2ObjectOpenHashMap<IntArrayList>();

		Node[] allNodes = graph.getNodes();
		for (int i = 0; i < allNodes.length; i++) {
			int nodeID = allNodes[i].getNodeID();
			int nodeDegree = allNodes[i].getNumberOfNeighbors();
			if (!nodesBuckets.containsKey(nodeDegree)) {
				nodesBuckets.put(nodeDegree, new IntArrayList());
				degreesAvailable.add(nodeDegree);
			}
			nodesBuckets.get(nodeDegree).add(nodeID);
		}

		// sort Degrees
		Collections.sort(degreesAvailable);

		// Store Edges sorted in buckets
		ArrayList<ArrayList<Edge>> edgesBuckets = new ArrayList<ArrayList<Edge>>(numberOfNodes);
		for (int i = 0; i < numberOfNodes; i++) {
			edgesBuckets.add(new ArrayList<Edge>());
		}
		Edge[] allEdges = graph.getEdges();
		for (int i = 0; i < allEdges.length; i++) {
			int sourceID = allEdges[i].getSourceID();
			int destinationID = allEdges[i].getDestinationID();
			edgesBuckets.get(sourceID - 1).add(allEdges[i]);
			edgesBuckets.get(destinationID - 1).add(allEdges[i]);
		}

		// visit degrees in ascending order
		for (int i = 0; i < degreesAvailable.size(); i++) {
			int degree = degreesAvailable.getInt(i);
			IntArrayList nodesWithDegree = nodesBuckets.get(degree);
			// visit nodes randomly
			Collections.shuffle(nodesWithDegree);
			while (!nodesWithDegree.isEmpty()) {
				int nodeID = nodesWithDegree.getInt(0);
				nodesWithDegree.removeInt(0);
				if(!unvisitedNodes.contains(nodeID)){
					continue;
				}
				// get heaviest edge to unvisited node
				// if none exist collapse the node by itself
				// and mark it as visited
				Edge heavyEdge = null;
				for (int j = edgesBuckets.get(nodeID - 1).size() - 1; j >= 0; j--) {
					heavyEdge = edgesBuckets.get(nodeID - 1).get(j);
					int sourceNodeID = heavyEdge.getSourceID();
					int destNodeID = heavyEdge.getDestinationID();
					int pairWeight = graph.getNode(sourceNodeID).getNodeWeight() + graph.getNode(destNodeID).getNodeWeight();
					int otherNodeID = sourceNodeID + destNodeID - nodeID;
					// check if the other node is visited
					if (!unvisitedNodes.contains(nodeID) ||!unvisitedNodes.contains(otherNodeID) || pairWeight > maxPartitionWeight) {
						heavyEdge = null;
						continue;
					}
					// if not visited before
					break;
				}
				
				RandomAccessIntHashSet nodeChilds;
				// check if any Edge was selected
				if (heavyEdge == null) {
					// collapse Node by itself
					nodeChilds = new RandomAccessIntHashSet();
					nodeChilds.add(nodeID);
					nodesTree.add(nodeChilds);
					unvisitedNodes.remove(nodeID);
				} else {
					nodeChilds = new RandomAccessIntHashSet();
					nodeChilds.add(heavyEdge.getSourceID());
					nodeChilds.add(heavyEdge.getDestinationID());
					nodesTree.add(nodeChilds);
					unvisitedNodes.remove(heavyEdge.getSourceID());
					unvisitedNodes.remove(heavyEdge.getDestinationID());
				}
				
			}
		}

		// iterate through remaining nodes, and collapse them by themselves
		Iterator<Integer> it = unvisitedNodes.iterator();
		while (it.hasNext()) {
			int singleNodeID = it.next();
			RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
			parentNode.add(singleNodeID);
			nodesTree.add(parentNode);
		}

		return nodesTree;
	}
}

package coarsening;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import structure.Edge;
import structure.Graph;
import structure.RandomSet;

/*
 * Heavy Edge Matching
 * Visit graph Node at random Order
 * Choose the heaviest Edge for visited node
 * mark both ends for edge
 * repeat process
 */

public class HeavyEdgeMatching extends Matching{

	public ArrayList<RandomSet<Integer>> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		// list all unvisited nodes
		int numberOfNodes = graph.getNumberOfNodes();
		ArrayList<Integer> unvisitedNodes = new ArrayList<Integer>(
				numberOfNodes);
		HashSet<Integer> visitedNodes = new HashSet<Integer>(numberOfNodes);
		ArrayList<RandomSet<Integer>> nodesTree = new ArrayList<RandomSet<Integer>>();
		for (int i = 0; i < numberOfNodes; i++) {
			unvisitedNodes.add(i + 1);
		}

		// Store Edges sorted in buckets
		ArrayList<ArrayList<Edge>> edgesBuckets = new ArrayList<ArrayList<Edge>>(
				numberOfNodes);
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

		// Randomize Nodes Access
		Collections.shuffle(unvisitedNodes);

		// get heaviest edge for each node visited
		int currentNodeID;
		while (!unvisitedNodes.isEmpty()) {
			currentNodeID = unvisitedNodes.get(0);
			// check if current node is visited before
			if (visitedNodes.contains(currentNodeID)) {
				unvisitedNodes.remove(0);
				continue;
			}
			int sourceNodeID, destNodeID, otherNodeID;
			Edge heavyEdge = null;
			// get heaviest edge to unvisited node
			// if none exist collapse the node by itself
			// and mark it as visited
			for (int j = edgesBuckets.get(currentNodeID - 1).size() - 1; j >= 0; j--) {
				heavyEdge = edgesBuckets.get(currentNodeID - 1).get(j);
				sourceNodeID = heavyEdge.getSourceID();
				destNodeID = heavyEdge.getDestinationID();
				int pairWeight = graph.getNode(sourceNodeID).getNodeWeight() + graph.getNode(destNodeID).getNodeWeight();
				otherNodeID = sourceNodeID + destNodeID - currentNodeID;
				// check if the other node is visited
				if (visitedNodes.contains(otherNodeID) || pairWeight > maxPartitionWeight) {
					heavyEdge = null;
					continue;
				}
				// if not visited before
				break;
			}
			RandomSet<Integer> nodeChilds;
			// check if any Edge was selected
			if (heavyEdge == null) {
				// collapse Node by itself
				nodeChilds = new RandomSet<Integer>();
				nodeChilds.add(currentNodeID);
				nodesTree.add(nodeChilds);
				visitedNodes.add(currentNodeID);
			} else {
				nodeChilds = new RandomSet<Integer>();
				nodeChilds.add(heavyEdge.getSourceID());
				nodeChilds.add(heavyEdge.getDestinationID());
				nodesTree.add(nodeChilds);
				visitedNodes.add(heavyEdge.getSourceID());
				visitedNodes.add(heavyEdge.getDestinationID());
			}
			unvisitedNodes.remove(0);
		}

		return nodesTree;
	}
}

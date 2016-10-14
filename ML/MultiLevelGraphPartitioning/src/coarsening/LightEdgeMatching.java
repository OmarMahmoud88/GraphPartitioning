package coarsening;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import structure.Edge;
import structure.Graph;

/*
 * Light Edge Matching
 * Visit graph Node at random Order
 * Choose the lightest Edge for visited node
 * mark both ends for edge
 * repeat process
 */

public class LightEdgeMatching extends Matching {

	@Override
	public ArrayList<ArrayList<Integer>> coarse(Graph graph, int outputGraphNumOfNodes) {
		// list all unvisited nodes
		int numberOfNodes = graph.getNumberOfNodes();
		ArrayList<Integer> unvisitedNodes = new ArrayList<Integer>(numberOfNodes);
		HashSet<Integer> visitedNodes = new HashSet<Integer>(numberOfNodes);
		ArrayList<ArrayList<Integer>> nodesTree = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < numberOfNodes; i++) {
			unvisitedNodes.add(i + 1);
		}

		// Store Edges sorted in buckets
		ArrayList<ArrayList<Edge>> edgesBuckets = new ArrayList<ArrayList<Edge>>(numberOfNodes);
		for (int i = 0; i < numberOfNodes; i++) {
			edgesBuckets.add(new ArrayList<Edge>());
		}
		Edge[] allEdges = graph.getEdges();
		for (int i = 0; i < allEdges.length; i++) {
			int sourceID = allEdges[i].getSourceID();
			int destinationID = allEdges[i].getDestinationID();
			// System.out.println(sourceID);
			edgesBuckets.get(sourceID - 1).add(allEdges[i]);
			edgesBuckets.get(destinationID - 1).add(allEdges[i]);
		}

		// Randomize Nodes Access
		Collections.shuffle(unvisitedNodes);

		// get lightest edge for each node visited
		int currentNodeID;
		while (!unvisitedNodes.isEmpty()) {
			currentNodeID = unvisitedNodes.get(0);
			// check if current node is visited before
			if (visitedNodes.contains(currentNodeID)) {
				unvisitedNodes.remove(0);
				continue;
			}
			int sourceNodeID, destNodeID, otherNodeID;
			Edge lightEdge = null;
			// get heaviest edge to unvisited node
			// if none exist collapse the node by itself
			// and mark it as visited
			for (int j = 0; j < edgesBuckets.get(currentNodeID - 1).size(); j++) {
				lightEdge = edgesBuckets.get(currentNodeID - 1).get(j);
				sourceNodeID = lightEdge.getSourceID();
				destNodeID = lightEdge.getDestinationID();
				otherNodeID = sourceNodeID + destNodeID - currentNodeID;
				// check if the other node is visited
				if (visitedNodes.contains(otherNodeID)) {
					lightEdge = null;
					continue;
				}
				// if not visited before
				break;
			}
			ArrayList<Integer> nodeChilds;
			// check if any Edge was selected
			if (lightEdge == null) {
				// collapse Node by itself
				nodeChilds = new ArrayList<Integer>();
				nodeChilds.add(currentNodeID);
				nodesTree.add(nodeChilds);
				visitedNodes.add(currentNodeID);
			} else {
				nodeChilds = new ArrayList<Integer>();
				nodeChilds.add(lightEdge.getSourceID());
				nodeChilds.add(lightEdge.getDestinationID());
				nodesTree.add(nodeChilds);
				visitedNodes.add(lightEdge.getSourceID());
				visitedNodes.add(lightEdge.getDestinationID());
			}
			unvisitedNodes.remove(0);
		}

		return nodesTree;
	}

}

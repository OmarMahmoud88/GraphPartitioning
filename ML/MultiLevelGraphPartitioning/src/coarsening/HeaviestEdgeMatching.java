package coarsening;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import structure.Edge;
import structure.Graph;

/*
 * Heaviest Edge Matching
 * Add the heaviest edge with unvisited ends first
 * It is a simple greedy algorithm, which has approximation ratio of 1/2
 */

public class HeaviestEdgeMatching extends Matching {

	@Override
	public ArrayList<ArrayList<Integer>> coarse(Graph graph) {
		// list all unvisited nodes
		int numberOfNodes = graph.getNumberOfNodes();
		HashSet<Integer> unvisitedNodes = new HashSet<Integer>(numberOfNodes);
		HashSet<Integer> visitedNodes = new HashSet<Integer>(numberOfNodes);
		ArrayList<ArrayList<Integer>> nodesTree = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < numberOfNodes; i++) {
			unvisitedNodes.add(i + 1);
		}

		// the edges is sorted, so we loop them from heavier to lighter
		// if any edge has any of its ends visited before skip it
		Edge[] allEdges = graph.getEdges();
		for (int i = allEdges.length - 1; i >= 0; i--) {
			int sourceID = allEdges[i].getSourceID();
			int destinationID = allEdges[i].getDestinationID();
			// check if the source or the destination is visited before
			if (visitedNodes.contains(sourceID) || visitedNodes.contains(destinationID)) {
				// do not thing
				continue;
			} else {
				// Collapse Edge ends
				ArrayList<Integer> parentNode = new ArrayList<>(2);
				parentNode.add(sourceID);
				parentNode.add(destinationID);
				nodesTree.add(parentNode);
				// add nodes to visited nodes
				visitedNodes.add(sourceID);
				visitedNodes.add(destinationID);
				unvisitedNodes.remove(destinationID);
				unvisitedNodes.remove(sourceID);
			}
		}

		// add remaining Nodes as parents
		for (Iterator<Integer> it = unvisitedNodes.iterator(); it.hasNext();) {
		    int nodeID = it.next();
		    ArrayList<Integer> parentNode = new ArrayList<>(1);
			parentNode.add(nodeID);
			nodesTree.add(parentNode);
			// add nodes to visited nodes
			visitedNodes.add(nodeID);
		    it.remove();
		}
		
		return nodesTree;
	}

}

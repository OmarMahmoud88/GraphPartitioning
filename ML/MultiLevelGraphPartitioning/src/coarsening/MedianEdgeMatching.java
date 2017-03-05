package coarsening;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import structure.Edge;
import structure.Graph;
import structure.RandomAccessIntHashSet;

/*
 * Heaviest Edge Matching
 * Add the heaviest edge with unvisited ends first
 * It is a simple greedy algorithm, which has approximation ratio of 1/2
 */

public class MedianEdgeMatching extends Matching {

	@Override
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		// list all unvisited nodes
		int numberOfNodes = graph.getNumberOfNodes();
		HashSet<Integer> unvisitedNodes = new HashSet<Integer>(numberOfNodes);
		HashSet<Integer> visitedNodes = new HashSet<Integer>(numberOfNodes);
		ArrayList<RandomAccessIntHashSet> nodesTree = new ArrayList<RandomAccessIntHashSet>();
		for (int i = 0; i < numberOfNodes; i++) {
			unvisitedNodes.add(i + 1);
		}
		Edge[] allEdges = graph.getEdges();
		// Add the median edge each time
		// if edges number is odd
		if (allEdges.length % 2 == 1) {
			int index = allEdges.length / 2;
			int sourceID = allEdges[index].getSourceID();
			int destinationID = allEdges[index].getDestinationID();
			int pairWeight = graph.getNode(sourceID).getNodeWeight() + graph.getNode(destinationID).getNodeWeight();
			// check if the source or the destination is visited before
			// check if the pair was matched will exceed max partition weight
			if (visitedNodes.contains(sourceID) || visitedNodes.contains(destinationID)
					|| pairWeight > maxPartitionWeight) {
				// do not thing
			} else {
				// Collapse Edge ends
				RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
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

		for (int i = (allEdges.length - 1) / 2; i >= 0; i--) {
			// the first half nodes
			int sourceID = allEdges[i].getSourceID();
			int destinationID = allEdges[i].getDestinationID();
			int pairWeight = graph.getNode(sourceID).getNodeWeight() + graph.getNode(destinationID).getNodeWeight();
			// check if the source or the destination is visited before
			// check if the pair was matched will exceed max partition weight
			if (visitedNodes.contains(sourceID) || visitedNodes.contains(destinationID)
					|| pairWeight > maxPartitionWeight) {
				// do not thing
				continue;
			} else {
				// Collapse Edge ends
				RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
				parentNode.add(sourceID);
				parentNode.add(destinationID);
				nodesTree.add(parentNode);
				// add nodes to visited nodes
				visitedNodes.add(sourceID);
				visitedNodes.add(destinationID);
				unvisitedNodes.remove(destinationID);
				unvisitedNodes.remove(sourceID);
			}
			// the second half nodes
			int index = allEdges.length - i - 1;
			sourceID = allEdges[index].getSourceID();
			destinationID = allEdges[index].getDestinationID();
			pairWeight = graph.getNode(sourceID).getNodeWeight() + graph.getNode(destinationID).getNodeWeight();
			// check if the source or the destination is visited before
			// check if the pair was matched will exceed max partition weight
			if (visitedNodes.contains(sourceID) || visitedNodes.contains(destinationID)
					|| pairWeight > maxPartitionWeight) {
				// do not thing
				continue;
			} else {
				// Collapse Edge ends
				RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
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
			RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
			parentNode.add(nodeID);
			nodesTree.add(parentNode);
			// add nodes to visited nodes
			visitedNodes.add(nodeID);
			it.remove();
		}

		return nodesTree;
	}

}

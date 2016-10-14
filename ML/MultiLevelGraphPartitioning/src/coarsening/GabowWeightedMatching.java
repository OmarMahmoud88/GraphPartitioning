package coarsening;

import java.util.ArrayList;
import java.util.HashSet;

import structure.Graph;
import utilities.WeightedMatchLong;

public class GabowWeightedMatching extends Matching {

	@Override
	public ArrayList<ArrayList<Integer>> coarse(Graph graph, int outputGraphNumOfNodes) {
		ArrayList<ArrayList<Integer>> nodesTree = new ArrayList<ArrayList<Integer>>();
		HashSet<Integer> matchedNodes = new HashSet<Integer>();
		// create adjacency matrix
		long[][] costs = graph.getAdjacencyMatrix();
		int[] mate = WeightedMatchLong.weightedMatchLong(costs, WeightedMatchLong.MINIMIZE);
		for (int i = 1; i <= graph.getNumberOfNodes(); i++) {
			ArrayList<Integer> parentNode = new ArrayList<Integer>();
			if(matchedNodes.contains(i)) continue;
			parentNode.add(i);
			matchedNodes.add(i);
			if (mate[i] != 0) {
				parentNode.add(mate[i]);
				matchedNodes.add(mate[i]);
			}
			nodesTree.add(parentNode);
		}
		return nodesTree;
	}

}

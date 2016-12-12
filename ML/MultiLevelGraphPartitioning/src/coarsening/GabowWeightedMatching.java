package coarsening;

import java.util.ArrayList;
import java.util.HashSet;

import structure.Graph;
import structure.RandomSet;
import utilities.WeightedMatchLong;

public class GabowWeightedMatching extends Matching {

	@Override
	public ArrayList<RandomSet<Integer>> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		ArrayList<RandomSet<Integer>> nodesTree = new ArrayList<RandomSet<Integer>>();
		HashSet<Integer> matchedNodes = new HashSet<Integer>();
		// create adjacency matrix
		long[][] costs = graph.getAdjacencyMatrix();
		int[] mate = WeightedMatchLong.weightedMatchLong(costs, WeightedMatchLong.MINIMIZE);
		for (int i = 1; i <= graph.getNumberOfNodes(); i++) {
			RandomSet<Integer> parentNode = new RandomSet<Integer>();
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

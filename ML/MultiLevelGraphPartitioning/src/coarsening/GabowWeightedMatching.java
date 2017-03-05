package coarsening;

import java.util.ArrayList;
import java.util.HashSet;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import structure.Graph;
import structure.RandomAccessIntHashSet;
import utilities.WeightedMatchLong;

public class GabowWeightedMatching extends Matching {

	@Override
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		ArrayList<RandomAccessIntHashSet> nodesTree = new ArrayList<RandomAccessIntHashSet>();
		HashSet<Integer> matchedNodes = new HashSet<Integer>();
		// create adjacency matrix
		long[][] costs = graph.getAdjacencyMatrix();
		int[] mate = WeightedMatchLong.weightedMatchLong(costs, WeightedMatchLong.MINIMIZE);
		for (int i = 1; i <= graph.getNumberOfNodes(); i++) {
			RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
			if (matchedNodes.contains(i))
				continue;
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

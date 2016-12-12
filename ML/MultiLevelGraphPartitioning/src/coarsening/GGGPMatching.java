package coarsening;

import java.util.ArrayList;

import partitioning.GreedyGraphGrowingPartitioning;
import partitioning.Partitioning;
import structure.Graph;
import structure.PartitionGroup;
/*
 * This class uses Greed Graph Growing algorithms to partition the graph to number of nodes,
 * after that we use each partition as cluster (matching set) for further enhancement. 
 */
import structure.RandomSet;

public class GGGPMatching extends Matching {

	@Override
	public ArrayList<RandomSet<Integer>> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		Partitioning gGGP = new GreedyGraphGrowingPartitioning(graph, outputGraphNumOfNodes, 20, (float) 0.1);
		PartitionGroup partsGroup = gGGP.getPartitions(graph, null,  outputGraphNumOfNodes, 20);
		return partsGroup.getAllPartitionsNodes();
	}

}

package coarsening;

import java.util.ArrayList;

import partitioning.GreedyGraphGrowingPartitioning;
import partitioning.Partitioning;
import partitioning.SpectralPartitioningGraphCutFiedlerVector;
import partitioning.SpectralPartitioningNormalizedCutFiedlerVector;
import structure.Graph;
import structure.PartitionGroup;
/*
 * This class uses Greed Graph Growing algorithms to partition the graph to number of nodes,
 * after that we use each partition as cluster (matching set) for further enhancement. 
 */

public class SpectralNormalizedCutFiedlerVectorMatching extends Matching {

	@Override
	public ArrayList<ArrayList<Integer>> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		Partitioning parting = new SpectralPartitioningNormalizedCutFiedlerVector(graph, outputGraphNumOfNodes, 20, (float) 0.1);
		PartitionGroup partsGroup = parting.getPartitions(graph, outputGraphNumOfNodes, 20);
		return partsGroup.getAllPartitionsNodes();
	}

}

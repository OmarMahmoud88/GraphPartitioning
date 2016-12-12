package coarsening;

import java.util.ArrayList;

import partitioning.Partitioning;
import partitioning.SpectralPartitioningNormalizedCutFiedlerVector;
import structure.Graph;
import structure.PartitionGroup;
import structure.RandomSet;
/*
 * This class uses Greed Graph Growing algorithms to partition the graph to number of nodes,
 * after that we use each partition as cluster (matching set) for further enhancement. 
 */

public class SpectralNormalizedCutFiedlerVectorMatching extends Matching {

	@Override
	public ArrayList<RandomSet<Integer>> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		Partitioning parting = new SpectralPartitioningNormalizedCutFiedlerVector(graph, outputGraphNumOfNodes, 20, (float) 0.1);
		PartitionGroup partsGroup = parting.getPartitions(graph, null, outputGraphNumOfNodes, 20);
		return partsGroup.getAllPartitionsNodes();
	}

}

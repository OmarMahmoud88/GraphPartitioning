package coarsening;

import java.util.ArrayList;

import partitioning.Partitioning;
import partitioning.SpectralPartitioningNormalizedCutFiedlerVector;
import structure.Graph;
import structure.PartitionGroup;
/*
 * This class uses Greed Graph Growing algorithms to partition the graph to number of nodes,
 * after that we use each partition as cluster (matching set) for further enhancement. 
 */
import structure.RandomAccessIntHashSet;

public class SpectralNormalizedCutFiedlerVectorMatching extends Matching {

	@Override
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		Partitioning parting = new SpectralPartitioningNormalizedCutFiedlerVector(graph, null, outputGraphNumOfNodes,
				20, (float) 0.1);
		PartitionGroup partsGroup = parting.getPartitions();
		return partsGroup.getAllPartitionsNodes();
	}

}

package coarsening;

import java.util.ArrayList;

import partitioning.Partitioning;
import partitioning.SpectralPartitioningGraphCutFiedlerVector;
import structure.Graph;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;

public class SpectralGraphCutFiedlerVectorMatching extends Matching {

	@Override
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		Partitioning parting = new SpectralPartitioningGraphCutFiedlerVector(graph, outputGraphNumOfNodes, 20, (float) 0.1);
		PartitionGroup partsGroup = parting.getPartitions(graph, null, outputGraphNumOfNodes, 20);
		return partsGroup.getAllPartitionsNodes();
	}

}

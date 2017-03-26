package coarsening;

import java.util.ArrayList;

import partitioning.Partitioning;
import partitioning.SpectralPartitioningGraphCutEigenVector;
import structure.Graph;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;

public class SpectralGraphCutEigenVectorMatching extends Matching {

	@Override
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		Partitioning parting = new SpectralPartitioningGraphCutEigenVector(graph, null, outputGraphNumOfNodes, 20, (float) 0.1);
		PartitionGroup partsGroup = parting.getPartitions();
		return partsGroup.getAllPartitionsNodes();
	}

}

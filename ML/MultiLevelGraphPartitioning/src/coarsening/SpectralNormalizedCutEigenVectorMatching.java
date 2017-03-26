package coarsening;

import java.util.ArrayList;

import partitioning.Partitioning;
import partitioning.SpectralPartitioningNormalizedCutEigenVector;
import structure.Graph;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;

public class SpectralNormalizedCutEigenVectorMatching extends Matching {

	@Override
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		Partitioning parting = new SpectralPartitioningNormalizedCutEigenVector(graph, null, outputGraphNumOfNodes, 20, (float) 0.1);
		PartitionGroup partsGroup = parting.getPartitions();
		return partsGroup.getAllPartitionsNodes();
	}

}

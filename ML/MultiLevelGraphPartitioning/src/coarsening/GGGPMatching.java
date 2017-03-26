package coarsening;

import java.util.ArrayList;

import partitioning.GreedyGraphGrowingPartitioning;
import partitioning.Partitioning;
import structure.Graph;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;

public class GGGPMatching extends Matching {

	@Override
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		Partitioning gGGP = new GreedyGraphGrowingPartitioning(graph, null, outputGraphNumOfNodes, 20, (float) 0.1);
		PartitionGroup partsGroup = gGGP.getPartitions();
		return partsGroup.getAllPartitionsNodes();
	}

}

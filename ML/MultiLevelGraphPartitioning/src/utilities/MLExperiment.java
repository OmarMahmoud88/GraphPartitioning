package utilities;

import structure.Graph;

public class MLExperiment {
	private Graph graph;
	private CoarseSchemes coarseScheme;
	private int estimateNumOfNodesInCoarsenGraph;
	private int exactNumOfNodesInCoarsenGraph;
	private PartitioningSchemes partitioningScheme;
	private int edgeCutBeforeRefinement;
	private int edgeCutAfterRefinement;
	private int imbalanceBeforeRefinement;
	private int imbalanceAfterRefinement;
	private int numberOfPartitions;
	private float imbalanceRatioAllowed;

	public MLExperiment(Graph graph, CoarseSchemes coarseScheme, int estimateNumOfNodesInCoarsenGraph,
			PartitioningSchemes partitioningScheme, int numberOfPartitions, float imbalanceRatioAllowed) {

	}
}

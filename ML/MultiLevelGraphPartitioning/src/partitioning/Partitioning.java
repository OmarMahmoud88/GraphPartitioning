package partitioning;

import structure.Graph;
import structure.PartitionGroup;

public abstract class Partitioning {
	protected Graph graph;
	protected int numberOfPartitions, numberOfTrials;
	protected float imbalanceRatio;
	protected int maxPartitionWeight, minPartitionWeight;

	public Partitioning(Graph graph, int numberOfPartitions, int numberOfTrials, float imbalanceRatio) {
		this.graph = graph;
		this.numberOfPartitions = numberOfPartitions;
		this.numberOfTrials = numberOfTrials;
		this.imbalanceRatio = imbalanceRatio;
		int totalNodesWeight = this.graph.getTotalNodesWeights();
		float exactPartitionWeight = (float) totalNodesWeight / numberOfPartitions;
		this.maxPartitionWeight = (int) (Math.ceil((double) totalNodesWeight / numberOfPartitions)
				* (1 + imbalanceRatio));
		this.maxPartitionWeight = (int) Math.ceil((1 + imbalanceRatio) * Math.ceil(exactPartitionWeight));
		this.minPartitionWeight = Math.max((int) Math.floor((1 - imbalanceRatio) * Math.floor(exactPartitionWeight)), 1);
	}

	public abstract PartitionGroup getPartitions(Graph gr, int numberOfPartitions, int numberOfTries);
	
}

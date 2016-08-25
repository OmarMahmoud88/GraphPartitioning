package refinement;

import java.util.HashMap;
import java.util.Iterator;

import structure.Edge;
import structure.Graph;
import structure.KLPair;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;

public class NaiiveKLRefinement {

	private Graph graph;
	private int numberOfPartitions;
	private int maxSwaps;
	private int numberOfSwapsApplied;
	private int minGainAllowed;
	private int minPartitionWeight;
	private int maxPartitionWeight;
	private PartitionGroup refinedPartitions;
	private HashMap<Integer, Integer> nodePartitionMap;

	public NaiiveKLRefinement(Graph graph, PartitionGroup partsGroup, int maxSwaps, int minGainAllowed,
			float imbalanceRatio) {
		// assign attributes
		this.graph = graph;
		this.maxSwaps = maxSwaps;
		this.minGainAllowed = minGainAllowed;
		this.numberOfSwapsApplied = 0;
		int totalGraphWeight = this.graph.getTotalNodesWeights();
		this.numberOfPartitions = partsGroup.getPartitionNumber();
		float exactPartitionWeight = (float) totalGraphWeight / this.numberOfPartitions;
		this.maxPartitionWeight = (int) Math.ceil((1 + imbalanceRatio) * (exactPartitionWeight));
		this.minPartitionWeight = (int) Math.floor((1 - imbalanceRatio) * (exactPartitionWeight));
		this.refinedPartitions = partsGroup;
		// create node -> partition map
		this.nodePartitionMap = new HashMap<Integer, Integer>(this.graph.getNumberOfNodes());
		for (int i = 0; i < this.refinedPartitions.getPartitionNumber(); i++) {
			int partID = i + 1;
			Partition partition = this.refinedPartitions.getPartition(partID);
			int partitionID = partition.getPartitionID();
			Iterator<Integer> partIt = partition.getNodeIDs().iterator();
			while (partIt.hasNext()) {
				int nodeID = partIt.next();
				this.nodePartitionMap.put(nodeID, partitionID);
			}
		}
		// begin refinement
		this.refinePartitions();

	}

	private void refinePartitions() {
		KLPair pairWithMaxGain;

		// terminate if max swaps reached
		while (this.numberOfSwapsApplied < this.maxSwaps) {
			pairWithMaxGain = this.getPairWithMaxGain();
			if (pairWithMaxGain == null) {
				// no more swaps allowed found
				break;
			}
			this.swap(pairWithMaxGain);
			this.numberOfSwapsApplied++;
		}
	}

	private void swap(KLPair pairWithMaxGain) {
		int node1ID = pairWithMaxGain.getSourceID();
		int node2ID = pairWithMaxGain.getDestinationID();
		// System.out.print("(" + node1ID + "," + node2ID + ")");
		// System.out.print(" || cut gain = " +
		// pairWithMaxGain.getEdgeCutGain());
		// System.out.println(" || balance gain = " +
		// pairWithMaxGain.getBalanceGain());
		// update partitions
		int partition1ID = this.nodePartitionMap.get(node1ID);
		int partition2ID = this.nodePartitionMap.get(node2ID);
		Partition partition1 = this.refinedPartitions.getPartition(partition1ID);
		Partition partition2 = this.refinedPartitions.getPartition(partition2ID);
		partition1.removeNode(node1ID);
		partition2.addNode(node1ID);
		partition2.removeNode(node2ID);
		partition1.addNode(node2ID);
		// update the map
		this.nodePartitionMap.put(node1ID, partition2ID);
		this.nodePartitionMap.put(node2ID, partition1ID);

	}

	private KLPair getPairWithMaxGain() {
		KLPair bestPair = null;
		int maxCutGain = Integer.MIN_VALUE;
		int maxCutBalance = Integer.MIN_VALUE;
		int maxGainNode1ID = -1;
		int maxGainNode2ID = -1;
		int partsNum = this.refinedPartitions.getPartitionNumber();
		for (int i = 1; i < partsNum; i++) {
			Partition part1 = this.refinedPartitions.getPartition(i);
			for (int j = i + 1; j < partsNum + 1; j++) {
				Partition part2 = this.refinedPartitions.getPartition(j);
				Iterator<Integer> it1 = part1.getNodeIDs().iterator();

				while (it1.hasNext()) {
					int node1ID = it1.next();
					Iterator<Integer> it2 = part2.getNodeIDs().iterator();
					while (it2.hasNext()) {
						int node2ID = it2.next();
						int cutGain = this.getEdgeCutGain(node1ID, node2ID);
						int balanceGain = this.getBalanceGain(node1ID, node2ID);
						if (balanceGain >= 0) {
							if (cutGain >= this.minGainAllowed) {
								if (cutGain > maxCutGain) {
									maxCutGain = cutGain;
									maxCutBalance = balanceGain;
									maxGainNode1ID = node1ID;
									maxGainNode2ID = node2ID;
								} else if (cutGain == maxCutGain) {
									if (balanceGain > maxCutBalance) {
										maxCutGain = cutGain;
										maxCutBalance = balanceGain;
										maxGainNode1ID = node1ID;
										maxGainNode2ID = node2ID;
									}
								}
							}
						}
					}
				}

			}
		}

		if (maxGainNode1ID > 0) {
			int minNodeID = Math.min(maxGainNode1ID, maxGainNode2ID);
			int maxNodeID = Math.max(maxGainNode1ID, maxGainNode2ID);
			bestPair = new KLPair(minNodeID, maxNodeID, maxCutGain, maxCutBalance);
		}
		return bestPair;
	}

	private int getBalanceGain(int node1ID, int node2ID) {
		int partition1ID = this.nodePartitionMap.get(node1ID);
		int partition2ID = this.nodePartitionMap.get(node2ID);
		int partition1Weight = this.refinedPartitions.getPartition(partition1ID).getPartitionWeight();
		int partition2Weight = this.refinedPartitions.getPartition(partition2ID).getPartitionWeight();
		int node1Weight = this.graph.getNode(node1ID).getNodeWeight();
		int node2Weight = this.graph.getNode(node2ID).getNodeWeight();
		int partition1NewWeight = partition1Weight + node2Weight - node1Weight;
		int partition2NewWeight = partition2Weight + node1Weight - node2Weight;
		int partition1OldImbalance = 0;
		int partition1NewImbalance = 0;
		int partition2OldImbalance = 0;
		int partition2NewImbalance = 0;
		// calculate old&New imbalance
		partition1OldImbalance = this.getWeightImbalance(partition1Weight);
		partition1NewImbalance = this.getWeightImbalance(partition1NewWeight);
		partition2OldImbalance = this.getWeightImbalance(partition2Weight);
		partition2NewImbalance = this.getWeightImbalance(partition2NewWeight);
		// calculate balance gain
		int balanceGain = (partition1OldImbalance - partition1NewImbalance)
				+ (partition2OldImbalance - partition2NewImbalance);

		return balanceGain;
	}

	private int getWeightImbalance(int partitionWeight) {
		int imbalance = 0;
		if (partitionWeight > this.maxPartitionWeight) {
			imbalance = this.maxPartitionWeight - partitionWeight;
		} else if (partitionWeight < this.minPartitionWeight) {
			imbalance = partitionWeight - this.minPartitionWeight;
		}
		return imbalance;
	}

	private int getEdgeCutGain(int node1ID, int node2ID) {
		final int partition1ID = this.nodePartitionMap.get(node1ID);
		final int partition2ID = this.nodePartitionMap.get(node2ID);
		int gain = 0;
		// gain if node1 transferred to partition2
		Node[] node1Neighbors = this.graph.getNode(node1ID).getNeighbors();
		for (int i = 0; i < node1Neighbors.length; i++) {
			int neighborID = node1Neighbors[i].getNodeID();
			int neighborPartitionID = this.nodePartitionMap.get(neighborID);
			int edgeWeight = this.graph.getEdge(node1ID, neighborID).getWeight();
			if (neighborPartitionID == partition1ID) {
				gain -= edgeWeight;
			} else if (neighborPartitionID == partition2ID) {
				gain += edgeWeight;
			}
		}

		// gain if node2 transferred to partition1
		Node[] node2Neighbors = this.graph.getNode(node2ID).getNeighbors();
		for (int i = 0; i < node2Neighbors.length; i++) {
			int neighborID = node2Neighbors[i].getNodeID();
			int neighborPartitionID = this.nodePartitionMap.get(neighborID);
			int edgeWeight = this.graph.getEdge(node2ID, neighborID).getWeight();
			if (neighborPartitionID == partition1ID) {
				gain += edgeWeight;
			} else if (neighborPartitionID == partition2ID) {
				gain -= edgeWeight;
			}
		}
		// get edge between node1 and node2 if exist
		Edge edge12 = this.graph.getEdge(node1ID, node2ID);
		if (edge12 != null) {
			int edgeWeight = edge12.getWeight();
			gain -= 2 * edgeWeight;
		}
		return gain;
	}

	public PartitionGroup getRefinedPartitions() {
		return refinedPartitions;
	}

	public int getNumberOfSwapsApplied() {
		return numberOfSwapsApplied;
	}
}

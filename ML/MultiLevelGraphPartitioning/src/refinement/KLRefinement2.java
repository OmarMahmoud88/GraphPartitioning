package refinement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import structure.Edge;
import structure.Graph;
import structure.KLPair;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;

public class KLRefinement2 {

	private Graph graph;
	private int numberOfPartitions;
	private int maxSwaps;
	private int numberOfSwapsApplied;
	private int minGainAllowed;
	private int minPartitionWeight;
	private int maxPartitionWeight;
	private PartitionGroup refinedPartitions;
	private HashMap<Integer, Integer> nodePartitionMap;
	private ArrayList<HashSet<Integer>> borderNodes;
	private PartitionGroup bestParts;
	private HashSet<Integer> negativeSwappedNodes;

	public KLRefinement2(Graph graph, PartitionGroup partsGroup, int maxSwaps, int minGainAllowed,
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
		// construct border nodes list
		this.borderNodes = new ArrayList<HashSet<Integer>>(this.refinedPartitions.getPartitionNumber());
		for (int i = 0; i < this.numberOfPartitions; i++) {
			this.borderNodes.add(new HashSet<Integer>());
		}
		for (int i = 0; i < this.refinedPartitions.getPartitionNumber(); i++) {
			int partID = i + 1;
			Partition partition = this.refinedPartitions.getPartition(partID);
			Iterator<Integer> partIt = partition.getNodeIDs().iterator();
			while (partIt.hasNext()) {
				int nodeID = partIt.next();
				boolean borderNode = false;
				Node cur = this.graph.getNode(nodeID);
				Node[] neighbors = cur.getNeighbors();
				for (int j = 0; j < neighbors.length; j++) {
					int neighborID = neighbors[j].getNodeID();
					if (!partition.containsNode(neighborID)) {
						int neighborPartitionID = partsGroup.getNodePartitionID(neighborID);
						this.borderNodes.get(neighborPartitionID - 1).add(neighbors[j].getNodeID());
						borderNode = true;
					}
				}
				if (borderNode == true) {
					this.borderNodes.get(partID - 1).add(nodeID);
				}
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
		// update border list
		// check first Node
		if (isBorderNode(partition1ID, node1ID)) {
			// remove from partition 1 border nodes
			this.borderNodes.get(partition1ID - 1).remove(node1ID);
			// add to partition 2 border nodes
			this.borderNodes.get(partition2ID - 1).add(node1ID);
		}
		// check second Node
		if (isBorderNode(partition2ID, node2ID)) {
			// remove from partition 2 border nodes
			this.borderNodes.get(partition2ID - 1).remove(node2ID);
			// add to partition 1 border nodes
			this.borderNodes.get(partition1ID - 1).add(node2ID);
		}
		// check first node neighbors
		Node[] node1Neighbors = this.graph.getNode(node1ID).getNeighbors();
		for (int i = 0; i < node1Neighbors.length; i++) {
			// we have 2 cases
			// 1- neighbor is in partition 1 and is not a border node => at to
			// border nodes
			// 2- neighbor is in partition 2 and is a border node => may or may
			// not be a border node, must check its neighbors
			// 3- if the neighbor is in another partition, it must be a border
			// node, and that won't change
			Node neighborNode = node1Neighbors[i];
			int neighborID = neighborNode.getNodeID();
			if (partition1.containsNode(neighborID)) {
				this.borderNodes.get(partition1ID - 1).add(neighborID);
			} else if (partition2.containsNode(neighborID)) {
				if (checkNodeBorderStatus(partition2ID, neighborID)) {
					this.borderNodes.get(partition2ID - 1).add(neighborID);
				} else {
					this.borderNodes.get(partition2ID - 1).remove(neighborID);
				}

			}

		}
		// check second node neighbors
		Node[] node2Neighbors = this.graph.getNode(node2ID).getNeighbors();
		for (int i = 0; i < node2Neighbors.length; i++) {
			// we have 2 cases
			// 1- neighbor is in partition 2 and is not a border node => at to
			// border nodes
			// 2- neighbor is in partition 1 and is a border node => may or may
			// not be a border node, must check its neighbors
			// 3- if the neighbor is in another partition, it must be a border
			// node, and that won't change
			Node neighborNode = node2Neighbors[i];
			int neighborID = neighborNode.getNodeID();
			if (partition2.containsNode(neighborID)) {
				this.borderNodes.get(partition2ID - 1).add(neighborID);
			} else if (partition1.containsNode(neighborID)) {
				if (checkNodeBorderStatus(partition1ID, neighborID)) {
					this.borderNodes.get(partition1ID - 1).add(neighborID);
				} else {
					this.borderNodes.get(partition1ID - 1).remove(neighborID);
				}

			}

		}
	}

	private boolean isBorderNode(int partitionID, int nodeID) {
		// TODO Auto-generated method stub
		return this.borderNodes.get(partitionID - 1).contains(nodeID);
	}

	// return true if border node, false otherwise
	private boolean checkNodeBorderStatus(int partID, int nodeID) {
		Node curNode = this.graph.getNode(nodeID);
		Node[] neighbors = curNode.getNeighbors();

		Partition curPart = this.refinedPartitions.getPartition(partID);
		for (int i = 0; i < neighbors.length; i++) {
			Node neighborNode = neighbors[i];
			int neighborID = neighborNode.getNodeID();
			if (!curPart.containsNode(neighborID)) {
				return true;
			}
		}
		return false;
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
				// Iterator<Integer> it1 = part1.getNodeIDs().iterator();
				Iterator<Integer> it1 = this.borderNodes.get(part1.getPartitionID() - 1).iterator();

				while (it1.hasNext()) {
					int node1ID = it1.next();
					// Iterator<Integer> it2 = part2.getNodeIDs().iterator();
					Iterator<Integer> it2 = this.borderNodes.get(part2.getPartitionID() - 1).iterator();
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

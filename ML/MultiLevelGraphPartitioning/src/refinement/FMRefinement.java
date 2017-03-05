package refinement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import structure.FMTransfer;
import structure.Graph;
import structure.IntFixedSizeHashSet;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;
import structure.Tuple;

public class FMRefinement {
	private Graph graph;
	private int numberOfPartitions;
	private int maxTransfers;
	private int numberOfTransfersApplied;
	private int minGainAllowed;
	private int minPartitionWeight;
	private int maxPartitionWeight;
	private PartitionGroup refinedPartitions;
	private HashMap<Integer, Integer> nodePartitionMap;
	private ArrayList<HashSet<Integer>> borderNodes;
	public PartitionGroup bestParts;
	private IntFixedSizeHashSet lockedNodes;
	private int nonPositiveSwapsApplied;
	private int maxNonPositiveSwaps;
	private int minEdgeCut;
	private int curEdgeCut;
	private HashMap<Tuple<Integer, Integer>, Integer> partNodeGainMap;
	private HashMap<Integer, HashMap<Integer, HashSet<Integer>>> gainPartNodeMap;
	private int maxEdgeCutGain;
	private int minEdgeCutGain;
	private RandomAccessIntHashSet graphSubset;

	public FMRefinement(Graph graph, RandomAccessIntHashSet graphSubset, PartitionGroup partsGroup, int maxSwaps,
			int maxNonPositiveSwaps, int minGainAllowed, float imbalanceRatio) {
		// assign attributes
		this.graph = graph;
		this.maxTransfers = maxSwaps;
		this.minGainAllowed = minGainAllowed;
		this.numberOfTransfersApplied = 0;
		this.lockedNodes = new IntFixedSizeHashSet(Math.max(16, maxNonPositiveSwaps / 2));
		this.maxNonPositiveSwaps = maxNonPositiveSwaps;
		this.nonPositiveSwapsApplied = 0;
		this.graphSubset = graphSubset;
		int totalGraphWeight = 0;
		if (graphSubset != null) {
			Iterator<Integer> subIt = graphSubset.iterator();
			while (subIt.hasNext()) {
				int subNodeID = subIt.next();
				totalGraphWeight += this.graph.getNode(subNodeID).getNodeWeight();
			}

		} else {
			totalGraphWeight = this.graph.getTotalNodesWeights();
		}
		this.numberOfPartitions = partsGroup.getPartitionNumber();
		float exactPartitionWeight = (float) totalGraphWeight / this.numberOfPartitions;
		this.maxPartitionWeight = (int) Math.ceil((1 + imbalanceRatio) * Math.ceil(exactPartitionWeight));
		this.minPartitionWeight = Math.max((int) Math.floor((1 - imbalanceRatio) * Math.floor(exactPartitionWeight)),
				1);
		this.refinedPartitions = partsGroup;
		this.minEdgeCut = partsGroup.getEdgeCut();
		this.curEdgeCut = this.minEdgeCut;
		this.maxEdgeCutGain = Integer.MIN_VALUE;
		this.minEdgeCutGain = Integer.MAX_VALUE;
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
					if (graphSubset != null) {
						if (!graphSubset.contains(neighborID)) {
							continue;
						}
					}
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
		// create Gain Bucket for borderNodes
		partNodeGainMap = new HashMap<Tuple<Integer, Integer>, Integer>();
		gainPartNodeMap = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();
		;
		createGainBucket();
		// begin refinement
		this.refinePartitions();
	}

	private void createGainBucket() {
		// loop all partitions
		for (int i = 0; i < this.borderNodes.size(); i++) {
			int partID = i + 1;
			// loop all border nodes in each partition
			Iterator<Integer> nodeIt = this.borderNodes.get(i).iterator();
			while (nodeIt.hasNext()) {
				int nodeID = nodeIt.next();
				// calculate gains for the node if transfered to any other
				// partition
				for (int j = 0; j < this.refinedPartitions.getPartitionNumber(); j++) {
					int newPartID = j + 1;
					if (partID == newPartID) {
						// transfer to the same partition
						continue;
					}

					int edgeCutGain = this.getNodeToPartEdgeCutGain(nodeID, newPartID);
					Tuple<Integer, Integer> partNode = new Tuple<Integer, Integer>(newPartID, nodeID);

					// check if gain already exists
					if (!this.gainPartNodeMap.containsKey(edgeCutGain)) {
						this.gainPartNodeMap.put(edgeCutGain, new HashMap<Integer, HashSet<Integer>>());
					}

					HashMap<Integer, HashSet<Integer>> partNodesMap = this.gainPartNodeMap.get(edgeCutGain);
					// check if part exists
					if (!partNodesMap.containsKey(newPartID)) {
						partNodesMap.put(newPartID, new HashSet<Integer>());
					}

					HashSet<Integer> nodesSet = partNodesMap.get(newPartID);

					// add node to hash set
					nodesSet.add(nodeID);
					// add attach the part node tuple to the gain
					this.partNodeGainMap.put(partNode, edgeCutGain);
					// update max&min edge cut gain
					this.maxEdgeCutGain = Math.max(this.maxEdgeCutGain, edgeCutGain);
					this.minEdgeCutGain = Math.min(this.minEdgeCutGain, edgeCutGain);
				}

			}
		}
	}

	private void refinePartitions() {
		FMTransfer fmTransfer;

		// terminate if max swaps reached
		while (this.numberOfTransfersApplied < this.maxTransfers
				&& this.nonPositiveSwapsApplied < this.maxNonPositiveSwaps) {
			fmTransfer = this.getTransferWithMaxGain();
			if (fmTransfer == null) {
				// no more transfers allowed found
				// System.out.println("No more Transfers.");
				break;
			}

			if (fmTransfer.edgeCutGain > 0) {
				// update swaps number
				this.numberOfTransfersApplied++;
				// update gain
				this.curEdgeCut -= fmTransfer.edgeCutGain;
			} else {
				// update swaps number
				this.numberOfTransfersApplied++;
				this.nonPositiveSwapsApplied++;
				// update gain
				this.curEdgeCut -= fmTransfer.edgeCutGain;
			}

			if (this.curEdgeCut >= this.minEdgeCut) {
				// if the gain is not the best
				// store the best partition Group we are having if not set
				if (this.bestParts == null) {
					this.bestParts = new PartitionGroup(this.refinedPartitions);
				}
			} else {
				// we are converging local minima
				// or got out of local minima
				this.minEdgeCut = this.curEdgeCut;
				this.bestParts = null;
				this.nonPositiveSwapsApplied = 0;
				this.lockedNodes.clear();

			}

			// Transfer Node
			// System.out.println("Transfer Node " + fmTransfer.nodeID + " to
			// partition " + fmTransfer.destPartitionID
			// + " with gain " + fmTransfer.edgeCutGain);
			this.transferNode(fmTransfer);

		}

		// if(this.numberOfTransfersApplied >= this.maxTransfers){
		// System.out.println("Transfers Allowed Consumed");
		// }
		// else if(this.nonPositiveSwapsApplied >= this.maxNonPositiveSwaps){
		// System.out.println("Negative Transfers Allowed Consumed");
		// }
	}

	private void transferNode(FMTransfer fmTransfer) {
		int nodeID = fmTransfer.nodeID;
		Node curNode = this.graph.getNode(nodeID);
		Node[] nodeNeighbors = curNode.getNeighbors();
		int destPartID = fmTransfer.destPartitionID;
		int curPartID = this.nodePartitionMap.get(nodeID);

		// update partitions
		Partition curPart = this.refinedPartitions.getPartition(curPartID);
		Partition destPart = this.refinedPartitions.getPartition(destPartID);
		curPart.removeNode(nodeID);
		destPart.addNode(nodeID);
		// update the map
		this.nodePartitionMap.put(nodeID, destPartID);
		// update border list
		// check first Node
		if (isBorderNode(curPartID, nodeID)) {
			// remove from partition 1 border nodes
			this.borderNodes.get(curPartID - 1).remove(nodeID);
			this.borderNodes.get(destPartID - 1).add(nodeID);
		}
		// check first node neighbors
		for (int i = 0; i < nodeNeighbors.length; i++) {
			// we have 2 cases
			// 1- neighbor is in original partition and is not a border node =>
			// add to
			// border nodes
			// 2- neighbor is in dest partition and is a border node => may or
			// may
			// not be a border node, must check its neighbors
			// 3- if the neighbor is in another partition, it must be a border
			// node, and that won't change
			Node neighborNode = nodeNeighbors[i];
			int neighborID = neighborNode.getNodeID();
			if (this.graphSubset != null) {
				if (!this.graphSubset.contains(neighborID)) {
					continue;
				}
			}
			if (curPart.containsNode(neighborID)) {
				this.borderNodes.get(curPartID - 1).add(neighborID);
			} else if (destPart.containsNode(neighborID)) {
				if (checkNodeBorderStatus(destPartID, neighborID)) {
					this.borderNodes.get(destPartID - 1).add(neighborID);
				} else {
					this.borderNodes.get(destPartID - 1).remove(neighborID);
				}

			}

		}
		// update gains nodes map
		// update node gains
		for (int i = 0; i < this.refinedPartitions.getPartitionNumber(); i++) {
			int partID = i + 1;
			Tuple<Integer, Integer> partNode = new Tuple<Integer, Integer>(partID, nodeID);
			int newEdgeCutGain = this.getNodeToPartEdgeCutGain(nodeID, partID);
			if (partID == curPartID) {
				// add the transfered node gain to the previous partition
				// check if the gain already exist, if not add new gain bucket
				if (!this.gainPartNodeMap.containsKey(newEdgeCutGain)) {
					this.gainPartNodeMap.put(newEdgeCutGain, new HashMap<Integer, HashSet<Integer>>());
				}
				HashMap<Integer, HashSet<Integer>> tempPartNodesMap = this.gainPartNodeMap.get(newEdgeCutGain);
				// check if partition exist in the hash map
				if (!tempPartNodesMap.containsKey(partID)) {
					tempPartNodesMap.put(partID, new HashSet<>());
				}

				HashSet<Integer> nodes = tempPartNodesMap.get(partID);
				nodes.add(nodeID);

				// add entry in part-node-gain map
				this.partNodeGainMap.put(partNode, newEdgeCutGain);

				// update max&min edge cut gain
				this.maxEdgeCutGain = Math.max(this.maxEdgeCutGain, newEdgeCutGain);
				this.minEdgeCutGain = Math.min(this.minEdgeCutGain, newEdgeCutGain);

			} else if (partID == destPartID) {
				int prevEdgeCutGain = this.partNodeGainMap.get(partNode);
				// remove the transfered node gain from the destination
				// partition
				HashMap<Integer, HashSet<Integer>> tempPartNodesMap = this.gainPartNodeMap.get(prevEdgeCutGain);
				// check if partition exist in the hash map
				if (!tempPartNodesMap.containsKey(partID)) {
					tempPartNodesMap.put(partID, new HashSet<>());
				}

				HashSet<Integer> nodes = tempPartNodesMap.get(partID);
				nodes.remove(nodeID);

				// remove entry from part-node-gain map
				this.partNodeGainMap.remove(partNode);
			} else {
				int prevEdgeCutGain = this.partNodeGainMap.get(partNode);
				// remove previous gain from maps
				this.gainPartNodeMap.get(prevEdgeCutGain).get(partID).remove(nodeID);
				// update the transfered node gain in partition
				if (!this.gainPartNodeMap.containsKey(newEdgeCutGain)) {
					this.gainPartNodeMap.put(newEdgeCutGain, new HashMap<Integer, HashSet<Integer>>());
				}
				HashMap<Integer, HashSet<Integer>> tempPartNodesMap = this.gainPartNodeMap.get(newEdgeCutGain);
				// check if partition exist in the hash map
				if (!tempPartNodesMap.containsKey(partID)) {
					tempPartNodesMap.put(partID, new HashSet<>());
				}

				HashSet<Integer> nodes = tempPartNodesMap.get(partID);
				nodes.add(nodeID);

				// update entry in part-node-gain map
				this.partNodeGainMap.put(partNode, newEdgeCutGain);

				// update max&min edge cut gain
				this.maxEdgeCutGain = Math.max(this.maxEdgeCutGain, newEdgeCutGain);
				this.minEdgeCutGain = Math.min(this.minEdgeCutGain, newEdgeCutGain);

			}

			// update neighbors gains in partition
			for (int j = 0; j < nodeNeighbors.length; j++) {
				// check if the neighbor node is a boundary node
				// if a boundary node, update its gain to the partition
				// if not check if it has a gain, if yes remove the gain
				// entries
				Node neighborNode = nodeNeighbors[j];
				int neighborID = neighborNode.getNodeID();
				if (this.graphSubset != null) {
					if (!this.graphSubset.contains(neighborID)) {
						continue;
					}
				}
				int neighborPartitionID = this.nodePartitionMap.get(neighborID);
				// if neighbor is in the same partition, there is no gain to
				// update
				if (neighborPartitionID == partID) {
					continue;
				}
				Tuple<Integer, Integer> partNeighbor = new Tuple<Integer, Integer>(partID, neighborID);
				int newNeighborGain = this.getNodeToPartEdgeCutGain(neighborID, partID);
				if (this.partNodeGainMap.containsKey(partNeighbor)) {
					int prevNeighborGain = this.partNodeGainMap.get(partNeighbor);
					if (this.isBorderNode(neighborPartitionID, neighborID)) {
						// update neighbor gain to this partition
						// remove previous gain from maps
						this.gainPartNodeMap.get(prevNeighborGain).get(partID).remove(neighborID);
						// update the transfered node gain in partition
						if (!this.gainPartNodeMap.containsKey(newNeighborGain)) {
							this.gainPartNodeMap.put(newNeighborGain, new HashMap<Integer, HashSet<Integer>>());
						}
						HashMap<Integer, HashSet<Integer>> tempPartNodesMap = this.gainPartNodeMap.get(newNeighborGain);
						// check if partition exist in the hash map
						if (!tempPartNodesMap.containsKey(partID)) {
							tempPartNodesMap.put(partID, new HashSet<>());
						}

						HashSet<Integer> nodes = tempPartNodesMap.get(partID);
						nodes.add(neighborID);

						// update entry in part-node-gain map
						this.partNodeGainMap.put(partNeighbor, newNeighborGain);

						// update max&min edge cut gain
						this.maxEdgeCutGain = Math.max(this.maxEdgeCutGain, newNeighborGain);
						this.minEdgeCutGain = Math.min(this.minEdgeCutGain, newNeighborGain);

					} else if (this.partNodeGainMap.containsKey(partNeighbor)) {
						// remove neighbor gains, as the neighbor was moved from
						// border
						this.gainPartNodeMap.get(prevNeighborGain).get(partID).remove(neighborID);
						this.partNodeGainMap.remove(partNeighbor);
					}
				} else {
					// new border node without gain
					// record its gain
					if (this.isBorderNode(neighborPartitionID, neighborID)) {
						// Add the neighbor node gain in partition
						if (!this.gainPartNodeMap.containsKey(newNeighborGain)) {
							this.gainPartNodeMap.put(newNeighborGain, new HashMap<Integer, HashSet<Integer>>());
						}
						HashMap<Integer, HashSet<Integer>> tempPartNodesMap = this.gainPartNodeMap.get(newNeighborGain);
						// check if partition exist in the hash map
						if (!tempPartNodesMap.containsKey(partID)) {
							tempPartNodesMap.put(partID, new HashSet<>());
						}

						HashSet<Integer> nodes = tempPartNodesMap.get(partID);
						nodes.add(neighborID);

						// update entry in part-node-gain map
						this.partNodeGainMap.put(partNeighbor, newNeighborGain);

						// update max&min edge cut gain
						this.maxEdgeCutGain = Math.max(this.maxEdgeCutGain, newNeighborGain);
						this.minEdgeCutGain = Math.min(this.minEdgeCutGain, newNeighborGain);
					}
				}

			}
		}

		// lock the node
		this.lockedNodes.addItem(nodeID);
	}

	private boolean isBorderNode(int partitionID, int nodeID) {
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
			if (this.graphSubset != null) {
				if (!this.graphSubset.contains(neighborID)) {
					continue;
				}
			}
			if (!curPart.containsNode(neighborID)) {
				return true;
			}
		}
		return false;
	}

	private FMTransfer getTransferWithMaxGain() {
		FMTransfer fmPair = null;
		for (int i = this.maxEdgeCutGain; i >= Math.max(this.minGainAllowed, this.minEdgeCutGain)
				&& fmPair == null; i--) {
			int gain = i;
			// check if max gain is not allowed
			if (gain < this.minGainAllowed) {
				return null; // terminate
			}

			if (this.gainPartNodeMap.containsKey(i)) {
				HashMap<Integer, HashSet<Integer>> partNodesMap = this.gainPartNodeMap.get(i);
				// loop all parts
				Iterator<Entry<Integer, HashSet<Integer>>> partsNodesIt = partNodesMap.entrySet().iterator();
				while (partsNodesIt.hasNext()) {
					Entry<Integer, HashSet<Integer>> partNodesEntry = partsNodesIt.next();
					int partID = partNodesEntry.getKey();

					HashSet<Integer> nodesSet = partNodesEntry.getValue();
					// loop all nodes in partition
					Iterator<Integer> nodesIt = nodesSet.iterator();
					while (nodesIt.hasNext()) {
						int nodeID = nodesIt.next();
						int nodePartitionID = this.nodePartitionMap.get(nodeID);
						Partition prt = this.refinedPartitions.getPartition(nodePartitionID);

						// if the partition has single node
						// do not empty the partition
						if (prt.getNumberOfNodes() < 2) {
							continue;
						}
						// check if node is locked
						if (this.lockedNodes.itemExists(nodeID)) {
							continue;
						}

						// calculate the balance gain
						// if allowed return the transfer
						// TODO: we here return the first available transfer
						// with the maximum gain, we can randomize it
						// or loop to find the max balance gain
						float balanceGain = this.getNodeToPartBalanceGain(nodeID, partID);
						if (balanceGain < 0) {
							continue; // non feasible transfer
						} else {
							fmPair = new FMTransfer(partID, nodeID);
							fmPair.balanceGain = balanceGain;
							fmPair.edgeCutGain = gain;

						}
					}

				}

			} else {
				continue;
			}
		}
		return fmPair; // if null no transfer is available
	}

	private float getNodeToPartBalanceGain(int nodeID, int partID) {
		float balanceGain = 0;
		int nodeWeight = this.graph.getNode(nodeID).getNodeWeight();
		int curPartID = this.nodePartitionMap.get(nodeID);
		int curPartWeight = this.refinedPartitions.getPartition(curPartID).getPartitionWeight();
		int futurePartWeight = this.refinedPartitions.getPartition(partID).getPartitionWeight();
		float curPartCurrentImbalance = this.getWeightImbalance(curPartWeight);
		float curPartFutureImbalance = this.getWeightImbalance(curPartWeight - nodeWeight);
		float futurePartCurrentImbalance = this.getWeightImbalance(futurePartWeight);
		float futurePartFutureImbalance = this.getWeightImbalance(futurePartWeight + nodeWeight);
		// calculate balance gain
		float maxPartCurrentImbalance = Math.max(curPartCurrentImbalance, futurePartCurrentImbalance);
		float maxPartFutureImbalance = Math.max(curPartFutureImbalance, futurePartFutureImbalance);
		balanceGain = maxPartCurrentImbalance - maxPartFutureImbalance;

		return balanceGain;
	}

	private float getWeightImbalance(int partitionWeight) {
		float imbalance = 0;
		if (partitionWeight > this.maxPartitionWeight) {
			imbalance = ((float) partitionWeight / this.maxPartitionWeight) - 1;
		}
		return Math.max(imbalance, 0);
	}

	private int getNodeToPartEdgeCutGain(int nodeID, int partID) {
		int gain = 0;
		int curPartID = this.nodePartitionMap.get(nodeID);
		// gain if node1 transferred to partition2
		Node[] nodeNeighbors = this.graph.getNode(nodeID).getNeighbors();
		for (int i = 0; i < nodeNeighbors.length; i++) {
			int neighborID = nodeNeighbors[i].getNodeID();
			if (this.graphSubset != null) {
				if (!this.graphSubset.contains(neighborID)) {
					continue;
				}
			}
			int neighborPartitionID = this.nodePartitionMap.get(neighborID);
			int edgeWeight = this.graph.getEdge(nodeID, neighborID).getWeight();
			if (neighborPartitionID == curPartID) {
				gain -= edgeWeight;
			} else if (neighborPartitionID == partID) {
				gain += edgeWeight;
			}
		}
		return gain;
	}

	public PartitionGroup getRefinedPartitions() {
		if (this.bestParts != null)
			return this.bestParts;
		return refinedPartitions;
	}

	public int getNumberOfSwapsApplied() {
		return numberOfTransfersApplied;
	}
}

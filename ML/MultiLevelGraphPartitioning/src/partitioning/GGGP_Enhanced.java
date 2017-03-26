package partitioning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import structure.Edge;
import structure.Graph;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;
import utilities.PartitionWeightComparator;

public class GGGP_Enhanced extends Partitioning {

	private RandomAccessIntHashSet graphSubset;

	// constructor
	public GGGP_Enhanced(Graph graph, RandomAccessIntHashSet graphSubset, int numberOfPartitions, int numberOfTrials,
			float imbalanceRatio) {
		super(graph, graphSubset, numberOfPartitions, numberOfTrials, imbalanceRatio);
	}

	// return partition group
	public PartitionGroup getPartitions() {
		PartitionGroup bestPartGroup = null;
		long edgeCut = 0;
		long minEdgeCut = Long.MAX_VALUE;
		// Get seeded Nodes for each Trial to avoid
		// Duplicate seeds
		int[] randomSeeds = this.graph.getNRandomNodesIDs(this.numberOfTrials, this.graphSubset);
		for (int i = 0; i < randomSeeds.length; i++) {
			PartitionGroup partGroup = new PartitionGroup(this.graph);
			int partitionID = 1;
			// Construct HashSet for the graph
			IntOpenHashSet unselectedNodesIDs;
			if (this.graphSubset != null) {
				unselectedNodesIDs = new IntOpenHashSet(graphSubset.size());
				IntListIterator subIt = graphSubset.iterator();
				int totalNodesWeight = 0;
				while (subIt.hasNext()) {
					int subNodeID = subIt.next();
					unselectedNodesIDs.add(subNodeID);
					totalNodesWeight += this.graph.getNode(subNodeID).getNodeWeight();
				}
				float exactPartitionWeight = (float) totalNodesWeight / numberOfPartitions;
				this.maxPartitionWeight = (int) Math.ceil((1 + imbalanceRatio) * Math.ceil(exactPartitionWeight));
				this.minPartitionWeight = Math
						.max((int) Math.floor((1 - imbalanceRatio) * Math.floor(exactPartitionWeight)), 1);
			} else {
				unselectedNodesIDs = this.graph.getCopyOfNodesIDs();
			}

			// Create first partition from the seeded Node
			Partition partition = constructPartition(randomSeeds[i], unselectedNodesIDs, partitionID);
			// Add partition to partitions list
			partGroup.addPartition(partition);

			// Construct Partitions
			while (unselectedNodesIDs.size() > 0) {
				// get seed for the new partition
				partitionID++;
				int seedNodeID = this.getRandomNode(unselectedNodesIDs);
				partition = constructPartition(seedNodeID, unselectedNodesIDs, partitionID);
				// Add partition to partitions list
				partGroup.addPartition(partition);
			}
			// All partitions are constructed, but maybe underSized due to the
			// existence of disconnected nodes
			// If there exist a node that does not belong to a partition
			// it is going to be added to adjacent partition regardless of
			// balance constraint
			// Later phase will take care of balancing the partitions again
			while (partGroup.getPartitionNumber() > this.numberOfPartitions) {
				partGroup = finalizePartitioning(partGroup);
			}
			// Calculate Edge Cut
			edgeCut = partGroup.getEdgeCut();
			if (edgeCut < minEdgeCut) {
				minEdgeCut = edgeCut;
				bestPartGroup = partGroup;
			}
		}

		return bestPartGroup;
	}

	private PartitionGroup finalizePartitioning(PartitionGroup partGroup) {
		if (partGroup.getPartitionNumber() == this.numberOfPartitions) {
			return partGroup;
		} else if (partGroup.getPartitionNumber() < this.numberOfPartitions) {
			ArrayList<Partition> partsList = partGroup.getAllPartitions();
			partsList.sort(new PartitionWeightComparator());
			if (partsList.size() < this.numberOfPartitions) {
				// we need to split the largest Partition
				Partition largestPartition = partsList.get(partsList.size() - 1);
				PartitionGroup modifiedPartGroup = splitPartition(partGroup, largestPartition);
				return finalizePartitioning(modifiedPartGroup);
			}

		} else {
			ArrayList<Partition> partsList = partGroup.getAllPartitions();
			partsList.sort(new PartitionWeightComparator());
			// the actual number of partition is larger than the requested
			// number
			// so partitions are merged
			int totalPartsNumber = partsList.size();
			// looping largest partitions
			for (int i = totalPartsNumber - 1; i >= totalPartsNumber - this.numberOfPartitions; i--) {
				// looping smaller partitions to be merged in largest ones
				Partition originalPart = partsList.get(i);
				int bestCutGain = 0;
				Partition bestPartition = null;
				for (int j = 0; j < totalPartsNumber - this.numberOfPartitions; j++) {
					Partition smallPart = partsList.get(j);
					if (originalPart.isPartitionNeighbor(smallPart)) {
						int cutGain = originalPart.getEdgeCutBetweenPartition(smallPart);
						if (cutGain > bestCutGain) {
							bestPartition = smallPart;
						}
					}
				}
				if (bestPartition != null) {
					PartitionGroup modifiedPartGroup = mergePartitions(partGroup, originalPart, bestPartition);
					return finalizePartitioning(modifiedPartGroup);
				}

			}
			// no neighbor partitions
			// merge the smallest main partitions, with the largest helper
			// partitions
			Partition originalPart = partsList.get(totalPartsNumber - this.numberOfPartitions);
			Partition bestPartition = partsList.get(totalPartsNumber - this.numberOfPartitions - 1);
			PartitionGroup modifiedPartGroup = mergePartitions(partGroup, originalPart, bestPartition);
			return finalizePartitioning(modifiedPartGroup);
		}
		System.err.println("Finalizing GGGP Enhanced Error: probable Disconnected Graph.");
		return null;
	}

	private PartitionGroup splitPartition(PartitionGroup partGroup, Partition largestPartition) {
		RandomAccessIntHashSet partSet = new RandomAccessIntHashSet(largestPartition.getNodeIDs());
		GGGP_Enhanced gggpE = new GGGP_Enhanced(this.graph, partSet, 2, 4, (float) 0.1);
		PartitionGroup partsG = gggpE.getPartitions();
		ArrayList<Partition> allPartition = partGroup.getAllPartitions();
		int partitionID = 3;
		for (Iterator<Partition> iterator = allPartition.iterator(); iterator.hasNext();) {
			Partition partition = (Partition) iterator.next();
			if (partition.getPartitionID() != largestPartition.getPartitionID()) {
				partition.setPartitionID(partitionID);
				partsG.addPartition(partition);
				partitionID++;
			}
		}
		return partsG;
	}

	private PartitionGroup mergePartitions(PartitionGroup partGroup, Partition part1, Partition part2) {
		PartitionGroup partsG = new PartitionGroup(this.graph);

		ArrayList<Partition> allPartition = partGroup.getAllPartitions();
		int partitionID = 2;
		for (Iterator<Partition> iterator = allPartition.iterator(); iterator.hasNext();) {
			Partition partition = (Partition) iterator.next();
			if (partition.getPartitionID() != part1.getPartitionID()
					&& partition.getPartitionID() != part2.getPartitionID()) {
				partition.setPartitionID(partitionID);
				partsG.addPartition(partition);
				partitionID++;
			}
		}

		// construct Merged Partitions
		IntOpenHashSet part2NodesIDs = part2.getNodeIDs();
		part1.setPartitionID(1);
		for (IntIterator iterator = part2NodesIDs.iterator(); iterator.hasNext();) {
			int nodeID = iterator.nextInt();
			part1.addNode(nodeID);
		}
		partsG.addPartition(part1);
		return partsG;
	}

	/*
	 * Due to the limitation in Java, Random element Cannot be acquired from
	 * HashSet in O(1) instead O(n) function will be needed TODO: optimize
	 * random function to O(1) using custom implementation to HashSet
	 */
	private int getRandomNode(IntOpenHashSet unselectedNodesIDs) {

		int size = unselectedNodesIDs.size();
		int item = new Random().nextInt(size);
		int i = 0;
		for (int obj : unselectedNodesIDs) {
			if (i == item)
				return obj;
			i = i + 1;
		}

		// some error has happened
		return -1;
	}

	/*
	 * Construct Partition using GGGP, give a seed Node
	 */
	private Partition constructPartition(int seedNodeID, IntOpenHashSet unselectedNodesIDs, int partitionID) {
		int nextNodeID = seedNodeID;
		// Create Partition List
		Partition partition = new Partition(this.graph, partitionID);
		// frontier node -> neighbors
		Int2ObjectOpenHashMap<IntOpenHashSet> frontierToNeighbors = new Int2ObjectOpenHashMap<IntOpenHashSet>();
		// neighbor node -> gain
		Int2IntOpenHashMap neighborsGains = new Int2IntOpenHashMap();
		// neighbor node -> frontier nodes
		Int2ObjectOpenHashMap<IntOpenHashSet> neighborToFrontiers = new Int2ObjectOpenHashMap<IntOpenHashSet>();
		int partitionWeight = 0;
		// Add Nodes to partition
		while (partitionWeight < this.minPartitionWeight && unselectedNodesIDs.size() > 0) {
			// if nextNodeID = -1 that means the frontierNodes are empty
			// and the rest of the graph is disconnected
			// in Case of standard GGGP algorithm we will choose new seed
			// , but in the enhanced version we will stop the partition
			// as it, and will balance it in later phases
			if (nextNodeID == -1) {
				break;
			}
			partition.addNode(nextNodeID);
			partitionWeight += this.graph.getNode(nextNodeID).getNodeWeight();
			// remove Node from free nodes Hash set
			unselectedNodesIDs.remove(nextNodeID);
			// after adding node to partition
			// the Node will be added to the frontier list
			nextNodeID = this.addNodeToFrontier(nextNodeID, frontierToNeighbors, neighborsGains, neighborToFrontiers,
					unselectedNodesIDs, partition);
		}

		// we will store frontier nodes of the partition, as we will be using
		// them later
		// IntOpenHashSet frontiersSet = (IntOpenHashSet)
		// frontierToNeighbors.keySet();
		// partition.setFrontierNodesIDs(frontiersSet);

		return partition;
	}

	/*
	 * This function modifies frontier after adding a node, return the node with
	 * min gain. the frontier nodes need to be updated 1- the new node must be
	 * removed from the neighbors list of the frontier nodes 2- the new node
	 * must be added as a frontier node, if it has unselected neighbors 3- the
	 * gains of all frontier nodes' neighbors need to be updated
	 */

	private int addNodeToFrontier(int nodeID, Int2ObjectOpenHashMap<IntOpenHashSet> frontierToNeighbors,
			Int2IntOpenHashMap neighborsGains, Int2ObjectOpenHashMap<IntOpenHashSet> neighborToFrontiers,
			IntOpenHashSet unselectedNodesIDs, Partition partition) {

		// get the nodes neighbors
		Node cur = this.graph.getNode(nodeID);
		Node[] curNeighbors = cur.getNeighbors();
		IntOpenHashSet unselectedNeighbors = new IntOpenHashSet();

		// filter neighbors selected in previous partitions
		for (int i = 0; i < curNeighbors.length; i++) {
			if (unselectedNodesIDs.contains(curNeighbors[i].getNodeID())) {
				unselectedNeighbors.add(curNeighbors[i].getNodeID());
			}
		}

		int minGain = Integer.MAX_VALUE;
		int minGainNodeID = -1;
		// iterate through all frontier nodes neighbors
		Iterator<Entry<Integer, Integer>> frontierNeighborsIt = neighborsGains.entrySet().iterator();
		while (frontierNeighborsIt.hasNext()) {
			Entry<Integer, Integer> entry = frontierNeighborsIt.next();
			int neighborID = entry.getKey();
			int neighborGain = entry.getValue();
			// check if this neighbor
			// if it is the new node remove it
			// if the new node is not a neighbor do nothing
			// if it is a neighbor, subtract the weight of edge
			if (neighborID == nodeID) {
				// the neighbor is the new added node
				// 1- remove from frontier to neighbors map
				// 2- remove from neighbor To Frontier map
				// 3- after the while loop remove from neighbors gains map, not
				// to mess the iterator
				IntOpenHashSet neighborFrontiers = neighborToFrontiers.get(neighborID);
				Iterator<Integer> neighborFrontiersIt = neighborFrontiers.iterator();
				while (neighborFrontiersIt.hasNext()) {
					int frontierID = neighborFrontiersIt.next();
					frontierToNeighbors.get(frontierID).remove(neighborID);
					if (frontierToNeighbors.get(frontierID).size() <= 0) {
						frontierToNeighbors.remove(frontierID);
					}
				}
				neighborToFrontiers.remove(neighborID);
			} else {

				Edge edg = this.graph.getEdge(nodeID, neighborID);
				if (edg == null) {
					// not a neighbor to the new added node
					// check its stored gain
					if (neighborGain < minGain) {
						minGain = neighborGain;
						minGainNodeID = neighborID;
					}
				} else {
					// is neighbor to the newly added node
					// update its gain in neighborsGains
					neighborGain -= edg.getWeight();
					neighborsGains.put(neighborID, neighborGain);

					// store the gain and its index if it was minimum
					if (neighborGain < minGain) {
						minGain = neighborGain;
						minGainNodeID = neighborID;
					}
				}
			}
		}

		// remove new added node from gains if exist (3)
		neighborsGains.remove(nodeID);
		// calculate the gain of unselected neighbors
		Iterator<Integer> unselectedNeighborsIt = unselectedNeighbors.iterator();
		while (unselectedNeighborsIt.hasNext()) {
			int unselectedNeighborId = unselectedNeighborsIt.next();
			if (!neighborsGains.containsKey(unselectedNeighborId)) {
				int neighborGain = getNodeGain(unselectedNeighborId, partition);
				unselectedNeighbors.add(unselectedNeighborId);
				neighborsGains.put(unselectedNeighborId, neighborGain);
				if (!neighborToFrontiers.containsKey(unselectedNeighborId)) {
					neighborToFrontiers.put(unselectedNeighborId, new IntOpenHashSet());
				}
				neighborToFrontiers.get(unselectedNeighborId).add(nodeID);
				if (neighborGain < minGain) {
					minGain = neighborGain;
					minGainNodeID = unselectedNeighborId;
				}
			}
		}

		// Add the node to frontier nodes if has unselected neighbors
		if (unselectedNeighbors.size() > 0) {
			frontierToNeighbors.put(nodeID, unselectedNeighbors);
		}
		return minGainNodeID;
	}

	private int getNodeGain(int nodeID, Partition partition) {
		int nodeOutsideEdgesWeight = 0;
		int nodeInsideEdgesWeight = 0;
		Node curNode = this.graph.getNode(nodeID);
		Node[] curNeighbors = curNode.getNeighbors();

		// get weights of edges inside partition
		// and edges outside of the partition
		for (int i = 0; i < curNeighbors.length; i++) {
			if (this.graphSubset != null)
				if (!this.graphSubset.contains(curNeighbors[i].getNodeID()))
					continue;
			if (partition.containsNode(curNeighbors[i].getNodeID())) {
				nodeInsideEdgesWeight += this.graph.getEdge(nodeID, curNeighbors[i].getNodeID()).getWeight();
			} else {
				Edge edge = this.graph.getEdge(nodeID, curNeighbors[i].getNodeID());
				nodeOutsideEdgesWeight += edge.getWeight();

			}
		}

		// calculate gain of adding the node to the partition
		// the gain will be the value added to the edge cut
		// the smaller the gain the better
		int gain = nodeOutsideEdgesWeight - nodeInsideEdgesWeight;

		return gain;
	}

}
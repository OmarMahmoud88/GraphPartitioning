package partitioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import structure.Edge;
import structure.Graph;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomSet;
import structure.Tuple;

public class GreedyGraphGrowingPartitioning extends Partitioning {

	private RandomSet<Integer> graphSubset;

	// constructor
	public GreedyGraphGrowingPartitioning(Graph graph, int numberOfPartitions, int numberOfTrials,
			float imbalanceRatio) {
		super(graph, numberOfPartitions, numberOfTrials, imbalanceRatio);
	}

	// return partition group
	public PartitionGroup getPartitions(Graph gr, RandomSet<Integer> graphSubset, int numberOfPartitions,
			int numberOfTries) {
		PartitionGroup bestPartGroup = null;
		long edgeCut = 0;
		long minEdgeCut = Long.MAX_VALUE;
		int partitionsRemained = numberOfPartitions;
		// Get seeded Nodes for each Trial to avoid
		// Duplicate seeds
		int[] randomSeeds = gr.getNRandomNodesIDs(numberOfTries, graphSubset);
		for (int i = 0; i < randomSeeds.length; i++) {
			// ArrayList<Partition> partitions = new
			// ArrayList<Partition>(numberOfPartitions);
			PartitionGroup partGroup = new PartitionGroup(this.graph);
			int partitionID = 1;
			partitionsRemained = numberOfPartitions;
			// Construct Hashset for the graph
			this.graphSubset = graphSubset;
			HashSet<Integer> unselectedNodesIDs;
			if (graphSubset != null) {
				unselectedNodesIDs = new HashSet<Integer>(graphSubset.size());
				Iterator<Integer> subIt = graphSubset.iterator();
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
			partitionsRemained--;

			// Construct Partitions
			while (partitionsRemained > 1) {
				// get seed for the new partition
				partitionID++;
				int seedNodeID = this.getRandomNode(unselectedNodesIDs);
				partition = constructPartition(seedNodeID, unselectedNodesIDs, partitionID);
				// Add partition to partitions list
				partGroup.addPartition(partition);
				partitionsRemained--;
			}
			// Construct Last Partition from the remaining nodes
			partitionID++;
			Iterator<Integer> it = unselectedNodesIDs.iterator();
			partition = new Partition(this.graph, partitionID);
			while (it.hasNext()) {
				partition.addNode(it.next());
				it.remove();
			}
			partGroup.addPartition(partition);
			// Calculate Edge Cut
			edgeCut = partGroup.getEdgeCut();
			if (edgeCut < minEdgeCut) {
				minEdgeCut = edgeCut;
				bestPartGroup = partGroup;
			}
			numberOfTries--;
		}

		return bestPartGroup;
	}

	/*
	 * Due to the limitation in Java, Random element Cannot be acquired from
	 * HashSet in O(1) instead O(n) function will be needed TODO: optimize
	 * random function to O(1) using custom implementation to HashSet
	 */
	private int getRandomNode(HashSet<Integer> unselectedNodesIDs) {

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
	private Partition constructPartition(int seedNodeID, HashSet<Integer> unselectedNodesIDs, int partitionID) {
		int nextNodeID = seedNodeID;
		// Create Partition List
		Partition partition = new Partition(this.graph, partitionID);
		HashMap<Integer, HashSet<Integer>> frontierToNeighbors = new HashMap<Integer, HashSet<Integer>>(); // frontier
																											// node
		// -> neighbors
		HashMap<Integer, Integer> neighborsGains = new HashMap<Integer, Integer>(); // neighbor
																					// node
																					// ->
																					// gain
		HashMap<Integer, HashSet<Integer>> neighborToFrontiers = new HashMap<Integer, HashSet<Integer>>(); // neighbor
																											// node
		// -> frontier
		// nodes
		int partitionWeight = 0;
		// Add Nodes to partition
		while (partitionWeight < this.minPartitionWeight && unselectedNodesIDs.size() > 0) {
			// if nextNodeID = -1 that means the frontierNodes are empty
			// and the rest of the graph is disconnected
			// in this case a new random seed is needed to
			if (nextNodeID == -1) {
				nextNodeID = this.getRandomNode(unselectedNodesIDs);
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

		return partition;
	}

	/*
	 * This function modifies frontier after adding a node, return the node with
	 * min gain. the frontier nodes need to be updated 1- the new node must be
	 * removed from the neighbors list of the frontier nodes 2- the new node
	 * must be added as a frontier node, if it has unselected neighbors 3- the
	 * gains of all frontier nodes' neighbors need to be updated
	 */

	private int addNodeToFrontier(int nodeID, HashMap<Integer, HashSet<Integer>> frontierToNeighbors,
			HashMap<Integer, Integer> neighborsGains, HashMap<Integer, HashSet<Integer>> neighborToFrontiers,
			HashSet<Integer> unselectedNodesIDs, Partition partition) {

		// get the nodes neighbors
		Node cur = this.graph.getNode(nodeID);
		Node[] curNeighbors = cur.getNeighbors();
		HashSet<Integer> unselectedNeighbors = new HashSet<Integer>();

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
				HashSet<Integer> neighborFrontiers = neighborToFrontiers.get(neighborID);
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
					neighborToFrontiers.put(unselectedNeighborId, new HashSet<Integer>());
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
	// private int addNodeToFrontier(int nodeID, HashMap<Integer,
	// ArrayList<Tuple<Integer, Integer>>> frontierNodes,
	// HashSet<Integer> unselectedNodesIDs, Partition partition) {
	//
	// // get the nodes neighbors
	// Node cur = this.graph.getNode(nodeID);
	// Node[] curNeighbors = cur.getNeighbors();
	// ArrayList<Integer> unselectedNeighbors = new
	// ArrayList<Integer>(curNeighbors.length);
	//
	// // filter neighbors selected in previous partitions
	// for (int i = 0; i < curNeighbors.length; i++) {
	// if (unselectedNodesIDs.contains(curNeighbors[i].getNodeID())) {
	// unselectedNeighbors.add(curNeighbors[i].getNodeID());
	// }
	// }
	//
	// int minGain = Integer.MAX_VALUE;
	// int minGainNodeID = -1;
	// // 1- iterate through all frontier nodes neighbors to upgrade their gain
	// Iterator<Entry<Integer, ArrayList<Tuple<Integer, Integer>>>> it =
	// frontierNodes.entrySet().iterator();
	// while (it.hasNext()) {
	// Map.Entry<Integer, ArrayList<Tuple<Integer, Integer>>> pair =
	// (Map.Entry<Integer, ArrayList<Tuple<Integer, Integer>>>) it
	// .next();
	// ArrayList<Tuple<Integer, Integer>> neighborsGain =
	// (ArrayList<Tuple<Integer, Integer>>) pair.getValue();
	// // loop through neighbors
	// // if it is the new node remove it
	// // if the new node is not a neighbor do nothing
	// // if it is a neighbor, subtract the weight of edge
	// // connecting them from the gain
	// for (int i = 0; i < neighborsGain.size(); i++) {
	// if (neighborsGain.get(i).x == nodeID) {
	// neighborsGain.remove(i);
	// // if neighbors list is empty, remove the parent
	// // node from the frontier
	// if (neighborsGain.size() == 0) {
	// it.remove();
	// }
	// i--;
	// continue;
	// } else {
	// int neighborID = neighborsGain.get(i).x;
	// Edge edg = this.graph.getEdge(nodeID, neighborID);
	// if (edg != null) {
	// int gain = neighborsGain.get(i).y - edg.getWeight();
	// Tuple<Integer, Integer> tup = new Tuple<Integer, Integer>(neighborID,
	// gain);
	// neighborsGain.set(i, tup);
	// // store the gain and its index if it was minimum
	// if (gain < minGain) {
	// minGain = gain;
	// minGainNodeID = neighborID;
	// }
	// } else {
	// int gain = neighborsGain.get(i).y;
	// if (gain < minGain) {
	// minGain = gain;
	// minGainNodeID = neighborID;
	// }
	// }
	// }
	// }
	// }
	// // Add the node to frontier nodes
	// // calculate the gain of unselected neighbors
	// ArrayList<Tuple<Integer, Integer>> unselectedNeighborsGains = new
	// ArrayList<Tuple<Integer, Integer>>(
	// unselectedNeighbors.size());
	// for (int i = 0; i < unselectedNeighbors.size(); i++) {
	// int neighborID = unselectedNeighbors.get(i);
	// int neighborGain = getNodeGain(neighborID, partition);
	// unselectedNeighborsGains.add(new Tuple<Integer, Integer>(neighborID,
	// neighborGain));
	// if (neighborGain < minGain) {
	// minGain = neighborGain;
	// minGainNodeID = neighborID;
	// }
	// }
	// if (unselectedNeighborsGains.size() > 0) {
	// frontierNodes.put(nodeID, unselectedNeighborsGains);
	// }
	// return minGainNodeID;
	// }

	private int getNodeGain(int nodeID, Partition partition) {
		int nodeOutsideEdgesWeight = 0;
		int nodeInsideEdgesWeight = 0;
		Node curNode = this.graph.getNode(nodeID);
		Node[] curNeighbors = curNode.getNeighbors();

		// get weights of edges inside partition
		// and edges outside of the partition
		for (int i = 0; i < curNeighbors.length; i++) {
			if (this.graphSubset != null)
				if (!this.graphSubset.contains(curNeighbors[i]))
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

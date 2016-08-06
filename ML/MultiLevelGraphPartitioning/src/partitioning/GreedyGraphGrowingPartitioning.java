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
import structure.Tuple;

public class GreedyGraphGrowingPartitioning {
	private Graph graph;
	private int numberOfPartitions, numberOfTrials;
	private float imbalanceRatio;
	private int maxPartitionWeight, minPartitionWeight;

	// current partition information

	public GreedyGraphGrowingPartitioning(Graph graph, int numberOfPartitions,
			int numberOfTrials, float imbalanceRatio) {
		this.graph = graph;
		this.numberOfPartitions = numberOfPartitions;
		this.numberOfTrials = numberOfTrials;
		this.imbalanceRatio = imbalanceRatio;
		int totalNodesWeight = this.graph.getTotalNodesWeights();
		float exactPartitionWeight = (float) totalNodesWeight
				/ numberOfPartitions;
		this.maxPartitionWeight = (int) (Math.ceil((double) totalNodesWeight
				/ numberOfPartitions) * (1 + imbalanceRatio));
		this.maxPartitionWeight = (int) Math.ceil((1 + imbalanceRatio)
				* (exactPartitionWeight));
		this.minPartitionWeight = (int) Math.floor((1 - imbalanceRatio)
				* (exactPartitionWeight));
	}

	public ArrayList<Partition> getPartitions(Graph gr, int numberOfPartitions,
			int numberOfTries) {
		ArrayList<Partition> bestPartitions = new ArrayList<Partition>(
				numberOfPartitions);
		long edgeCut = 0;
		long minEdgeCut = Long.MAX_VALUE;
		int partitionsRemained = numberOfPartitions;
		// Get seeded Nodes for each Trial to avoid
		// Duplicate seeds
		int[] randomSeeds = gr.getNRandomNodesIDs(numberOfTries);
		for (int i = 0; i < randomSeeds.length; i++) {
			ArrayList<Partition> partitions = new ArrayList<Partition>(
					numberOfPartitions);
			int partitionID = 1;
			partitionsRemained = numberOfPartitions;
			// Construct Hashset for the graph
			HashSet<Integer> unselectedNodesIDs = this.graph
					.getCopyOfNodesIDs();
			// Create first partition from the seeded Node
			Partition partition = constructPartition(randomSeeds[i],
					unselectedNodesIDs, partitionID);
			// Add partition to partitions list
			partitions.add(partition);
			partitionsRemained--;

			// Construct Partitions
			while (partitionsRemained > 1) {
				// get seed for the new partition
				partitionID ++;
				int seedNodeID = this.getRandomNode(unselectedNodesIDs);
				partition = constructPartition(seedNodeID, unselectedNodesIDs, partitionID);
				// Add partition to partitions list
				partitions.add(partition);
				partitionsRemained--;
			}
			// Construct Last Partition from the remaining nodes
			partitionID ++;
			Iterator<Integer> it = unselectedNodesIDs.iterator();
			partition = new Partition(this.graph, partitionID);
			while (it.hasNext()) {
				partition.addNode(it.next());
				it.remove();
			}
			partitions.add(partition);
			// Calculate Edge Cut
			edgeCut = this.getEdgeCut(partitions);
			if (edgeCut < minEdgeCut) {
				minEdgeCut = edgeCut;
				bestPartitions = partitions;
			}
			// Trial finished
			numberOfTries--;
		}
		System.out.println(minEdgeCut);

		return bestPartitions;
	}

	private long getEdgeCut(ArrayList<Partition> partitions) {
		long edgeCut = 0;
		for (int i = 0; i < partitions.size(); i++) {
			Iterator<Integer> it = partitions.get(i).getNodeIDs().iterator();
			while (it.hasNext()) {
				int nodeID = it.next();
				Node curNode = this.graph.getNode(nodeID);
				Node[] neighbors = curNode.getNeighbors();
				for (int j = 0; j < neighbors.length; j++) {
					if (!partitions.get(i).containsNode(
							neighbors[j].getNodeID())) {
						edgeCut += this.graph.getEdge(nodeID,
								neighbors[j].getNodeID()).getWeight();
					}
				}
			}
		}
		return edgeCut / 2;
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
	private Partition constructPartition(int seedNodeID,
			HashSet<Integer> unselectedNodesIDs, int partitionID) {
		int nextNodeID = seedNodeID;
		// Create Partition List
		Partition partition = new Partition(this.graph, partitionID);
		// Create Frontier Nodes List
		HashMap<Integer, ArrayList<Tuple<Integer, Integer>>> frontierNodes = new HashMap<Integer, ArrayList<Tuple<Integer, Integer>>>();
		int partitionWeight = 0;
		// Add Nodes to partition
		while (partitionWeight < this.minPartitionWeight
				&& unselectedNodesIDs.size() > 0) {
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
			nextNodeID = this.addNodeToFrontier(nextNodeID, frontierNodes,
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
	private int addNodeToFrontier(int nodeID,
			HashMap<Integer, ArrayList<Tuple<Integer, Integer>>> frontierNodes,
			HashSet<Integer> unselectedNodesIDs, Partition partition) {

		// get the nodes neighbors
		Node cur = this.graph.getNode(nodeID);
		Node[] curNeighbors = cur.getNeighbors();
		ArrayList<Integer> unselectedNeighbors = new ArrayList<Integer>(
				curNeighbors.length);

		// filter neighbors selected in previous partitions
		for (int i = 0; i < curNeighbors.length; i++) {
			if (unselectedNodesIDs.contains(curNeighbors[i].getNodeID())) {
				unselectedNeighbors.add(curNeighbors[i].getNodeID());
			}
		}

		int minGain = Integer.MAX_VALUE;
		int minGainNodeID = -1;
		// 1- iterate through all frontier nodes neighbors to upgrade their gain
		Iterator it = frontierNodes.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			int currentNodeID = (int) pair.getKey();
			ArrayList<Tuple<Integer, Integer>> neighborsGain = (ArrayList<Tuple<Integer, Integer>>) pair
					.getValue();
			// loop through neighbors
			// if it is the new node remove it
			// if the new node is not a neighbor do nothing
			// if it is a neighbor, subtract the weight of edge
			// connecting them from the gain
			for (int i = 0; i < neighborsGain.size(); i++) {
				if (neighborsGain.get(i).x == nodeID) {
					neighborsGain.remove(i);
					// if neighbors list is empty, remove the parent
					// node from the frontier
					if (neighborsGain.size() == 0) {
						it.remove();
					}
					i--;
					continue;
				} else {
					int neighborID = neighborsGain.get(i).x;
					Edge edg = this.graph.getEdge(nodeID, neighborID);
					if (edg != null) {
						int gain = neighborsGain.get(i).y - edg.getWeight();
						Tuple<Integer, Integer> tup = new Tuple<Integer, Integer>(
								neighborID, gain);
						neighborsGain.set(i, tup);
						// store the gain and its index if it was minimum
						if (gain < minGain) {
							minGain = gain;
							minGainNodeID = neighborID;
						}
					} else {
						int gain = neighborsGain.get(i).y;
						if (gain < minGain) {
							minGain = gain;
							minGainNodeID = neighborID;
						}
					}
				}
			}
		}
		// Add the node to frontier nodes
		// calculate the gain of unselected neighbors
		ArrayList<Tuple<Integer, Integer>> unselectedNeighborsGains = new ArrayList<Tuple<Integer, Integer>>(
				unselectedNeighbors.size());
		for (int i = 0; i < unselectedNeighbors.size(); i++) {
			int neighborID = unselectedNeighbors.get(i);
			int neighborGain = getNodeGain(neighborID, partition);
			unselectedNeighborsGains.add(new Tuple<Integer, Integer>(
					neighborID, neighborGain));
			if (neighborGain < minGain) {
				minGain = neighborGain;
				minGainNodeID = neighborID;
			}
		}
		if (unselectedNeighborsGains.size() > 0) {
			frontierNodes.put(nodeID, unselectedNeighborsGains);
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
			if (partition.containsNode(curNeighbors[i].getNodeID())) {
				nodeInsideEdgesWeight += this.graph.getEdge(nodeID,
						curNeighbors[i].getNodeID()).getWeight();
			} else {
				Edge edge = this.graph.getEdge(nodeID,
						curNeighbors[i].getNodeID());
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

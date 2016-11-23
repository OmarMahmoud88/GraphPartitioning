/*
 * How KL algorithm is implemented
 * KL algorithm scans all pairs of nodes which are in different partitions
 * and Swap the pair with the greatest gain (greatest decrease to edge-cut)
 * so if the number of iterations (I), the algorithm will be O(I*N*N) where N is the number of nodes
 * to minimize the time, we won't calculate the pair gain each time, instead
 * 1- we will calculate the pairs gain once in O(N*N) time.
 * 2- Store the gains in O(N*N) space.
 * 3- Sort gain list in O(N*N*log N).
 * 4- After swapping the pair with the largest gain,
 *  at most 2N - 3 pairs' gain needs to be updated,
 *   those will be updated in O(N) and O(N*logN) swaps is needed to resort the gains list,
 *   so the this step O(I*N*logN)
 * so this implementation will be O(N*N) space, and O(N*log(N)(I+N)) time 
 * 
 */

package refinement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import structure.Edge;
import structure.Graph;
import structure.KLPair;
import structure.Node;
import structure.Partition;
import structure.Tuple;

public class KLRefinement {
	private Graph graph;
	private int numberOfPartitions;
	private int maxSwaps;
	private int numberOfSwapsApplied;
	private int minGainAllowed;
	private int minPartitionWeight;
	private int maxPartitionWeight;
	private float maxImbalance;
	private ArrayList<Partition> refinedPartitions;

	// these arrays will contain the weight of edges for each node
	// in node residing partition, and edges to other partitions
	int[][] nodesEdgesWeight_In_to_Partitions;
	HashMap<Tuple<Integer, Integer>, KLPair> pairsMap;
	ArrayList<KLPair> pairsList;

	public KLRefinement(Graph graph, ArrayList<Partition> partitions,
			int maxSwaps, int minGainAllowed, float imbalanceRatio) {
		// assign attributes
		this.graph = graph;
		this.maxSwaps = maxSwaps;
		this.minGainAllowed = minGainAllowed;
		this.numberOfSwapsApplied = 0;
		int totalGraphWeight = this.graph.getTotalNodesWeights();
		this.numberOfPartitions = partitions.size();
		float exactPartitionWeight = (float) totalGraphWeight
				/ this.numberOfPartitions;
		this.maxPartitionWeight = (int) Math.ceil((1 + imbalanceRatio)
				* (exactPartitionWeight));
		this.minPartitionWeight = (int) Math.floor((1 - imbalanceRatio)
				* (exactPartitionWeight));
		this.refinedPartitions = partitions;
		// initialize nodes partitions matrix
		this.nodesEdgesWeight_In_to_Partitions = this
				.getNodesEdgesWeightToPartition();
		// initialize pairs and calculate gain
		int initialSpace = this.graph.getNumberOfNodes()
				* this.numberOfPartitions;
		this.pairsMap = new HashMap<Tuple<Integer, Integer>, KLPair>(
				initialSpace);
		this.pairsList = new ArrayList<KLPair>(initialSpace);
		this.initializePairs();
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

	/*
	 * this method swap the nodes in pair 1- update the partitions 2- update
	 * nodesEdgesWeight_In_to_Partitions matrix 3- update pairsMap&pairsList
	 */
	private void swap(KLPair pairWithMaxGain) {
		System.out.print("cut gain = " + pairWithMaxGain.getEdgeCutGain());
		System.out.println(" || balance gain = " + pairWithMaxGain.getBalanceGain());
		// update partitions
		int node1ID = pairWithMaxGain.getSourceID();
		int node2ID = pairWithMaxGain.getDestinationID();
		Node[] neighbors1 = this.graph.getNode(node1ID).getNeighbors();
		Node[] neighbors2 = this.graph.getNode(node2ID).getNeighbors();
		int partition1ID = this.getNodePartition(node1ID);
		int partition2ID = this.getNodePartition(node2ID);
		Partition partition1 = this.refinedPartitions.get(partition1ID-1);
		Partition partition2 = this.refinedPartitions.get(partition2ID-1);
		partition1.removeNode(node1ID);
		partition2.addNode(node1ID);
		partition2.removeNode(node2ID);
		partition1.addNode(node2ID);

		// update nodesEdgesWeight_In_to_Partitions matrix
		this.updateNodeEdgesWeight_In_to_Partitions(pairWithMaxGain);

		HashSet<Integer> nodesBalanceUpdated = new HashSet<Integer>();
		HashSet<Integer> nodesEdgeCutUpdated = new HashSet<Integer>();

		// update pairs
		// the current pair edgeCut gain and balance gain has changed *-1
		float balanceGain = pairWithMaxGain.getBalanceGain();
		int edgeCutGain = pairWithMaxGain.getEdgeCutGain();
		pairWithMaxGain.setBalanceGain(-1 * balanceGain);
		pairWithMaxGain.setEdgeCutGain(-1 * edgeCutGain);
		nodesBalanceUpdated.add(node1ID);
		nodesEdgeCutUpdated.add(node1ID);
		nodesBalanceUpdated.add(node2ID);
		nodesEdgeCutUpdated.add(node2ID);
		// pairs contains node1 and all partition2 nodes must be deleted
		// pairs contains node2 and all partition1 nodes must be deleted
		// we have no way to access these pairs directly in pairList
		// the only way would take O(N*N) to loop the whole list
		// alternative solution is to set the pairs gain as zero
		// modifying the pairs in pairsMap will modify the pairs in pairsList
		// Hopefully :)
		this.zeroPairsInPartition(node1ID, partition2ID);
		this.zeroPairsInPartition(node2ID, partition1ID);
		// pairs contains node1 and all partition1 nodes must be created
		// pairs contains node2 and all partition2 nodes must be created
		this.createPairsInPartition(node1ID, partition1ID);
		this.createPairsInPartition(node2ID, partition2ID);
		// all neighbors for the swapped pair as well has their gains changed
		// and must be updated, and must be recalculated
		// we will only recalculate edge cut gain
		// as balance gain will be calculated in the next steps for a superset
		// of pairs
		for (int i = 0; i < neighbors1.length; i++) {
			int neighborID = neighbors1[i].getNodeID();
			if (nodesEdgeCutUpdated.contains(neighborID)) {
				continue;
			}
			this.recalculateEdgeCutOfPairsContain(neighborID);
			nodesEdgeCutUpdated.add(neighborID);
		}
		for (int i = 0; i < neighbors2.length; i++) {
			int neighborID = neighbors2[i].getNodeID();
			if (nodesEdgeCutUpdated.contains(neighborID)) {
				continue;
			}
			this.recalculateEdgeCutOfPairsContain(neighborID);
			nodesEdgeCutUpdated.add(neighborID);
		}

		// all pairs that has one node in either of the partitions
		// have their balance gain changed and must be recalculated
		Iterator<Integer> it1 = partition1.getNodeIDs().iterator();
		while (it1.hasNext()) {
			int partitionNodeID = it1.next();
			if (nodesBalanceUpdated.contains(partitionNodeID)) {
				continue;
			}
			this.recalculateBalanceOfPairsContain(partitionNodeID);
			nodesBalanceUpdated.add(partitionNodeID);
		}
		Iterator<Integer> it2 = partition1.getNodeIDs().iterator();
		while (it2.hasNext()) {
			int partitionNodeID = it2.next();
			if (nodesBalanceUpdated.contains(partitionNodeID)) {
				continue;
			}
			this.recalculateBalanceOfPairsContain(partitionNodeID);
			nodesBalanceUpdated.add(partitionNodeID);
		}

		// now we need to resort the pairs list
		Collections.sort(this.pairsList);

	}

	private void recalculateBalanceOfPairsContain(int nodeID) {
		Node[] allNodes = this.graph.getNodes();
		for (int i = 0; i < allNodes.length; i++) {
			int node2ID = allNodes[i].getNodeID();
			int min = Math.min(nodeID, node2ID);
			int max = Math.max(nodeID, node2ID);
			Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>(min,
					max);
			if (this.pairsMap.containsKey(tuple)) {
				KLPair pair = this.pairsMap.get(tuple);
				int balanceGain = this.getPairBalanceGain(pair);
				pair.setBalanceGain(balanceGain);
			}
		}

	}

	private int getPairBalanceGain(KLPair klPair) {
		int node1ID = klPair.getSourceID();
		int node2ID = klPair.getDestinationID();
		int partition1ID = this.getNodePartition(node1ID);
		int partition2ID = this.getNodePartition(node2ID);
		int balanceGain = this.getPairBalanceGain(node1ID, partition1ID,
				node2ID, partition2ID);
		return balanceGain;
	}

	private void recalculateEdgeCutOfPairsContain(int nodeID) {
		Node[] allNodes = this.graph.getNodes();
		for (int i = 0; i < allNodes.length; i++) {
			int node2ID = allNodes[i].getNodeID();
			int min = Math.min(nodeID, node2ID);
			int max = Math.max(nodeID, node2ID);
			Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>(min,
					max);
			if (this.pairsMap.containsKey(tuple)) {
				KLPair pair = this.pairsMap.get(tuple);
				int edgeCutGain = this.getPairEdgeCutGain(pair);
				pair.setEdgeCutGain(edgeCutGain);
			}
		}

	}

	private int getPairEdgeCutGain(KLPair klPair) {
		int node1ID = klPair.getSourceID();
		int node2ID = klPair.getDestinationID();
		int partition1ID = this.getNodePartition(node1ID);
		int partition2ID = this.getNodePartition(node2ID);
		int edgeCutGain = this.getPairEdgeCutGain(node1ID, partition1ID,
				node2ID, partition2ID);
		return edgeCutGain;

	}

	/*
	 * This method create all pairs between specified node and all nodes in
	 * specified partition
	 */
	private void createPairsInPartition(int nodeID, int partitionID) {
		int mainPartitionID = this.getNodePartition(nodeID);
		Partition partition = this.refinedPartitions.get(partitionID - 1);
		HashSet<Integer> partitionNodesIDs = partition.getNodeIDs();
		Iterator<Integer> it = partitionNodesIDs.iterator();
		while (it.hasNext()) {
			int partNodeID = it.next();
			int minID = Math.min(partNodeID, nodeID);
			int maxID = Math.max(partNodeID, nodeID);
			Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>(minID,
					maxID);
			// check if pair exist
			// the only case this case could happen is between the swapped node
			// as they already a pair, and their gains were multiplied by -1
			// already
			// so we will neglect them here
			if (!this.pairsMap.containsKey(tuple)) {
				int edgeCutGain = this.getPairEdgeCutGain(nodeID,
						mainPartitionID, partNodeID, partitionID);
				int balanceGain = this.getPairBalanceGain(nodeID,
						mainPartitionID, partNodeID, partitionID);
				KLPair pair = new KLPair(minID, maxID, edgeCutGain, balanceGain);
				this.pairsMap.put(tuple, pair);
				this.pairsList.add(pair);
			}

		}

	}

	/*
	 * This method get all pairs between specified node and all nodes in
	 * specified partition then set both balance gain and edge cut gain to zero
	 */
	private void zeroPairsInPartition(int nodeID, int partitionID) {
		Partition partition = this.refinedPartitions.get(partitionID - 1);
		HashSet<Integer> partitionNodesIDs = partition.getNodeIDs();
		Iterator<Integer> it = partitionNodesIDs.iterator();
		while (it.hasNext()) {
			int partNodeID = it.next();
			int minID = Math.min(partNodeID, nodeID);
			int maxID = Math.max(partNodeID, nodeID);
			// check if pair exist
			KLPair pair = this.pairsMap.get(new Tuple<Integer, Integer>(minID,
					maxID));
			if (pair != null) {
				pair.setBalanceGain(0);
				pair.setEdgeCutGain(0);
			}
		}
	}

	/*
	 * This method will update the matrix after swapping pair of nodes, between
	 * partition
	 */
	private void updateNodeEdgesWeight_In_to_Partitions(KLPair swappedPair) {
		int node1ID = swappedPair.getSourceID();
		int node2ID = swappedPair.getDestinationID();
		int oldPartition1ID = this.getNodePartition(node2ID); // new partition2
		int oldPartition2ID = this.getNodePartition(node1ID); // new partition1
		Node[] neighbors1 = this.graph.getNode(node1ID).getNeighbors();
		Node[] neighbors2 = this.graph.getNode(node2ID).getNeighbors();
		HashSet<Integer> nodesProcessed = new HashSet<Integer>(
				neighbors1.length + 2);
		// check if there is edge between node1 and node2
		Edge edge12 = this.graph.getEdge(node1ID, node2ID);
		int edge12Weight = 0;
		if (edge12 != null) {
			edge12Weight = edge12.getWeight();
		}
		// update the matrix
		this.nodesEdgesWeight_In_to_Partitions[node1ID - 1][oldPartition1ID - 1] += edge12Weight;
		this.nodesEdgesWeight_In_to_Partitions[node1ID - 1][oldPartition2ID - 1] -= edge12Weight;
		this.nodesEdgesWeight_In_to_Partitions[node2ID - 1][oldPartition1ID - 1] -= edge12Weight;
		this.nodesEdgesWeight_In_to_Partitions[node2ID - 1][oldPartition2ID - 1] += edge12Weight;
		nodesProcessed.add(node1ID);
		nodesProcessed.add(node2ID);
		// update neighbors of the pair
		// beware that nodes are already swapped
		// loop node1 neighbors
		for (int i = 0; i < neighbors1.length; i++) {
			int neighborID = neighbors1[i].getNodeID();
			// if the neighbor processed before, do not process it again
			if (nodesProcessed.contains(neighborID)) {
				continue;
			}
			Edge edge13 = this.graph.getEdge(node1ID, neighborID);
			Edge edge23 = this.graph.getEdge(node2ID, neighborID);
			int edge13Weight = edge13.getWeight();
			int edge23Weight = 0;
			if (edge23 != null) {
				edge23Weight = edge23.getWeight();
			}
			this.nodesEdgesWeight_In_to_Partitions[neighborID - 1][oldPartition1ID - 1] += (edge23Weight - edge13Weight);
			this.nodesEdgesWeight_In_to_Partitions[neighborID - 1][oldPartition2ID - 1] += (edge13Weight - edge23Weight);
			nodesProcessed.add(neighborID);
		}
		// loop node2 neighbors
		for (int i = 0; i < neighbors2.length; i++) {
			int neighborID = neighbors2[i].getNodeID();
			// if the neighbor processed before, do not process it again
			if (nodesProcessed.contains(neighborID)) {
				continue;
			}
			Edge edge14 = this.graph.getEdge(node1ID, neighborID);
			Edge edge24 = this.graph.getEdge(node2ID, neighborID);
			int edge14Weight = 0;
			int edge24Weight = edge24.getWeight();
			if (edge14 != null) {
				edge14Weight = edge14.getWeight();
			}
			this.nodesEdgesWeight_In_to_Partitions[neighborID - 1][oldPartition1ID - 1] += (edge24Weight - edge14Weight);
			this.nodesEdgesWeight_In_to_Partitions[neighborID - 1][oldPartition2ID - 1] += (edge14Weight - edge24Weight);
		}

	}

	/*
	 * This method will return the pair that holding the max edgecut gain only
	 * the pair with zero or positive balance gain will be accepted Meaning no
	 * pair will be accepted if it will increase the imbalance of all the
	 * partitions
	 */
	private KLPair getPairWithMaxGain() {
		KLPair pairWithMaxGain = null;
		// only the pair with zero or positive balance gain
		// will be accepted
		for (int i = this.pairsList.size() - 1; i >= 0; i--) {
			KLPair pair = this.pairsList.get(i);
			if (pair.getBalanceGain() >= 0 && pair.getEdgeCutGain() >= this.minGainAllowed) {
				pairWithMaxGain = pair;
				break;
			}
		}
		return pairWithMaxGain;
	}

	/*
	 * this method check each partition for Node and is O(P) at worst, consider
	 * searching Hashset is O(1) TODO: we can make a reverse Hashmap
	 * node->partition to get it in O(1) but we can consider P as constant for
	 * now
	 */
	private int getNodePartition(int nodeID) {
		int partitionID = -1;
		for (int i = 0; i < refinedPartitions.size(); i++) {
			if (refinedPartitions.get(i).containsNode(nodeID)) {
				partitionID = refinedPartitions.get(i).getPartitionID();
				break;
			}
		}
		return partitionID;
	}

	private int[][] getNodesEdgesWeightToPartition() {
		int[][] nodesEdgesWeight_In_to_Partitions = new int[this.graph
				.getNumberOfNodes()][this.numberOfPartitions];
		;
		// loop through partitions
		for (int i = 0; i < this.refinedPartitions.size(); i++) {
			// iterate nodes in partitions
			Partition curPartition = this.refinedPartitions.get(i);
			Iterator<Integer> it = curPartition.getNodeIDs().iterator();
			;
			while (it.hasNext()) {
				int curNodeID = it.next();
				Node curNode = this.graph.getNode(curNodeID);
				Node[] curNeighbors = curNode.getNeighbors();
				// loop through neighbors
				// check if it exist in this partition add
				// the weight of edge between them as a gain
				for (int j = 0; j < curNeighbors.length; j++) {
					int curNeighborID = curNeighbors[j].getNodeID();
					int neighborPartitionID = this
							.getNodePartition(curNeighborID);
					nodesEdgesWeight_In_to_Partitions[curNodeID - 1][neighborPartitionID - 1] += this.graph
							.getEdge(curNodeID, curNeighborID).getWeight();
				}
			}
		}
		return nodesEdgesWeight_In_to_Partitions;
	}

	public void initializePairs() {

		// get all pairs gain in different partitions
		for (int i = 0; i < this.refinedPartitions.size() - 1; i++) {
			Partition partition1 = this.refinedPartitions.get(i);
			int partition1ID = partition1.getPartitionID();
			int partition1Weight = partition1.getPartitionWeight();
			for (int j = i + 1; j < this.refinedPartitions.size(); j++) {
				Partition partition2 = this.refinedPartitions.get(j);
				int partition2ID = partition2.getPartitionID();
				int partition2Weight = partition2.getPartitionWeight();
				Iterator<Integer> partition1It = partition1.getNodeIDs().iterator();
				
				while (partition1It.hasNext()) {
					int node1ID = partition1It.next();
					int node1Weight = this.graph.getNode(node1ID)
							.getNodeWeight();
					Iterator<Integer> partition2It = partition2.getNodeIDs()
							.iterator();
					while (partition2It.hasNext()) {
						int node2ID = partition2It.next();
						int node2Weight = this.graph.getNode(node2ID)
								.getNodeWeight();

						// calculate edgecut gain
						int edgeCutGain = this.getPairEdgeCutGain(node1ID,
								partition1ID, node2ID, partition2ID);
						// calculate balance gain.
						int balanceGain = this.getPairBalanceGain(node1ID,
								partition1ID, node2ID, partition2ID);
						// create pair
						int minNodeID = Math.min(node1ID, node2ID);
						int maxNodeID = Math.max(node1ID, node2ID);
						Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>(
								minNodeID, maxNodeID);
						KLPair pair;

						if (this.pairsMap.containsKey(tuple)) {
							pair = this.pairsMap.get(tuple);
						} else {
							pair = new KLPair(node1ID, node2ID, edgeCutGain,
									balanceGain);
						}

						this.pairsList.add(pair);
						this.pairsMap.put(tuple, pair);
					}

				}
			}
		}
		Collections.sort(this.pairsList);
	}

	/*
	 * this method calculate Balance gain of a couple of nodes
	 */
	private int getPairBalanceGain(int node1ID, int partition1ID, int node2ID,
			int partition2ID) {
		int partition1Weight = this.refinedPartitions.get(partition1ID - 1)
				.getPartitionWeight();
		int partition2Weight = this.refinedPartitions.get(partition2ID - 1)
				.getPartitionWeight();
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

	/*
	 * this method calculate Edge Cut gain of a couple of nodes
	 */
	private int getPairEdgeCutGain(int node1ID, int partition1ID, int node2ID,
			int partition2ID) {
		Edge edge12 = this.graph.getEdge(node1ID, node2ID);
		int edge12Weight = 0;
		if (edge12 != null) {
			edge12Weight = edge12.getWeight();
		}
		int edgeCutGain = this.nodesEdgesWeight_In_to_Partitions[node1ID-1][partition2ID-1];
		edgeCutGain += this.nodesEdgesWeight_In_to_Partitions[node2ID-1][partition1ID-1];
		edgeCutGain -= 2 * edge12Weight;
		edgeCutGain -= this.nodesEdgesWeight_In_to_Partitions[node1ID-1][partition1ID-1];
		edgeCutGain -= this.nodesEdgesWeight_In_to_Partitions[node2ID-1][partition2ID-1];

		return edgeCutGain;
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
	
	public ArrayList<Partition> getRefinedPartitions() {
		return refinedPartitions;
	}
	
	public int getNumberOfSwapsApplied() {
		return numberOfSwapsApplied;
	}
}

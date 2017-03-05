package structure;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class Partition {
	private int partitionID;
	private Graph graph;
	private IntOpenHashSet nodeIDs;
	private int partitionWeight = 0;
	private int numberOfNodes = 0;

	public Partition(Graph graph, int partitionID) {
		this.graph = graph;
		this.partitionID = partitionID;
	}

	public int addNode(int nodeID) {
		if (this.nodeIDs == null) {
			this.nodeIDs = new IntOpenHashSet();
		}
		// node already exist
		if (this.nodeIDs.contains(nodeID)) {
			return -1;
		}

		this.nodeIDs.add(nodeID);
		int nodeWeight = this.graph.getNode(nodeID).getNodeWeight();
		this.partitionWeight += nodeWeight;
		this.numberOfNodes++;

		return 0;
	}

	public int removeNode(int nodeID) {
		// partition is empty
		if (this.nodeIDs == null) {
			return -2;
		}
		// node does not exist
		if (!this.nodeIDs.contains(nodeID)) {
			return -1;
		}

		this.nodeIDs.remove(nodeID);
		int nodeWeight = this.graph.getNode(nodeID).getNodeWeight();
		this.partitionWeight -= nodeWeight;
		this.numberOfNodes--;

		return 0;
	}

	public boolean containsNode(int nodeID) {
		// partition is empty
		if (this.nodeIDs == null) {
			return false;
		}

		return this.nodeIDs.contains(nodeID);
	}

	public void print() {
		Iterator<Integer> it = this.nodeIDs.iterator();
		System.out.println();
		System.out.println("Partition # " + this.partitionID);
		System.out.println("Partition Weight = " + this.partitionWeight);
		System.out.println("Number of nodes in partition = "
				+ this.numberOfNodes);
		System.out.print("{");
		while (it.hasNext()) {
			int nodeID = it.next();
			int nodeWeight = this.graph.getNode(nodeID).getNodeWeight();
			System.out.print(nodeID + "(" + nodeWeight + ")" + ",");

		}
		System.out.println("}");

	}

	/*
	 * Getter & Setters
	 */
	public IntOpenHashSet getNodeIDs() {
		return nodeIDs;
	}

	public int getPartitionWeight() {
		return partitionWeight;
	}

	public int getNumberOfNodes() {
		return numberOfNodes;
	}

	public int getPartitionID() {
		return partitionID;
	}

	public void setPartitionID(int partitionID) {
		this.partitionID = partitionID;
		
	}

}

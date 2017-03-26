package structure;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class Partition {
	private int partitionID;
	private Graph graph;
	private IntOpenHashSet nodeIDs;
	private int partitionWeight = 0;
	private int numberOfNodes = 0;
	private IntOpenHashSet borderNodesIDs = null;

	public Partition(Graph graph, int partitionID) {
		this.graph = graph;
		this.partitionID = partitionID;
	}

	// this constructor will merge 2 partitions for the same graph
	public Partition(Graph graph, int partitionID, Partition part1, Partition part2) {
		this.graph = graph;
		this.partitionID = partitionID;
		// Add first Partition Nodes
		IntOpenHashSet firstPartNodesIDs = part1.getNodeIDs();
		IntIterator firstPartNodesIt = firstPartNodesIDs.iterator();
		while (firstPartNodesIt.hasNext()) {
			int nodeID = firstPartNodesIt.nextInt();
			this.addNode(nodeID);
		}
		// Add Second Partition Nodes
		IntOpenHashSet secondPartNodesIDs = part1.getNodeIDs();
		IntIterator secondPartNodesIt = secondPartNodesIDs.iterator();
		while (secondPartNodesIt.hasNext()) {
			int nodeID = secondPartNodesIt.nextInt();
			this.addNode(nodeID);
		}
		// Merge Border Nodes
		this.borderNodesIDs = new IntOpenHashSet();
		IntOpenHashSet part1BorderNodesIDs = part1.getBorderNodesIDs();
		IntOpenHashSet part2BorderNodesIDs = part2.getBorderNodesIDs();

		IntIterator part1BorderIt = part1BorderNodesIDs.iterator();
		while (part1BorderIt.hasNext()) {
			int curNodeID = part1BorderIt.nextInt();
			Node curNode = this.graph.getNode(curNodeID);
			Node[] curNodeNeighbors = curNode.getNeighbors();
			for (int i = 0; i < curNodeNeighbors.length; i++) {
				int neighborID = curNodeNeighbors[i].nodeID;
				if (!this.containsNode(neighborID)) {
					borderNodesIDs.add(curNodeID);
					break;
				}
			}
		}

		IntIterator part2BorderIt = part2BorderNodesIDs.iterator();
		while (part2BorderIt.hasNext()) {
			int curNodeID = part2BorderIt.nextInt();
			Node curNode = this.graph.getNode(curNodeID);
			Node[] curNodeNeighbors = curNode.getNeighbors();
			for (int i = 0; i < curNodeNeighbors.length; i++) {
				int neighborID = curNodeNeighbors[i].nodeID;
				if (!this.containsNode(neighborID)) {
					borderNodesIDs.add(curNodeID);
					break;
				}
			}
		}

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
		System.out.println("Number of nodes in partition = " + this.numberOfNodes);
		System.out.print("{");
		while (it.hasNext()) {
			int nodeID = it.next();
			int nodeWeight = this.graph.getNode(nodeID).getNodeWeight();
			System.out.print(nodeID + "(" + nodeWeight + ")" + ",");

		}
		System.out.println("}");

	}

	public boolean isPartitionNeighbor(Partition part) {
		IntOpenHashSet thisPartitionBorderNodes = this.getBorderNodesIDs();
		IntOpenHashSet otherPartitionBorderNodes = part.getBorderNodesIDs();

		IntIterator thisBorderIt = thisPartitionBorderNodes.iterator();
		while (thisBorderIt.hasNext()) {
			int borderNodeID = thisBorderIt.next();
			Node borderNode = this.graph.getNode(borderNodeID);
			Node[] borderNodeNeighbors = borderNode.getNeighbors();
			for (int i = 0; i < borderNodeNeighbors.length; i++) {
				int neighborID = borderNodeNeighbors[i].getNodeID();
				if (otherPartitionBorderNodes.contains(neighborID)) {
					return true;
				}
			}
		}
		return false;
	}

	public int getEdgeCutBetweenPartition(Partition part) {
		int edgeCut = 0;
		IntOpenHashSet thisPartitionBorderNodes = this.getBorderNodesIDs();
		IntOpenHashSet otherPartitionBorderNodes = part.getBorderNodesIDs();

		IntIterator thisBorderIt = thisPartitionBorderNodes.iterator();
		while (thisBorderIt.hasNext()) {
			int borderNodeID = thisBorderIt.next();
			Node borderNode = this.graph.getNode(borderNodeID);
			Node[] borderNodeNeighbors = borderNode.getNeighbors();
			for (int i = 0; i < borderNodeNeighbors.length; i++) {
				int neighborID = borderNodeNeighbors[i].getNodeID();
				if (otherPartitionBorderNodes.contains(neighborID)) {
					edgeCut += this.graph.getEdge(borderNodeID, neighborID).getWeight();
				}
			}
		}
		return edgeCut;
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

	public IntOpenHashSet getBorderNodesIDs() {
		if (borderNodesIDs == null) {
			borderNodesIDs = new IntOpenHashSet();
			IntOpenHashSet allPartitionNodesIDs = this.getNodeIDs();
			IntIterator allNodesIt = allPartitionNodesIDs.iterator();
			while (allNodesIt.hasNext()) {
				int curNodeID = allNodesIt.nextInt();
				Node curNode = this.graph.getNode(curNodeID);
				Node[] curNodeNeighbors = curNode.getNeighbors();
				for (int i = 0; i < curNodeNeighbors.length; i++) {
					int neighborID = curNodeNeighbors[i].nodeID;
					if (!this.containsNode(neighborID)) {
						borderNodesIDs.add(curNodeID);
						break;
					}
				}
			}
		}
		return borderNodesIDs;
	}

	public void setBorderNodesIDs(IntOpenHashSet borderNodesIDs) {
		this.borderNodesIDs = borderNodesIDs;
	}

}

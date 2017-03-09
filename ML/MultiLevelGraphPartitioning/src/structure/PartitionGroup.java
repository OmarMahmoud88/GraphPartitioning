package structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class PartitionGroup {

	Int2ObjectOpenHashMap<Partition> partitions;
	int partitionNumber;
	int edgeCut = -1; // -1 means it is not set
	Graph graph;

	public PartitionGroup(Graph graph) {
		this.graph = graph;
		this.partitions = new Int2ObjectOpenHashMap<Partition>();
		this.partitionNumber = 0;
		this.edgeCut = -1;
	}

	public PartitionGroup(PartitionGroup partsGroup) {
		this.graph = partsGroup.graph;
		this.partitions = new Int2ObjectOpenHashMap<Partition>();
		this.edgeCut = -1;
		ArrayList<RandomAccessIntHashSet> partsNodes = partsGroup.getAllPartitionsNodes();

		for (int i = 0; i < partsNodes.size(); i++) {
			Partition part = new Partition(this.graph, i + 1);
			Iterator<Integer> partsNodesIt = partsNodes.get(i).iterator();
			while (partsNodesIt.hasNext()) {
				part.addNode(partsNodesIt.next());

			}
			this.addPartition(part);
		}
	}

	public void addPartition(Partition part) {
		int partID = part.getPartitionID();
		this.partitions.put(partID, part);
		this.partitionNumber++;
		this.edgeCut = -1;
	}

	public int getPartitionNumber() {
		return partitionNumber;
	}

	public ArrayList<RandomAccessIntHashSet> getAllPartitionsNodes() {
		ArrayList<RandomAccessIntHashSet> nodesTree = new ArrayList<RandomAccessIntHashSet>();
		Iterator<Entry<Integer, Partition>> partsIt = this.partitions.entrySet().iterator();
		while (partsIt.hasNext()) {
			Map.Entry<Integer, Partition> tuple = partsIt.next();
			Partition part = tuple.getValue();
			if (part.getNumberOfNodes() > 0) {
				Iterator<Integer> it = part.getNodeIDs().iterator();
				RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
				while (it.hasNext()) {
					int nodeID = it.next();
					parentNode.add(nodeID);
				}
				nodesTree.add(parentNode);
			}
		}
		return nodesTree;
	}

	public int getEdgeCut() {
		int edgeCut = 0;
		if (this.edgeCut < 0) {
			Iterator<Entry<Integer, Partition>> partsIt = this.partitions.entrySet().iterator();
			while (partsIt.hasNext()) {
				Map.Entry<Integer, Partition> tuple = partsIt.next();
				Partition part = tuple.getValue();
				if (part.getNumberOfNodes() > 0) {
					Iterator<Integer> it = part.getNodeIDs().iterator();
					while (it.hasNext()) {
						int nodeID = it.next();
						Node curNode = this.graph.getNode(nodeID);
						Node[] neighbors = curNode.getNeighbors();
						for (int j = 0; j < neighbors.length; j++) {
							if (!part.containsNode(neighbors[j].getNodeID())) {
								edgeCut += this.graph.getEdge(nodeID, neighbors[j].getNodeID()).getWeight();
							}
						}
					}
				}
			}
			edgeCut = edgeCut / 2;
		} else {
			edgeCut = this.edgeCut;
		}
		return edgeCut;
	}

	public float getPartitionImbalance() {
		int partWeight = (int) Math.ceil(((float) this.graph.getTotalNodesWeights()) / this.partitionNumber);
		int maxPartWeight = partWeight;
		float imbalance = 0;
		Iterator<Entry<Integer, Partition>> partsIt = this.partitions.entrySet().iterator();
		while (partsIt.hasNext()) {
			Map.Entry<Integer, Partition> tuple = partsIt.next();
			Partition part = tuple.getValue();
			int curPartitionWeight = part.getPartitionWeight();
			if (curPartitionWeight > partWeight) {
				maxPartWeight = curPartitionWeight;
			}
		}

		imbalance = ((float) maxPartWeight) / partWeight;
		return imbalance;
	}

	public Partition getPartition(int partID) {
		Partition part = this.partitions.get(partID);
		return part;
	}

	public int getNodePartitionID(int nodeID) {
		Iterator<Entry<Integer, Partition>> it = this.partitions.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, Partition> entry = it.next();
			Partition part = entry.getValue();
			if (part.containsNode(nodeID)) {
				return part.getPartitionID();
			}
		}
		return -1;
	}

	public boolean containsPartition(int curPartID) {
		return this.partitions.containsKey(curPartID);
	}

	public String toString() {
		StringBuilder parts = new StringBuilder();
		for (int i = 1; i <= this.graph.getNumberOfNodes(); i++) {
			parts.append(this.getNodePartitionID(i) + "\r\n");
		}

		return parts.toString();
	}
}

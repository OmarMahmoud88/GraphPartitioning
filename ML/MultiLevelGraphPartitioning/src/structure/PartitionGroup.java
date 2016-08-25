package structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PartitionGroup {

	HashMap<Integer, Partition> partitions;
	int partitionNumber;
	int edgeCut = -1; // -1 means it is not set
	Graph graph;

	public PartitionGroup(Graph graph) {
		this.graph = graph;
		this.partitions = new HashMap<Integer, Partition>();
		this.partitionNumber = 0;
		this.edgeCut = -1;
	}

	public void addPartition(Partition part) {
		this.partitions.put(part.getPartitionID(), part);
		this.partitionNumber++;
		this.edgeCut = -1;
	}

	public int getPartitionNumber() {
		return partitionNumber;
	}

	public int getEdgeCut() {
		int edgeCut = 0;
		if (this.edgeCut < 0) {
			Iterator<Entry<Integer, Partition>> partsIt = this.partitions.entrySet().iterator();
			while (partsIt.hasNext()) {
				Map.Entry<Integer, Partition> tuple = partsIt.next();
				Partition part = tuple.getValue();
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
			edgeCut = edgeCut / 2;
		} else {
			edgeCut = this.edgeCut;
		}
		return edgeCut;
	}

	public Partition getPartition(int partID) {
		Partition part = this.partitions.get(partID);
		return part;
	}
}

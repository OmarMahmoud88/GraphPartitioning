package structure;

import java.util.ArrayList;
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
	
	public PartitionGroup(PartitionGroup partsGroup) {
		this.graph = partsGroup.graph;
		this.partitions = new HashMap<Integer, Partition>();
		this.edgeCut = -1;
		ArrayList<ArrayList<Integer>> partsNodes = partsGroup.getAllPartitionsNodes();
		for (int i = 0; i < partsNodes.size(); i++) {
			Partition part = new Partition(this.graph, i+1);
			for (int j = 0; j < partsNodes.get(i).size(); j++) {
				part.addNode(partsNodes.get(i).get(j));
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

	public ArrayList<ArrayList<Integer>> getAllPartitionsNodes() {
		ArrayList<ArrayList<Integer>> nodesTree = new ArrayList<ArrayList<Integer>>();
		Iterator<Entry<Integer, Partition>> partsIt = this.partitions.entrySet().iterator();
		while (partsIt.hasNext()) {
			Map.Entry<Integer, Partition> tuple = partsIt.next();
			Partition part = tuple.getValue();
			Iterator<Integer> it = part.getNodeIDs().iterator();
			ArrayList<Integer> parentNode = new ArrayList<>(part.getNumberOfNodes());
			while (it.hasNext()) {
				int nodeID = it.next();
				parentNode.add(nodeID);
			}
			nodesTree.add(parentNode);
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
	
	public float getPartitionImbalance(){
		int partWeight = (int) Math.ceil(((float)this.graph.getTotalNodesWeights())/this.partitionNumber);
		int maxPartWeight = partWeight;
		float imbalance = 0;
		Iterator<Entry<Integer, Partition>> partsIt = this.partitions.entrySet().iterator();
		while (partsIt.hasNext()) {
			Map.Entry<Integer, Partition> tuple = partsIt.next();
			Partition part = tuple.getValue();
			int curPartitionWeight = part.getPartitionWeight();
			if(curPartitionWeight > partWeight){
				maxPartWeight = curPartitionWeight;
			}
		}
		
		imbalance = ((float)maxPartWeight)/partWeight;
		return imbalance;
	}

	public Partition getPartition(int partID) {
		Partition part = this.partitions.get(partID);
		return part;
	}
	
	public int getNodePartitionID(int nodeID){
		Iterator<Entry<Integer, Partition>> it = this.partitions.entrySet().iterator();
		while(it.hasNext()){
			Entry<Integer, Partition> entry = it.next();
			Partition part = entry.getValue();
			if(part.containsNode(nodeID)){
				return part.getPartitionID();
			}
		}
		return -1;
	}

	public boolean containsPartition(int curPartID) {
		return this.partitions.containsKey(curPartID);
	}
}

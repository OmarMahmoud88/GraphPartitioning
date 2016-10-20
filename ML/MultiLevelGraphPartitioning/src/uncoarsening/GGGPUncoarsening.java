package uncoarsening;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import partitioning.GreedyGraphGrowingPartitioning;
import partitioning.Partitioning;
import refinement.NaiiveKLRefinement;
import structure.CoarseGraph;
import structure.Graph;
import structure.Partition;
import structure.PartitionGroup;
import structure.SubGraph;

public class GGGPUncoarsening extends Uncoarsening {

	@Override
	public PartitionGroup Uncoarsen(Graph originalGraph, CoarseGraph cGraph) {
		// TODO Auto-generated method stub
		PartitionGroup parts = new PartitionGroup(originalGraph);
		ArrayList<ArrayList<Integer>> nodesTree = cGraph.getNodesTree();
		int orgPartitionID = 1;
		for (int i = 0; i < nodesTree.size(); i++) {

			if (nodesTree.get(i).size() < 3) {
				for (int j = 0; j < nodesTree.get(i).size(); j++) {
					Partition origPart = new Partition(originalGraph, orgPartitionID);
					int origNodeID = nodesTree.get(i).get(j);
					origPart.addNode(origNodeID);
					parts.addPartition(origPart);
					orgPartitionID++;
				}
			} else {
				Graph subGraph = new SubGraph(originalGraph, nodesTree.get(i));
				int numOfTrials = Math.min(10, subGraph.getNumberOfNodes() / 2);
				Partitioning partAlg = new GreedyGraphGrowingPartitioning(subGraph, 2, numOfTrials, (float) 0.1);
				PartitionGroup partsGroup = partAlg.getPartitions(subGraph, 2, numOfTrials);
				NaiiveKLRefinement kl = null;
				kl = new NaiiveKLRefinement(subGraph, partsGroup, 10, 0, (float) 0.1);
				PartitionGroup refinedParts = kl.getRefinedPartitions();
				for (int j = 1; j <= refinedParts.getPartitionNumber(); j++) {
					Partition origPart = new Partition(originalGraph, orgPartitionID);
					Partition subPart = refinedParts.getPartition(j);
					HashSet<Integer> subPartNodes = subPart.getNodeIDs();
					Iterator<Integer> it = subPartNodes.iterator();
					while (it.hasNext()) {
						int subNodeID = it.next();
						int orgNodeID = ((SubGraph) subGraph).getOriginalNodeID(subNodeID);
						origPart.addNode(orgNodeID);

					}
					parts.addPartition(origPart);
					orgPartitionID++;
				}
			}

		}

		return parts;
	}

}

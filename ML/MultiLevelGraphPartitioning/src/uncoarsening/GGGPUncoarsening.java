package uncoarsening;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import partitioning.GreedyGraphGrowingPartitioning;
import partitioning.Partitioning;
import refinement.FMRefinement;
import structure.CoarseGraph;
import structure.Graph;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomSet;

public class GGGPUncoarsening extends Uncoarsening {

	@Override
	public PartitionGroup Uncoarsen(Graph originalGraph, CoarseGraph cGraph) {
		PartitionGroup parts = new PartitionGroup(originalGraph);
		ArrayList<RandomSet<Integer>> nodesTree = cGraph.getNodesTree();
		int orgPartitionID = 1;
		for (int i = 0; i < nodesTree.size(); i++) {
			if (nodesTree.get(i).size() < 3) {
				Iterator<Integer> childsIt = nodesTree.get(i).iterator();
				while (childsIt.hasNext()) {
					Partition origPart = new Partition(originalGraph, orgPartitionID);
					int origNodeID = childsIt.next();
					origPart.addNode(origNodeID);
					parts.addPartition(origPart);
					orgPartitionID++;
				}
			} else {
//				Graph subGraph = new SubGraph(originalGraph, nodesTree.get(i));
				int numOfTrials = Math.min(10, nodesTree.get(i).size() / 2);
				Partitioning partAlg = new GreedyGraphGrowingPartitioning(originalGraph, 2, numOfTrials, (float) 0.1);
				PartitionGroup partsGroup = partAlg.getPartitions(originalGraph, nodesTree.get(i), 2, numOfTrials);
				FMRefinement fm = new FMRefinement(originalGraph, nodesTree.get(i), partsGroup, 10, 10, -1000, (float) 0.1);
				PartitionGroup refinedParts = fm.getRefinedPartitions();
				
				Partition part1 = refinedParts.getPartition(1);
				part1.setPartitionID(orgPartitionID);
				parts.addPartition(part1);
				orgPartitionID++;
				Partition part2 = refinedParts.getPartition(2);
				part2.setPartitionID(orgPartitionID);
				parts.addPartition(part2);
				orgPartitionID++;
			}

		}

		return parts;
	}

}

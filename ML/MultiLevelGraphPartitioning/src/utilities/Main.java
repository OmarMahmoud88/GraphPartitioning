package utilities;

import java.util.ArrayList;
import java.util.Iterator;

import partitioning.GreedyGraphGrowingPartitioning;
import refinement.KLRefinement;
import structure.CoarseGraph;
import structure.Graph;
import structure.Node;
import structure.Partition;
import coarsening.HeavyEdgeMatching;

public class Main {

	public static void main(String[] args) {
		String fileSrc = "graphs/4elt.graph";
		Graph x = new Graph(fileSrc);
		ArrayList<Graph> graphs = new ArrayList<Graph>();
		graphs.add(x);
		Graph last = x;
		while (last.getNumberOfNodes() > 100) {
			graphs.add(new CoarseGraph(last, HeavyEdgeMatching.coarse(last)));
			last = graphs.get(graphs.size() - 1);
		}
		GreedyGraphGrowingPartitioning gGGP = new GreedyGraphGrowingPartitioning(
				last,2, 20, 0);
		ArrayList<Partition> partitions = gGGP.getPartitions(last, 2, 20);

		KLRefinement kl = new KLRefinement(last, partitions, 10, 0, (float)0.0);
		ArrayList<Partition> refinedParts = kl.getRefinedPartitions();
		System.out.println("number of swaps = " + kl.getNumberOfSwapsApplied());
		System.out.println("edge Cut after refinement = " + getEdgeCut(refinedParts, last));
//		for (int i = 0; i < partitions.size(); i++) {
//			Partition currentPartition = partitions.get(i);
//			currentPartition.print();
//		}
		// ArrayList<ArrayList<Integer>> nodesTree1 = hEM1.coarse(x);
		// CoarseGraph x1 = new CoarseGraph(x, nodesTree1);
		// ArrayList<ArrayList<Integer>> nodesTree2 = hEM1.coarse(x1);
		// CoarseGraph x2 = new CoarseGraph(x1, nodesTree2);
		// x2.printGraph();
		// for (int i = 0; i < nodesTree1.size(); i++) {
		// System.out.print(i + "==>(");
		// for (int j = 0; j < nodesTree1.get(i).size(); j++) {
		// System.out.print(nodesTree1.get(i).get(j) + ",");
		// }
		// System.out.println(")");
		// }
		// for (int i = 0; i < nodesTree2.size(); i++) {
		// System.out.print(i + "==>(");
		// for (int j = 0; j < nodesTree2.get(i).size(); j++) {
		// System.out.print(nodesTree2.get(i).get(j) + ",");
		// }
		// System.out.println(")");
		// }
	}
	
	private static long getEdgeCut(ArrayList<Partition> partitions, Graph graph) {
		long edgeCut = 0;
		for (int i = 0; i < partitions.size(); i++) {
			Iterator<Integer> it = partitions.get(i).getNodeIDs().iterator();
			while (it.hasNext()) {
				int nodeID = it.next();
				Node curNode = graph.getNode(nodeID);
				Node[] neighbors = curNode.getNeighbors();
				for (int j = 0; j < neighbors.length; j++) {
					if (!partitions.get(i).containsNode(
							neighbors[j].getNodeID())) {
						edgeCut += graph.getEdge(nodeID,
								neighbors[j].getNodeID()).getWeight();
					}
				}
			}
		}
		return edgeCut / 2;
	}

}

package uncoarsening;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import algorithms.METIS_Origin;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import statistics.METIS_Origin_Run;
import structure.CoarseGraph;
import structure.Graph;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;
import structure.SubGraph;

public class METIS_Uncoarsening extends Uncoarsening {

	@Override
	public PartitionGroup Uncoarsen(Graph originalGraph, CoarseGraph cGraph) {
		PartitionGroup parts = new PartitionGroup(originalGraph);
		ArrayList<RandomAccessIntHashSet> nodesTree = cGraph.getNodesTree();
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
				Graph subGraph = new SubGraph(originalGraph, nodesTree.get(i));
				subGraph.setGraphName("SubGraph");
				PartitionGroup partsGroup = null;
				try {
					@SuppressWarnings("unchecked")
					Class<Object> coarseningClass = (Class<Object>) Class
							.forName("coarsening.OrderedHeavyEdgeMatching");
					@SuppressWarnings("unchecked")
					Class<Object> partitioningClass = (Class<Object>) Class
							.forName("partitioning.GreedyGraphGrowingPartitioning");
					METIS_Origin mO = new METIS_Origin(subGraph, 2, coarseningClass, partitioningClass, 4, (float) 0.1,
							1, 1, 10, 10, -1000);

					PartitionGroup subParts = null;
					try {
						subParts = ((METIS_Origin_Run) mO.partitionGraph()).getPartsGroup();
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					partsGroup = new PartitionGroup(originalGraph);
					ArrayList<Partition> subPartsList = subParts.getAllPartitions();

					for (Iterator<Partition> subPartsIt = subPartsList.iterator(); subPartsIt.hasNext();) {
						Partition subPart = (Partition) subPartsIt.next();
						Partition orgPart = new Partition(originalGraph, subPart.getPartitionID());
						IntOpenHashSet subPartNodesIDs = subPart.getNodeIDs();
						for (IntIterator subNodesIt = subPartNodesIDs.iterator(); subNodesIt.hasNext();) {
							int subNodeID = subNodesIt.nextInt();
							int orgNodeID = ((SubGraph) subGraph).getOriginalNodeID(subNodeID);
							orgPart.addNode(orgNodeID);
						}
						partsGroup.addPartition(orgPart);
					}
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Partition part1 = partsGroup.getPartition(1);
				part1.setPartitionID(orgPartitionID);
				parts.addPartition(part1);
				orgPartitionID++;
				Partition part2 = partsGroup.getPartition(2);
				part2.setPartitionID(orgPartitionID);
				parts.addPartition(part2);
				orgPartitionID++;
			}

		}

		return parts;
	}

}

package utilities;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import coarsening.Matching;
import partitioning.Partitioning;
import refinement.KLRefinement1;
import refinement.KLRefinement2;
import structure.CoarseGraph;
import structure.Graph;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;
import uncoarsening.GGGPUncoarsening;
import uncoarsening.Uncoarsening;

public class Main {
	private static int numberOfPartitions = 2;
	private static float minPartitionWeight;
	private static float maxPartitionWeight;
	private static float weightImbalanceRatio = 0;

	public static void main(String[] args)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException {
		String[] graphNames = getGraphNames("graphs");
		// loop all graphs
		for (int i = 0; i < graphNames.length; i++) {
			if (!graphNames[i].equals("3elt")) {
				continue;
			}
			String fileSrc = "graphs/" + graphNames[i] + ".graph";

			Graph originalGraph = new Graph(fileSrc);

			// if (originalGraph.getNumberOfNodes() < 5000) {
			// continue;
			// }
			System.out.println(graphNames[i]);
			System.out.println("Number of partitions = " + numberOfPartitions);
			minPartitionWeight = (((float) originalGraph.getTotalNodesWeights()) / numberOfPartitions)
					* (1 - weightImbalanceRatio);
			maxPartitionWeight = (((float) originalGraph.getTotalNodesWeights()) / numberOfPartitions)
					* (1 + weightImbalanceRatio);

			// get list of coarsen Class available in coarsening package
			ArrayList<Class<Object>> coarseningClasses = getClasses("bin/coarsening", "coarsening"); // loop
																										// allcoarsening
																										// schemes
			for (int j = 0; j < coarseningClasses.size(); j++) {
				if (coarseningClasses.get(j).getName().contains("GabowWeightedMatching")
						|| coarseningClasses.get(j).getName().contains("Blossom")
						|| !coarseningClasses.get(j).getName().contains("Heav"))
					continue;
				ArrayList<Graph> graphs = new ArrayList<Graph>();
				graphs.add(originalGraph);
				Graph intermediate = originalGraph;
				Matching match = (Matching) coarseningClasses.get(j).newInstance();
				System.out.println(match.getSchemeName()); // coarse graph
															// repeatedly till
															// defined threshold
				ArrayList<ArrayList<Integer>> originalNodesTree = null;
				int lastGraphNodesNumber = originalGraph.getNumberOfNodes() + 1;
				while (intermediate.getNumberOfNodes() > 100
						&& lastGraphNodesNumber > intermediate.getNumberOfNodes()) {
					lastGraphNodesNumber = intermediate.getNumberOfNodes();
					ArrayList<ArrayList<Integer>> nodesTree = match.coarse(intermediate, 100, maxPartitionWeight);
					ArrayList<ArrayList<Integer>> mappedNodesTree = new ArrayList<ArrayList<Integer>>(nodesTree.size()); // map
																															// the
																															// nodestree
																															// to
																															// the
																															// original
																															// graph
					if (originalNodesTree == null) {
						originalNodesTree = nodesTree;
					} else {
						for (int k = 0; k < nodesTree.size(); k++) {
							ArrayList<Integer> currentNodes = new ArrayList<Integer>();
							for (int l = 0; l < nodesTree.get(k).size(); l++) {
								int originalIndex = nodesTree.get(k).get(l) - 1;
								currentNodes.addAll(originalNodesTree.get(originalIndex));
							}
							mappedNodesTree.add(currentNodes);
						}
						originalNodesTree = mappedNodesTree;
					}
					intermediate = new CoarseGraph(originalGraph, originalNodesTree);
				}
				Graph cGraph = intermediate;
				// loop partitioning algorithms
				ArrayList<Class<Object>> partitioningClasses = getClasses("bin/partitioning", "partitioning");
				for (int k = 0; k < partitioningClasses.size(); k++) {
//					if(!partitioningClasses.get(k).getName().contains("Greedy")) continue;
					System.out.println(partitioningClasses.get(k).getName());
					Constructor<Object> partConstructor = partitioningClasses.get(k).getConstructor(Graph.class,
							Integer.TYPE, Integer.TYPE, Float.TYPE);
					Partitioning gGGP = (Partitioning) partConstructor.newInstance(cGraph, numberOfPartitions, 1000, 0);
					PartitionGroup partsGroup = gGGP.getPartitions(cGraph, numberOfPartitions, 20);
					System.out.println("edge Cut before refinement = " + partsGroup.getEdgeCut());
					System.out.println("Partition Imbalance before refinement = " + partsGroup.getPartitionImbalance());
//					KLRefinement1 kl1 = new KLRefinement1(cGraph, partsGroup, 500, 0, (float) 0.0);
					KLRefinement2 kl2 = new KLRefinement2(cGraph, partsGroup, 500, 20, -100, (float) 0.1);
//					PartitionGroup refinedParts1;
					PartitionGroup refinedParts2;
//					refinedParts1 = kl1.getRefinedPartitions();
					refinedParts2 = kl2.getRefinedPartitions();
					int counter = 2;
					// uncoarse Graph
					Graph curGraph = (CoarseGraph) cGraph;
					Uncoarsening gGGP_UC = new GGGPUncoarsening();
					while (curGraph.getNumberOfNodes() < originalGraph.getNumberOfNodes() / 2) {
						PartitionGroup pG = gGGP_UC.Uncoarsen(originalGraph, (CoarseGraph) curGraph);
						Graph prevGraph = new CoarseGraph(originalGraph, pG.getAllPartitionsNodes());
//						PartitionGroup uncoarsenPartitions1 = uncoarsenPartitions((CoarseGraph) curGraph, prevGraph,
//								refinedParts1);
						PartitionGroup uncoarsenPartitions2 = uncoarsenPartitions((CoarseGraph) curGraph, prevGraph,
								refinedParts2);
//						kl1 = new KLRefinement1(prevGraph, uncoarsenPartitions1, 500, 0, (float) 0.0);
						kl2 = new KLRefinement2(prevGraph, uncoarsenPartitions2, 500, 20, -100, (float) 0.1/counter);
//						refinedParts1 = kl1.getRefinedPartitions();
						refinedParts2 = kl2.getRefinedPartitions();

						curGraph = prevGraph;
						counter *= 2;
					}
					
//					PartitionGroup uncoarsenPartitions1 = uncoarsenPartitions((CoarseGraph) curGraph, originalGraph,
//							refinedParts1);
					PartitionGroup uncoarsenPartitions2 = uncoarsenPartitions((CoarseGraph) curGraph, originalGraph,
							refinedParts2);
					
//					kl1 = new KLRefinement1(originalGraph, uncoarsenPartitions1, 500, 0, (float) 0.0);
					kl2 = new KLRefinement2(originalGraph, uncoarsenPartitions2, 500, 20, -100, (float) 0.0);
//					refinedParts1 = kl1.getRefinedPartitions();
					refinedParts2 = kl2.getRefinedPartitions();
//					System.out.println("edge Cut after refinement1 = " + refinedParts1.getEdgeCut());
					System.out.println("edge Cut after refinement2 = " + refinedParts2.getEdgeCut());
					System.out.println("Partition Imbalance after refinement = " + refinedParts2.getPartitionImbalance());
				}

			}

		}
	}

	private static PartitionGroup uncoarsenPartitions(CoarseGraph curGraph, Graph previousGraph,
			PartitionGroup refinedParts) {
		Node[] prevGraphNodes = previousGraph.getNodes();
		PartitionGroup uncoarsenParts = new PartitionGroup(previousGraph);
		for (int i = 0; i < prevGraphNodes.length; i++) {
			Node prevGraphNode = prevGraphNodes[i];
			// get original graph nodes
			ArrayList<Integer> prevGraphNodeChilds = previousGraph.getNodeChilds(prevGraphNode.getNodeID());
			// get the parent node in curGraph
			int curGraphNodeID = curGraph.getParentNodeIDOf(prevGraphNodeChilds.get(0));
			// get parentNode Partition
			int curPartID = refinedParts.getNodePartitionID(curGraphNodeID);
			Partition part = null;
			// check if the current partition exists
			if (uncoarsenParts.containsPartition(curPartID)) {
				part = uncoarsenParts.getPartition(curPartID);
			} else {
				part = new Partition(previousGraph, curPartID);
				uncoarsenParts.addPartition(part);
			}
			// add node to part
			part.addNode(prevGraphNode.getNodeID());

		}
		return uncoarsenParts;
	}

	private static String[] getGraphNames(String folderPath) {

		File folder = new File(folderPath);
		FileFilter graphFileFilter = new GraphFileFilter();
		File[] listOfFiles = folder.listFiles(graphFileFilter);
		String[] graphNames = new String[listOfFiles.length];
		for (int i = 0; i < listOfFiles.length; i++) {
			graphNames[i] = listOfFiles[i].getName().replace(".graph", "");
		}
		return graphNames;
	}

	private static ArrayList<Class<Object>> getClasses(String packagePath, String packageName)
			throws ClassNotFoundException {
		File folder = new File(packagePath);
		FileFilter classFileFilter = new ClassFileFilter();
		File[] listOfFiles = folder.listFiles(classFileFilter);
		String[] classNames = new String[listOfFiles.length];
		for (int i = 0; i < listOfFiles.length; i++) {
			classNames[i] = listOfFiles[i].getName().replace(".class", "");
		}

		ArrayList<Class<Object>> classes = new ArrayList<Class<Object>>();
		for (int i = 0; i < classNames.length; i++) {
			@SuppressWarnings("unchecked")
			Class<Object> cls = (Class<Object>) Class.forName(packageName + "." + classNames[i]);
			if (!Modifier.isAbstract(cls.getModifiers())) {
				classes.add(cls);
			}
		}
		return classes;
	}
}

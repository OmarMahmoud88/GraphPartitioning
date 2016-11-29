package utilities;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import coarsening.Matching;
import partitioning.Partitioning;
import refinement.FMRefinement;
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
	private static float maxCoarseNodeWeight;

	final private static HashSet<String> coarseningSchemes = new HashSet<String>(
			Arrays.asList("coarsening.HeavyEdgeMatching"));
	final private static HashSet<String> partitioningSchemes = new HashSet<String>(
			Arrays.asList("partitioning.GreedyGraphGrowingPartitioning"));
//	final private static HashSet<String> excludedGraphs = new HashSet<String>(
//			Arrays.asList("brack2x", "wing", "m14b", "fe_rotor", "144", "auto", "memplus",
//					"bcsstk33", "bcsstk31", "598a", "fe_ocean", "fe_tooth", "wave"));
	final private static HashSet<String> excludedGraphs = new HashSet<String>(
			Arrays.asList());
	final private static int numberOfTrials = 10;
	final private static int numberOfRuns = 100;
	final private static float imbalanceRatio = (float) 0.001;
	final private static int refinementIterations = 100;
	final private static int maxCoarsenGraphNumOfNodes = 100;
	final private static int maxNegativeRefinementSteps = 20;
	final private static int maxNegativeRefinementGain = -10000;

	public static void main(String[] args)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException {
		String[] graphNames = getGraphNames("../../graphs");
		// loop all graphs
		for (int i = 0; i < graphNames.length; i++) {
			if (!graphNames[i].equals("cti")) {
				continue;
			}
			if (excludedGraphs.contains(graphNames[i]))
				continue;
			String fileSrc = "../../graphs/" + graphNames[i] + ".graph";

			Graph originalGraph = new Graph(fileSrc);
			System.out.println(graphNames[i]);
			// if (originalGraph.getNumberOfNodes() > 20000) {
			// System.out.println("skipped");
			// continue;
			// }
			System.out.println("Number of partitions = " + numberOfPartitions);
			System.out.println("imbalanceRatio = " + imbalanceRatio);

			maxCoarseNodeWeight = (((float) originalGraph.getTotalNodesWeights()) / (numberOfPartitions));

			// multiple Runs
			int run_ID = 1;
			while (run_ID <= numberOfRuns) {
				System.out.println("Run #" + run_ID);

				// get list of coarsen Class available in coarsening package
				ArrayList<Class<Object>> coarseningClasses = getClasses("bin/coarsening", "coarsening"); // loop
																											// allcoarsening
																											// schemes
				for (int j = 0; j < coarseningClasses.size(); j++) {
					if (!coarseningSchemes.contains(coarseningClasses.get(j).getName()))
						continue;
					ArrayList<Graph> graphs = new ArrayList<Graph>();
					graphs.add(originalGraph);
					Graph intermediate = originalGraph;
					Matching match = (Matching) coarseningClasses.get(j).newInstance();
					System.out.println(match.getSchemeName()); // coarse graph
																// repeatedly
																// till
																// defined
																// threshold
					ArrayList<ArrayList<Integer>> originalNodesTree = null;
					int lastGraphNodesNumber = originalGraph.getNumberOfNodes() + 11;
					while (intermediate.getNumberOfNodes() > 100
							&& lastGraphNodesNumber - intermediate.getNumberOfNodes() > 10) {
						lastGraphNodesNumber = intermediate.getNumberOfNodes();
						ArrayList<ArrayList<Integer>> nodesTree = match.coarse(intermediate, maxCoarsenGraphNumOfNodes,
								maxCoarseNodeWeight);
						// map the nodes tree to the original graph
						ArrayList<ArrayList<Integer>> mappedNodesTree = new ArrayList<ArrayList<Integer>>(
								nodesTree.size());
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
					// System.out.println(cGraph.getNumberOfNodes());
					// loop partitioning algorithms
					ArrayList<Class<Object>> partitioningClasses = getClasses("bin/partitioning", "partitioning");
					for (int k = 0; k < partitioningClasses.size(); k++) {
						if (!partitioningSchemes.contains(partitioningClasses.get(k).getName()))
							continue;
						System.out.println(partitioningClasses.get(k).getName());
						Constructor<Object> partConstructor = partitioningClasses.get(k).getConstructor(Graph.class,
								Integer.TYPE, Integer.TYPE, Float.TYPE);
						Partitioning gGGP = (Partitioning) partConstructor.newInstance(cGraph, numberOfPartitions,
								numberOfTrials, imbalanceRatio);
						PartitionGroup partsGroup = gGGP.getPartitions(cGraph, numberOfPartitions, 20);
						System.out.println("edge Cut before refinement = " + partsGroup.getEdgeCut());
						System.out.println(
								"Partition Imbalance before refinement = " + partsGroup.getPartitionImbalance());
						// KLRefinement1 kl1 = new KLRefinement1(cGraph,
						// partsGroup,
						// 500, 0, (float) 0.0);
						// KLRefinement2 kl2 = new KLRefinement2(cGraph,
						// partsGroup, refinementIterations,
						// maxNegativeRefinementSteps,
						// maxNegativeRefinementGain, (float) 0.1);
						FMRefinement fm = new FMRefinement(cGraph, partsGroup, refinementIterations,
								maxNegativeRefinementSteps, maxNegativeRefinementGain, imbalanceRatio);
						// PartitionGroup refinedParts1;
						PartitionGroup refinedParts2;
						// refinedParts1 = kl1.getRefinedPartitions();
						// refinedParts2 = kl2.getRefinedPartitions();
						refinedParts2 = fm.getRefinedPartitions();
						int counter = 2;
						// uncoarse Graph
						Graph curGraph = (CoarseGraph) cGraph;
						Uncoarsening gGGP_UC = new GGGPUncoarsening();
						while (curGraph.getNumberOfNodes() < originalGraph.getNumberOfNodes() / 2) {
							PartitionGroup pG = gGGP_UC.Uncoarsen(originalGraph, (CoarseGraph) curGraph);
							Graph prevGraph = new CoarseGraph(originalGraph, pG.getAllPartitionsNodes());
							// PartitionGroup uncoarsenPartitions1 =
							// uncoarsenPartitions((CoarseGraph) curGraph,
							// prevGraph,
							// refinedParts1);
							PartitionGroup uncoarsenPartitions2 = uncoarsenPartitions((CoarseGraph) curGraph, prevGraph,
									refinedParts2);
							// kl1 = new KLRefinement1(prevGraph,
							// uncoarsenPartitions1, 500, 0, (float) 0.0);
							// kl2 = new KLRefinement2(prevGraph,
							// uncoarsenPartitions2, refinementIterations,
							// maxNegativeRefinementSteps,
							// maxNegativeRefinementGain, imbalanceRatio);
							fm = new FMRefinement(prevGraph, uncoarsenPartitions2, refinementIterations,
									maxNegativeRefinementSteps, maxNegativeRefinementGain,  Math.max(((float)0.2/1000), imbalanceRatio));
							// refinedParts1 = kl1.getRefinedPartitions();
							// refinedParts2 = kl2.getRefinedPartitions();
							refinedParts2 = fm.getRefinedPartitions();

							curGraph = prevGraph;
							counter ++;
						}

						// PartitionGroup uncoarsenPartitions1 =
						// uncoarsenPartitions((CoarseGraph) curGraph,
						// originalGraph,
						// refinedParts1);
						PartitionGroup uncoarsenPartitions2 = uncoarsenPartitions((CoarseGraph) curGraph, originalGraph,
								refinedParts2);

						// kl1 = new KLRefinement1(originalGraph,
						// uncoarsenPartitions1, 500, 0, (float) 0.0);
						// kl2 = new KLRefinement2(originalGraph,
						// uncoarsenPartitions2, 1000, 20,
						// maxNegativeRefinementGain, imbalanceRatio);
						fm = new FMRefinement(originalGraph, uncoarsenPartitions2, 10000,
								maxNegativeRefinementSteps, maxNegativeRefinementGain, imbalanceRatio);
						// refinedParts1 = kl1.getRefinedPartitions();
						// refinedParts2 = kl2.getRefinedPartitions();
						refinedParts2 = fm.getRefinedPartitions();
						// System.out.println("edge Cut after refinement1 = " +
						// refinedParts1.getEdgeCut());
						System.out.println("edge Cut after refinement2 = " + refinedParts2.getEdgeCut());
						System.out.println(
								"Partition Imbalance after refinement = " + refinedParts2.getPartitionImbalance());
						// verify partitions 1
						boolean verify = true;
						HashSet<Integer> allNodes = new HashSet<Integer>(originalGraph.getNumberOfNodes());
						for (int l = 0; l < originalGraph.getNumberOfNodes(); l++) {
							allNodes.add(l + 1);
						}
						for (int l = 0; l < refinedParts2.getPartitionNumber() && verify; l++) {
							Partition part = refinedParts2.getPartition(l + 1);
							Iterator<Integer> nodesIt = part.getNodeIDs().iterator();
							while (nodesIt.hasNext()) {
								int nodeID = nodesIt.next();
								if (allNodes.contains(nodeID)) {
									allNodes.remove(nodeID);
								} else {
									verify = false;
									break;
								}
							}
						}

						if (!allNodes.isEmpty()) {
							verify = false;
						}
						if (verify) {
							System.out.println("verification success");
						} else {
							System.out.println("verification failed");
						}

					}

				}
				run_ID++;
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

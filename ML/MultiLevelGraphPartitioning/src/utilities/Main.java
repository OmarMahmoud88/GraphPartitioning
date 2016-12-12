package utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

import coarsening.Matching;
import partitioning.Partitioning;
import refinement.FMRefinement;
import structure.CoarseGraph;
import structure.Graph;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomSet;
import uncoarsening.GGGPUncoarsening;
import uncoarsening.Uncoarsening;

public class Main {
	private static int numberOfPartitions = 2;
	private static float maxCoarseNodeWeight;

	final private static HashSet<String> coarseningSchemes = new HashSet<String>(
			Arrays.asList("coarsening.HeavyEdgeMatching"));
	final private static HashSet<String> partitioningSchemes = new HashSet<String>(
			Arrays.asList("partitioning.GreedyGraphGrowingPartitioning"));
	// final private static HashSet<String> excludedGraphs = new
	// HashSet<String>(
	// Arrays.asList("brack2x", "wing", "m14b", "fe_rotor", "144", "auto",
	// "memplus",
	// "bcsstk33", "bcsstk31", "598a", "fe_ocean", "fe_tooth", "wave"));
	// final private static HashSet<String> excludedGraphs = new
	// HashSet<String>(Arrays.asList("brack2", "144", "m14b", "memplus",
	// "598a"));
	final private static HashSet<String> excludedGraphs = new HashSet<String>(Arrays.asList());
	final private static int numberOfTrials = 10;
	final private static int numberOfRuns = 100;
	final private static float[] imbalanceRatiosList = new float[] { (float) 0.001999, (float) 0.010999,
			(float) 0.030999, (float) 0.050999, (float) 0.100999 };
	private static float imbalanceRatio = (float) 0.001;
	final private static int refinementIterations = 100;
	private static int maxCoarsenGraphNumOfNodes = 100;
	final private static int maxNegativeRefinementSteps = 100;
	final private static int finalRefinementIterations = 100;
	final private static int maxFinalNegativeRefinementSteps = 100;
	final private static int maxNegativeRefinementGain = -10000;

	public static void main(String[] args)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException, IOException {
		String[] graphNames = getGraphNames("../../graphs");
		Path resultsFolderPath = Paths.get("../../results");
		// Check if folder Exists, if not create it
		if (!Files.exists(resultsFolderPath)) {
			Files.createDirectories(resultsFolderPath);
		}
		// loop number of partitions
		for (int x = 2; x < 33; x *= 2) {
			numberOfPartitions = x;
			maxCoarsenGraphNumOfNodes = 30 * x;
			// partition folder
			Path partitionFolderPath = Paths.get(resultsFolderPath.toString() + "/Parts_" + numberOfPartitions);
			// Check if folder Exists, if not create it
			if (!Files.exists(partitionFolderPath)) {
				Files.createDirectories(partitionFolderPath);
			}
			// loop imbalance ratios
			for (int y = 0; y < imbalanceRatiosList.length; y++) {
				imbalanceRatio = imbalanceRatiosList[y];
				// imbalance folder
				Path imbalanceFolderPath = Paths.get(partitionFolderPath + "/imbalance_" + imbalanceRatio);
				// Check if folder Exists, if not create it
				if (!Files.exists(imbalanceFolderPath)) {
					Files.createDirectories(imbalanceFolderPath);
				}
				// loop all graphs
				for (int i = 0; i < graphNames.length; i++) {
					if (excludedGraphs.contains(graphNames[i]))
						continue;

//					if (!graphNames[i].equals("144")) {
//						continue;
//					}
					String fileSrc = "../../graphs/" + graphNames[i] + ".graph";

					// graph folder
					Path graphFolderPath = Paths.get(imbalanceFolderPath + "/" + graphNames[i]);
					// Check if folder Exists, if not create it
					if (!Files.exists(graphFolderPath)) {
						Files.createDirectories(graphFolderPath);
					} else {
						continue;
					}
					System.out.println(graphNames[i]);

					Graph originalGraph = new Graph(fileSrc);
					// maxCoarseNodeWeight = (((float)
					// originalGraph.getTotalNodesWeights()) /
					// (numberOfPartitions));
					maxCoarseNodeWeight = (((float) 3 * originalGraph.getTotalNodesWeights())
							/ (maxCoarsenGraphNumOfNodes));
					// System.out.println(maxCoarseNodeWeight);

					String uniqueID = UUID.randomUUID().toString();
					// Experiment Folder
					Path experimentFolder = Paths.get(graphFolderPath + "/" + uniqueID);
					// Check if folder Exists, if not create it
					if (!Files.exists(experimentFolder)) {
						Files.createDirectories(experimentFolder);
					}
					// Record Experiment parameters
					StringBuilder experimentDetails = new StringBuilder();
					experimentDetails.append("Number Of Partitions = " + numberOfPartitions + "\r\n");
					experimentDetails.append("maximum Coarse Node Weight = " + maxCoarseNodeWeight + "\r\n");
					experimentDetails.append("Trials number for initial partitioning = " + numberOfTrials + "\r\n");
					experimentDetails.append("Number of runs = " + numberOfRuns + "\r\n");
					experimentDetails.append("imbalanceRatio = " + imbalanceRatio + "\r\n");
					experimentDetails.append("refinementIterations = " + refinementIterations + "\r\n");
					experimentDetails.append("maxCoarsenGraphNumOfNodes = " + maxCoarsenGraphNumOfNodes + "\r\n");
					experimentDetails.append("maxNegativeRefinementSteps = " + maxNegativeRefinementSteps + "\r\n");
					experimentDetails.append("finalRefinementIterations = " + finalRefinementIterations + "\r\n");
					experimentDetails
							.append("maxFinalNegativeRefinementSteps = " + maxFinalNegativeRefinementSteps + "\r\n");
					experimentDetails.append("maxNegativeRefinementGain = " + maxNegativeRefinementGain + "\r\n");

					// write Experiment parameters to file
					BufferedWriter bwr = new BufferedWriter(
							new FileWriter(new File(experimentFolder + "/Expirement_Parameters.txt")));
					bwr.write(experimentDetails.toString());
					bwr.flush();
					bwr.close();

					// multiple Runs
					int run_ID = 1;
					StringBuilder runsOutput = new StringBuilder();
					int bestEdgeCut = Integer.MAX_VALUE;
					float bestEdgeCutImbalance = 0;
					int bestBalancedEdgecut = Integer.MAX_VALUE;
					float bestBalancedEdgeCutImbalance = 0;
					PartitionGroup bestPartition = null;
					PartitionGroup bestBalancedPartition = null;
					while (run_ID <= numberOfRuns) {
						runsOutput.append("Run # " + run_ID + "\r\n");
						// get list of coarsen Class available in coarsening
						// package
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
							ArrayList<RandomSet<Integer>> originalNodesTree = null;
							int lastGraphNodesNumber = originalGraph.getNumberOfNodes() + 11;
							while (intermediate.getNumberOfNodes() > maxCoarsenGraphNumOfNodes
									&& lastGraphNodesNumber - intermediate.getNumberOfNodes() > 10) {
								lastGraphNodesNumber = intermediate.getNumberOfNodes();
								ArrayList<RandomSet<Integer>> nodesTree = match.coarse(intermediate,
										maxCoarsenGraphNumOfNodes, maxCoarseNodeWeight);
								// map the nodes tree to the original graph
								ArrayList<RandomSet<Integer>> mappedNodesTree = new ArrayList<RandomSet<Integer>>(
										nodesTree.size());
								if (originalNodesTree == null) {
									originalNodesTree = nodesTree;
								} else {
									for (int k = 0; k < nodesTree.size(); k++) {
										RandomSet<Integer> currentNodes = new RandomSet<Integer>();
										Iterator<Integer> childNodesIt = nodesTree.get(k).iterator();
										while (childNodesIt.hasNext()) {
											int originalIndex = childNodesIt.next() - 1;
											currentNodes.addAll(originalNodesTree.get(originalIndex));

										}
										mappedNodesTree.add(currentNodes);
									}
									originalNodesTree = mappedNodesTree;
								}
								Graph oldIntermediate = intermediate;
								intermediate = new CoarseGraph(oldIntermediate, nodesTree);
							}

							CoarseGraph cGraph = (CoarseGraph) intermediate;
							cGraph.switchParentGraph(originalGraph, originalNodesTree);

							// System.out.println("Coarsen Graph # of nodes = "
							// + cGraph.getNumberOfNodes());
							// loop partitioning algorithms
							ArrayList<Class<Object>> partitioningClasses = getClasses("bin/partitioning",
									"partitioning");
							for (int k = 0; k < partitioningClasses.size(); k++) {
								if (!partitioningSchemes.contains(partitioningClasses.get(k).getName()))
									continue;
								Constructor<Object> partConstructor = partitioningClasses.get(k)
										.getConstructor(Graph.class, Integer.TYPE, Integer.TYPE, Float.TYPE);
								Partitioning gGGP = (Partitioning) partConstructor.newInstance(cGraph,
										numberOfPartitions, numberOfTrials, imbalanceRatio);
								PartitionGroup partsGroup = gGGP.getPartitions(cGraph, null, numberOfPartitions, 20);
								FMRefinement fm = new FMRefinement(cGraph, null, partsGroup, refinementIterations,
										maxNegativeRefinementSteps, maxNegativeRefinementGain, imbalanceRatio);
								PartitionGroup refinedParts2;
								refinedParts2 = fm.getRefinedPartitions();
								// uncoarse Graph
								Graph curGraph = (CoarseGraph) cGraph;
								Uncoarsening gGGP_UC = new GGGPUncoarsening();
								while (curGraph.getNumberOfNodes() < originalGraph.getNumberOfNodes() / 2) {
									PartitionGroup pG = gGGP_UC.Uncoarsen(originalGraph, (CoarseGraph) curGraph);
									Graph prevGraph = new CoarseGraph(originalGraph, pG.getAllPartitionsNodes());
									PartitionGroup uncoarsenPartitions2 = uncoarsenPartitions((CoarseGraph) curGraph,
											prevGraph, refinedParts2);
									fm = new FMRefinement(prevGraph, null, uncoarsenPartitions2, refinementIterations,
											maxNegativeRefinementSteps, maxNegativeRefinementGain,
											Math.max(((float) 0.2 / 1000), imbalanceRatio));
									refinedParts2 = fm.getRefinedPartitions();

									curGraph = prevGraph;
								}

								PartitionGroup uncoarsenPartitions2 = uncoarsenPartitions((CoarseGraph) curGraph,
										originalGraph, refinedParts2);

								fm = new FMRefinement(originalGraph, null, uncoarsenPartitions2,
										finalRefinementIterations, maxFinalNegativeRefinementSteps,
										maxNegativeRefinementGain, imbalanceRatio);
								refinedParts2 = fm.getRefinedPartitions();

								float imbalance = refinedParts2.getPartitionImbalance();
								int imbalance_times_1000 = (int) ((imbalance - 1) * 1000);
								int runEdgeCut = refinedParts2.getEdgeCut();
								runsOutput.append("Edge Cut = " + runEdgeCut + "\r\n");
								runsOutput.append("imbalance = " + imbalance + "\r\n");
								if ((int) ((imbalanceRatio) * 1000) >= imbalance_times_1000) {
									// balanced
									if (runEdgeCut < bestBalancedEdgecut) {
										// found new best balanced edge cut
										bestBalancedPartition = refinedParts2;
										bestBalancedEdgecut = runEdgeCut;
										bestBalancedEdgeCutImbalance = imbalance;
									} else if (runEdgeCut == bestBalancedEdgecut) {
										if (bestBalancedEdgeCutImbalance > imbalance) {
											// found new best balanced edge cut
											bestBalancedPartition = refinedParts2;
											bestBalancedEdgecut = runEdgeCut;
											bestBalancedEdgeCutImbalance = imbalance;
										}
									}
								} else {
									// unbalanced
									if (runEdgeCut < bestEdgeCut) {
										// found new best balanced edge cut
										bestPartition = refinedParts2;
										bestEdgeCut = runEdgeCut;
										bestEdgeCutImbalance = imbalance;
									} else if (runEdgeCut == bestEdgeCut) {
										if (bestEdgeCutImbalance > imbalance) {
											// found new best balanced edge cut
											bestBalancedPartition = refinedParts2;
											bestEdgeCut = runEdgeCut;
											bestBalancedEdgeCutImbalance = imbalance;
										}
									}
								}

								// verify partitions
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
									runsOutput.append("verification success\r\n");
								} else {
									runsOutput.append("verification failed\r\n");
								}

							}

						}
						System.out.println(run_ID);
						run_ID++;
					}

					// write Experiment Runs Results to file
					bwr = new BufferedWriter(
							new FileWriter(new File(experimentFolder + "/Expirement_Runs_Results.txt")));
					bwr.write(runsOutput.toString());
					bwr.flush();
					bwr.close();

					//
					if (bestBalancedEdgecut <= bestEdgeCut) {
						bestPartition = bestBalancedPartition;
						bestEdgeCut = bestBalancedEdgecut;
						bestEdgeCutImbalance = bestBalancedEdgeCutImbalance;
					}

					// Record Experiment Summary
					StringBuilder experimentSummary = new StringBuilder();
					experimentSummary.append("Best Balanced Partitions \r\n");
					experimentSummary.append("Edge Cut = " + bestBalancedEdgecut + "\r\n");
					experimentSummary.append("Imbalance = " + bestBalancedEdgeCutImbalance + "\r\n");
					experimentSummary.append("Best Partitions \r\n");
					experimentSummary.append("Edge Cut = " + bestEdgeCut + "\r\n");
					experimentSummary.append("Imbalance = " + bestEdgeCutImbalance + "\r\n");

					// write Experiment Summary to file
					bwr = new BufferedWriter(new FileWriter(new File(experimentFolder + "/Expirement_Summary.txt")));
					bwr.write(experimentSummary.toString());
					bwr.flush();
					bwr.close();

					// Record Best balanced Partition
					String bestBalancedPartitionString = "";
					if (bestBalancedPartition != null) {
						bestBalancedPartitionString = bestBalancedPartition.toString();
					}
					// write partition to file
					bwr = new BufferedWriter(
							new FileWriter(new File(experimentFolder + "/Best_Balanced_Partition.txt")));
					bwr.write(bestBalancedPartitionString);
					bwr.flush();
					bwr.close();

					// Record Best Partition
					String bestPartitionString = "";
					if (bestPartition != null) {
						bestPartitionString = bestPartition.toString();
					}
					// write partition to file
					bwr = new BufferedWriter(new FileWriter(new File(experimentFolder + "/Best_Partition.txt")));
					bwr.write(bestPartitionString);
					bwr.flush();
					bwr.close();

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
			RandomSet<Integer> prevGraphNodeChilds = previousGraph.getNodeChilds(prevGraphNode.getNodeID());
			// get the parent node in curGraph
			int curGraphNodeID = curGraph.getParentNodeIDOf(prevGraphNodeChilds.iterator().next());
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

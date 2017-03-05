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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import coarsening.Matching;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import partitioning.Partitioning;
import refinement.FMRefinement;
import structure.CoarseGraph;
import structure.Graph;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;
import uncoarsening.GGGPUncoarsening;
import uncoarsening.Uncoarsening;

public class Main {
	private static int numberOfPartitions = 2;
	private static float maxCoarseNodeWeight;

	final private static HashSet<String> coarseningSchemes = new HashSet<String>(
			Arrays.asList("coarsening.OrderedHeaviestEdgeMatching","coarsening.HeaviestEdgeMatching","coarsening.OrderedHeavyEdgeMatching","coarsening.HeavyEdgeMatching","coarsening.LightEdgeMatching"));
	final private static HashSet<String> partitioningSchemes = new HashSet<String>(
			Arrays.asList("partitioning.GreedyGraphGrowingPartitioning"));
	final private static HashSet<String> excludedGraphs = new HashSet<String>(Arrays.asList("auto"));
	final private static int numberOfTrials = 5;
	final private static int numberOfRuns = 10;
	final private static float[] imbalanceRatiosList = new float[] { (float) 0.001999, (float) 0.010999,
			(float) 0.030999, (float) 0.050999 };
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
		Path resultsFolderPath = Paths.get("../../results_1");
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

					// if (!graphNames[i].equals("144")) {
					// continue;
					// }
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
					maxCoarseNodeWeight = (((float) 3 * originalGraph.getTotalNodesWeights())
							/ (maxCoarsenGraphNumOfNodes));

					// get list of coarsen Class available in coarsening
					// package
					ArrayList<Class<Object>> coarseningClasses = getClasses("bin/coarsening", "coarsening"); // loop
																												// allcoarsening
																												// schemes
					for (int j = 0; j < coarseningClasses.size(); j++) {
						if (!coarseningSchemes.contains(coarseningClasses.get(j).getName()))
							continue;
						
						System.out.println(coarseningClasses.get(j).getName());
						// Coarsening folder
						Path coarseningFolderPath = Paths
								.get(graphFolderPath + "/" + coarseningClasses.get(j).getName());
						// Check if folder Exists, if not create it
						if (!Files.exists(coarseningFolderPath)) {
							Files.createDirectories(coarseningFolderPath);
						}

						ArrayList<Graph> graphs = new ArrayList<Graph>();
						graphs.add(originalGraph);
						Graph intermediate = originalGraph;
						Matching match = (Matching) coarseningClasses.get(j).newInstance();
						ArrayList<RandomAccessIntHashSet> originalNodesTree = null;
						int lastGraphNodesNumber = originalGraph.getNumberOfNodes() + 11;
						int coarseningIterations = 0;
						while (intermediate.getNumberOfNodes() > maxCoarsenGraphNumOfNodes
								&& lastGraphNodesNumber - intermediate.getNumberOfNodes() > 0) {
							coarseningIterations++;
							lastGraphNodesNumber = intermediate.getNumberOfNodes();
							ArrayList<RandomAccessIntHashSet> nodesTree = match.coarse(intermediate,
									maxCoarsenGraphNumOfNodes, maxCoarseNodeWeight);
							// map the nodes tree to the original graph
							ArrayList<RandomAccessIntHashSet> mappedNodesTree = new ArrayList<RandomAccessIntHashSet>(
									nodesTree.size());
							if (originalNodesTree == null) {
								originalNodesTree = nodesTree;
							} else {
								for (int k = 0; k < nodesTree.size(); k++) {
									RandomAccessIntHashSet currentNodes = new RandomAccessIntHashSet();
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
//							for (int k = 0; k < nodesTree.size(); k++) {
//								for (int k2 = 0; k2 < nodesTree.get(k).size(); k2++) {
//									System.out.print(nodesTree.get(k).get(k2) + "\t");
//								}
//								System.out.println();
//							}
//							System.out.println("==============================================================================");
							intermediate = new CoarseGraph(oldIntermediate, nodesTree);
						}

						CoarseGraph cGraph = (CoarseGraph) intermediate;
						cGraph.switchParentGraph(originalGraph, originalNodesTree);
						int coarseGraphNumOfNodes = cGraph.getNumberOfNodes();
						// Record Coarsening Details
						StringBuilder coarseningDetails = new StringBuilder();
						coarseningDetails.append("Coarsening Scheme = " + coarseningClasses.get(j).getName() + "\r\n");
						coarseningDetails.append("Maximum Coarse Node Weight = " + maxCoarseNodeWeight + "\r\n");
						coarseningDetails.append("Coarsening Number Of Iteration = " + coarseningIterations + "\r\n");
						coarseningDetails.append("Coarse Graph Num of nodes = " + coarseGraphNumOfNodes + "\r\n");

						// write Experiment parameters to file
						BufferedWriter bwr = new BufferedWriter(
								new FileWriter(new File(coarseningFolderPath + "/Coarening_Details.txt")));
						bwr.write(coarseningDetails.toString());
						bwr.flush();
						bwr.close();

						// loop partitioning algorithms
						ArrayList<Class<Object>> partitioningClasses = getClasses("bin/partitioning", "partitioning");

						for (int k = 0; k < partitioningClasses.size(); k++) {
							if (!partitioningSchemes.contains(partitioningClasses.get(k).getName()))
								continue;

							// Partitioning folder
							Path partitioningFolderPath = Paths
									.get(coarseningFolderPath + "/" + partitioningClasses.get(k).getName());
							// Check if folder Exists, if not create it
							if (!Files.exists(partitioningFolderPath)) {
								Files.createDirectories(partitioningFolderPath);
							}

							// Run folder
							Path runFolderPath = Paths
									.get(partitioningFolderPath + "/Run_" + System.currentTimeMillis());
							// Check if folder Exists, if not create it
							if (!Files.exists(runFolderPath)) {
								Files.createDirectories(runFolderPath);
							}
							// multiple Runs
							int run_ID = 1;
							StringBuilder runsOutput = new StringBuilder();
							int bestEdgeCut = Integer.MAX_VALUE;
							float bestEdgeCutImbalance = 0;
							int bestBalancedEdgecut = Integer.MAX_VALUE;
							float bestBalancedEdgeCutImbalance = 0;
							PartitionGroup bestPartition = null;
							PartitionGroup bestBalancedPartition = null;
							Duration avgRunDuration = Duration.ZERO;
							while (run_ID <= numberOfRuns) {
								Instant start = Instant.now();
								runsOutput.append("Run # " + run_ID + "\r\n");

								Constructor<Object> partConstructor = partitioningClasses.get(k)
										.getConstructor(Graph.class, Integer.TYPE, Integer.TYPE, Float.TYPE);
								Partitioning gGGP = (Partitioning) partConstructor.newInstance(cGraph,
										numberOfPartitions, numberOfTrials, imbalanceRatio);
								PartitionGroup partsGroup = gGGP.getPartitions(cGraph, null, numberOfPartitions,
										numberOfTrials);
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
								IntOpenHashSet allNodes = new IntOpenHashSet(originalGraph.getNumberOfNodes());
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

								System.out.print(run_ID + "\t");
								run_ID++;
								Instant end = Instant.now();
								Duration runDuration = Duration.between(start, end);
								avgRunDuration = avgRunDuration.plus(runDuration);
								runsOutput.append("Running Time = " + runDuration.toString() + "\r\n");
								System.out.println(runDuration.toString());
							}

							// write Experiment Runs Results to file
							bwr = new BufferedWriter(
									new FileWriter(new File(runFolderPath + "/Expirement_Runs_Results.txt")));
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
							experimentSummary.append(
									"Average Run time = " + avgRunDuration.dividedBy(numberOfRuns).toString() + "\r\n");

							// write Experiment Summary to file
							bwr = new BufferedWriter(
									new FileWriter(new File(runFolderPath + "/Expirement_Summary.txt")));
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
									new FileWriter(new File(runFolderPath + "/Best_Balanced_Partition.txt")));
							bwr.write(bestBalancedPartitionString);
							bwr.flush();
							bwr.close();

							// Record Best Partition
							String bestPartitionString = "";
							if (bestPartition != null) {
								bestPartitionString = bestPartition.toString();
							}
							// write partition to file
							bwr = new BufferedWriter(new FileWriter(new File(runFolderPath + "/Best_Partition.txt")));
							bwr.write(bestPartitionString);
							bwr.flush();
							bwr.close();
						}

					}

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
			RandomAccessIntHashSet prevGraphNodeChilds = previousGraph.getNodeChilds(prevGraphNode.getNodeID());
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

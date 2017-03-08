package algorithms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

public class METIS_Enhanced {
	private int numberOfPartitions;
	private float maxCoarseNodeWeight;
	private int initPartTrials;
	private int numberOfRuns;
	private String graphName;
	private float imbalanceRatio;
	private int refinementIterations;
	private int maxCoarsenGraphNumOfNodes;
	private int maxNegativeRefinementSteps;
	private int finalRefinementIterations;
	private int maxFinalNegativeRefinementSteps;
	private int maxNegativeRefinementGain;
	private Class<Object> coarseningClass;
	private Class<Object> partitioningClass;
	private String graphFilePath;

	public METIS_Enhanced(int numberOfPartitions, Class<Object> coarseningClass,
			Class<Object> partitioningClass, String graphName, int initPartTrials, int numberOfRuns,
			float imbalanceRatio, int refinementIterations, int maxNegativeRefinementSteps,
			int finalRefinementIterations, int maxFinalNegativeRefinementSteps, int maxNegativeRefinementGain)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException, IOException {

		// Assignments
		BufferedWriter bwr;
		this.graphName = graphName;
		this.graphFilePath = "../../graphs/" + this.graphName + ".graph";
		this.numberOfPartitions = numberOfPartitions;
		this.initPartTrials = initPartTrials;
		this.numberOfRuns = numberOfRuns;
		this.imbalanceRatio = imbalanceRatio;
		this.refinementIterations = refinementIterations;
		this.maxCoarsenGraphNumOfNodes = 30 * this.numberOfPartitions;
		this.maxNegativeRefinementSteps = maxNegativeRefinementSteps;
		this.finalRefinementIterations = finalRefinementIterations;
		this.maxFinalNegativeRefinementSteps = maxFinalNegativeRefinementSteps;
		this.maxNegativeRefinementGain = maxNegativeRefinementGain;
		this.coarseningClass = coarseningClass;
		this.partitioningClass = partitioningClass;

		// Experiments Folder
		Path resultsFolderPath = Paths.get("../../Experiments");
		// Check if folder Exists, if not create it
		if (!Files.exists(resultsFolderPath)) {
			Files.createDirectories(resultsFolderPath);
		}

		// graph folder
		Path graphFolderPath = Paths.get(resultsFolderPath + "/" + this.graphName);
		// Check if folder Exists, if not create it
		if (!Files.exists(graphFolderPath)) {
			Files.createDirectories(graphFolderPath);
		}

		// Class Folder
		Path classFolderPath = Paths.get(graphFolderPath.toString() + "/METIS_Enhanced");
		// Check if folder Exists, if not create it
		if (!Files.exists(classFolderPath)) {
			Files.createDirectories(classFolderPath);
		}

		// partition folder
		Path partitionFolderPath = Paths.get(classFolderPath.toString() + "/Parts_" + this.numberOfPartitions);
		// Check if folder Exists, if not create it
		if (!Files.exists(partitionFolderPath)) {
			Files.createDirectories(partitionFolderPath);
		}

		// imbalance folder
		Path imbalanceFolderPath = Paths.get(partitionFolderPath + "/imbalance_" + this.imbalanceRatio);
		// Check if folder Exists, if not create it
		if (!Files.exists(imbalanceFolderPath)) {
			Files.createDirectories(imbalanceFolderPath);
		}

		// Coarsening Folder
		Path coarseningFolder = Paths.get(imbalanceFolderPath + "/" + this.coarseningClass.getName());
		// Check if folder Exists, if not create it
		if (!Files.exists(coarseningFolder)) {
			Files.createDirectories(coarseningFolder);
		}

		// initial Partitioning Algorithm Folder
		Path partitioningFolder = Paths.get(coarseningFolder + "/" + this.partitioningClass.getName());
		// Check if folder Exists, if not create it
		if (!Files.exists(partitioningFolder)) {
			Files.createDirectories(partitioningFolder);
		}

		// Experiment Folder
		Path experimentFolder = Paths.get(partitioningFolder + "/" + System.currentTimeMillis());
		// Check if folder Exists, if not create it
		if (!Files.exists(experimentFolder)) {
			Files.createDirectories(experimentFolder);
		}

		Graph originalGraph = new Graph(this.graphFilePath);
		this.maxCoarseNodeWeight = (((float) originalGraph.getTotalNodesWeights())
				/ (this.maxCoarsenGraphNumOfNodes));

		ArrayList<Graph> graphs = new ArrayList<Graph>();
		graphs.add(originalGraph);
		Graph intermediate = originalGraph;
		Matching match = (Matching) this.coarseningClass.newInstance();
		ArrayList<RandomAccessIntHashSet> originalNodesTree = null;
		int lastGraphNodesNumber = originalGraph.getNumberOfNodes() + 1;
		int coarseningIterations = 0;
		while (intermediate.getNumberOfNodes() > maxCoarsenGraphNumOfNodes) {
			if (lastGraphNodesNumber == intermediate.getNumberOfNodes()) {
				this.maxCoarseNodeWeight *= 1.1;
			}
			coarseningIterations++;
			lastGraphNodesNumber = intermediate.getNumberOfNodes();
			ArrayList<RandomAccessIntHashSet> nodesTree = match.coarse(intermediate, maxCoarsenGraphNumOfNodes,
					this.maxCoarseNodeWeight);
			// map the nodes tree to the original graph
			ArrayList<RandomAccessIntHashSet> mappedNodesTree = new ArrayList<RandomAccessIntHashSet>(nodesTree.size());
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
			intermediate = new CoarseGraph(oldIntermediate, nodesTree);
		}

		CoarseGraph cGraph = (CoarseGraph) intermediate;
		cGraph.switchParentGraph(originalGraph, originalNodesTree);
		int coarseGraphNumOfNodes = cGraph.getNumberOfNodes();

		// Record Coarsening Details
		StringBuilder coarseningDetails = new StringBuilder();
		coarseningDetails.append("Coarsening Scheme = " + this.coarseningClass.getName() + "\r\n");
		coarseningDetails.append("Maximum Coarse Node Weight = " + maxCoarseNodeWeight + "\r\n");
		coarseningDetails.append("Coarsening Number Of Iteration = " + coarseningIterations + "\r\n");
		coarseningDetails.append("Coarse Graph Num of nodes = " + coarseGraphNumOfNodes + "\r\n");

		// Write to file
		bwr = new BufferedWriter(new FileWriter(new File(experimentFolder + "/Coarening_Details.txt")));
		bwr.write(coarseningDetails.toString());
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
		Duration avgRunDuration = Duration.ZERO;
		while (run_ID <= this.numberOfRuns) {
			Instant start = Instant.now();
			runsOutput.append("Run # " + run_ID + "\r\n");

			Constructor<Object> partConstructor = this.partitioningClass.getConstructor(Graph.class, Integer.TYPE,
					Integer.TYPE, Float.TYPE);
			Partitioning gGGP = (Partitioning) partConstructor.newInstance(cGraph, this.numberOfPartitions,
					this.initPartTrials, this.imbalanceRatio);
			PartitionGroup partsGroup = gGGP.getPartitions(cGraph, null, this.numberOfPartitions, this.initPartTrials);
			FMRefinement fm = new FMRefinement(cGraph, null, partsGroup, this.refinementIterations,
					this.maxNegativeRefinementSteps, this.maxNegativeRefinementGain, this.imbalanceRatio);
			PartitionGroup refinedParts2;
			refinedParts2 = fm.getRefinedPartitions();
			// uncoarse Graph
			Graph curGraph = (CoarseGraph) cGraph;
			Uncoarsening gGGP_UC = new GGGPUncoarsening();
			while (curGraph.getNumberOfNodes() < originalGraph.getNumberOfNodes() / 2) {
				PartitionGroup pG = gGGP_UC.Uncoarsen(originalGraph, (CoarseGraph) curGraph);
				Graph prevGraph = new CoarseGraph(originalGraph, pG.getAllPartitionsNodes());
				PartitionGroup uncoarsenPartitions2 = uncoarsenPartitions((CoarseGraph) curGraph, prevGraph,
						refinedParts2);
				fm = new FMRefinement(prevGraph, null, uncoarsenPartitions2, this.refinementIterations,
						this.maxNegativeRefinementSteps, this.maxNegativeRefinementGain,
						Math.max(((float) 0.2 / 1000), this.imbalanceRatio));
				refinedParts2 = fm.getRefinedPartitions();

				curGraph = prevGraph;
			}

			PartitionGroup uncoarsenPartitions2 = uncoarsenPartitions((CoarseGraph) curGraph, originalGraph,
					refinedParts2);

			fm = new FMRefinement(originalGraph, null, uncoarsenPartitions2, this.finalRefinementIterations,
					this.maxFinalNegativeRefinementSteps, this.maxNegativeRefinementGain, this.imbalanceRatio);
			refinedParts2 = fm.getRefinedPartitions();

			float imbalance = refinedParts2.getPartitionImbalance();
			int imbalance_times_1000 = (int) ((imbalance - 1) * 1000);
			int runEdgeCut = refinedParts2.getEdgeCut();
			runsOutput.append("Edge Cut = " + runEdgeCut + "\r\n");
			runsOutput.append("imbalance = " + imbalance + "\r\n");
			if ((int) ((this.imbalanceRatio) * 1000) >= imbalance_times_1000) {
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
		bwr = new BufferedWriter(new FileWriter(new File(experimentFolder + "/Expirement_Runs_Results.txt")));
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
		experimentSummary.append("coarseScheme = " + this.coarseningClass.getName() + "\r\n");
		experimentSummary.append("coarseGraphNumOfNodes = " + coarseGraphNumOfNodes + "\r\n");
		experimentSummary.append("coarseningIterations = " + coarseningIterations + "\r\n");
		experimentSummary.append("Intial Partition Scheme = " + this.partitioningClass.getName() + "\r\n");
		experimentSummary.append("======================================================================\r\n");
		experimentSummary.append("Best Balanced Partitions \r\n");
		experimentSummary.append("Edge Cut = " + bestBalancedEdgecut + "\r\n");
		experimentSummary.append("Imbalance = " + bestBalancedEdgeCutImbalance + "\r\n");
		experimentSummary.append("Best Partitions \r\n");
		experimentSummary.append("Edge Cut = " + bestEdgeCut + "\r\n");
		experimentSummary.append("Imbalance = " + bestEdgeCutImbalance + "\r\n");
		experimentSummary
				.append("Average Run time = " + avgRunDuration.dividedBy(this.numberOfRuns).toString() + "\r\n");

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
		bwr = new BufferedWriter(new FileWriter(new File(experimentFolder + "/Best_Balanced_Partition.txt")));
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

	private PartitionGroup uncoarsenPartitions(CoarseGraph curGraph, Graph previousGraph, PartitionGroup refinedParts) {
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

}

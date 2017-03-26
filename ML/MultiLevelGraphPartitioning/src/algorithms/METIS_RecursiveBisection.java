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
import java.util.ArrayList;
import java.util.Iterator;

import coarsening.Matching;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import partitioning.Partitioning;
import refinement.FMBalance;
import refinement.FMRefinement;
import statistics.ExperimentRun;
import statistics.METIS_Origin_Run;
import structure.CoarseGraph;
import structure.Graph;
import structure.Partition;
import structure.PartitionGroup;
import structure.RandomAccessIntHashSet;
import structure.SubGraph;

public class METIS_RecursiveBisection {

	private int numberOfPartitions;
	private float initialMaxCoarseNodeWeight;
	private float finalMaxCoarseNodeWeight;
	private int initPartTrials;
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
	private float coarseNodeWeightThreshold;
	private ArrayList<Graph> graphs;
	private Graph originalGraph;
	private int coarseGraphNumOfNodes = 0;
	private int actualCoarseningIterations = 0;
	private int totalCoarseningIterations = 0;
	private long coarseningTime;
	private long initialPartitioningTime;
	private int initialPartitioningEdgeCut;
	private long uncoarseningPhaseTime;
	private int finalEdgeCut;
	private float initialPartitiningImbalance;
	private float finalImbalance;
	private ExperimentRun run;

	public METIS_RecursiveBisection(Graph graph, int numberOfPartitions, Class<Object> coarseningClass,
			Class<Object> partitioningClass, int initPartTrials, float imbalanceRatio, int refinementIterations,
			int maxNegativeRefinementSteps, int finalRefinementIterations, int maxFinalNegativeRefinementSteps,
			int maxNegativeRefinementGain) {
		// Assignments
		this.graphName = graph.getGraphName();
		this.numberOfPartitions = numberOfPartitions;
		this.initPartTrials = initPartTrials;
		this.imbalanceRatio = imbalanceRatio;
		this.refinementIterations = refinementIterations;
		this.maxCoarsenGraphNumOfNodes = (int) Math.max(20 * (Math.log(this.numberOfPartitions) / Math.log(2)),
				30 * this.numberOfPartitions);

		this.maxNegativeRefinementSteps = maxNegativeRefinementSteps;
		this.finalRefinementIterations = finalRefinementIterations;
		this.maxFinalNegativeRefinementSteps = maxFinalNegativeRefinementSteps;
		this.maxNegativeRefinementGain = maxNegativeRefinementGain;
		this.coarseningClass = coarseningClass;
		this.partitioningClass = partitioningClass;

		this.originalGraph = graph;// new Graph(this.graphFilePath);

		this.coarseNodeWeightThreshold = (((float) originalGraph.getTotalNodesWeights()) / this.numberOfPartitions);
		// this.initialMaxCoarseNodeWeight = (((float)
		// originalGraph.getTotalNodesWeights())
		// / (this.maxCoarsenGraphNumOfNodes));
		this.initialMaxCoarseNodeWeight = this.coarseNodeWeightThreshold;
		this.finalMaxCoarseNodeWeight = this.initialMaxCoarseNodeWeight;

	}

	public ExperimentRun partitionGraph() throws InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException {
		this.run = new METIS_Origin_Run();
		this.graphs = new ArrayList<Graph>();
		CoarseGraph cGraph = coarseGraph();
		((METIS_Origin_Run) this.run).setCoarseningScheme(this.coarseningClass.getName());
		((METIS_Origin_Run) this.run).setInitialMaxCoarseNodeWeight(this.initialMaxCoarseNodeWeight);
		((METIS_Origin_Run) this.run).setFinalMaxCoarseNodeWeight(this.finalMaxCoarseNodeWeight);
		((METIS_Origin_Run) this.run).setActualCoarseningIterations(this.actualCoarseningIterations);
		((METIS_Origin_Run) this.run).setTotalCoarseningIterations(this.totalCoarseningIterations);
		((METIS_Origin_Run) this.run).setCoarseGraphNumOfNodes(this.coarseGraphNumOfNodes);
		((METIS_Origin_Run) this.run).setCoarseningTime(this.coarseningTime);
		/*****************************************/
		PartitionGroup parts = null;
		if (cGraph == null) {
			parts = initialPartition(this.originalGraph, this.numberOfPartitions);
		} else {
			parts = initialPartition(cGraph, this.numberOfPartitions);
		}

		((METIS_Origin_Run) this.run).setPartitioningScheme(this.partitioningClass.getName());
		((METIS_Origin_Run) this.run).setInitialPartitiningImbalance(this.initialPartitiningImbalance);
		((METIS_Origin_Run) this.run).setInitialPartitioningEdgeCut(this.initialPartitioningEdgeCut);
		((METIS_Origin_Run) this.run).setInitialPartitioningTime(this.initialPartitioningTime);
		/*****************************************/
		parts = uncoarsenGraph(parts);
		((METIS_Origin_Run) this.run).setUncoarseningScheme("Standard");
		((METIS_Origin_Run) this.run).setUncoarseningPhaseTime(this.uncoarseningPhaseTime);
		/*****************************************/
		((METIS_Origin_Run) this.run).setPartsGroup(parts);
		((METIS_Origin_Run) this.run).setFinalEdgeCut(this.finalEdgeCut);
		((METIS_Origin_Run) this.run).setFinalImbalance(this.finalImbalance);
		((METIS_Origin_Run) this.run)
				.setOverAllTime(this.coarseningTime + this.initialPartitioningTime + this.uncoarseningPhaseTime);
		((METIS_Origin_Run) this.run).setVerified(this.verifyPartitions(parts));

		return run;
	}

	private CoarseGraph coarseGraph() throws InstantiationException, IllegalAccessException {
		long start = System.nanoTime();
		graphs = new ArrayList<Graph>();
		this.graphs.add(originalGraph);
		Graph intermediate = originalGraph;
		Matching match = (Matching) this.coarseningClass.newInstance();
		int lastGraphNodesNumber = originalGraph.getNumberOfNodes() + 1;
		this.actualCoarseningIterations = 0;
		this.totalCoarseningIterations = 0;
		this.coarseGraphNumOfNodes = 0;

		while (intermediate.getNumberOfNodes() > this.maxCoarsenGraphNumOfNodes) {
			CoarseGraph cG = null;
			this.totalCoarseningIterations++;
			if (lastGraphNodesNumber == intermediate.getNumberOfNodes()) {
				this.finalMaxCoarseNodeWeight *= 1.1;
				this.graphs.remove(this.graphs.size() - 1);
				intermediate = this.graphs.get(this.graphs.size() - 1);
				cG = new CoarseGraph(intermediate,
						match.coarse(intermediate, this.maxCoarsenGraphNumOfNodes, this.finalMaxCoarseNodeWeight));
				this.graphs.add(cG);
				intermediate = this.graphs.get(this.graphs.size() - 1);
			} else {
				this.actualCoarseningIterations++;
				lastGraphNodesNumber = intermediate.getNumberOfNodes();
				cG = new CoarseGraph(intermediate,
						match.coarse(intermediate, this.maxCoarsenGraphNumOfNodes, this.finalMaxCoarseNodeWeight));
				this.graphs.add(cG);
				intermediate = this.graphs.get(this.graphs.size() - 1);
			}
		}

		lastGraphNodesNumber = intermediate.getNumberOfNodes();
		try {
			CoarseGraph cGraph = (CoarseGraph) intermediate;
			this.coarseGraphNumOfNodes = cGraph.getNumberOfNodes();
			this.coarseningTime = System.nanoTime() - start;
			return cGraph;
		} catch (Exception e) {
			this.coarseGraphNumOfNodes = originalGraph.getNumberOfNodes();
			this.coarseningTime = System.nanoTime() - start;
			return null;
		}

	}

	private PartitionGroup initialPartition(Graph cGraph, int numberOfPartitions)
			throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {

		long start = System.nanoTime();
		PartitionGroup partsGroup = null;
		if (numberOfPartitions == 2) {
			// System.out.println("Start Bisection of Graph size = " +
			// cGraph.getNumberOfNodes());
			Constructor<Object> partConstructor = this.partitioningClass.getConstructor(Graph.class,
					RandomAccessIntHashSet.class, Integer.TYPE, Integer.TYPE, Float.TYPE);
			Partitioning gGGP = (Partitioning) partConstructor.newInstance(cGraph, null, this.numberOfPartitions,
					this.initPartTrials, this.imbalanceRatio);
			partsGroup = gGGP.getPartitions();
			// System.out.println("End Bisection");
		} else {
			// System.out.println("Start Initial partitioning of " +
			// numberOfPartitions + "Partitions");
			METIS_RecursiveBisection mRB = new METIS_RecursiveBisection(cGraph, 2, this.coarseningClass,
					this.partitioningClass, 4, this.imbalanceRatio, 10, 10, 10, 10, -10000);
			PartitionGroup intermediatePartitions = mRB.partitionGraph().getPartsGroup();

			int orgPartID = 1;
			partsGroup = new PartitionGroup(cGraph);

			Partition part1 = intermediatePartitions.getPartition(1);
			SubGraph subGraph1 = new SubGraph(cGraph, part1);
			mRB = new METIS_RecursiveBisection(subGraph1, this.numberOfPartitions / 2, this.coarseningClass,
					this.partitioningClass, 4, this.imbalanceRatio, 10, 10, 10, 10, -10000);
			PartitionGroup subParts1 = mRB.partitionGraph().getPartsGroup();

			ArrayList<Partition> subPartsList = subParts1.getAllPartitions();
			for (Iterator<Partition> subPartsIt = subPartsList.iterator(); subPartsIt.hasNext();) {
				Partition subPart = (Partition) subPartsIt.next();
				Partition orgPart = new Partition(cGraph, orgPartID++);
				IntOpenHashSet subPartNodesIDs = subPart.getNodeIDs();
				for (IntIterator subNodesIt = subPartNodesIDs.iterator(); subNodesIt.hasNext();) {
					int subNodeID = subNodesIt.nextInt();
					int orgNodeID = ((SubGraph) subGraph1).getOriginalNodeID(subNodeID);
					orgPart.addNode(orgNodeID);
				}
				partsGroup.addPartition(orgPart);
			}

			Partition part2 = intermediatePartitions.getPartition(2);
			SubGraph subGraph2 = new SubGraph(cGraph, part2);
			mRB = new METIS_RecursiveBisection(subGraph2, this.numberOfPartitions / 2, this.coarseningClass,
					this.partitioningClass, 4, this.imbalanceRatio, 10, 10, 10, 10, -10000);
			PartitionGroup subParts2 = mRB.partitionGraph().getPartsGroup();

			subPartsList = subParts2.getAllPartitions();
			for (Iterator<Partition> subPartsIt = subPartsList.iterator(); subPartsIt.hasNext();) {
				Partition subPart = (Partition) subPartsIt.next();
				Partition orgPart = new Partition(cGraph, orgPartID++);
				IntOpenHashSet subPartNodesIDs = subPart.getNodeIDs();
				for (IntIterator subNodesIt = subPartNodesIDs.iterator(); subNodesIt.hasNext();) {
					int subNodeID = subNodesIt.nextInt();
					int orgNodeID = ((SubGraph) subGraph2).getOriginalNodeID(subNodeID);
					orgPart.addNode(orgNodeID);
				}
				partsGroup.addPartition(orgPart);
			}
			// System.out.println("End Initial partitioning of " +
			// numberOfPartitions + "Partitions");
		}

		this.initialPartitioningTime = System.nanoTime() - start;
		this.initialPartitioningEdgeCut = partsGroup.getEdgeCut();
		this.initialPartitiningImbalance = partsGroup.getPartitionImbalance();
		return partsGroup;
	}

	private PartitionGroup uncoarsenGraph(PartitionGroup parts) {
		long start = System.nanoTime();
		int graphIndex = this.graphs.size() - 1;
		CoarseGraph curGraph = null;
		Graph previousGraph = null;
		PartitionGroup currentParts = parts;
		while (graphIndex > 0) {
			curGraph = (CoarseGraph) graphs.get(graphIndex);
			currentParts = this.refinePartitions(curGraph, currentParts);
			previousGraph = graphs.get(graphIndex - 1);
			currentParts = this.uncoarsenPartitions(curGraph, previousGraph, currentParts);
			graphIndex--;
		}
		if (previousGraph == null) {
			currentParts = this.lastRefinePartitions(originalGraph, currentParts);
		} else {
			currentParts = this.lastRefinePartitions(previousGraph, currentParts);
		}

		this.uncoarseningPhaseTime = System.nanoTime() - start;

		this.finalEdgeCut = currentParts.getEdgeCut();
		this.finalImbalance = currentParts.getPartitionImbalance();
		return currentParts;
	}

	private PartitionGroup uncoarsenPartitions(CoarseGraph curGraph, Graph previousGraph, PartitionGroup refinedParts) {
		PartitionGroup uncoarsenParts = new PartitionGroup(previousGraph);
		int partitionNum = refinedParts.getPartitionNumber();
		for (int i = 1; i < partitionNum + 1; i++) {
			Partition part = refinedParts.getPartition(i);
			int partitionID = part.getPartitionID();
			Partition uncoarsenPart = new Partition(previousGraph, partitionID);
			if (part.getNumberOfNodes() > 0) {
				IntOpenHashSet nodes = part.getNodeIDs();
				Iterator<Integer> it = nodes.iterator();
				while (it.hasNext()) {
					int nodeID = it.next();
					RandomAccessIntHashSet childsNodes = curGraph.getNodeChilds(nodeID);
					for (int j = 0; j < childsNodes.size(); j++) {
						uncoarsenPart.addNode(childsNodes.get(j));
					}
				}
			}
			uncoarsenParts.addPartition(uncoarsenPart);
		}
		return uncoarsenParts;
	}

	private PartitionGroup refinePartitions(Graph gr, PartitionGroup parts) {

		int level = (int) Math.ceil(((double) this.originalGraph.getNumberOfNodes()) / gr.getNumberOfNodes());
		float levelImbalance = (float) Math.min(0.2, Math.log(level) * this.imbalanceRatio);
		PartitionGroup refinedParts = parts;
		if (parts.getPartitionImbalance() > (1 + levelImbalance)) {
			FMBalance fMB = new FMBalance(gr, null, refinedParts, -1, this.maxNegativeRefinementGain, levelImbalance);
			refinedParts = fMB.getRefinedPartitions();
		}

		FMRefinement fMR = new FMRefinement(gr, null, refinedParts, this.refinementIterations,
				this.maxNegativeRefinementSteps, this.maxNegativeRefinementGain, levelImbalance);
		refinedParts = fMR.getRefinedPartitions();
		return refinedParts;
	}

	private PartitionGroup lastRefinePartitions(Graph gr, PartitionGroup parts) {
		PartitionGroup refinedParts = parts;
		if (parts.getPartitionImbalance() > (1 + this.imbalanceRatio)) {
			FMBalance fMB = new FMBalance(gr, null, refinedParts, -1, this.maxNegativeRefinementGain,
					this.imbalanceRatio);
			refinedParts = fMB.getRefinedPartitions();
		}

		FMRefinement fm = new FMRefinement(gr, null, parts, this.finalRefinementIterations,
				this.maxFinalNegativeRefinementSteps, this.maxNegativeRefinementGain, this.imbalanceRatio);
		refinedParts = fm.getRefinedPartitions();
		return refinedParts;
	}

	// Collecting Data
	public StringBuilder getCoarseningDetails() {
		StringBuilder coarseningDetails = new StringBuilder();
		coarseningDetails.append("Coarsening Scheme = " + this.coarseningClass.getName() + "\r\n");
		coarseningDetails.append("Initial Maximum Coarse Node Weight = " + this.initialMaxCoarseNodeWeight + "\r\n");
		coarseningDetails.append("Final Maximum Coarse Node Weight = " + this.finalMaxCoarseNodeWeight + "\r\n");
		coarseningDetails.append("Coarsening Actual Number Of Iteration = " + this.actualCoarseningIterations + "\r\n");
		coarseningDetails.append("Coarsening Total Number Of Iteration = " + this.totalCoarseningIterations + "\r\n");
		coarseningDetails.append("Coarse Graph Num of nodes = " + this.coarseGraphNumOfNodes + "\r\n");
		coarseningDetails.append("Coarsing phase time = " + this.coarseningTime + " nano-Seconds\r\n");

		return coarseningDetails;
	}

	public StringBuilder getInitialPartitioningDetails() {
		StringBuilder initialPartitioningDetails = new StringBuilder();
		initialPartitioningDetails.append("Initial Partitioning Scheme = " + this.partitioningClass.getName() + "\r\n");
		initialPartitioningDetails
				.append("Initial Partitioning Imbalance = " + this.initialPartitiningImbalance + "\r\n");
		initialPartitioningDetails.append("Initial Partitioning EdgeCut = " + this.initialPartitioningEdgeCut + "\r\n");
		initialPartitioningDetails
				.append("Initial partitioning phase time = " + this.initialPartitioningTime + " nano-Seconds\r\n");

		return initialPartitioningDetails;
	}

	public StringBuilder getUncoarseningDetails() {
		StringBuilder uncoarseningDetails = new StringBuilder();
		uncoarseningDetails.append("Uncoarsening Scheme = Standard \r\n");
		uncoarseningDetails.append("Uncoarsening phase time = " + this.uncoarseningPhaseTime + " nano-Seconds\r\n");

		return uncoarseningDetails;
	}

	public StringBuilder getFinalDetails() {
		StringBuilder finalDetails = new StringBuilder();
		finalDetails.append("Final EdgeCut = " + this.finalEdgeCut + "\r\n");
		finalDetails.append("Final Imbalance = " + this.finalImbalance + "\r\n");
		finalDetails.append("Overall Time Consumed = " + this.coarseningTime + this.initialPartitioningTime
				+ this.uncoarseningPhaseTime + " nano-Seconds\r\n");

		return finalDetails;
	}

	private Path createStoreFolder() throws IOException {
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
		Path classFolderPath = Paths.get(graphFolderPath.toString() + "/METIS");
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

		return experimentFolder;
	}

	public void recordExperimentDetails() throws IOException {
		Path experimentFolder = createStoreFolder();
		BufferedWriter bwr;
		// Record Coarsening Details
		bwr = new BufferedWriter(new FileWriter(new File(experimentFolder + "/Coarening_Details.txt")));
		bwr.write(this.getCoarseningDetails().toString());
		bwr.flush();
		bwr.close();
	}

	public boolean verifyPartitions(PartitionGroup parts) {
		// verify partitions
		boolean verify = true;
		IntOpenHashSet allNodes = new IntOpenHashSet(originalGraph.getNumberOfNodes());
		for (int l = 0; l < originalGraph.getNumberOfNodes(); l++) {
			allNodes.add(l + 1);
		}
		for (int l = 0; l < parts.getPartitionNumber() && verify; l++) {
			Partition part = parts.getPartition(l + 1);
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

		return verify;
	}
}

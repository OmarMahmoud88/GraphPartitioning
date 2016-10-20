package utilities;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import coarsening.Matching;
import partitioning.Partitioning;
import refinement.NaiiveKLRefinement;
import structure.CoarseGraph;
import structure.Graph;
import structure.Node;
import structure.Partition;
import structure.PartitionGroup;
import uncoarsening.GGGPUncoarsening;
import uncoarsening.Uncoarsening;

public class Main {

	public static void main(String[] args)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException {

		String[] graphNames = getGraphNames("graphs");
		// loop all graphs
		for (int i = 0; i < graphNames.length; i++) {
			// if (!graphNames[i].equals("3elt")) {
			// continue;
			// }
			String fileSrc = "graphs/" + graphNames[i] + ".graph";
			
			Graph originalGraph = new Graph(fileSrc);
			if(originalGraph.getNumberOfNodes() > 5000){
				continue;
			}
			System.out.println(graphNames[i]);
			// get list of coarsen Class available in coarsening package
			ArrayList<Class<Object>> coarseningClasses = getClasses("bin/coarsening", "coarsening");
			// loop all coarsening schemes
			for (int j = 0; j < coarseningClasses.size(); j++) {
				if (coarseningClasses.get(j).getName().contains("GabowWeightedMatching")
						|| coarseningClasses.get(j).getName().contains("Blossom"))
					continue;
				ArrayList<Graph> graphs = new ArrayList<Graph>();
				graphs.add(originalGraph);
				Graph intermediate = originalGraph;
				Matching match = (Matching) coarseningClasses.get(j).newInstance();
				System.out.println(match.getSchemeName());
				// coarse graph repeatedly till defined threshold
				ArrayList<ArrayList<Integer>> originalNodesTree = null;
				while (intermediate.getNumberOfNodes() > 100) {
					ArrayList<ArrayList<Integer>> nodesTree = match.coarse(intermediate, 100);
					ArrayList<ArrayList<Integer>> mappedNodesTree = new ArrayList<ArrayList<Integer>>(nodesTree.size());
					// map the nodes tree to the original graph
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
					Constructor<Object> partConstructor = partitioningClasses.get(k).getConstructor(Graph.class,
							Integer.TYPE, Integer.TYPE, Float.TYPE);
					Partitioning gGGP = (Partitioning) partConstructor.newInstance(cGraph, 2, 20, 0);
					PartitionGroup partsGroup = gGGP.getPartitions(cGraph, 2, 20);
					System.out.println("edge Cut before refinement = " + partsGroup.getEdgeCut());
					NaiiveKLRefinement kl = new NaiiveKLRefinement(cGraph, partsGroup, 10, 0, (float) 0.0);
					PartitionGroup refinedParts = kl.getRefinedPartitions();
					// uncoarse Graph
					Graph curGraph = (CoarseGraph) cGraph;
					Uncoarsening gGGP_UC = new GGGPUncoarsening();
					while (curGraph.getNumberOfNodes() < originalGraph.getNumberOfNodes() / 2) {
						PartitionGroup pG = gGGP_UC.Uncoarsen(originalGraph, (CoarseGraph) curGraph);
						Graph prevGraph = new CoarseGraph(originalGraph, pG.getAllPartitionsNodes());
						PartitionGroup uncoarsenPartitions = uncoarsenPartitions((CoarseGraph) curGraph, prevGraph,
								refinedParts);
						kl = new NaiiveKLRefinement(prevGraph, uncoarsenPartitions, 10, 0, (float) 0.0);
						refinedParts = kl.getRefinedPartitions();
						curGraph = prevGraph;
					}
					PartitionGroup uncoarsenPartitions = uncoarsenPartitions((CoarseGraph) curGraph, originalGraph,
							refinedParts);
					kl = new NaiiveKLRefinement(originalGraph, uncoarsenPartitions, 10, 0, (float) 0.0);
					refinedParts = kl.getRefinedPartitions();
					System.out.println("edge Cut after refinement = " + refinedParts.getEdgeCut());
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

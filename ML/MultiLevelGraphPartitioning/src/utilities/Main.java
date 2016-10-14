package utilities;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import coarsening.Matching;
import partitioning.Partitioning;
import refinement.NaiiveKLRefinement;
import structure.CoarseGraph;
import structure.Graph;
import structure.Partition;
import structure.PartitionGroup;

public class Main {

	public static void main(String[] args)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException {

		String[] graphNames = getGraphNames("graphs");
		// loop all graphs
		for (int i = 0; i < graphNames.length; i++) {
//			if (!graphNames[i].equals("3elt")) {
//				continue;
//			}
			String fileSrc = "graphs/" + graphNames[i] + ".graph";
			System.out.println(graphNames[i]);
			Graph x = new Graph(fileSrc);
			

			// get list of coarsen Class available in coarsening package
			ArrayList<Class> coarseningClasses = getClasses("bin/coarsening", "coarsening");
			// loop all coarsening schemes
			for (int j = 0; j < coarseningClasses.size(); j++) {
				if(coarseningClasses.get(j).getName().contains("GabowWeightedMatching")) continue;
				ArrayList<Graph> graphs = new ArrayList<Graph>();
				graphs.add(x);
				Graph last = x;
				Matching match = (Matching) coarseningClasses.get(j).newInstance();
				System.out.println(match.getSchemeName());
				while (last.getNumberOfNodes() > 100) {
					graphs.add(new CoarseGraph(last, match.coarse(last, 100)));
					last = graphs.get(graphs.size() - 1);
					//System.out.println("number of nodes = " + last.getNumberOfNodes());
				}
				ArrayList<Class> partitioningClasses = getClasses("bin/partitioning", "partitioning");
				for (int k = 0; k < partitioningClasses.size(); k++) {
					Constructor partConstructor = partitioningClasses.get(k).getConstructor(Graph.class, Integer.TYPE,
							Integer.TYPE, Float.TYPE);
					Partitioning gGGP = (Partitioning) partConstructor.newInstance(last, 2, 20, 0);
					// Partitioning gGGP = new
					// GreedyGraphGrowingPartitioning(last, 2, 20, 0);
					PartitionGroup partsGroup = gGGP.getPartitions(last, 2, 20);
					System.out.println("edge Cut before refinement = " + partsGroup.getEdgeCut());
					// KLRefinement kl = new KLRefinement(last, partitions, 10,
					// 0,
					// (float)
					// 0.0);
					NaiiveKLRefinement kl = new NaiiveKLRefinement(last, partsGroup, 10, 0, (float) 0.0);
					PartitionGroup refinedParts = kl.getRefinedPartitions();
					// System.out.println("number of swaps = " +
					// kl.getNumberOfSwapsApplied());
					// System.out.println("edge Cut after refinement = " +
					// getEdgeCut(refinedParts, last));
					int graphIndex = graphs.size() - 1;
					while (graphIndex > 0) {
						CoarseGraph curGraph = (CoarseGraph) graphs.get(graphIndex);
						Graph previousGraph = graphs.get(graphIndex - 1);
						PartitionGroup uncoarsenPartitions = uncoarsenPartitions(curGraph, previousGraph, refinedParts);
						// System.out.println("edge Cut before refinement = " +
						// getEdgeCut(uncoarsenPartitions, previousGraph));
						// kl = new KLRefinement(previousGraph,
						// uncoarsenPartitions,
						// 10,
						// 0,
						// (float) 0.0);
						kl = new NaiiveKLRefinement(previousGraph, uncoarsenPartitions, 10, 0, (float) 0.0);
						refinedParts = kl.getRefinedPartitions();
						// System.out.println("number of swaps = " +
						// kl.getNumberOfSwapsApplied());
						// System.out.println("edge Cut after refinement = " +
						// getEdgeCut(refinedParts, previousGraph));
						graphIndex--;
					}
					System.out.println("edge Cut after refinement = " + refinedParts.getEdgeCut());
				}

			}
		}
	}

	private static PartitionGroup uncoarsenPartitions(CoarseGraph curGraph, Graph previousGraph,
			PartitionGroup refinedParts) {
		PartitionGroup uncoarsenParts = new PartitionGroup(previousGraph);
		int partitionNum = refinedParts.getPartitionNumber();
		for (int i = 1; i < partitionNum + 1; i++) {
			Partition part = refinedParts.getPartition(i);
			int partitionID = part.getPartitionID();
			Partition uncoarsenPart = new Partition(previousGraph, partitionID);
			HashSet<Integer> nodes = part.getNodeIDs();
			Iterator<Integer> it = nodes.iterator();
			while (it.hasNext()) {
				int nodeID = it.next();
				ArrayList<Integer> childsNodes = curGraph.getNodeChilds(nodeID);
				for (int j = 0; j < childsNodes.size(); j++) {
					uncoarsenPart.addNode(childsNodes.get(j));
				}
			}
			uncoarsenParts.addPartition(uncoarsenPart);
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

	private static ArrayList<Class> getClasses(String packagePath, String packageName) throws ClassNotFoundException {
		File folder = new File(packagePath);
		FileFilter classFileFilter = new ClassFileFilter();
		File[] listOfFiles = folder.listFiles(classFileFilter);
		String[] classNames = new String[listOfFiles.length];
		for (int i = 0; i < listOfFiles.length; i++) {
			classNames[i] = listOfFiles[i].getName().replace(".class", "");
		}

		ArrayList<Class> classes = new ArrayList<Class>();
		for (int i = 0; i < classNames.length; i++) {
			Class cls = Class.forName(packageName + "." + classNames[i]);
			if (!Modifier.isAbstract(cls.getModifiers())) {
				classes.add(cls);
			}
		}
		return classes;
	}
}

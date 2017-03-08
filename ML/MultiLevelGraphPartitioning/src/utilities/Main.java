package utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import algorithms.METIS;
import algorithms.METIS_Enhanced;

public class Main {
	private static int numberOfPartitions = 2;

	final private static HashSet<String> coarseningSchemes = new HashSet<String>(Arrays.asList(
			"coarsening.OrderedHeaviestEdgeMatching", "coarsening.HeaviestEdgeMatching",
			"coarsening.OrderedHeavyEdgeMatching", "coarsening.HeavyEdgeMatching", "coarsening.LightEdgeMatching"));
	final private static HashSet<String> partitioningSchemes = new HashSet<String>(
			Arrays.asList("partitioning.GreedyGraphGrowingPartitioning"));
	final private static HashSet<String> excludedGraphs = new HashSet<String>(Arrays.asList("auto", "m14b"));
	final private static int numberOfTrials = 5;
	final private static int numberOfRuns = 20;
	final private static float[] imbalanceRatiosList = new float[] { (float) 0.001999, (float) 0.010999,
			(float) 0.030999, (float) 0.050999 };
	final private static int refinementIterations = 100;
	final private static int maxNegativeRefinementSteps = 100;
	final private static int finalRefinementIterations = 100;
	final private static int maxFinalNegativeRefinementSteps = 100;
	final private static int maxNegativeRefinementGain = -10000;

	public static void main(String[] args)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException, IOException {
		String[] graphNames = getGraphNames("../../graphs");
		String tabs = "";
		METIS_Enhanced expME = null;
		METIS expM = null;
		// loop number of partitions
		for (int x = 2; x < 33; x *= 2) {
			numberOfPartitions = x;
			System.out.println(tabs + "Number of Partitions = " + numberOfPartitions);
			// loop imbalance ratios
			tabs += "    ";
			for (int y = 0; y < imbalanceRatiosList.length; y++) {
				System.out.println(tabs + "Imbalance Ratio = " + imbalanceRatiosList[y]);
				// loop all graphs
				tabs += "    ";
				for (int i = 0; i < graphNames.length; i++) {
					if (excludedGraphs.contains(graphNames[i]))
						continue;
					System.out.println(tabs + "Graph = " + graphNames[i]);
					// loop all coarsening schemes
					ArrayList<Class<Object>> coarseningClasses = getClasses("bin/coarsening", "coarsening");
					tabs += "    ";
					for (int j = 0; j < coarseningClasses.size(); j++) {
						if (!coarseningSchemes.contains(coarseningClasses.get(j).getName()))
							continue;
						System.out.println(tabs + "Coarsening Class = " + coarseningClasses.get(j));
						// loop partitioning algorithms
						ArrayList<Class<Object>> partitioningClasses = getClasses("bin/partitioning", "partitioning");
						tabs += "    ";
						for (int k = 0; k < partitioningClasses.size(); k++) {
							if (!partitioningSchemes.contains(partitioningClasses.get(k).getName()))
								continue;
							System.out.println(tabs + "Partitioning Class = " + partitioningClasses.get(k));

							// System.out.println(tabs + " METIS Enhanced");
							// expME = new METIS_Enhanced(numberOfPartitions,
							// coarseningClasses.get(j),
							// partitioningClasses.get(k), graphNames[i],
							// numberOfTrials, numberOfRuns,
							// imbalanceRatiosList[y], refinementIterations,
							// maxNegativeRefinementSteps,
							// finalRefinementIterations,
							// maxFinalNegativeRefinementSteps,
							// maxNegativeRefinementGain);

							System.out.println();
							System.out.println(tabs + "    METIS");
							expM = new METIS(numberOfPartitions, coarseningClasses.get(j), partitioningClasses.get(k),
									graphNames[i], numberOfTrials, numberOfRuns, imbalanceRatiosList[y],
									refinementIterations, maxNegativeRefinementSteps, finalRefinementIterations,
									maxFinalNegativeRefinementSteps, maxNegativeRefinementGain);
						}
						tabs = tabs.substring(4);
					}
					tabs = tabs.substring(4);
				}
				tabs = tabs.substring(4);
			}
			tabs = tabs.substring(4);
		}
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

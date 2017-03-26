package utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import algorithms.METIS_Origin;
import algorithms.METIS_RecursiveBisection;
import statistics.ExperimentRun;
import statistics.ExperimentRunGroup;
import structure.Graph;

public class Main {

	final private static HashSet<String> coarseningSchemes = new HashSet<String>(Arrays.asList(
			// "coarsening.OrderedHeaviestEdgeMatching",
			// "coarsening.HeaviestEdgeMatching",
			"coarsening.OrderedHeavyEdgeMatching"
	// , "coarsening.HeavyEdgeMatching"
	// "coarsening.LightEdgeMatching"
	));
	final private static HashSet<String> partitioningSchemes = new HashSet<String>(
			Arrays.asList("partitioning.GreedyGraphGrowingPartitioning", "partitioning.GGGP_Enhanced"));
	final private static HashSet<String> excludedGraphs = new HashSet<String>(
			// Arrays.asList("auto", "m14b", "fe_body", "fe_pwt", "bcsstk31",
			// "bcsstk29"));
			Arrays.asList());
	final private static int numberOfTrials = 5;
	final private static int numberOfRuns = 10;
	final private static float[] imbalanceRatiosList = new float[] { (float) 0.001999, (float) 0.010999,
			(float) 0.030999, (float) 0.050999 };
	final private static int refinementIterations = 10;
	final private static int maxNegativeRefinementSteps = 10;
	final private static int finalRefinementIterations = 10;
	final private static int maxFinalNegativeRefinementSteps = 10;
	final private static int maxNegativeRefinementGain = -10000;

	@SuppressWarnings("unchecked")
	public static void main(String[] args)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException,
			SecurityException, IllegalArgumentException, InvocationTargetException, IOException {
		String[] graphNames = getGraphNames("../../graphs");
		String tabs = "";
		Graph graph = null;
		ArrayList<Class<Object>> coarseningClasses = getClasses("bin/coarsening", "coarsening");
		ArrayList<Class<Object>> partitioningClasses = getClasses("bin/partitioning", "partitioning");
		int numberOfPartitions = 0;
		StringBuilder logBuilder = new StringBuilder(1024);
		long currentTime = System.currentTimeMillis();
		String logFileName = "log_" + currentTime + ".txt";
		Path logFilePath = createLogFile("../../Experiments", logFileName);

		BufferedWriter bwr = new BufferedWriter(new FileWriter(new File(logFilePath.toString())));
		logBuilder.append("Job # " + currentTime + "\r\n");
		logBuilder.append("***********************" + "\r\n");
		// loop number of partitions
		for (int x = 32; x >= 2; x /= 2) {
			numberOfPartitions = x;
			logBuilder.append(tabs + "Number of Partitions = " + numberOfPartitions + "\r\n");
			System.out.println(tabs + "Number of Partitions = " + numberOfPartitions);
			tabs += "    ";
			// loop imbalance ratios
			for (int y = 0; y < imbalanceRatiosList.length; y++) {
				logBuilder.append(tabs + "Imbalance Ratio = " + imbalanceRatiosList[y] + "\r\n");
				System.out.println(tabs + "Imbalance Ratio = " + imbalanceRatiosList[y]);
				// loop all graphs
				tabs += "    ";
				for (int i = 0; i < graphNames.length; i++) {
					if (excludedGraphs.contains(graphNames[i])
					// || !graphNames[i].equals("memplus")
					) {
						continue;
					}
					String graphFilePath = "../../graphs/" + graphNames[i] + ".graph";
					graph = new Graph(graphFilePath);
					graph.setGraphName(graphNames[i]);

					logBuilder.append(tabs + "Graph = " + graphNames[i] + "\r\n");
					System.out.println(tabs + "Graph = " + graphNames[i]);
					// loop all coarsening schemes
					tabs += "    ";
					for (int j = 0; j < coarseningClasses.size(); j++) {
						if (!coarseningSchemes.contains(coarseningClasses.get(j).getName()))
							continue;
						logBuilder.append(tabs + "Coarsening Class = " + coarseningClasses.get(j) + "\r\n");
						// loop partitioning algorithms
						tabs += "    ";
						for (int k = 0; k < partitioningClasses.size(); k++) {
							if (!partitioningSchemes.contains(partitioningClasses.get(k).getName()))
								continue;
							logBuilder.append(tabs + "Partitioning Class = " + partitioningClasses.get(k) + "\r\n");
							//
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

							// System.out.println();
							// System.out.println(tabs + " METIS");
							// expM = new METIS(numberOfPartitions,
							// coarseningClasses.get(j),
							// partitioningClasses.get(k),
							// graphNames[i], numberOfTrials, numberOfRuns,
							// imbalanceRatiosList[y],
							// refinementIterations, maxNegativeRefinementSteps,
							// finalRefinementIterations,
							// maxFinalNegativeRefinementSteps,
							// maxNegativeRefinementGain);

							METIS_RecursiveBisection expMO = null;
							ExperimentRun run = null;
							ExperimentRunGroup runGroup = new ExperimentRunGroup(numberOfPartitions,
									imbalanceRatiosList[y], graphNames[i], coarseningClasses.get(j).getName(),
									partitioningClasses.get(k).getName(), numberOfTrials, refinementIterations,
									maxNegativeRefinementSteps, finalRefinementIterations,
									maxFinalNegativeRefinementSteps, maxNegativeRefinementGain);
							Path storingFolder = createStoreFolder(graphNames[i], numberOfPartitions,
									imbalanceRatiosList[y], coarseningClasses.get(j), partitioningClasses.get(k),
									(Class<Object>) Class.forName("algorithms.METIS_RecursiveBisection"));
							if (storingFolder == null) {
								runGroup = null;
								storingFolder = null;
								continue;
							}
							tabs += "    ";
							for (int l = 1; l <= numberOfRuns; l++) {
								logBuilder.append(tabs + "Run # " + l + "\r\n");
								System.out.println(tabs + "Run # " + l);
								expMO = new METIS_RecursiveBisection(graph, numberOfPartitions,
										coarseningClasses.get(j), partitioningClasses.get(k), numberOfTrials,
										imbalanceRatiosList[y], refinementIterations, maxNegativeRefinementSteps,
										finalRefinementIterations, maxFinalNegativeRefinementSteps,
										maxNegativeRefinementGain);

								run = expMO.partitionGraph();
								run.setRunID(l);
								runGroup.addExperimentRun(run);
								// deallocate Objects
								expMO = null;
								run = null;
							}
							writeToFile(storingFolder, "Experiment_Parameters.txt",
									runGroup.getRunGroupParametersFileDump());
							writeToFile(storingFolder, "Runs_Details.txt", runGroup.getRunGroupFileDump());
							if (runGroup.getBestRun() != null) {
								writeToFile(storingFolder, "Best_Run.txt",
										runGroup.getBestRun().getPartsGroup().toString());
							}
							if (runGroup.getBestBalancedRun() != null) {
								writeToFile(storingFolder, "Best_Balance_Run.txt",
										runGroup.getBestBalancedRun().getPartsGroup().toString());
							}
							writeToFile(storingFolder, "Summary.txt", runGroup.getRunGroupSummary());
							// Log
							if (logBuilder.length() > 1024) {
								bwr.write(logBuilder.toString());
								bwr.flush();
								System.out.println("Flush");
								logBuilder = new StringBuilder(1024);
							}

							tabs = tabs.substring(4);
							// deallocate Objects
							runGroup = null;
							storingFolder = null;
						}
						tabs = tabs.substring(4);
					}
					tabs = tabs.substring(4);
					// deallocate Objects
					graph = null;
				}
				tabs = tabs.substring(4);
			}
			tabs = tabs.substring(4);
		}
		bwr.write(logBuilder.toString());
		bwr.flush();
		System.out.println("Flush");
		bwr.close();
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

	private static Path createStoreFolder(String graphName, int numberOfPartitions, float imbalanceRatio,
			Class<Object> coarseningClass, Class<Object> partitioningClass, Class<Object> algorithm)
			throws IOException {
		// Experiments Folder
		Path resultsFolderPath = Paths.get("../../Experiments");
		// Check if folder Exists, if not create it
		if (!Files.exists(resultsFolderPath)) {
			Files.createDirectories(resultsFolderPath);
		}

		// graph folder
		Path graphFolderPath = Paths.get(resultsFolderPath + "/" + graphName);
		// Check if folder Exists, if not create it
		if (!Files.exists(graphFolderPath)) {
			Files.createDirectories(graphFolderPath);
		}

		// Class Folder
		Path classFolderPath = Paths.get(graphFolderPath.toString() + "/" + algorithm.getName());
		// Check if folder Exists, if not create it
		if (!Files.exists(classFolderPath)) {
			Files.createDirectories(classFolderPath);
		}

		// partition folder
		Path partitionFolderPath = Paths.get(classFolderPath.toString() + "/Parts_" + numberOfPartitions);
		// Check if folder Exists, if not create it
		if (!Files.exists(partitionFolderPath)) {
			Files.createDirectories(partitionFolderPath);
		}

		// imbalance folder
		Path imbalanceFolderPath = Paths.get(partitionFolderPath + "/imbalance_" + imbalanceRatio);
		// Check if folder Exists, if not create it
		if (!Files.exists(imbalanceFolderPath)) {
			Files.createDirectories(imbalanceFolderPath);
		}

		// Coarsening Folder
		Path coarseningFolder = Paths.get(imbalanceFolderPath + "/" + coarseningClass);
		// Check if folder Exists, if not create it
		if (!Files.exists(coarseningFolder)) {
			Files.createDirectories(coarseningFolder);
		}

		// initial Partitioning Algorithm Folder
		Path partitioningFolder = Paths.get(coarseningFolder + "/" + partitioningClass.getName());
		// Check if folder Exists, if not create it
		if (!Files.exists(partitioningFolder)) {
			Files.createDirectories(partitioningFolder);
		}

		if (new File(partitioningFolder.toString()).listFiles().length == 0) {
			// Experiment Folder
			Path experimentFolder = Paths.get(partitioningFolder + "/" + System.currentTimeMillis());
			// Check if folder Exists, if not create it
			if (!Files.exists(experimentFolder)) {
				Files.createDirectories(experimentFolder);
			}
			return experimentFolder;
		}

		return null;
	}

	private static Path createLogFile(String path, String fileName) throws IOException {
		Path resultsFolderPath = Paths.get(path);
		// Check if folder Exists, if not create it
		if (!Files.exists(resultsFolderPath)) {
			Files.createDirectories(resultsFolderPath);
		}

		Path logFilePath = Paths.get(path + "/" + fileName);
		// Check if folder Exists, if not create it
		if (!Files.exists(logFilePath)) {
			Files.createFile(logFilePath);
		}
		return logFilePath;
	}

	private static void writeToFile(Path folderPath, String fileName, String content) throws IOException {
		BufferedWriter bwr = new BufferedWriter(new FileWriter(new File(folderPath + "/" + fileName)));
		bwr.write(content);
		bwr.flush();
		bwr.close();
	}
}

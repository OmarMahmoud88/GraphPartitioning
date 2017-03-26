import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MainClass {
	final static int[] parts = { 2, 4, 8, 16, 32 };
	final static String[] imbalances = { "0.001999", "0.010999", "0.030999", "0.050999" };
	final static String rootPath = "/home/omar/git/GraphPartitioning/Experiments";
	final static String graphsFolderPath = "C:\\Users\\omar\\git\\GraphPartitioning\\graphs";
	final static int trials = 10;
	final static int refinementIterations = 100;
	final static String algorithm = "algorithms.METIS_Origin";
	final static String coarseningUsed = "class coarsening.OrderedHeavyEdgeMatching";
	final private static String[] coarseningSchemes = { "class coarsening.OrderedHeavyEdgeMatching" };
	final private static String[] partitioningSchemes = { "partitioning.GGGP_Enhanced",
			"partitioning.GreedyGraphGrowingPartitioning" };

	public static void main(String[] args) throws IOException {
		// create document
		// Create blank workbook
		XSSFWorkbook workbook = new XSSFWorkbook();
		String[] graphs = getFoldersNames(rootPath);
		Cell cell;
		for (int k = 0; k < imbalances.length; k++) {
			String currentImbalance = imbalances[k];
			int rowid = 2;
			// Create sheet for each imbalance
			// Create a blank sheet
			XSSFSheet spreadSheet = workbook.createSheet("imbalance_" + currentImbalance);
			createHeader(spreadSheet);
			for (int i = 0; i < graphs.length; i++) {
				String graphResultsPath = rootPath + "/" + graphs[i];
				XSSFRow row = spreadSheet.createRow(rowid++);
				int cellID = 0;
				cell = row.createCell(cellID++);
				cell.setCellValue(graphs[i]);
				String algorithmResultsPath = graphResultsPath + "/" + algorithm;
				for (int j = 0; j < parts.length; j++) {
					String partResultsPath = algorithmResultsPath + "/Parts_" + parts[j];
					String imbalanceResultsPath = partResultsPath + "/imbalance_" + currentImbalance;
					String coarseningResultsPath = imbalanceResultsPath + "/" + coarseningUsed;
					for (int l = 0; l < partitioningSchemes.length; l++) {
						String partitioningResultsPath = coarseningResultsPath + "/" + partitioningSchemes[l];
						String[] runs = getFoldersNames(partitioningResultsPath);
						for (int m = 0; m < runs.length; m++) {
							String runResultsPath = partitioningResultsPath + "/" + runs[m];
							String summaryFilePath = runResultsPath + "/Summary.txt";
							ExperimentSummary eS = new ExperimentSummary(summaryFilePath);
							cell = row.createCell(cellID++);
							cell.setCellValue((eS.bestBalancedEdgeCut == -1)
									? (eS.bestEdgeCut + "(" + eS.bestEdgeCutImbalance + ")")
									: (eS.bestBalancedEdgeCut + ""));
							cell = row.createCell(cellID++);
							cell.setCellValue(eS.averageEdgeCut);
						}
					}
				}
			}
		}

		writeWorkBookToFile(workbook);
	}

	private static void writeWorkBookToFile(XSSFWorkbook workbook) throws IOException {
		// Write the workbook in file system
		FileOutputStream out = new FileOutputStream(new File("Writesheet.xlsx"));
		workbook.write(out);
		out.close();
		System.out.println("Writesheet.xlsx written successfully");

	}

	private static void createHeader(XSSFSheet spreadSheet) {
		int rowid = 0;
		// create parts row
		XSSFRow firstRow = spreadSheet.createRow(rowid++);
		// creates partitions row
		XSSFRow secondRow = spreadSheet.createRow(rowid++);
		int cellid = 1;
		// empty cell
		// each 4 cells should contains on part
		for (int i = 2; i <= 32; i *= 2) {
			Cell cell = firstRow.createCell(cellid);
			cell.setCellValue("" + i);

			cell = secondRow.createCell(cellid + 0);
			cell.setCellValue("E_GGGP_Best");

			cell = secondRow.createCell(cellid + 1);
			cell.setCellValue("E_GGGP_AVG");

			cell = secondRow.createCell(cellid + 2);
			cell.setCellValue("GGGP_Best");

			cell = secondRow.createCell(cellid + 3);
			cell.setCellValue("GGGP_AVG");
			cellid += 4;
		}

	}

	private static String[] getFoldersNames(String folderPath) {

		File folder = new File(folderPath);
		FileFilter folderFilter = new DirectoryFilter();
		File[] listOfFolders = folder.listFiles(folderFilter);
		String[] folderNames = new String[listOfFolders.length];
		for (int i = 0; i < listOfFolders.length; i++) {
			folderNames[i] = listOfFolders[i].getName();
		}
		return folderNames;
	}

}

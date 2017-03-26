import java.io.BufferedReader;
import java.io.FileReader;

public class CoarseningDetails {

	String coarseningScheme;
	int maxNodeWeight;
	int coarseningIterations;
	int numOfCoarseNodes;

	public CoarseningDetails(String filePath) {
		FileReader in;
		BufferedReader br;
		String line;
		try {
			in = new FileReader(filePath);
			br = new BufferedReader(in);
			int lineCount = 1;
			while ((line = br.readLine()) != null) {
				int startIndex = line.indexOf(" = ") + 3;
				switch (lineCount) {
				case 1:
					coarseningScheme = line.substring(startIndex);
				case 2:
					this.maxNodeWeight = Integer.parseInt(line.substring(startIndex));
				case 3:
					this.coarseningIterations = Integer.parseInt(line.substring(startIndex));
				case 4:
					this.numOfCoarseNodes = Integer.parseInt(line.substring(startIndex));
				default:
					break;
				}
				lineCount++;
			}
			in.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

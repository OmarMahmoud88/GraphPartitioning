import java.io.BufferedReader;
import java.io.FileReader;

public class ExperimentSummary {
	int bestBalancedEdgeCut;
	float bestBalancedImbalance;
	int bestEdgeCut;
	float bestEdgeCutImbalance;
	long averageRunningTime;
	float averageEdgeCut;

	public ExperimentSummary(String filePath) {
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
				case 3:
					// Best Edge cut
					try {
						this.bestEdgeCut = Integer.parseInt(line.substring(startIndex));
					} catch (Exception e) {
						this.bestEdgeCut = -1;
					}

				case 4:
					// Best imbalance
					try {
						this.bestEdgeCutImbalance = Float.parseFloat(line.substring(startIndex));
					} catch (Exception e) {
						// TODO: handle exception
						this.bestEdgeCutImbalance = -1;
					}

				case 8:
					// Best balanced Edge cut
					try {
						this.bestBalancedEdgeCut = Integer.parseInt(line.substring(startIndex));
					} catch (Exception e) {
						// TODO: handle exception
						this.bestBalancedEdgeCut = -1;
					}

				case 9:
					// Best balanced imbalance
					try {
						this.bestBalancedImbalance = Float.parseFloat(line.substring(startIndex));

					} catch (Exception e) {
						// TODO: handle exception
						this.bestBalancedImbalance = -1;
					}

				case 11:
					// average running time in nanoseconds
					try {
						this.averageRunningTime = Long.parseLong(line.substring(startIndex));
					} catch (Exception e) {
						// TODO: handle exception
						this.averageRunningTime = -1;
					}

				case 12:
					// average EdgeCut
					try {
						this.averageEdgeCut = Float.parseFloat(line.substring(startIndex));
					} catch (Exception e) {
						// TODO: handle exception
						this.averageEdgeCut = -1;
					}

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

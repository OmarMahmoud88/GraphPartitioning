import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;

public class PartitioningDetails {
	int balancedEdgeCut;
	float balancedImbalance;
	int imbalancedEdgeCut;
	float imbalancedImbalance;
	Duration averageRunTime;

	public PartitioningDetails(String filePath) {
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
					// do Nothing
				case 2:
					// Best balanced Edge cut
					this.balancedEdgeCut = Integer.parseInt(line.substring(startIndex));
				case 3:
					// Best balanced imbalance
					this.balancedImbalance = Float.parseFloat(line.substring(startIndex));
				case 4:
					// do Nothing
				case 5:
					// Best Edge cut
					this.imbalancedEdgeCut = Integer.parseInt(line.substring(startIndex));
				case 6:
					// Best Cut Imbalance
					this.imbalancedImbalance = Float.parseFloat(line.substring(startIndex));
				case 7:
					// Average Duration
					this.averageRunTime = Duration.parse(line.substring(startIndex));
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

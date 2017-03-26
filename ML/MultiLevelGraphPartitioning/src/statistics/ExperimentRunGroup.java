package statistics;

public class ExperimentRunGroup {

	private int numberOfRuns;
	private float allowedImbalance;
	private int numberOfPartitions;
	private String graphName;
	private String coarseningScheme;
	private String partitioningScheme;
	private long totalTime;
	private long totalEdgeCut;
	private ExperimentRun bestBalancedRun;
	private ExperimentRun bestRun;
	private StringBuilder runGroupFileDump;
	private int initialPartitioningNumberOfTrials;
	private int refinementIterations;
	private int maxNegativeRefinementSteps;
	private int finalRefinementIterations;
	private int maxFinalNegativeRefinementSteps;
	private int maxNegativeRefinementGain;

	public ExperimentRunGroup(int numberOfPartitions, float allowedImbalance, String graphName, String coarseningScheme,
			String partitioningScheme, int initialPartitioningNumberOfTrials, int refinementIterations,
			int maxNegativeRefinementSteps, int finalRefinementIterations, int maxFinalNegativeRefinementSteps,
			int maxNegativeRefinementGain) {
		this.numberOfRuns = 0;
		this.numberOfPartitions = numberOfPartitions;
		this.allowedImbalance = 1 + allowedImbalance;
		this.graphName = graphName;
		this.coarseningScheme = coarseningScheme;
		this.partitioningScheme = partitioningScheme;
		this.initialPartitioningNumberOfTrials = initialPartitioningNumberOfTrials;
		this.refinementIterations = refinementIterations;
		this.maxNegativeRefinementSteps = maxNegativeRefinementSteps;
		this.finalRefinementIterations = finalRefinementIterations;
		this.maxFinalNegativeRefinementSteps = maxFinalNegativeRefinementSteps;
		this.maxNegativeRefinementGain = maxNegativeRefinementGain;
		this.bestBalancedRun = null;
		this.bestRun = null;
		this.totalEdgeCut = 0;
		this.totalTime = 0;
		this.runGroupFileDump = new StringBuilder();
	}

	public void addExperimentRun(ExperimentRun run) {
		this.numberOfRuns++;
		this.totalTime += run.getOverAllTime();
		this.totalEdgeCut += run.getFinalEdgeCut();
		this.bestBalancedRun = getBestBalancedRun(run);
		this.bestRun = getBestRun(run);
		this.runGroupFileDump.append(run.getDetailsFileDump());
	}

	private ExperimentRun getBestBalancedRun(ExperimentRun run) {
		if (run == null || run.getFinalImbalance() > this.allowedImbalance)
			return this.bestBalancedRun;

		if (this.bestBalancedRun == null)
			return run;

		if (run.getFinalEdgeCut() < this.bestBalancedRun.getFinalEdgeCut())
			return run;
		else if (run.getFinalEdgeCut() == this.bestBalancedRun.getFinalEdgeCut()) {
			if (run.getFinalImbalance() < this.bestBalancedRun.getFinalImbalance()) {
				return run;
			}
		}
		return this.bestBalancedRun;
	}

	private ExperimentRun getBestRun(ExperimentRun run) {
		if (run == null)
			return this.bestRun;

		if (this.bestRun == null)
			return run;

		if (run.getFinalEdgeCut() < this.bestRun.getFinalEdgeCut()) {
			return run;
		}
		return this.bestRun;
	}

	public String getRunGroupParametersFileDump() {
		StringBuilder groupParameters = new StringBuilder();

		groupParameters.append("Number of partitions = " + this.numberOfPartitions + "\r\n");
		groupParameters.append("Imbalance ration allowed = " + this.allowedImbalance + "\r\n");
		groupParameters.append("Graph name = " + this.graphName + "\r\n");
		groupParameters.append("Coarsening scheme = " + this.coarseningScheme + "\r\n");
		groupParameters.append("Partitioning Scheme = " + this.partitioningScheme + "\r\n");
		groupParameters.append("Initial partitioning Number of Trials = " + this.initialPartitioningNumberOfTrials + "\r\n");
		groupParameters.append("Refinement Iterations= " + this.refinementIterations + "\r\n");
		groupParameters.append("Max Negative Refinement Steps = " + this.maxNegativeRefinementSteps + "\r\n");
		groupParameters.append("Final Step Refinement Iterations = " + this.finalRefinementIterations + "\r\n");
		groupParameters.append("Max Final Negative Refinement Steps = " + this.maxFinalNegativeRefinementSteps + "\r\n");
		groupParameters.append("Max Negative Gain Allowed = " + this.maxNegativeRefinementGain + "\r\n");

		return groupParameters.toString();
	}

	public String getRunGroupSummary() {
		StringBuilder groupSummary = new StringBuilder();
		groupSummary.append("Best partition \r\n");
		groupSummary.append("--------------\r\n");
		if (this.bestRun != null) {
			groupSummary.append("EdgeCut = " + this.bestRun.finalEdgeCut + "\r\n");
			groupSummary.append("Imbalance = " + this.bestRun.finalImbalance + "\r\n");
		} else {
			groupSummary.append("EdgeCut = N/A\r\n");
			groupSummary.append("Imbalance = N/A\r\n");
		}
		groupSummary.append("==================================\r\n");
		groupSummary.append("Best balance partition \r\n");
		groupSummary.append("--------------\r\n");
		if (this.bestBalancedRun != null) {
			groupSummary.append("EdgeCut = " + this.bestBalancedRun.finalEdgeCut + "\r\n");
			groupSummary.append("Imbalance = " + this.bestBalancedRun.finalImbalance + "\r\n");
		} else {
			groupSummary.append("EdgeCut = N/A\r\n");
			groupSummary.append("Imbalance = N/A\r\n");
		}
		groupSummary.append("==================================\r\n");
		groupSummary.append("Average Running Time = " + this.getAvgTime() + "\r\n");
		groupSummary.append("Average EdgeCut = " + this.getAvgEdgeCut() + "\r\n");
		groupSummary.append("******************************************************************\r\n");
		groupSummary.append("******************************************************************\r\n");

		return groupSummary.toString();
	}

	public String getRunGroupFileDump() {
		return runGroupFileDump.toString();
	}

	/* Getters & Setters */
	public double getAvgTime() {
		return ((double) this.totalTime) / this.numberOfRuns;
	}

	public double getAvgEdgeCut() {
		return ((double) this.totalEdgeCut) / this.numberOfRuns;
	}

	public ExperimentRun getBestBalancedRun() {
		return bestBalancedRun;
	}

	public ExperimentRun getBestRun() {
		return bestRun;
	}
}

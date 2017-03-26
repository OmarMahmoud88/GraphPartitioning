package statistics;

public class METIS_Origin_Run extends ExperimentRun {
	private String coarseningScheme;
	private float initialMaxCoarseNodeWeight;
	private float finalMaxCoarseNodeWeight;
	private int actualCoarseningIterations;
	private int totalCoarseningIterations;
	private int coarseGraphNumOfNodes;
	private long coarseningTime;
	/*****************************************/
	private String partitioningScheme;
	private float initialPartitiningImbalance;
	private int initialPartitioningEdgeCut;
	private long initialPartitioningTime;
	/*****************************************/
	private String uncoarseningScheme;
	private long uncoarseningPhaseTime;

	/*****************************************/

	public String getDetailsFileDump() {
		StringBuilder runDetails = new StringBuilder();
		runDetails.append("Run # " + this.runID + "\r\n");
		runDetails.append("--------------\r\n");
		runDetails.append("\tCoarsening Scheme = " + this.coarseningScheme + "\r\n");
		runDetails.append("\tInitial Maximum Coarse Node Weight = " + this.initialMaxCoarseNodeWeight + "\r\n");
		runDetails.append("\tFinal Maximum Coarse Node Weight = " + this.finalMaxCoarseNodeWeight + "\r\n");
		runDetails.append("\tCoarsening Actual Number Of Iteration = " + this.actualCoarseningIterations + "\r\n");
		runDetails.append("\tCoarsening Total Number Of Iteration = " + this.totalCoarseningIterations + "\r\n");
		runDetails.append("\tCoarse Graph Num of nodes = " + this.coarseGraphNumOfNodes + "\r\n");
		runDetails.append("\tCoarsing phase time = " + this.coarseningTime + " nano-Seconds\r\n");
		runDetails.append("==================================\r\n");
		runDetails.append("\tInitial Partitioning Scheme = " + this.partitioningScheme + "\r\n");
		runDetails.append("\tInitial Partitioning Imbalance = " + this.initialPartitiningImbalance + "\r\n");
		runDetails.append("\tInitial Partitioning EdgeCut = " + this.initialPartitioningEdgeCut + "\r\n");
		runDetails.append("\tInitial partitioning phase time = " + this.initialPartitioningTime + " nano-Seconds\r\n");
		runDetails.append("==================================\r\n");
		runDetails.append("\tUncoarsening Scheme = " + this.uncoarseningScheme + "\r\n");
		runDetails.append("\tUncoarsening phase time = " + this.uncoarseningPhaseTime + " nano-Seconds\r\n");
		runDetails.append("==================================\r\n");
		runDetails.append("\tFinal EdgeCut = " + this.finalEdgeCut + "\r\n");
		runDetails.append("\tFinal Imbalance = " + this.finalImbalance + "\r\n");
		runDetails.append("\tOverall Time Consumed = "
				+ (this.coarseningTime + this.initialPartitioningTime + this.uncoarseningPhaseTime)
				+ " nano-Seconds\r\n");
		runDetails.append("\tVerified = " + this.verified + "\r\n");
		runDetails.append("******************************************************************\r\n");
		runDetails.append("******************************************************************\r\n");

		return runDetails.toString();
	}

	/* Getters & Setters */

	public String getCoarseningScheme() {
		return coarseningScheme;
	}

	public void setCoarseningScheme(String coarseningScheme) {
		this.coarseningScheme = coarseningScheme;
	}

	public float getInitialMaxCoarseNodeWeight() {
		return initialMaxCoarseNodeWeight;
	}

	public void setInitialMaxCoarseNodeWeight(float initialMaxCoarseNodeWeight) {
		this.initialMaxCoarseNodeWeight = initialMaxCoarseNodeWeight;
	}

	public float getFinalMaxCoarseNodeWeight() {
		return finalMaxCoarseNodeWeight;
	}

	public void setFinalMaxCoarseNodeWeight(float finalMaxCoarseNodeWeight) {
		this.finalMaxCoarseNodeWeight = finalMaxCoarseNodeWeight;
	}

	public int getActualCoarseningIterations() {
		return actualCoarseningIterations;
	}

	public void setActualCoarseningIterations(int actualCoarseningIterations) {
		this.actualCoarseningIterations = actualCoarseningIterations;
	}

	public int getTotalCoarseningIterations() {
		return totalCoarseningIterations;
	}

	public void setTotalCoarseningIterations(int totalCoarseningIterations) {
		this.totalCoarseningIterations = totalCoarseningIterations;
	}

	public int getCoarseGraphNumOfNodes() {
		return coarseGraphNumOfNodes;
	}

	public void setCoarseGraphNumOfNodes(int coarseGraphNumOfNodes) {
		this.coarseGraphNumOfNodes = coarseGraphNumOfNodes;
	}

	public long getCoarseningTime() {
		return coarseningTime;
	}

	public void setCoarseningTime(long coarseningTime) {
		this.coarseningTime = coarseningTime;
	}

	public String getPartitioningScheme() {
		return partitioningScheme;
	}

	public void setPartitioningScheme(String partitioningScheme) {
		this.partitioningScheme = partitioningScheme;
	}

	public int getInitialPartitioningEdgeCut() {
		return initialPartitioningEdgeCut;
	}

	public void setInitialPartitioningEdgeCut(int initialPartitioningEdgeCut) {
		this.initialPartitioningEdgeCut = initialPartitioningEdgeCut;
	}

	public float getInitialPartitiningImbalance() {
		return initialPartitiningImbalance;
	}

	public void setInitialPartitiningImbalance(float initialPartitiningImbalance) {
		this.initialPartitiningImbalance = initialPartitiningImbalance;
	}

	public long getInitialPartitioningTime() {
		return initialPartitioningTime;
	}

	public void setInitialPartitioningTime(long initialPartitioningTime) {
		this.initialPartitioningTime = initialPartitioningTime;
	}

	public String getUncoarseningScheme() {
		return uncoarseningScheme;
	}

	public void setUncoarseningScheme(String uncoarseningScheme) {
		this.uncoarseningScheme = uncoarseningScheme;
	}

	public long getUncoarseningPhaseTime() {
		return uncoarseningPhaseTime;
	}

	public void setUncoarseningPhaseTime(long uncoarseningPhaseTime) {
		this.uncoarseningPhaseTime = uncoarseningPhaseTime;
	}

}

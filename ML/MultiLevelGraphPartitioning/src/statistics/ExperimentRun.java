package statistics;

import structure.PartitionGroup;

public abstract class ExperimentRun {
	protected int runID;
	protected PartitionGroup partsGroup;
	protected int finalEdgeCut;
	protected float finalImbalance;
	protected long overAllTime;
	protected boolean verified;

	abstract public String getDetailsFileDump();

	/* Getters & Setters */
	public int getRunID() {
		return runID;
	}

	public void setRunID(int runID) {
		this.runID = runID;
	}

	public PartitionGroup getPartsGroup() {
		return partsGroup;
	}

	public void setPartsGroup(PartitionGroup partsGroup) {
		this.partsGroup = partsGroup;
	}

	public int getFinalEdgeCut() {
		return finalEdgeCut;
	}

	public void setFinalEdgeCut(int finalEdgeCut) {
		this.finalEdgeCut = finalEdgeCut;
	}

	public float getFinalImbalance() {
		return finalImbalance;
	}

	public void setFinalImbalance(float finalImbalance) {
		this.finalImbalance = finalImbalance;
	}

	public long getOverAllTime() {
		return overAllTime;
	}

	public void setOverAllTime(long overAllTime) {
		this.overAllTime = overAllTime;
	}

	public boolean getVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}
}

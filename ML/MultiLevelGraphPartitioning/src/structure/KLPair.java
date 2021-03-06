package structure;

public class KLPair implements Comparable<KLPair> {
	private int sourceID, destinationID, edgeCutGain, balanceGain;

	/*
	 * Constructors
	 */
	public KLPair(int sourceID, int destinationID, int edgeCutGain,
			int balanceGain) {
		this.sourceID = sourceID;
		this.destinationID = destinationID;
		this.edgeCutGain = edgeCutGain;
		this.balanceGain = balanceGain;
	}

	/*
	 * Comparable Methods
	 */

	@Override
	public int compareTo(KLPair arg0) {
		int result = this.edgeCutGain - arg0.edgeCutGain;
		if (result == 0) {
			result = this.balanceGain - arg0.balanceGain;
		}
		return result;
	}

	/*
	 * Getters & Setters
	 */
	public int getSourceID() {
		return sourceID;
	}

	public int getDestinationID() {
		return destinationID;
	}

	public int getEdgeCutGain() {
		return edgeCutGain;
	}

	public void setEdgeCutGain(int edgeCutGain) {
		this.edgeCutGain = edgeCutGain;
	}

	public int getBalanceGain() {
		return balanceGain;
	}

	public void setBalanceGain(int balanceGain) {
		this.balanceGain = balanceGain;
	}
}

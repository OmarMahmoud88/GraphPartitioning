package structure;

public class FMTransfer {

	public int destPartitionID, nodeID, edgeCutGain;
	public float balanceGain;
	
	public FMTransfer(int destPartitionID, int nodeID) {
		this.destPartitionID = destPartitionID;
		this.nodeID = nodeID;
	}
}

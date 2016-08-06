package structure;

public class Edge implements Comparable<Edge>{
	private int sourceID;
	private int destinationID;
	private int weight;
	
	public Edge(int sourceID, int destinationID, int weight){
		this.sourceID = sourceID;
		this.destinationID = destinationID;
		this.weight = weight;
	}

	@Override
	public int compareTo(Edge o) {
		// TODO Auto-generated method stub
		return this.weight - o.weight;
	}
	/*
	 * Setters & getters
	 */
	public int getSourceID() {
		return sourceID;
	}

	public void setSourceID(int sourceID) {
		this.sourceID = sourceID;
	}

	public int getDestinationID() {
		return destinationID;
	}

	public void setDestinationID(int destinationID) {
		this.destinationID = destinationID;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

}

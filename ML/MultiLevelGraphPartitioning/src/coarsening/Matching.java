package coarsening;

import java.util.ArrayList;

import structure.Graph;
import structure.RandomSet;

public abstract class Matching {
	// if outputGraphNumOfNodes == -1 just ignore it
	public abstract ArrayList<RandomSet<Integer>> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight);
	
	public String getSchemeName(){
		
		String name = this.getClass().getName();
		return name;
	}
}

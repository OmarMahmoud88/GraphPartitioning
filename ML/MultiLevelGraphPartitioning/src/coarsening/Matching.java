package coarsening;

import java.util.ArrayList;

import structure.Graph;

public abstract class Matching {
	// if outputGraphNumOfNodes == -1 just ignore it
	public abstract ArrayList<ArrayList<Integer>> coarse(Graph graph, int outputGraphNumOfNodes);
	
	public String getSchemeName(){
		
		String name = this.getClass().getName();
		return name;
	}
}

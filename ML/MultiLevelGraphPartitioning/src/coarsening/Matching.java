package coarsening;

import java.util.ArrayList;

import structure.Graph;

public abstract class Matching {
	public abstract ArrayList<ArrayList<Integer>> coarse(Graph graph);
	
	public String getSchemeName(){
		String name = this.getClass().getName();
		return name;
	}
}

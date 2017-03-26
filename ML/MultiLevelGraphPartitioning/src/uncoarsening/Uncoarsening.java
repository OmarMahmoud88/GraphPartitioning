package uncoarsening;

import structure.CoarseGraph;
import structure.Graph;
import structure.PartitionGroup;

public abstract class Uncoarsening {
	public abstract PartitionGroup Uncoarsen(Graph originalGraph, CoarseGraph cGraph) ;

	public String getSchemeName() {

		String name = this.getClass().getName();
		return name;
	}
}

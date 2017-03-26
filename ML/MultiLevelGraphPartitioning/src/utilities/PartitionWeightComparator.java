package utilities;

import java.util.Comparator;

import structure.Partition;

public class PartitionWeightComparator implements Comparator<Partition> {

	@Override
	public int compare(Partition part1, Partition part2) {
		if (part1.getPartitionWeight() > part2.getPartitionWeight()) {
			return 1;
		} else if (part1.getPartitionWeight() < part2.getPartitionWeight()) {
			return -1;
		}
		return 0;
	}

}

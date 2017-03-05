package coarsening;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import structure.Edge;
import structure.Graph;
import structure.RandomAccessIntHashSet;

public class OrderedHeaviestEdgeMatching  extends Matching{
	public ArrayList<RandomAccessIntHashSet> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		// list all unvisited nodes
		int numberOfNodes = graph.getNumberOfNodes();
		IntOpenHashSet unvisitedNodes = new IntOpenHashSet(numberOfNodes);
		for (int i = 1; i < numberOfNodes+1; i++) {
			unvisitedNodes.add(i);
		}
		
		IntArrayList degreesAvailable = new IntArrayList();
		ArrayList<RandomAccessIntHashSet> nodesTree = new ArrayList<RandomAccessIntHashSet>();
		

		// Store Edges sorted in buckets of degrees
		Int2ObjectOpenHashMap<ArrayList<Edge>> edgesBuckets = new Int2ObjectOpenHashMap<ArrayList<Edge>>(degreesAvailable.size());

		Edge[] allEdges = graph.getEdges();
		for (int i = 0; i < allEdges.length; i++) {
			int sourceID = allEdges[i].getSourceID();
			int destinationID = allEdges[i].getDestinationID();
			
			int sourceDegree = graph.getNode(sourceID).getNumberOfNeighbors();
			if (!edgesBuckets.containsKey(sourceDegree)) {
				edgesBuckets.put(sourceDegree, new ArrayList<Edge>());
				degreesAvailable.add(sourceDegree);
			}
			edgesBuckets.get(sourceDegree).add(allEdges[i]);
			
			int destDegree = graph.getNode(destinationID).getNumberOfNeighbors();
			if (!edgesBuckets.containsKey(destDegree)) {
				edgesBuckets.put(destDegree, new ArrayList<Edge>());
				degreesAvailable.add(sourceDegree);
			}
			edgesBuckets.get(destDegree).add(allEdges[i]);
		}
		
		// sort Degrees
		Collections.sort(degreesAvailable);

		// visit degrees in ascending order
		for (int i = 0; i < degreesAvailable.size(); i++) {
			int degree = degreesAvailable.getInt(i);
			ArrayList<Edge> edgesEndsWithDegree = edgesBuckets.get(degree);
			for (int j = edgesEndsWithDegree.size()-1; j > -1 ; j--) {
				Edge edge = edgesEndsWithDegree.get(j);
				int sourceNodeID = edge.getSourceID();
				int destNodeID = edge.getDestinationID();
				int newNodeWeight = graph.getNode(sourceNodeID).getNodeWeight() + graph.getNode(destNodeID).getNodeWeight();
				if (unvisitedNodes.contains(sourceNodeID)&& unvisitedNodes.contains(destNodeID) && newNodeWeight <= maxPartitionWeight) {
					RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
					parentNode.add(sourceNodeID);
					parentNode.add(destNodeID);
					nodesTree.add(parentNode);
					unvisitedNodes.remove(sourceNodeID);
					unvisitedNodes.remove(destNodeID);
				}
			}
			
		}
		
		// iterate through remaining nodes, and collapse them by themselves
		Iterator<Integer> it =  unvisitedNodes.iterator();
		while (it.hasNext()) {
			int singleNodeID = it.next();
			RandomAccessIntHashSet parentNode = new RandomAccessIntHashSet();
			parentNode.add(singleNodeID);
			nodesTree.add(parentNode);
		}
		
		return nodesTree;
	}
}

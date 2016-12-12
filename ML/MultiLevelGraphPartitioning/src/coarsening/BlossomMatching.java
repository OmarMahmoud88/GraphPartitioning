package coarsening;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import structure.Graph;
import structure.Node;
import structure.NodesMatching;
import structure.RandomSet;
import structure.Tuple;
import structure.UnionFind;

public class BlossomMatching extends Matching {

	/* The graph we are matching on. */
	private Graph graph;

	/* Algorithm data structures below. */
	NodesMatching matching;
	/** Storage of the forest, even and odd levels */
	private int[] even, odd;

	/** Special 'nil' vertex. */
	private static final int nil = -1;

	/** Queue of 'even' (free) vertices to start paths from. */
	private Queue<Integer> queue;

	/** Union-Find to store blossoms. */
	private UnionFind uf;

	/**
	 * Map stores the bridges of the blossom - indexed by with support vertices.
	 */
	private final Map<Integer, Tuple<Integer, Integer>> bridges = new HashMap<Integer, Tuple<Integer, Integer>>();

	/** Temporary array to fill with path information. */
	private int[] path;

	/**
	 * Temporary bit sets when walking down 'trees' to check for paths/blossoms.
	 */
	private BitSet vAncestors, wAncestors;

	/**
	 * Find an augmenting path an alternate it's matching. If an augmenting path
	 * was found then the search must be restarted. If a blossom was detected
	 * the blossom is contracted and the search continues.
	 *
	 * @return an augmenting path was found
	 */
	private boolean augment() {

		// reset data structures
		Arrays.fill(even, nil);
		Arrays.fill(odd, nil);
		uf.clear();
		bridges.clear();
		queue.clear();

		// queue every unmatched vertex and place in the
		// even level (level = 0)
		for (int v = 0; v < graph.getOrder(); v++) {
			if (matching.unmatched(v)) {
				even[v] = v;
				queue.add(v);
			}
		}

		// for each 'free' vertex, start a bfs search
		while (!queue.isEmpty()) {
			int curNodeID = (int) queue.poll();
			Node curNode = this.graph.getNode(curNodeID+1);

			Node[] curNodeNeighbors = curNode.getNeighbors();
			for (int j = 0; j < curNodeNeighbors.length; ++j) {
				Node neighbor = curNodeNeighbors[j];
				int neighborID = neighbor.getNodeID()-1;

				// the endpoints of the edge are both at even levels in the
				// forest - this means it is either an augmenting path or
				// a blossom
				if (even[uf.find(neighborID)] != nil) {
					if (check(curNodeID, neighborID))
						return true;
				}

				// add the edge to the forest if is not already and extend
				// the tree with this matched edge
				else if (odd[neighborID] == nil) {
					odd[neighborID] = curNodeID;
					int neighborNeighborID = matching.other(neighborID);
					// add the matched edge (potential though a blossom) if it
					// isn't in the forest already
					if (even[uf.find(neighborNeighborID)] == nil) {
						even[neighborNeighborID] = neighborID;
						queue.add(neighborNeighborID);
					}
				}
			}
		}

		// no augmenting paths, matching is maximum
		return false;
	}

	/**
	 * An edge was found which connects two 'even' vertices in the forest. If
	 * the vertices have the same root we have a blossom otherwise we have
	 * identified an augmenting path. This method checks for these cases and
	 * responds accordingly.
	 * <p/>
	 *
	 * If an augmenting path was found - then it's edges are alternated and the
	 * method returns true. Otherwise if a blossom was found - it is contracted
	 * and the search continues.
	 *
	 * @param v
	 *            endpoint of an edge
	 * @param w
	 *            another endpoint of an edge
	 * @return a path was augmented
	 */
	private boolean check(int v, int w) {

		// self-loop (within blossom) ignored
		if (uf.connected(v, w))
			return false;

		vAncestors.clear();
		wAncestors.clear();
		int vCurr = v;
		int wCurr = w;

		// walk back along the trees filling up 'vAncestors' and 'wAncestors'
		// with the vertices in the tree - vCurr and wCurr are the 'even'
		// parents
		// from v/w along the tree
		while (true) {

			vCurr = parent(vAncestors, vCurr);
			wCurr = parent(wAncestors, wCurr);

			// v and w lead to the same root - we have found a blossom. We
			// travelled all the way down the tree thus vCurr (and wCurr) are
			// the base of the blossom
			if (vCurr == wCurr) {
				blossom(v, w, vCurr);
				return false;
			}

			// we are at the root of each tree and the roots are different, we
			// have found and augmenting path
			if (uf.find(even[vCurr]) == vCurr && uf.find(even[wCurr]) == wCurr) {
				augment(v);
				augment(w);
				matching.match(v, w);
				return true;
			}

			// the current vertex in 'v' can be found in w's ancestors they must
			// share a root - we have found a blossom whose base is 'vCurr'
			if (wAncestors.get(vCurr)) {
				blossom(v, w, vCurr);
				return false;
			}

			// the current vertex in 'w' can be found in v's ancestors they must
			// share a root, we have found a blossom whose base is 'wCurr'
			if (vAncestors.get(wCurr)) {
				blossom(v, w, wCurr);
				return false;
			}
		}
	}

	/**
	 * Access the next ancestor in a tree of the forest. Note we go back two
	 * places at once as we only need check 'even' vertices.
	 *
	 * @param ancestors
	 *            temporary set which fills up the path we traversed
	 * @param curr
	 *            the current even vertex in the tree
	 * @return the next 'even' vertex
	 */
	private int parent(BitSet ancestors, int curr) {
		curr = uf.find(curr);
		ancestors.set(curr);
		int parent = uf.find(even[curr]);
		if (parent == curr)
			return curr; // root of tree
		ancestors.set(parent);
		return uf.find(odd[parent]);
	}

	/**
	 * Create a new blossom for the specified 'bridge' edge.
	 *
	 * @param v
	 *            adjacent to w
	 * @param w
	 *            adjacent to v
	 * @param base
	 *            connected to the stem (common ancestor of v and w)
	 */
	private void blossom(int v, int w, int base) {
		base = uf.find(base);
		int[] supports1 = blossomSupports(v, w, base);
		int[] supports2 = blossomSupports(w, v, base);

		for (int i = 0; i < supports1.length; i++)
			uf.union(supports1[i], supports1[0]);
		for (int i = 0; i < supports2.length; i++)
			uf.union(supports2[i], supports2[0]);

		even[uf.find(base)] = even[base];
	}

	/**
	 * Creates the blossom 'supports' for the specified blossom 'bridge' edge
	 * (v, w). We travel down each side to the base of the blossom ('base')
	 * collapsing vertices and point any 'odd' vertices to the correct 'bridge'
	 * edge. We do this by indexing the birdie to each vertex in the 'bridges'
	 * map.
	 *
	 * @param v
	 *            an endpoint of the blossom bridge
	 * @param w
	 *            another endpoint of the blossom bridge
	 * @param base
	 *            the base of the blossom
	 */
	private int[] blossomSupports(int v, int w, int base) {

		int n = 0;
		path[n++] = uf.find(v);
		Tuple<Integer, Integer> b = new Tuple<Integer, Integer>(v, w);
		while (path[n - 1] != base) {
			int u = even[path[n - 1]];
			path[n++] = u;
			this.bridges.put(u, b);
			// contracting the blossom allows us to continue searching from odd
			// vertices (any odd vertices are now even - part of the blossom
			// set)
			queue.add(u);
			path[n++] = uf.find(odd[u]);
		}

		return Arrays.copyOf(path, n);
	}

	/**
	 * Augment all ancestors in the tree of vertex 'v'.
	 *
	 * @param v
	 *            the leaf to augment from
	 */
	private void augment(int v) {
		int n = buildPath(path, 0, v, nil);
		for (int i = 2; i < n; i += 2) {
			matching.match(path[i], path[i - 1]);
		}
	}

	/**
	 * Builds the path backwards from the specified 'start' vertex until the
	 * 'goal'. If the path reaches a blossom then the path through the blossom
	 * is lifted to the original graph.
	 *
	 * @param path
	 *            path storage
	 * @param i
	 *            offset (in path)
	 * @param start
	 *            start vertex
	 * @param goal
	 *            end vertex
	 * @return the number of items set to the path[].
	 */
	private int buildPath(int[] path, int i, int start, int goal) {
		while (true) {

			// lift the path through the contracted blossom
			while (odd[start] != nil) {

				Tuple<Integer, Integer> bridge = bridges.get(start);

				// add to the path from the bridge down to where 'start'
				// is - we need to reverse it as we travel 'up' the blossom
				// and then...
				int j = buildPath(path, i, bridge.first(), start);
				reverse(path, i, j - 1);
				i = j;

				// ... we travel down the other side of the bridge
				start = bridge.second();
			}
			path[i++] = start;

			// root of the tree
			if (matching.unmatched(start))
				return i;

			path[i++] = matching.other(start);

			// end of recursive
			if (path[i - 1] == goal)
				return i;

			start = odd[path[i - 1]];
		}
	}

	/** Utility to reverse a section of a fixed size array */
	static void reverse(int[] path, int i, int j) {
		while (i < j) {
			int tmp = path[i];
			path[i] = path[j];
			path[j] = tmp;
			i++;
			j--;
		}
	}

	@Override
	public ArrayList<RandomSet<Integer>> coarse(Graph graph, int outputGraphNumOfNodes, float maxPartitionWeight) {
		this.graph = graph;
		this.matching = new NodesMatching(graph.getOrder());
		this.even = new int[graph.getOrder()];
		this.odd = new int[graph.getOrder()];

		this.queue = new LinkedList<Integer>();
		this.uf = new UnionFind(graph.getOrder());

		// tmp storage of paths in the algorithm
		path = new int[graph.getOrder()];
		vAncestors = new BitSet(graph.getOrder());
		wAncestors = new BitSet(graph.getOrder());

		// continuously augment while we find new paths, each
		// path increases the matching cardinality by 2
		while (augment()) {
		}

		int numberOfNodes = this.graph.getNumberOfNodes();
		HashSet<Integer> unvisitedNodes = new HashSet<Integer>(numberOfNodes);
		ArrayList<RandomSet<Integer>> nodesTree = new ArrayList<RandomSet<Integer>>();
		for (int i = 0; i < numberOfNodes; i++) {
			unvisitedNodes.add(i + 1);
		}

		Iterable<Tuple<Integer, Integer>> matches = this.matching.matches();
		Iterator<Tuple<Integer, Integer>> it = matches.iterator();
		while (it.hasNext()) {
			Tuple<Integer, Integer> match = it.next();
			RandomSet<Integer> nodeChilds = new RandomSet<Integer>();
			nodeChilds.add(match.first()+1);
			unvisitedNodes.remove(match.first()+1);
			nodeChilds.add(match.second()+1);
			unvisitedNodes.remove(match.second()+1);
			nodesTree.add(nodeChilds);
		}
		// iterate remaining nodes
		Iterator<Integer> rIt = unvisitedNodes.iterator();
		while (rIt.hasNext()) {
			RandomSet<Integer> nodeChilds = new RandomSet<Integer>();
			nodeChilds.add(rIt.next());
			nodesTree.add(nodeChilds);
		}

		return nodesTree;
	}

}

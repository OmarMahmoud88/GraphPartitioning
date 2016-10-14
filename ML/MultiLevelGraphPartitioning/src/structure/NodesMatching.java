package structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodesMatching {

	/** Indicates an unmatched vertex. */
	private static final int UNMATCHED = -1;

	/** Storage of which each vertex is matched with. */
	private int[] match;

	/**
     * Create a matching of the given size.
     *
     * @param n number of items
     */
    public NodesMatching(int n) {
        this.match = new int[n];
        Arrays.fill(match, UNMATCHED);
    }

	boolean matched(int v) {
		return !unmatched(v);
	}

	/**
	 * Is the vertex v 'unmatched'.
	 *
	 * @param v
	 *            a vertex
	 * @return the vertex has no matching
	 */
	public boolean unmatched(int v) {
		int w = match[v];
		return w < 0 || match[w] != v;
	}

	/**
	 * Access the vertex matched with 'v'.
	 *
	 * @param v
	 *            a vertex
	 * @return matched vertex
	 * @throws IllegalArgumentException
	 *             the vertex is currently unmatched
	 */
	public int other(int v) {
		if (unmatched(v))
			throw new IllegalArgumentException(v + " is not matched");
		return match[v];
	}

	/**
	 * Add the edge '{u,v}' to the matched edge set. Any existing matches for
	 * 'u' or 'v' are removed from the matched set.
	 *
	 * @param u
	 *            a vertex
	 * @param v
	 *            another vertex
	 */
	public void match(int u, int v) {
		// set the new match, don't need to update existing - we only provide
		// access to bidirectional mappings
		match[u] = v;
		match[v] = u;
	}

	/**
	 * Access the current non-redundant set of edges.
	 *
	 * @return matched pairs
	 */
	public Iterable<Tuple<Integer, Integer>> matches() {

		List<Tuple<Integer, Integer>> tuples = new ArrayList<Tuple<Integer, Integer>>(match.length / 2);

		for (int v = 0; v < match.length; v++) {
			int w = match[v];
			if (w > v && match[w] == v) {
				tuples.add(new Tuple<Integer, Integer>(v,w));
			}
		}

		return tuples;
	}

	/**
	 * Allocate a matching with enough capacity for the given graph.
	 *
	 * @param g
	 *            a graph
	 * @return matching
	 */
	static NodesMatching empty(Graph g) {
		return new NodesMatching(g.getOrder());
	}
}

package com.dlouvton.badger.provisioner.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.dlouvton.badger.util.CustomLoggerFactory;

public class Graph {
	private static Logger LOG = CustomLoggerFactory.getLogger(Graph.class);

	private Graph() {
	}

	static class Edge {
		public final Component from;
		public final Component to;

		public Edge(Component from, Component to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public boolean equals(Object obj) {
			Edge e = (Edge) obj;
			return e.from == from && e.to == to;
		}
	}

	public static List<Component> topsort(Collection<Component> allNodes) {
		ArrayList<Component> L = new ArrayList<Component>();

		// S <- Set of all nodes with no incoming edges
		HashSet<Component> S = new HashSet<Component>();
		for (Component n : allNodes) {
			if (n.inEdges.size() == 0) {
				S.add(n);
			}
		}

		while (!S.isEmpty()) {
			// remove a node n from S
			Component n = S.iterator().next();
			S.remove(n);
			L.add(n);

			for (Iterator<Edge> it = n.outEdges.iterator(); it.hasNext();) {
				Edge e = it.next();
				Component m = e.to;
				it.remove();// Remove edge from n
				m.inEdges.remove(e);// Remove edge from m
				if (m.inEdges.isEmpty()) {
					S.add(m);
				}
			}
		}
		// Check to see if all edges are removed
		boolean cycle = false;
		for (Component n : allNodes) {
			if (!n.inEdges.isEmpty()) {
				cycle = true;
				break;
			}
		}

		if (cycle) {
			throw new ModelException(
					"Cycle present on your environment model, topological sort not possible");
		}

		LOG.fine("Topological Sort: " + Arrays.toString(L.toArray()));
		return L;
	}
}
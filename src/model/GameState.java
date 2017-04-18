package model;

import graph.Node;

import java.util.HashSet;
import java.util.Set;

public final class GameState {
	private String id;
	private final Set<Node> enabledNodeSet;
	
	public GameState() {
		this.enabledNodeSet = new HashSet<Node>();
	}
	
	public boolean addEnabledNode(final Node node) {
		return this.enabledNodeSet.add(node);
	}
	
	public Set<Node> getEnabledNodeSet() {
		return this.enabledNodeSet;
	}
	
	public boolean contain(final Node node) {
		return this.enabledNodeSet.contains(node);
	}
	
	public void clear() {
		this.enabledNodeSet.clear();
	}
	
	public void print() {
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Active Nodes");
		for (Node node : this.enabledNodeSet) {
			System.out.println(node.getId() + "\t" + node.getType());
		}
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
	}
	
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result;
//		return result;
//	}
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		GameState other = (GameState) obj;
//		for(Node node : other.getEnabledNodeSet())
//			if(!this.enabledNodeSet.contains(node))
//				return false;
//		return true;
//	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		GameState other = (GameState) obj;
		if (!this.id.equals(other.id)) {
			return false;
		}
		return true;
	}
	
	public void createID() {
		int maxNodeID = 1;
		for (Node node : this.enabledNodeSet) {
			if (maxNodeID < node.getId()) {
				maxNodeID = node.getId();
			}
		}
		char[] idChar = new char[maxNodeID];
		for (int i = 0; i < maxNodeID; i++) {
			idChar[i] = '0';
		}
		
		if (!this.enabledNodeSet.isEmpty()) {
			for (Node node : this.enabledNodeSet) {
				idChar[node.getId() - 1] = '1';
			}
		}
		this.id = new String(idChar);	
	}
	
	public String getID() {
		return this.id;
	}
}

package model;

import graph.Node;

public final class SecurityAlert {
	private final Node node;
	private final boolean isActive;
	
	public SecurityAlert(final Node aNode, final boolean aIsActive) {
		if (aNode == null) {
			throw new IllegalArgumentException();
		}
		this.node = aNode;
		this.isActive = aIsActive;
	}
	
	public boolean isActiveAlert() {
		return this.isActive;
	}
	
	public Node getNode() {
		return this.node;
	}
}

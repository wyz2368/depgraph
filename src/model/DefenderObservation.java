package model;

import java.util.HashSet;
import java.util.Set;

public final class DefenderObservation {
	private final Set<SecurityAlert> alertSet;
	
	public DefenderObservation() {
		this.alertSet = new HashSet<SecurityAlert>();
	}
	
	public boolean addAlert(final SecurityAlert alert) {
		return this.alertSet.add(alert);
	}
	
	public Set<SecurityAlert> getAlertSet() {
		return this.alertSet;
	}
	
	public boolean contain(final SecurityAlert alert) {
		return this.alertSet.contains(alert);
	}
	
	public void clear() {
		this.alertSet.clear();
	}
	
	public void print() {
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Defender observation: ");
		for (SecurityAlert alert : this.alertSet) {
			System.out.print("Node: " + alert.getNode().getId() + "\t" + "Alert: " + alert.getAlert());
		}
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
	}
}

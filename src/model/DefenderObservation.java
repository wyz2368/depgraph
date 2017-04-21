package model;

import java.util.HashSet;
import java.util.Set;

public final class DefenderObservation {
	private final Set<SecurityAlert> alertSet;
	
	public DefenderObservation() {
		this.alertSet = new HashSet<SecurityAlert>();
	}
	
	public boolean addAlert(final SecurityAlert alert) {
		if (alert == null) {
			throw new IllegalArgumentException();
		}
		assert !isDuplicateAlert(alert);
		return this.alertSet.add(alert);
	}
	
	public Set<SecurityAlert> getAlertSet() {
		return this.alertSet;
	}
	
	public void clear() {
		this.alertSet.clear();
	}
	
	public void print() {
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Defender observation: ");
		for (SecurityAlert alert : this.alertSet) {
			System.out.print("Node: " + alert.getNode().getId() + "\t" + "Alert: " + alert.isActiveAlert());
		}
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
	}
	
	private boolean isDuplicateAlert(final SecurityAlert alert) {
		if (alert == null) {
			throw new IllegalArgumentException();
		}
		for (final SecurityAlert oldAlert: this.alertSet) {
			if (oldAlert.getNode().equals(alert.getNode())) {
				return true;
			}
		}
		return false;
	}
}

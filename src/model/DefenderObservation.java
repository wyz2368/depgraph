package model;

import java.util.HashSet;
import java.util.Set;

import game.GameSimulation;

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
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
		GameSimulation.printIfDebug("Defender observation: ");
		for (SecurityAlert alert : this.alertSet) {
			GameSimulation.printIfDebug("Node: " + alert.getNode().getId() + "\t" + "Alert: " + alert.isActiveAlert());
		}
		GameSimulation.printIfDebug("");
		GameSimulation.printIfDebug("--------------------------------------------------------------------");
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

package model;

import java.util.HashSet;
import java.util.Set;

public class DefenderObservation {
	Set<SecurityAlert> alertSet;
	public DefenderObservation()
	{
		this.alertSet = new HashSet<SecurityAlert>();
	}
	public boolean addAlert(SecurityAlert alert)
	{
		return alertSet.add(alert);
	}
	public Set<SecurityAlert> getAlertSet()
	{
		return this.alertSet;
	}
	public boolean contain(SecurityAlert alert)
	{
		return this.alertSet.contains(alert);
	}
	public void clear()
	{
		this.alertSet.clear();
	}
	public void print() {
		// TODO Auto-generated method stub
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Defender observation: ");
		for(SecurityAlert alert : alertSet)
		{
			System.out.print("Node: " + alert.getNode().getId() + "\t" + "Alert: " + alert.getAlert());
		}
		System.out.println();
		System.out.println("--------------------------------------------------------------------");
	}
}

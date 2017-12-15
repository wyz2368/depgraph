package rl;

import java.util.Map;
import java.util.Set;

import graph.Edge;
import graph.Node;

import java.util.HashMap;
import java.util.HashSet;

import model.DefenderObservation;
import model.DependencyGraph;
import model.SecurityAlert;

public final class RLDefenderCraftedObservation {

	/*
	 * lists for each node in vertexSet() of DependencyGraph, in order:
	 *     probActive
	 *     -- based on Bayesian update from observation, with uniform prior
	 *     probCanActivate
	 *     -- use probActive over all parent nodes
	 *     -- for probability exploit is available, 
	 *     		assuming independence over parent active probabilities
	 *     successProbability
	 *     attackCost
	 *     defenseCost
	 *     attackerReward
	 *     defenderReward
	 *     -- will be negative
	 *     distToGoal
	 *     -- length of shortest path to a goal node descendant, 
	 *     		0 if goal node, max value if none
	 *     attackerRewardGoalSubtree
	 *     -- sum of attacker rewards in goal nodes in subtree 
	 *     		with this node as root
	 *     defenderRewardGoalSubtree
	 *     -- sum of defender rewards in goal nodes in subtree 
	 *     		with this node as root; will be non-positive
	 */
	
	private final Map<Integer, Double> probActive;
	
	private final Map<Integer, Double> probCanActivate;
	
	private final Map<Integer, Double> successProbability;
	
	private final Map<Integer, Double> attackCost;
	
	private final Map<Integer, Double> attackValue;
	
	private final Map<Integer, Double> defenseCost;
	
	private final Map<Integer, Double> defenseValue;
	
	private final Map<Integer, Integer> distanceToGoal;
	
	private final Map<Integer, Double> subtreeAttackReward;
	
	private final Map<Integer, Double> subtreeDefensePenalty;

	
	public RLDefenderCraftedObservation(final DefenderObservation defObs, 
		final DependencyGraph depGraph) {
		assert defObs != null;
		assert depGraph != null;
		this.probActive = getProbActive(defObs, depGraph);
		this.probCanActivate = getProbCanActivate(depGraph);
		this.successProbability = getSuccessProb(depGraph);
		this.attackCost = getAttackCost(depGraph);
		this.attackValue = getAttackValue(depGraph);
		this.defenseValue = getDefenseValue(depGraph);
		this.defenseCost = getDefenseCost(depGraph);
		this.distanceToGoal = getDistanceToGoal(depGraph);
		this.subtreeAttackReward = getSubtreeAttReward(depGraph);
		this.subtreeDefensePenalty = getSubtreeDefPenalty(depGraph);
	}
	
	@Override
	public String toString() {
		return "RLDefenderObservation [probActive=" + this.probActive 
			+ "\n, probCanActivate=" + this.probCanActivate
			+ "\n, successProbability=" + this.successProbability 
			+ "\n, attackCost=" + this.attackCost + "\n, attackValue="
			+ this.attackValue + "\n, defenseCost=" + this.defenseCost 
			+ "\n, defenseValue=" + this.defenseValue + "\n, distanceToGoal="
			+ this.distanceToGoal + "\n, subtreeAttackReward=" 
			+ this.subtreeAttackReward + "\n, subtreeDefensePenalty="
			+ this.subtreeDefensePenalty + "]";
	}

	private static Map<Integer, Double> getSubtreeDefPenalty(
		final DependencyGraph depGraph) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			final double subtreeDefPenalty = 
				depGraph.subtreeDefensePenalty(node);
			result.put(id, subtreeDefPenalty);
		}
		return result;
	}
	
	private static Map<Integer, Double> getSubtreeAttReward(
		final DependencyGraph depGraph) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			final double subtreeAttReward = depGraph.subtreeAttackReward(node);
			result.put(id, subtreeAttReward);
		}
		return result;
	}
	
	private static Map<Integer, Integer> getDistanceToGoal(
		final DependencyGraph depGraph) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Integer> result = new HashMap<Integer, Integer>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			final int distToGoal = depGraph.distanceToGoal(node);
			result.put(id, distToGoal);
		}
		return result;
	}
	
	private static Map<Integer, Double> getAttackValue(
		final DependencyGraph depGraph) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			final double attackValue = node.getAReward();
			result.put(id, attackValue);
		}
		return result;
	}
	
	private static Map<Integer, Double> getDefenseCost(
		final DependencyGraph depGraph) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			final double defenseCost = node.getDCost();
			result.put(id, defenseCost);
		}
		return result;
	}
	
	private static Map<Integer, Double> getDefenseValue(
		final DependencyGraph depGraph) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			final double defenseValue = node.getDPenalty();
			result.put(id, defenseValue);
		}
		return result;
	}
	
	private static Map<Integer, Double> getAttackCost(
		final DependencyGraph depGraph) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			double attackCost = 0.0;
			if (node.isOrType()) {
				// OR node -> use mean of in-edges
				for (final Edge edge: depGraph.incomingEdgesOf(node)) {
					attackCost += edge.getACost();
				}
				attackCost /= (1.0 * depGraph.inDegreeOf(node));
			} else {
				// AND node -> use own attack cost
				attackCost = node.getACost();
			}
			result.put(id, attackCost);
		}
		return result;
	}
	
	private static Map<Integer, Double> getSuccessProb(
		final DependencyGraph depGraph) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			double successProb = 1.0;
			if (node.isOrType()) {
				// OR node -> use probability that any exploit will work
				for (final Edge edge: depGraph.incomingEdgesOf(node)) {
					successProb *= (1 - edge.getActProb());
				}
				successProb = 1.0 - successProb;
			} else {
				// AND node -> use actProb
				successProb = node.getActProb();
			}
			result.put(id, successProb);
		}
		return result;
	}
	
	private static Set<Integer> getParentIds(
		final Node node, final DependencyGraph depGraph) {
		assert depGraph.vertexSet().contains(node);
		final Set<Integer> result = new HashSet<Integer>();
		for (Edge edge: depGraph.incomingEdgesOf(node)) {
			result.add(edge.getsource().getId());
		}
		return result;
	}
	
	private Map<Integer, Double> getProbCanActivate(
		final DependencyGraph depGraph
	) {
		assert !depGraph.vertexSet().isEmpty();
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			final Set<Integer> parentIds = getParentIds(node, depGraph);
			double prob = 1.0;
			if (node.isOrType()) {
				// Pr: not all parents are inactive
				for (int parentId: parentIds) {
					prob *= (1 - this.probActive.get(parentId));
				}
				prob = 1.0 - prob;
			} else {
				// Pr: every parent is active
				for (int parentId: parentIds) {
					prob *= this.probActive.get(parentId);
				}
			}
			result.put(id, prob);
		}
		return result;
	}
	
	private static Map<Integer, Double> getProbActive(
		final DefenderObservation defObs,
		final DependencyGraph depGraph
	) {
		final Map<Integer, Double> result = new HashMap<Integer, Double>();
		
		final Set<Integer> activeAlerts = new HashSet<Integer>();
		for (SecurityAlert alert: defObs.getAlertSet()) {
			if (alert.isActiveAlert()) {
				activeAlerts.add(alert.getNode().getId());
			}
		}
		
		for (Node node: depGraph.vertexSet()) {
			final int id = node.getId();
			// Pr: active alert, given actually active
			final double posActiveProb = node.getPosActiveProb();
			// Pr: active alert, given actually inactive
			final double posInactiveProb = node.getPosActiveProb();
			double activeProb = 0.0;
			if (activeAlerts.contains(id)) {
				// has active alert
				activeProb = posActiveProb / (posActiveProb + posInactiveProb);
			} else {
				// has inactive alert
				activeProb = 
					(1 - posActiveProb) 
						/ ((1 - posActiveProb) + (1 - posInactiveProb));
			}
			result.put(id, activeProb);
		}
		return result;
	}
}

package agent;

import java.util.Map;

import agent.Attacker.AttackerParam;
import agent.Attacker.AttackerType;
import agent.Defender.DefenderParam;
import agent.Defender.DefenderType;

public final class AgentFactory {
	
	private AgentFactory() {
		// private constructor
	}
	
	public static Attacker createAttacker(final String attackerName, final Map<String, Double> attackerParams, final double discFact) {
		assert attackerName != null && AttackerType.valueOf(attackerName) != null;
		assert attackerParams != null;
		AttackerType attType = AttackerType.valueOf(attackerName);
		
		if (attType == AttackerType.UNIFORM) {
			assert attackerParams.containsKey(AttackerParam.maxNumSelectCandidate.toString())
				&& attackerParams.containsKey(AttackerParam.minNumSelectCandidate.toString())
				&& attackerParams.containsKey(AttackerParam.numSelectCandidateRatio.toString());
			return new UniformAttacker(attackerParams.get(AttackerParam.maxNumSelectCandidate.toString())
				, attackerParams.get(AttackerParam.minNumSelectCandidate.toString())
				, attackerParams.get(AttackerParam.numSelectCandidateRatio.toString()));
		} else if (attType == AttackerType.VALUE_PROPAGATION) {
			assert attackerParams.containsKey(AttackerParam.maxNumSelectCandidate.toString()) 
			&& attackerParams.containsKey(AttackerParam.minNumSelectCandidate.toString())
			&& attackerParams.containsKey(AttackerParam.numSelectCandidateRatio.toString())
			&& attackerParams.containsKey(AttackerParam.qrParam.toString());
			return new ValuePropagationAttacker(attackerParams.get(AttackerParam.maxNumSelectCandidate.toString())
				, attackerParams.get(AttackerParam.minNumSelectCandidate.toString())
				, attackerParams.get(AttackerParam.numSelectCandidateRatio.toString())
				, attackerParams.get(AttackerParam.qrParam.toString())
				, discFact);
		} else if (attType == AttackerType.RANDOM_WALK) {
			assert attackerParams.containsKey(AttackerParam.numRWSample.toString()) 
			&& attackerParams.containsKey(AttackerParam.qrParam.toString());
			return new RandomWalkAttacker(attackerParams.get(AttackerParam.numRWSample.toString())
				, attackerParams.get(AttackerParam.qrParam.toString()), discFact);
		}
		return null;
	}
	
	public static Defender createDefender(final String defenderName, final Map<String, Double> defenderParams, final double discFact) {
		assert defenderName != null && DefenderType.valueOf(defenderName) != null;
		assert defenderParams != null;
		DefenderType defType = DefenderType.valueOf(defenderName);
		
		if (defType == DefenderType.UNIFORM) {
			assert defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			&& defenderParams.containsKey(DefenderParam.minNumRes.toString())
			&& defenderParams.containsKey(DefenderParam.numResRatio.toString());
			return new UniformDefender(defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString()));
		} else if (defType == DefenderType.MINCUT) {
			assert defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			&& defenderParams.containsKey(DefenderParam.minNumRes.toString())
			&& defenderParams.containsKey(DefenderParam.numResRatio.toString());
			return new MinCutDefender(defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString()));
		} else if (defType == DefenderType.GOAL_ONLY) {
			assert defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			&& defenderParams.containsKey(DefenderParam.minNumRes.toString())
			&& defenderParams.containsKey(DefenderParam.numResRatio.toString())
			&& defenderParams.containsKey(DefenderParam.logisParam.toString());
			return new GoalOnlyDefender(defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString())
				, defenderParams.get(DefenderParam.logisParam.toString()), discFact);
		} else if (defType == DefenderType.vsVALUE_PROPAGATION) {
			assert defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			&& defenderParams.containsKey(DefenderParam.minNumRes.toString())
			&& defenderParams.containsKey(DefenderParam.numResRatio.toString())
			&& defenderParams.containsKey(DefenderParam.logisParam.toString())
			&& defenderParams.containsKey(DefenderParam.bThres.toString())
			&& defenderParams.containsKey(DefenderParam.qrParam.toString())
			&& defenderParams.containsKey(DefenderParam.maxNumAttCandidate.toString())
			&& defenderParams.containsKey(DefenderParam.minNumAttCandidate.toString())
			&& defenderParams.containsKey(DefenderParam.numAttCandidateRatio.toString());
			return new ValuePropagationvsDefender(defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString())
				, defenderParams.get(DefenderParam.logisParam.toString())
				, discFact
				, defenderParams.get(DefenderParam.bThres.toString())
				, defenderParams.get(DefenderParam.qrParam.toString())
				, defenderParams.get(DefenderParam.maxNumAttCandidate.toString())
				, defenderParams.get(DefenderParam.minNumAttCandidate.toString())
				, defenderParams.get(DefenderParam.numAttCandidateRatio.toString()));
		} else if (defType == DefenderType.vsRANDOM_WALK) {
			assert defenderParams.containsKey(DefenderParam.logisParam.toString())
			&& defenderParams.containsKey(DefenderParam.bThres.toString())
			&& defenderParams.containsKey(DefenderParam.qrParam.toString())
			&& defenderParams.containsKey(DefenderParam.numRWSample.toString());
			return new RandomWalkvsDefender(defenderParams.get(DefenderParam.logisParam.toString())
				, discFact
				, defenderParams.get(DefenderParam.bThres.toString())
				, defenderParams.get(DefenderParam.qrParam.toString())
				, defenderParams.get(DefenderParam.numRWSample.toString()));
		}

		return null;
	 }
}

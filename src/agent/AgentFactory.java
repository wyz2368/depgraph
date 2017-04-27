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
	
	public static Attacker createAttacker(final String attackerName,
		final Map<String, Double> attackerParams, final double discFact) {
		assert attackerName != null && AttackerType.valueOf(attackerName) != null;
		assert attackerParams != null;
		if (discFact <= 0.0 || discFact > 1.0) {
			throw new IllegalArgumentException();
		}
		AttackerType attType = AttackerType.valueOf(attackerName);
		if (attType == AttackerType.UNIFORM) {
			if (!attackerParams.containsKey(AttackerParam.MAX_NUM_SELECT_CAND.toString())
				|| !attackerParams.containsKey(AttackerParam.MIN_NUM_SELECT_CAND.toString())
				|| !attackerParams.containsKey(AttackerParam.NUM_SELECT_CAND_RATIO.toString())
				|| !attackerParams.containsKey(AttackerParam.STDEV.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 4;
			if (attackerParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new UniformAttacker(attackerParams.get(AttackerParam.MAX_NUM_SELECT_CAND.toString())
				, attackerParams.get(AttackerParam.MIN_NUM_SELECT_CAND.toString())
				, attackerParams.get(AttackerParam.NUM_SELECT_CAND_RATIO.toString()),
				attackerParams.get(AttackerParam.STDEV.toString()));
		} else if (attType == AttackerType.VALUE_PROPAGATION) {
			if (!attackerParams.containsKey(AttackerParam.MAX_NUM_SELECT_CAND.toString())
			|| !attackerParams.containsKey(AttackerParam.MIN_NUM_SELECT_CAND.toString())
			|| !attackerParams.containsKey(AttackerParam.NUM_SELECT_CAND_RATIO.toString())
			|| !attackerParams.containsKey(AttackerParam.QR_PARAM.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 4;
			if (attackerParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new ValuePropagationAttacker(
				attackerParams.get(AttackerParam.MAX_NUM_SELECT_CAND.toString())
				, attackerParams.get(AttackerParam.MIN_NUM_SELECT_CAND.toString())
				, attackerParams.get(AttackerParam.NUM_SELECT_CAND_RATIO.toString())
				, attackerParams.get(AttackerParam.QR_PARAM.toString())
				, discFact);
		} else if (attType == AttackerType.RANDOM_WALK) {
			if (!attackerParams.containsKey(AttackerParam.NUM_RW_SAMPLE.toString()) 
			|| !attackerParams.containsKey(AttackerParam.QR_PARAM.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 2;
			if (attackerParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new RandomWalkAttacker(attackerParams.get(AttackerParam.NUM_RW_SAMPLE.toString())
				, attackerParams.get(AttackerParam.QR_PARAM.toString()), discFact);
		}
		throw new IllegalArgumentException();
	}
	
	public static Defender createDefender(final String defenderName,
		final Map<String, Double> defenderParams, final double discFact) {
		assert defenderName != null && DefenderType.valueOf(defenderName) != null;
		assert defenderParams != null;
		if (discFact <= 0.0 || discFact > 1.0) {
			throw new IllegalArgumentException();
		}
		DefenderType defType = DefenderType.valueOf(defenderName);
		
		if (defType == DefenderType.UNIFORM) {
			if (!defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.minNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.numResRatio.toString())
			|| !defenderParams.containsKey(AttackerParam.STDEV.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 4;
			if (defenderParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new UniformDefender(defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString())
				, defenderParams.get(DefenderParam.stdev.toString()));
		} else if (defType == DefenderType.MINCUT) {
			if (!defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.minNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.numResRatio.toString())
			|| !defenderParams.containsKey(AttackerParam.STDEV.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 4;
			if (defenderParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new MinCutDefender(
				defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString()),
				defenderParams.get(DefenderParam.stdev.toString()));
		} else if (defType == DefenderType.ROOT_ONLY) {
			if (!defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.minNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.numResRatio.toString())
			|| !defenderParams.containsKey(DefenderParam.stdev.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 4;
			if (defenderParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new RootOnlyDefender(
				defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString()),
				defenderParams.get(DefenderParam.stdev.toString()));
		} else if (defType == DefenderType.GOAL_ONLY) {
			if (!defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.minNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.numResRatio.toString())
			|| !defenderParams.containsKey(DefenderParam.logisParam.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 4;
			if (defenderParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new GoalOnlyDefender(defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString())
				, defenderParams.get(DefenderParam.logisParam.toString()), discFact);
		} else if (defType == DefenderType.vsVALUE_PROPAGATION) {
			if (!defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.minNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.numResRatio.toString())
			|| !defenderParams.containsKey(DefenderParam.logisParam.toString())
			|| !defenderParams.containsKey(DefenderParam.bThres.toString())
			|| !defenderParams.containsKey(DefenderParam.qrParam.toString())
			|| !defenderParams.containsKey(DefenderParam.maxNumAttCandidate.toString())
			|| !defenderParams.containsKey(DefenderParam.minNumAttCandidate.toString())
			|| !defenderParams.containsKey(DefenderParam.numAttCandidateRatio.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 9;
			if (defenderParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new ValuePropagationVsDefender(defenderParams.get(DefenderParam.maxNumRes.toString())
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
			if (!defenderParams.containsKey(DefenderParam.logisParam.toString())
			|| !defenderParams.containsKey(DefenderParam.bThres.toString())
			|| !defenderParams.containsKey(DefenderParam.qrParam.toString())
			|| !defenderParams.containsKey(DefenderParam.numRWSample.toString())
			|| !defenderParams.containsKey(DefenderParam.isRandomized.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 5;
			if (defenderParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new RandomWalkVsDefender(defenderParams.get(DefenderParam.logisParam.toString())
				, discFact
				, defenderParams.get(DefenderParam.bThres.toString())
				, defenderParams.get(DefenderParam.qrParam.toString())
				, defenderParams.get(DefenderParam.numRWSample.toString())
				, defenderParams.get(DefenderParam.isRandomized.toString()));
		} else if (defType == DefenderType.vsUNIFORM) {
			if (!defenderParams.containsKey(DefenderParam.maxNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.minNumRes.toString())
			|| !defenderParams.containsKey(DefenderParam.numResRatio.toString())
			|| !defenderParams.containsKey(DefenderParam.logisParam.toString())
			|| !defenderParams.containsKey(DefenderParam.bThres.toString())
			|| !defenderParams.containsKey(DefenderParam.maxNumAttCandidate.toString())
			|| !defenderParams.containsKey(DefenderParam.minNumAttCandidate.toString())
			|| !defenderParams.containsKey(DefenderParam.numAttCandidateRatio.toString())) {
				throw new IllegalArgumentException();
			}
			final int expectedKeys = 8;
			if (defenderParams.keySet().size() != expectedKeys) {
				throw new IllegalArgumentException();
			}
			return new UniformVsDefender(defenderParams.get(DefenderParam.logisParam.toString())
				, discFact
				, defenderParams.get(DefenderParam.bThres.toString())
				, defenderParams.get(DefenderParam.maxNumRes.toString())
				, defenderParams.get(DefenderParam.minNumRes.toString())
				, defenderParams.get(DefenderParam.numResRatio.toString())
				, defenderParams.get(DefenderParam.maxNumAttCandidate.toString())
				, defenderParams.get(DefenderParam.minNumAttCandidate.toString())
				, defenderParams.get(DefenderParam.numAttCandidateRatio.toString()));
		}
		throw new IllegalArgumentException();
	 }
}

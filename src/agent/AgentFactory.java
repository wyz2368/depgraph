package agent;

import java.util.Map;

import agent.Attacker.ATTACKER_PARAM;
import agent.Attacker.ATTACKER_TYPE;
import agent.Defender.DEFENDER_PARAM;
import agent.Defender.DEFENDER_TYPE;
public class AgentFactory {
	
	 public static Attacker createAttacker(final String attackerName, final Map<String, Double> attackerParams, double discFact) {
        assert attackerName != null && ATTACKER_TYPE.valueOf(attackerName) != null;
        assert attackerParams != null;
        ATTACKER_TYPE attType = ATTACKER_TYPE.valueOf(attackerName);
        
        if(attType == ATTACKER_TYPE.UNIFORM)
        {
        	assert attackerParams.containsKey(ATTACKER_PARAM.maxNumSelectCandidate.toString())
        		&& attackerParams.containsKey(ATTACKER_PARAM.minNumSelectCandidate.toString())
        		&& attackerParams.containsKey(ATTACKER_PARAM.numSelectCandidateRatio.toString());
        	return new UniformAttacker(attackerParams.get(ATTACKER_PARAM.maxNumSelectCandidate.toString())
        			, attackerParams.get(ATTACKER_PARAM.minNumSelectCandidate.toString())
        			, attackerParams.get(ATTACKER_PARAM.numSelectCandidateRatio.toString()));
        }
        else if(attType == ATTACKER_TYPE.VALUE_PROPAGATION)
        {
        	assert attackerParams.containsKey(ATTACKER_PARAM.maxNumSelectCandidate.toString()) 
        	&& attackerParams.containsKey(ATTACKER_PARAM.minNumSelectCandidate.toString())
    		&& attackerParams.containsKey(ATTACKER_PARAM.numSelectCandidateRatio.toString())
    		&& attackerParams.containsKey(ATTACKER_PARAM.qrParam.toString());
        	return new ValuePropagationAttacker(attackerParams.get(ATTACKER_PARAM.maxNumSelectCandidate.toString())
        			, attackerParams.get(ATTACKER_PARAM.minNumSelectCandidate.toString())
        			, attackerParams.get(ATTACKER_PARAM.numSelectCandidateRatio.toString())
        			, attackerParams.get(ATTACKER_PARAM.qrParam.toString())
        			, discFact);
        }
        else if(attType == ATTACKER_TYPE.RANDOM_WALK)
        {
        	assert attackerParams.containsKey(ATTACKER_PARAM.numRWSample.toString()) 
    		&& attackerParams.containsKey(ATTACKER_PARAM.qrParam.toString());
        	return new RandomWalkAttacker(attackerParams.get(ATTACKER_PARAM.numRWSample.toString())
        			, attackerParams.get(ATTACKER_PARAM.qrParam.toString()), discFact);
        }
		else
			return null;	
    }
	 public static Defender createDefender(final String defenderName, final Map<String, Double> defenderParams, double discFact) {
	        assert defenderName != null && DEFENDER_TYPE.valueOf(defenderName) != null;
	        assert defenderParams != null;
	        DEFENDER_TYPE defType = DEFENDER_TYPE.valueOf(defenderName);
	        
	        if(defType == DEFENDER_TYPE.UNIFORM)
	        {
	        	assert defenderParams.containsKey(DEFENDER_PARAM.maxNumRes.toString())
	        	&& defenderParams.containsKey(DEFENDER_PARAM.minNumRes.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.numResRatio.toString());
				return new UniformDefender(defenderParams.get(DEFENDER_PARAM.maxNumRes.toString())
						, defenderParams.get(DEFENDER_PARAM.minNumRes.toString())
						, defenderParams.get(DEFENDER_PARAM.numResRatio.toString()));
	        }
			else if(defType == DEFENDER_TYPE.MINCUT)
			{
				assert defenderParams.containsKey(DEFENDER_PARAM.maxNumRes.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.minNumRes.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.numResRatio.toString());
				return new MinCutDefender(defenderParams.get(DEFENDER_PARAM.maxNumRes.toString())
						, defenderParams.get(DEFENDER_PARAM.minNumRes.toString())
						, defenderParams.get(DEFENDER_PARAM.numResRatio.toString()));
			}
			else if(defType == DEFENDER_TYPE.GOAL_ONLY)
			{
				assert defenderParams.containsKey(DEFENDER_PARAM.maxNumRes.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.minNumRes.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.numResRatio.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.logisParam.toString());
				return new GoalOnlyDefender(defenderParams.get(DEFENDER_PARAM.maxNumRes.toString())
						, defenderParams.get(DEFENDER_PARAM.minNumRes.toString())
						, defenderParams.get(DEFENDER_PARAM.numResRatio.toString())
						, defenderParams.get(DEFENDER_PARAM.logisParam.toString()), discFact);
			}
			else if(defType == DEFENDER_TYPE.vsVALUE_PROPAGATION)
			{
				assert defenderParams.containsKey(DEFENDER_PARAM.maxNumRes.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.minNumRes.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.numResRatio.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.logisParam.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.bThres.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.qrParam.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.maxNumAttCandidate.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.minNumAttCandidate.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.numAttCandidateRatio.toString());
				return new ValuePropagationvsDefender(defenderParams.get(DEFENDER_PARAM.maxNumRes.toString())
						, defenderParams.get(DEFENDER_PARAM.minNumRes.toString())
						, defenderParams.get(DEFENDER_PARAM.numResRatio.toString())
						, defenderParams.get(DEFENDER_PARAM.logisParam.toString())
						, discFact
						, defenderParams.get(DEFENDER_PARAM.bThres.toString())
						, defenderParams.get(DEFENDER_PARAM.qrParam.toString())
						, defenderParams.get(DEFENDER_PARAM.maxNumAttCandidate.toString())
						, defenderParams.get(DEFENDER_PARAM.minNumAttCandidate.toString())
						, defenderParams.get(DEFENDER_PARAM.numAttCandidateRatio.toString()));
			}
			else if(defType == DEFENDER_TYPE.vsRANDOM_WALK)
			{
				assert defenderParams.containsKey(DEFENDER_PARAM.logisParam.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.bThres.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.qrParam.toString())
				&& defenderParams.containsKey(DEFENDER_PARAM.numRWSample.toString());
				return new RandomWalkvsDefender(defenderParams.get(DEFENDER_PARAM.logisParam.toString())
						, discFact
						, defenderParams.get(DEFENDER_PARAM.bThres.toString())
						, defenderParams.get(DEFENDER_PARAM.qrParam.toString())
						, defenderParams.get(DEFENDER_PARAM.numRWSample.toString()));
			}
			else
				return null;
	 }
	 
}

package agent;


import java.util.List;

import model.AttackerAction;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomGenerator;

public abstract class Attacker {
	public enum ATTACKER_TYPE {UNIFORM, VALUE_PROPAGATION, RANDOM_WALK;
	public String toString(){
		switch(this){
		case UNIFORM: return "UN";
		case VALUE_PROPAGATION: return "VP";
		case RANDOM_WALK: return "RW";
		default: return "";
		}	
	}};
	public enum ATTACKER_PARAM{maxNumSelectCandidate, minNumSelectCandidate
		, numSelectCandidateRatio
		, qrParam, numRWSample;
		public String toString(){
		switch(this){
		case maxNumSelectCandidate: return "maxNumSelectCandidate";
		case minNumSelectCandidate: return "minNumSelectCandidate";
		case numSelectCandidateRatio: return "numSelectCandidateRatio";
		case qrParam: return "qrParam";
		case numRWSample: return "numRWSample";
		default: return "";
		}	
	}};
	private ATTACKER_TYPE attType;

	public Attacker(ATTACKER_TYPE attType){
		this.attType = attType;
	}

	public ATTACKER_TYPE getAType()
	{
		return this.attType;
	}

	public abstract AttackerAction sampleAction(DependencyGraph depGraph, int curTimeStep, int numTimeStep, RandomGenerator rng);
	public abstract List<AttackerAction> sampleAction(DependencyGraph depGraph, int curTimeStep, int numTimeStep
			, RandomGenerator rng, int numSample, boolean isReplacement);
}

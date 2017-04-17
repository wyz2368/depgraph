package agent;


import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomGenerator;

public abstract class Defender {
	public enum DEFENDER_TYPE {UNIFORM, MINCUT, GOAL_ONLY, ROOT_ONLY
		, vsVALUE_PROPAGATION, vsRANDOM_WALK, vsUNIFORM;
	@Override
	public String toString(){
		switch(this){
		case UNIFORM: return "UN";
		case MINCUT: return "MC";
		case GOAL_ONLY: return "GO";
		case ROOT_ONLY: return "RO";
		case vsVALUE_PROPAGATION: return "vVP";
		case vsRANDOM_WALK: return "vRW";
		case vsUNIFORM: return "vUN";
		default: return "";
		}	
<<<<<<< HEAD
	}}
	public enum DEFENDER_PARAM{maxNumRes, minNumRes, numResRatio, maxNumAttCandidate, numAttCandidateRatio, logisParam, bThres;
	@Override
=======
	}};
	public enum DEFENDER_PARAM{maxNumRes, minNumRes, numResRatio
		, maxNumAttCandidate, minNumAttCandidate, numAttCandidateRatio
		, logisParam, bThres
		, qrParam
		, numRWSample;
>>>>>>> 1a593272b83306625e9587bc1e5e7d2ba03f128e
	public String toString(){
	switch(this){
	case maxNumRes: return "maxNumRes";
	case minNumRes: return "minNumRes";
	case numResRatio: return "numResRatio";
	case maxNumAttCandidate: return "maxNumAttCandidate";
	case numAttCandidateRatio: return "numAttCandidateRatio";
	case minNumAttCandidate: return "minNumAttCandidate";
	case logisParam: return "logisParam";
	case bThres: return "bThres";
	case qrParam: return "qrParam";
	case numRWSample: return "numRWSample";
	default: return "";
	}	
	}}

	DEFENDER_TYPE dType;
	public Defender(DEFENDER_TYPE dType)
	{
		this.dType = dType;
	}
	
	public DEFENDER_TYPE getDType()
	{
		return this.dType;
	}
	
	public abstract DefenderAction sampleAction(DependencyGraph depGraph, int curTimeStep, int numTimeStep
			, DefenderBelief dBelief, RandomGenerator rng);
	public abstract DefenderBelief updateBelief(DependencyGraph depGraph,
			DefenderBelief currentBelief, DefenderAction dAction,
			DefenderObservation dObservation, int curTimeStep, int numTimeStep,
			RandomGenerator rng);
}

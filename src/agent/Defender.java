package agent;


import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomGenerator;

public abstract class Defender {
	public enum DEFENDER_TYPE {UNIFORM, MINCUT, GOAL_ONLY
		, vsVALUE_PROPAGATION, vsRANDOM_WALK, vsUNIFORM;
	public String toString(){
		switch(this){
		case UNIFORM: return "U";
		case MINCUT: return "MC";
		case GOAL_ONLY: return "GO";
		case vsVALUE_PROPAGATION: return "vVP";
		case vsRANDOM_WALK: return "vRW";
		case vsUNIFORM: return "vU";
		default: return "";
		}	
	}};
	public enum DEFENDER_PARAM{maxNumRes, minNumRes, numResRatio, maxNumAttCandidate, numAttCandidateRatio, logisParam, bThres;
	public String toString(){
	switch(this){
	case maxNumRes: return "maxNumRes";
	case minNumRes: return "minNumRes";
	case numResRatio: return "numResRatio";
	case maxNumAttCandidate: return "maxNumAttCandidate";
	case numAttCandidateRatio: return "numAttCandidateRatio";
	case logisParam: return "logisParam";
	case bThres: return "bThres";
	default: return "";
	}	
	}};

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

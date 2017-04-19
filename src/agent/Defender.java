package agent;

import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

import java.util.List;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import graph.Node;

public abstract class Defender {
	public enum DefenderType {
		UNIFORM, MINCUT, GOAL_ONLY, ROOT_ONLY
		, vsVALUE_PROPAGATION, vsRANDOM_WALK, vsUNIFORM;
		
		@Override
		public String toString() {
			switch(this) {
			case UNIFORM: return "UN";
			case MINCUT: return "MC";
			case GOAL_ONLY: return "GO";
			case ROOT_ONLY: return "RO";
			case vsVALUE_PROPAGATION: return "vVP";
			case vsRANDOM_WALK: return "vRW";
			case vsUNIFORM: return "vUN";
			default: return "";
			}	
		}
	}
	
	public enum DefenderParam {
		maxNumRes, minNumRes, numResRatio
		, maxNumAttCandidate, minNumAttCandidate, numAttCandidateRatio
		, logisParam, bThres
		, qrParam
		, numRWSample;
		
		@Override
		public String toString() {
			switch(this) {
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
		}
	}

	private DefenderType dType;
	
	public Defender(final DefenderType dTypeCur) {
		if (dTypeCur == null) {
			throw new IllegalArgumentException();
		}
		this.dType = dTypeCur;
	}
	
	public final DefenderType getDType() {
		return this.dType;
	}
	
	public abstract DefenderAction sampleAction(DependencyGraph depGraph,
		int curTimeStep, int numTimeStep
		, DefenderBelief dBelief, RandomGenerator rng);
	
	public abstract DefenderBelief updateBelief(DependencyGraph depGraph,
		DefenderBelief currentBelief, DefenderAction dAction,
		DefenderObservation dObservation, int curTimeStep, int numTimeStep,
		RandomGenerator rng);
	
	public static final DefenderAction simpleSampleAction(
		final List<Node> dCandidateNodeList,
		final int numNodetoProtect,
		final AbstractIntegerDistribution rnd
	) {
		if (dCandidateNodeList == null || numNodetoProtect < 0 || rnd == null) {
			throw new IllegalArgumentException();
		}
		DefenderAction action = new DefenderAction();
		
		boolean[] isChosen = new boolean[dCandidateNodeList.size()];
		for (int i = 0; i < dCandidateNodeList.size(); i++) {
			isChosen[i] = false;
		}
		int count = 0;
		while (count < numNodetoProtect) {
			int idx = rnd.sample();
			if (!isChosen[idx]) {
				action.addNodetoProtect(dCandidateNodeList.get(idx));
				isChosen[idx] = true;
				count++;
			}
				
		}
		return action;
	}
}
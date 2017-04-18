package agent;

import java.util.List;

import model.AttackerAction;
import model.DependencyGraph;

import org.apache.commons.math3.random.RandomGenerator;

public abstract class Attacker {
	public enum AttackerType {
		UNIFORM, VALUE_PROPAGATION, RANDOM_WALK;
		
		@Override
		public String toString() {
			switch(this) {
			case UNIFORM: return "UN";
			case VALUE_PROPAGATION: return "VP";
			case RANDOM_WALK: return "RW";
			default: return "";
			}
		}
	}
	
	public enum AttackerParam {
		MAX_NUM_SELECT_CAND, MIN_NUM_SELECT_CAND,
		NUM_SELECT_CAND_RATIO, QR_PARAM, NUM_RW_SAMPLE;
		
		@Override
		public String toString() {
			switch(this) {
			case MAX_NUM_SELECT_CAND: return "maxNumSelectCandidate";
			case MIN_NUM_SELECT_CAND: return "minNumSelectCandidate";
			case NUM_SELECT_CAND_RATIO: return "numSelectCandidateRatio";
			case QR_PARAM: return "qrParam";
			case NUM_RW_SAMPLE: return "numRWSample";
			default: return "";
			}	
		}
	}
	
	private AttackerType attType;

	public Attacker(final AttackerType aAttType) {
		if (aAttType == null) {
			throw new IllegalArgumentException();
		}
		this.attType = aAttType;
	}

	public final AttackerType getAType() {
		return this.attType;
	}

	public abstract AttackerAction sampleAction(
		DependencyGraph depGraph, int curTimeStep, int numTimeStep, RandomGenerator rng);
	
	public abstract List<AttackerAction> sampleAction(
		DependencyGraph depGraph, int curTimeStep, int numTimeStep
		, RandomGenerator rng, int numSample, boolean isReplacement);
}

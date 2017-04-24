package agent;

import org.apache.commons.math3.random.RandomGenerator;

import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

public abstract class ValuePropVsDefSuper extends Defender {
	
	private double thres; // to remove game state from belief
	private double discFact;

	// defender's assumption about attacker
	private double qrParam; 
	private int maxNumAttCandidate; 
	private int minNumAttCandidate;
	private double numAttCandidateRatio;
	
	// number of simulations to approximate update
	private static final int DEFAULT_NUM_STATE_SAMPLE = 30;
	private int numStateSample = DEFAULT_NUM_STATE_SAMPLE;
	private int numAttActionSample = DEFAULT_NUM_STATE_SAMPLE;
	
	public ValuePropVsDefSuper(
		final DefenderType type,
		final double discFact, 
		final double thres,
		final double qrParam, 
		final double maxNumAttCandidate, 
		final double minNumAttCandidate,
		final double numAttCandidateRatio
	) {
		super(type);
		if (
			discFact < 0.0 || discFact > 1.0 || !isProb(thres)
			|| minNumAttCandidate < 1 || maxNumAttCandidate < minNumAttCandidate
			|| !isProb(numAttCandidateRatio)
		) {
			throw new IllegalArgumentException();
		}
		this.discFact = discFact;
		this.thres = thres;
		this.qrParam = qrParam;
		this.maxNumAttCandidate = (int) maxNumAttCandidate;
		this.minNumAttCandidate = (int) minNumAttCandidate;
		this.numAttCandidateRatio = numAttCandidateRatio;
	}

	@Override
	public final DefenderBelief updateBelief(
			final DependencyGraph depGraph,
			final DefenderBelief dBelief, 
			final DefenderAction dAction,
			final DefenderObservation dObservation, 
			final int curTimeStep, 
			final int numTimeStep,
			final RandomGenerator rng) {
		if (curTimeStep < 0 || numTimeStep < curTimeStep || dBelief == null || rng == null
				|| dObservation == null || dAction == null
			) {
				throw new IllegalArgumentException();
			}
		
		Attacker attacker = new ValuePropagationAttacker(this.maxNumAttCandidate, this.minNumAttCandidate
			, this.numAttCandidateRatio, this.qrParam, this.discFact);
		
		return updateBelief(depGraph
				, dBelief
				, dAction
				, dObservation
				, curTimeStep, numTimeStep
				, rng
				, attacker
				, this.numAttActionSample
				, this.numStateSample
				, this.thres); 
	}
	
	private static boolean isProb(final double i) {
		return i >= 0.0 && i <= 1.0;
	}
}

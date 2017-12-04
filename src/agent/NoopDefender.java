package agent;

import org.apache.commons.math3.random.RandomGenerator;

import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

public final class NoopDefender extends Defender {
	
	public NoopDefender() {
		super(DefenderType.NOOP);
	}

	@Override
	public DefenderAction sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep,
		final DefenderBelief dBelief,
		final RandomGenerator rng) {
		return new DefenderAction(); // noop action
	}

	@Override
	public DefenderBelief updateBelief(final DependencyGraph depGraph,
		final DefenderBelief currentBelief, final DefenderAction dAction,
		final DefenderObservation dObservation,
		final int curTimeStep, final int numTimeStep,
		final RandomGenerator rng) {
		return new DefenderBelief(); // an empty belief
	}
}

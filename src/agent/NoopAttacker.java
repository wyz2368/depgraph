package agent;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import model.AttackerAction;
import model.DependencyGraph;

public final class NoopAttacker extends Attacker {
	
	public NoopAttacker() {
		super(AttackerType.NOOP);
	}

	@Override
	public AttackerAction sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep,
		final RandomGenerator rng) {
		if (depGraph == null || curTimeStep < 0
			|| numTimeStep < curTimeStep || rng == null) {
			throw new IllegalArgumentException();
		}
		return new AttackerAction(); // noop action
	}

	@Override
	public List<AttackerAction> sampleAction(final DependencyGraph depGraph,
		final int curTimeStep, final int numTimeStep, final RandomGenerator rng,
		final int numSample, final boolean isReplacement) {
		if (depGraph == null || curTimeStep < 0
			|| numTimeStep < curTimeStep || rng == null
			|| numSample < 1) {
			throw new IllegalArgumentException();
		}
		final List<AttackerAction> result = new ArrayList<AttackerAction>();
		while (result.size() < numSample) {
			result.add(new AttackerAction()); // noop action
		}
		return result;
	}
}

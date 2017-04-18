package main;

import org.apache.commons.math3.random.RandomDataGenerator;

public final class TestRandomGenerator {
	
	private TestRandomGenerator() {
		// private constructor
	}
	
	public static void main(final String[] args) {
		RandomDataGenerator rng = new RandomDataGenerator();
		final int seed = 100;
		rng.reSeed(seed);
		final int mean = 10;
		final int lowCount = 5;
		for (int i = 0; i < lowCount; i++) {
			System.out.println(rng.nextGaussian(mean, 1));
		}
		final int highCount = 100;
		for (int i = 0; i < highCount; i++) {
			rng.nextInt(0, 1);
		}
		for (int i = 0; i < lowCount; i++) {
			System.out.println(rng.nextGaussian(mean, 1));
		}
	}
}

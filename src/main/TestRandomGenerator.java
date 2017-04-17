package main;

import org.apache.commons.math3.random.RandomDataGenerator;

public final class TestRandomGenerator {
	public static void main(final String[] args) {
		RandomDataGenerator rng = new RandomDataGenerator();
		rng.reSeed(100);
		for (int i = 0; i < 5; i++) {
			System.out.println(rng.nextGaussian(10, 1));
		}
		for (int i = 0; i < 100; i++) {
			rng.nextInt(0, 1);
		}
		for (int i = 0; i < 5; i++) {
			System.out.println(rng.nextGaussian(10, 1));
		}
	}
}

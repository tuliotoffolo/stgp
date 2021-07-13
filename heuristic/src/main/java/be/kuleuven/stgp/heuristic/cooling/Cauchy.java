package be.kuleuven.stgp.heuristic.cooling;

/**
 * Created by Jan on 24-6-2015.
 */
public class Cauchy extends SaCoolingSchedule {

	/* has a fatter tail than Boltzmann */

	private int iterations;

	public Cauchy(int iterations) {
		super(iterations, getInitialTemp(iterations), 1);
//		System.out.println(INITIAL_TEMP);
//		System.out.println(INITIAL_TEMP / Math.log(iterations-1));
//		System.out.println(INITIAL_TEMP / Math.log(iterations));
	}

	@Override
	public void coolDown() {
		iterations++;
		temp = INITIAL_TEMP / iterations;
	}

	@Override
	public void reset() {
		temp = INITIAL_TEMP;
		iterations = 0;
	}

	private static double getInitialTemp(double iterations) {
		// at this cooling rate, the temp at it-1 will == 1
		// thus at it it will be < 1
		return iterations-1;
	}
}

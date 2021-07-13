package be.kuleuven.stgp.heuristic.cooling;

/**
 * Created by Jan on 24-6-2015.
 */
public class Boltzmann extends SaCoolingSchedule {

	private int iterations;

	public Boltzmann(int iterations) {
		super(iterations, getInitialTemp(iterations), 1);

//		System.out.println(INITIAL_TEMP);
//		System.out.println(INITIAL_TEMP / Math.log(iterations-1));
//		System.out.println(INITIAL_TEMP / Math.log(iterations));

		reset();
	}

	@Override
	public void coolDown() {
		iterations++;
		temp = INITIAL_TEMP / Math.log(iterations);
	}

	@Override
	public void reset() {
		temp = INITIAL_TEMP;
		iterations = 0;
	}

	private static double getInitialTemp(double iterations) {
		// at this cooling rate, the temp at it-1 will == 1
		// thus at it it will be < 1
		return Math.log(iterations-1);
	}
}

package be.kuleuven.stgp.heuristic.cooling;

/**
 * Created by Jan on 24-6-2015.
 */
public class Quenching extends SaCoolingSchedule {

	private final double COOLING_RATE;
	private int iterations;

	public Quenching(int iterations, double initialTemp, double finalTemp) {
		super(iterations, initialTemp, finalTemp);
		COOLING_RATE = getCooling(iterations, initialTemp, finalTemp);
//		System.out.println(INITIAL_TEMP * Math.pow(COOLING_RATE, 0));
//		System.out.println(INITIAL_TEMP * Math.pow(COOLING_RATE, iterations-1));
//		System.out.println(INITIAL_TEMP * Math.pow(COOLING_RATE, iterations));
//		System.out.println(INITIAL_TEMP * Math.pow(COOLING_RATE, iterations+1));
		reset();
	}

	@Override
	public void coolDown(){
		temp *= COOLING_RATE;
//		iterations++;
//		temp = INITIAL_TEMP * Math.pow(COOLING_RATE, iterations);
	}

	@Override
	public void reset() {
		temp = INITIAL_TEMP;
		iterations = 0;
	}

	private static double getCooling(double iterations, double initialTemp, double finalTemp) {
		// at this cooling rate, the temp at it-1 will == 1
		// thus at it it will be < 1
		return Math.pow(finalTemp/initialTemp, 1d / (iterations-1d));
	}

}

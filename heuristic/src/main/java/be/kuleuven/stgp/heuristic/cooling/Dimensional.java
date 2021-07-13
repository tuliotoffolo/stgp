package be.kuleuven.stgp.heuristic.cooling;

/**
 * Created by Jan on 24-6-2015.
 */
public class Dimensional extends SaCoolingSchedule {

	private final int TOTAL_IT;
	private final double D;

	private int iterations;

	public Dimensional(int iterations, double initialTemp, Double d) {
		super(iterations, initialTemp, 1);
		TOTAL_IT = iterations;
		D = d;

//		this.iterations = 0; setTemp(); System.out.println("temp 0: " + temp);
//		this.iterations = 1; setTemp(); System.out.println("temp 1: " + temp);
//		this.iterations = iterations-1; setTemp(); System.out.println("temp it-1: " + temp);
//		this.iterations = iterations; setTemp(); System.out.println("temp it: " + temp);

		reset();
	}

	@Override
	public void coolDown() {
		iterations++;
		setTemp();
	}

	@Override
	public void reset() {
		temp = INITIAL_TEMP;
		iterations = 0;
	}

	private void setTemp(){
		temp = Math.pow(INITIAL_TEMP, 1d-Math.pow((double)iterations/((double)TOTAL_IT-1d), 1d/D));
//		System.out.println(iterations);
//		System.out.println(temp);
	}
}

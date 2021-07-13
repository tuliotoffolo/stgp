package be.kuleuven.stgp.heuristic.cooling;
/**
 * Created by Jan on 24-6-2015.
 */
public abstract class SaCoolingSchedule {

	protected final int MAX_ITERATIONS;
	protected final double INITIAL_TEMP;
	protected final double FINAL_TEMP;
	protected double temp;

	protected SaCoolingSchedule(int iterations, double initialTemp, double finalTemp){
		MAX_ITERATIONS = iterations;
		INITIAL_TEMP = initialTemp;
		FINAL_TEMP = finalTemp;
		reset();
	}

	public double getTemp(){
		return temp;
	}

	public int getMaxIterations(){
		return MAX_ITERATIONS;
	}

	public boolean isCooledDown(){
		return temp < FINAL_TEMP;
	}

	public abstract void coolDown();

	public abstract void reset();

	public static void main(String[] args){
		double INITIAL = 100;
		double FINAL = 1;
		int ITERATIONS = 123456;

		System.out.println(INITIAL + " -> " + FINAL + "  in " + ITERATIONS);

//		SaCoolingSchedule schedule = new Boltzmann(ITERATIONS);
//		SaCoolingSchedule schedule = new Cauchy(ITERATIONS);
//		SaCoolingSchedule schedule = new Dimensional(ITERATIONS, INITIAL, 1.0);
		SaCoolingSchedule schedule = new Quenching(ITERATIONS, INITIAL, FINAL);

		// -------------------------------------
		int its = 0;
		while(!schedule.isCooledDown()){
			its++;
			schedule.coolDown();
		}
		System.out.println("1) its: " + its);

		// -------------------------------------
		its = 0;
		schedule.reset();
		while(!schedule.isCooledDown()){
			its++;
			schedule.coolDown();
		}
		System.out.println("2) its: " + its);
	}

}

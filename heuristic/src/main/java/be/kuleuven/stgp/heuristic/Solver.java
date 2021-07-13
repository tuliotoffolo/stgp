package be.kuleuven.stgp.heuristic;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.core.model.solution.Solution;
import be.kuleuven.stgp.heuristic.model.Constructive;
import be.kuleuven.stgp.heuristic.model.MtSolution;
import be.kuleuven.stgp.heuristic.move.Move;
import be.kuleuven.stgp.heuristic.move.RuinAndRecreateMove;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jan on 23-10-2015.
 */
public class Solver {

	public static final int DELAY_MS = 100;
	public static final int PROB_SIZE = 500;

	private final Random random = new Random(1);
	private final SolverListener listener;
	private final ArrayList<Move> moves = new ArrayList<>();

	private double maxIt;
	private long startTime;
	private int it;
	private double temp;

	private MtSolution bestSolution;
	private MtSolution currentSolution;

	public Solver(SolverListener listener) {
		this.listener = listener;
	}

	public Solution solve(Problem problem, int seconds) {

		//Timer timer = new Timer();
		//timer.schedule(new TimerTask() {
		//	@Override
		//	public void run() {
		//		stop();
		//	}
		//}, seconds*1000);

		startTime = System.currentTimeMillis();

		/* create moves */
		createMoves(problem);

		/* meta settings ----------------------------------- */

		final double INITIAL_TEMP = 100;			// 10
		final double FINAL_TEMP = 1;			// 0.5
		final double ITERATIONS = 80_000_000;	// 80_000_000
		final double COOLING_RATE = getCooling(ITERATIONS, INITIAL_TEMP, FINAL_TEMP);
		maxIt = ITERATIONS;

		listener.println("SA settings: " + INITIAL_TEMP + " " + FINAL_TEMP + " " + ITERATIONS);

		/* create initial solution ------------------------- */
		currentSolution = Constructive.create(problem, random);
		bestSolution = new MtSolution(currentSolution);
		printImproved();

		currentSolution.convert().validate(System.err);

		/* [meta] init ------------------------------------- */
		temp = INITIAL_TEMP;
		it = 0;

		/* loop -------------------------------------------- */
		while (temp > FINAL_TEMP) {

			double oldDist = currentSolution.objective();
			// move
			Move move = moves.get(random.nextInt(moves.size()));
			MtSolution neighbour = move.getNeighbour(currentSolution);
			double newDist = neighbour.objective();

			// [meta] accept?
			boolean accept = accept(oldDist, newDist, temp);
			int infDelta = neighbour.nInfeasible() - currentSolution.nInfeasible();
			if (accept && infDelta <= 0) {
				currentSolution = neighbour;
				if (newDist < bestSolution.objective() && currentSolution.isFeasible()) {
					bestSolution = new MtSolution(currentSolution);
					printImproved();
				}
			}

			// [meta] update
			temp *= COOLING_RATE;
			it++;
		}

		/* finished ---------------------------------------- */

		listener.println("Finished in " + (System.currentTimeMillis() - startTime) / 1000 + " sec");
		return bestSolution.convert();
	}

	public void stop() {
		temp = 0;
	}

	private double getCooling(double iterations, double initialTemp, double finalTemp) {
		// at this cooling rate, the temp at it-1 will == 1
		// thus at it it will be < 1
		return Math.pow(finalTemp / initialTemp, 1d / (iterations - 1));
	}

	public void printProgress() {
		listener.println("[progress] " + statsString() + "\t" + bestSolution + "\t" + currentSolution);
	}

	private void printImproved() {
		listener.println("[improved] " + statsString() + "\t" + bestSolution);
	}

	private String statsString() {
		return String.format("%.2f%% (%.2f° %d sec)", ((double) it / maxIt) * 100, temp, (System.currentTimeMillis() - startTime) / 1000);
	}

	private void createMoves(Problem problem) {
		moves.clear();
		moves.add(new RuinAndRecreateMove(problem, random));
	}

	private int calcMaxIt(double initialTemp, double finalTemp, double coolingRate) {
		double a = Math.log(coolingRate);
		double b = Math.log(finalTemp / initialTemp);
		return (int) (b / a);
	}

	// Calculate the acceptance probability
	private boolean accept(double oldEnergy, double newEnergy, double temperature) {
		// If the new solution is better, accept it
		if (newEnergy < oldEnergy) {
			return true;
		}
		// If the new solution is worse, calculate an acceptance probability
		double prob = Math.exp((oldEnergy - newEnergy) / temperature);
		return random.nextDouble() < prob;
	}

}

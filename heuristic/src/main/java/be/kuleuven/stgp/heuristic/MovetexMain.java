package be.kuleuven.stgp.heuristic;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.core.model.solution.Solution;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Jan on 10-12-2015.
 */
public class MovetexMain {

	public static Problem readProblem(String path) {

		try {
			return new Problem(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

	public static void writeSolution(Solution solution, String path) {
		try {
			solution.write(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runSolver() {
//		Problem problem = readProblem("D:/KU Leuven/ProjectOptimalisatie2015-Studenten/flanders.prob");
//		Problem problem = readProblem("D:/KU Leuven/ProjectOptimalisatie2015-Studenten/hidden/flanders-4-2.prob");
//		Problem problem = readProblem("../data/flanders-u17.prob");
		Problem problem = readProblem("../data/flanders-u13.prob");
		Solver solver = new Solver(SolverListener.PRINT_LISTENER);

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				solver.printProgress();
			}
		}, 2000, 2000);

		Solution solution = solver.solve(problem, 6000);
		int obj = (int) solution.getObjective();
		writeSolution(solution, "../solutions/flanders-u13_" + obj + ".sol");
//		writeSolution(solution, "../solutions/flanders-u17_" + obj + ".sol");
//		writeSolution(solution, "D:/KU Leuven/ProjectOptimalisatie2015-Studenten/flanders-4-2_codes_" + obj +".sol");

		System.exit(0);
	}

	private static final void runFullList() {
		String[] instances = new String[]{
				"flanders-1-1",
				"flanders-1-2",
				"flanders-1-3",
				"flanders-2-1",
				"flanders-2-2",
				"flanders-2-3",
				"flanders-3-1",
				"flanders-3-2",
				"flanders-3-3",
				"flanders-4-1",
				"flanders-4-2",
				"flanders-4-3",
				"flanders-5-1",
				"flanders-5-2",
				"flanders-5-3",
		};

//		runFeasibilityTest(instances[15]);

	}

	public static void main(String[] args) {

		runSolver();
//		runFullList();
	}


}

package be.kuleuven.stgp.heuristic;

import be.kuleuven.stgp.core.model.solution.Solution;

/**
 * Created by Jan on 5-11-2015.
 */
public interface SolverListener {

	SolverListener PRINT_LISTENER = new SolverListener() {
		@Override
		public void improved(Solution solution) {
//			boolean feasible = solution.validate();
//			System.out.println("[imp] " + solution.getObjective() + " (" + solution.getNLeagues() + ") " + (feasible? "" : " infeasible"));
			System.out.println("[imp] " + solution.getObjective() + " (" + solution.getNLeagues() + ")");
		}

		@Override
		public void println(String str) {
			System.out.println(str);
		}
	};

	SolverListener NULL_LISTENER = new SolverListener() {
		@Override
		public void improved(Solution solution) {}

		@Override
		public void println(String str) {}
	};

	void improved(Solution solution);
	void println(String str);

}

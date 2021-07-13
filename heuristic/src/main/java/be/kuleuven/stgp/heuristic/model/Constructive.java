package be.kuleuven.stgp.heuristic.model;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.core.model.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * Created by Jan on 10-12-2015.
 */
public class Constructive {

	public static MtSolution create(Problem problem, Random random){

		BestFit bestFit = new BestFit(problem, random);
		MtSolution solution = new MtSolution(problem);
		ArrayList<Team> teams = new ArrayList(Arrays.asList(problem.teams));
		Collections.shuffle(teams, random);

		for (int i = 0; i < teams.size(); i++) {
			Team team = teams.get(i);
			bestFit.insertAtBestOrNew(solution, team);
		}

		assert solution.debugSolution();
		assert solution.objective() == solution.convert().getObjective() : solution.objective() + " should be " + solution.convert().getObjective();

		return solution;
	}

}

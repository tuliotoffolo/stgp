package be.kuleuven.stgp.heuristic.move;

import be.kuleuven.stgp.core.model.Division;
import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.core.model.Team;
import be.kuleuven.stgp.core.model.solution.Solution;
import be.kuleuven.stgp.heuristic.model.League;
import be.kuleuven.stgp.heuristic.model.MtSolution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

/**
 * Created by Jan on 10-12-2015.
 */
public class RuinAndRecreateMove extends Move {

	public RuinAndRecreateMove(Problem problem, Random random) {
		super(problem, random);
	}

	@Override
	public MtSolution getNeighbour(MtSolution original) {

		assert original.debugSolution();
		MtSolution solution = new MtSolution(original);
		assert solution.nInfeasible() <= solution.problem.teams.length;
		assert solution.debugSolution();

		int minNleauges = 1;
		int maxNleauges = 100;

//		/* every cat the same prob */
//		int catId = RANDOM.nextInt(solution.problem.divisions.length);
		/* larger cat, larger prob */

		ArrayList<League> catLeagues = solution.getLeagues();

		maxNleauges = Math.max(maxNleauges, catLeagues.size());
		int nLeagues = RANDOM.nextInt(maxNleauges + 1 - minNleauges) + minNleauges;
		Collections.shuffle(catLeagues, RANDOM);
		ArrayList<Team> removedTeams = new ArrayList<>();

		int leagueIt = 0;

		for (int k = 0; k < nLeagues && leagueIt < catLeagues.size(); ) {

			League league = catLeagues.get(leagueIt++);
			k++;

			int minRemove = 1;
			int maxRemove = 2;
			int maxFeasibleRemove = league.teams.size() - solution.problem.minLeagueSize;

			if (RANDOM.nextDouble() < 0.9) {
				maxRemove = Math.min(maxRemove, maxFeasibleRemove);
			}

//			if(maxRemove>maxFeasibleRemove){
//				if (RANDOM.nextDouble() < 0.9) {
//					maxRemove = Math.min(maxRemove, maxFeasibleRemove);
//				}
//			}else{
//				if (RANDOM.nextDouble() < 0.9) {
//					maxRemove = maxFeasibleRemove;
//				}
//			}

			if (maxRemove + 1 - minRemove > 0) {

				assert solution.debugInfeasibility();
				assert maxRemove + 1 - minRemove > 0 : maxRemove + " - " + minRemove + "<= 0";
				int remove = RANDOM.nextInt(maxRemove + 1 - minRemove) + minRemove;

				Collections.shuffle(league.teams, RANDOM);
				int teamIt = 0;
				for (int i = 0; i < remove && teamIt < league.teams.size(); ) {
					Team team = league.teams.get(teamIt++);
					double objectiveDelta = league.deltaIfRemove(team);
					league.removeTeam(team, objectiveDelta);
					removedTeams.add(team);
					i++;
				}

			}
		}

		Collections.shuffle(removedTeams, RANDOM);

		/* create some new leagues */
		if (RANDOM.nextDouble() < 0.99) {
			int maxNewLeagues = removedTeams.size() / solution.problem.minLeagueSize;
			if (maxNewLeagues > 0) {
				assert solution.nInfeasible() <= solution.problem.teams.length;
				int infBefore = solution.nInfeasible();
				maxNewLeagues = RANDOM.nextInt(maxNewLeagues) + 1;
				for (int i = 0; i < maxNewLeagues; i++) {
					BESTFIT.insertInNewLeague(solution, removedTeams.remove(removedTeams.size() - 1));
				}
				assert solution.nInfeasible() <= solution.problem.teams.length : infBefore + "  " + solution.nInfeasible();
			}

		}

		/* fill the infeasible leagues first */
		for (int i = 0; i < removedTeams.size(); i++) {
			if (BESTFIT.insertAtBest(solution.getInfeasibleLeagues(), removedTeams.get(i))) {
				removedTeams.remove(i);
				i--;
			}
		}

		/* add the remaining teams */
		BESTFIT.insertAtBest(solution.getLeagues(), removedTeams);

		if (removedTeams.isEmpty()) {
			assert original.debugSolution();
			assert solution.debugSolution() : "original inf:" + original.nInfeasible() + "    new inf:" + solution.nInfeasible();
			return solution;
		} else {
			return original;
		}

	}
}

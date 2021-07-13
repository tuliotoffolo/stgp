package be.kuleuven.stgp.heuristic.model;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.core.model.Team;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Jan on 10-12-2015.
 */
public class BestFit {

	private final Problem problem;
	private final Random RANDOM;

	public BestFit(Problem problem, Random random) {
		this.problem = problem;
		this.RANDOM = random;
	}

//	public void insertAtBestOrNew(MtSolution solution, ArrayList<Team> teams) {
//		for (int i = 0; i < teams.size(); i++) {
//			insertAtBestOrNew(solution, teams.get(i));
//		}
//	}

//	public void insertAtBestOrNew(MtSolution solution, Team team) {
//		if (!insertAtBest(solution.getLeagues(team.division.id), team)) {
//			insertInNewLeague(solution, team);
//		}
//	}

	public void insertAtBestOrNew(MtSolution solution, Team team) {
		if (!insertAtBest(solution.getLeagues(), team)) {
			insertInNewLeague(solution, team);
		}
	}

	public League insertInNewLeague(MtSolution solution, Team team) {
		return new League(solution, team);
	}

	public void insertAtBest(ArrayList<League> leagues, ArrayList<Team> teams) {
		for (int i = 0; i < teams.size(); i++) {
			if (insertAtBest(leagues, teams.get(i))) {
				teams.remove(i--);
			}
		}
	}

	public boolean insertAtBest(ArrayList<League> leagues, Team team) {
		double bestDelta = Double.MAX_VALUE;
		League bestLeague = null;

		for (int i = 0; i < leagues.size(); i++) {
			League league = leagues.get(i);

			if (!league.canAddSize(1)) continue;
			if (!league.canAddLevel(team)) continue;
			if (!league.canAddTimeDist(team)) continue;
			if (!league.canAddSameClub(team)) continue;

			double delta = league.deltaIfAdd(team);
			if (bestLeague == null || (delta < bestDelta && RANDOM.nextDouble() < 0.8)) {
				bestDelta = delta;
				bestLeague = league;
			}
		}

		if (bestLeague != null) {
			bestLeague.addTeam(team, bestDelta);
			return true;
		} else {
			return false;
		}
	}

}

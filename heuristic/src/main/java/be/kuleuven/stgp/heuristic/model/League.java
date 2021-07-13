package be.kuleuven.stgp.heuristic.model;

import be.kuleuven.stgp.core.model.Team;

import java.util.ArrayList;

/**
 * Created by Jan on 10-12-2015.
 */
public class League {

	private final MtSolution solution;
	private int minLevel;
	private int maxLevel;
	public final ArrayList<Team> teams;
	private double objective;
	boolean infeasible;

	public League(MtSolution solution, Team firstTeam) {
		this.solution = solution;
		this.teams = new ArrayList<>();
		this.objective = 0;

		teams.add(firstTeam);
		maxLevel = firstTeam.level + solution.problem.maxLevelDiff;
		minLevel = firstTeam.level - solution.problem.maxLevelDiff;
		if (solution.problem.minLeagueSize <= 1) {
			infeasible = false;
		} else {
			infeasible = true;
			solution.incInfeasible();
		}

		solution.getLeagues().add(this);
	}

	League(MtSolution solution, League league) {
		this.solution = solution;
		this.minLevel = league.minLevel;
		this.maxLevel = league.maxLevel;
		this.teams = new ArrayList<>(league.teams);
		this.objective = league.objective;
		this.infeasible = league.infeasible;
	}

	/* checkers ---------------------------------------------------------------------------------------------------- */

	public boolean canAddSameClub(Team team) {
		int nFromSameClub = 0;
		for (int i = 0; i < teams.size(); i++) {
			Team t = teams.get(i);
			if (t.club == team.club) {
				nFromSameClub++;
				if (nFromSameClub >= solution.problem.maxTeamSameClub) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean canAddTimeDist(Team team) {
		for (int i = 0; i < teams.size(); i++) {
			Team t = teams.get(i);
			if (team.getTravelTimeTo(t) > solution.problem.maxTravelTime) return false;
			if (team.getTravelDistTo(t) > solution.problem.maxTravelDist) return false;
		}
		return true;
	}

	public boolean canAddLevel(Team team) {
		if (team.level < minLevel) return false;
		if (team.level > maxLevel) return false;
		return true;
	}

	public boolean canAddSize(int n) {
		return solution.problem.maxLeagueSize >= teams.size() + n;
	}

	public boolean canRemoveSize(int n) {
		return solution.problem.minLeagueSize <= teams.size() - n;
	}

	public double deltaIfAdd(Team team) {
		int timeDelta = 0;
		int distDelta = 0;
		for (int i = 0; i < teams.size(); i++) {
			Team t = teams.get(i);
			timeDelta += team.getTravelTimeTo(t);
			distDelta += team.getTravelDistTo(t);
		}
		return solution.toObjectiveDelta(timeDelta, distDelta);
	}

	public double deltaIfRemove(Team team) {
		int timeDelta = 0;
		int distDelta = 0;
		for (int i = 0; i < teams.size(); i++) {
			Team t = teams.get(i);
			if (t != team) {
				timeDelta -= team.getTravelTimeTo(t);
				distDelta -= team.getTravelDistTo(t);
			}
		}
		return solution.toObjectiveDelta(timeDelta, distDelta);
	}

	/* manipulate -------------------------------------------------------------------------------------------------- */

	public void addTeam(Team team, double objectiveDelta) {
		assert teams.size() > 0;
		int newMaxLevel = team.level + solution.problem.maxLevelDiff;
		int newMinLevel = team.level - solution.problem.maxLevelDiff;
		maxLevel = Math.min(maxLevel, newMaxLevel);
		minLevel = Math.max(minLevel, newMinLevel);
		teams.add(team);
		solution.addObjective(objectiveDelta);
		objective += objectiveDelta;
		assert teams.size() <= solution.problem.maxLeagueSize;
		checkInfeasibleSize();
	}

	public void removeTeam(Team team, double objectiveDelta) {
		teams.remove(team);
		solution.addObjective(objectiveDelta);

		// levels
		int minPresentLevel = Integer.MAX_VALUE;
		int maxPresentLevel = Integer.MIN_VALUE;

		for (int i = 0; i < teams.size(); i++) {
			Team t = teams.get(i);
			minPresentLevel = Math.min(minPresentLevel, t.level);
			maxPresentLevel = Math.max(maxPresentLevel, t.level);
		}

		objective += objectiveDelta;

		minLevel = maxPresentLevel - solution.problem.maxLevelDiff;
		maxLevel = minPresentLevel + solution.problem.maxLevelDiff;

		checkInfeasibleSize();

		if (teams.size() == 0) {
			removeFromSolution();
		}
	}

	private void removeFromSolution() {
		solution.addObjective(-objective);
		solution.getLeagues().remove(this);
		if (infeasible) {
			solution.decInfeasible();
		}
	}

	private void checkInfeasibleSize() {
		boolean newInfeasible = teams.size() < solution.problem.minLeagueSize || teams.size() > solution.problem.maxLeagueSize;
		if (newInfeasible != infeasible) {
			infeasible = newInfeasible;
			if (infeasible) {
				solution.incInfeasible();
			} else {
				solution.decInfeasible();
			}
		}
	}
}

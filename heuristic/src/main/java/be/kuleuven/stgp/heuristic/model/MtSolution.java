package be.kuleuven.stgp.heuristic.model;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.core.model.Team;
import be.kuleuven.stgp.core.model.solution.Solution;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Jan on 10-12-2015.
 */
public class MtSolution {

	public final Problem problem;

	private final ArrayList<League> leagues;
	private double objective;
	private int nInfeasible;

	MtSolution(Problem problem) {
		this.problem = problem;
		this.objective = 0;
		this.nInfeasible = 0;
		leagues = new ArrayList<>();
	}

	public MtSolution(MtSolution solution) {
		assert solution.debugSolution();
		this.problem = solution.problem;
		this.objective = solution.objective;
		this.nInfeasible = solution.nInfeasible;
		this.leagues = new ArrayList<>();
		for (int j = 0; j < solution.leagues.size(); j++) {
			this.leagues.add(new League(this, solution.leagues.get(j)));
		}
		assert debugSolution();
	}

//	void addToObjective(int time, int distance) {
//		objective += time * problem.weightTravelTime;
//		objective += distance * problem.weightTravelDist;
//	}

	public int size() {
		return leagues.size();
	}

	public ArrayList<League> getLeagues() {
		return leagues;
	}

	void addObjective(double delta) {
		objective += delta;
	}

	void incInfeasible() {
		nInfeasible++;
	}

	void decInfeasible() {
		nInfeasible--;
	}

	public int nInfeasible() {
		return nInfeasible;
	}

	public boolean isFeasible() {
		return nInfeasible == 0;
	}

	double toObjectiveDelta(int time, int distance) {
		return 2 * (problem.weightTravelTime * time + problem.weightTravelDist * distance);
	}

	public double objective() {
		return objective;
	}

	public Solution convert() {
		Solution solution = new Solution(problem);
		for (int i = 0; i < leagues.size(); i++) {
			League league = leagues.get(i);
			be.kuleuven.stgp.core.model.solution.League l = new be.kuleuven.stgp.core.model.solution.League(problem);
			for (int j = 0; j < league.teams.size(); j++) {
				l.add(league.teams.get(j));
			}
			solution.addLeague(l);
		}

		solution.validate();

		return solution;
	}

	public ArrayList<League> getInfeasibleLeagues() {
		ArrayList<League> infLeagues = new ArrayList<>();
		for (int i = 0; i < leagues.size(); i++) {
			League l = leagues.get(i);
			if (l.infeasible) {
				infLeagues.add(l);
			}
		}
		return infLeagues;
	}

	public boolean debugSolution() {
		int infCount = 0;
		HashSet<Team> teamSet = new HashSet<>();
		for (int i = 0; i < leagues.size(); i++) {
			League league = leagues.get(i);
			for (int j = 0; j < league.teams.size(); j++) {
				Team team = league.teams.get(j);
				if (teamSet.contains(team)) {
					System.err.print("duplicate teams");
					return false;
				}
				teamSet.add(team);
			}
			if (league.infeasible) {
				infCount++;
			}
		}
		if (teamSet.size() != problem.teams.length) {
			System.err.println("teams in solution: " + teamSet.size() + " should be: " + problem.teams.length);
			return false;
		}
		if (infCount != nInfeasible) {
			System.err.println("infeasible leagues in solution: " + nInfeasible + " should be: " + infCount);
			System.err.println(this);
			return false;
		}

		return true;
	}

	public boolean debugInfeasibility() {
		int infCount = 0;
		for (int i = 0; i < leagues.size(); i++) {
			League league = leagues.get(i);
			if (league.infeasible) {
				infCount++;
			}
		}
		if (infCount != nInfeasible) {
			System.err.println("infeasible leagues in solution: " + nInfeasible + " should be: " + infCount);
			System.err.println(this);
			return false;
		}
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Solution[");
		sb.append("obj=").append((int) objective);
		sb.append(", inf=").append(nInfeasible);
		sb.append(", leagues=").append(leagues.size());
		sb.append("]");
		return sb.toString();
	}

}

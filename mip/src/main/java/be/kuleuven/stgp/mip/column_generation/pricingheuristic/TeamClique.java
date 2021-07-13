package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.mip.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Jan on 1-3-2016.
 */
public class TeamClique {

	private final Problem problem;
	public final int id;
	private final ArrayList<PricingTeam> teams;
	public final HashSet<TeamClique> conflictingCliques;
	private final HashMap<Integer, Integer> teamsPerClub;
	private double objectiveNoGamma;
	private double gamma;
	private int minLevel;
	private int maxLevel;

	public int nConflictingCliquesInLeague = 0;

	public TeamClique(PricingTeam pricingTeam) {
		this.problem = pricingTeam.team.problem;
		this.id = pricingTeam.team.id;
		this.teamsPerClub = new HashMap<>();
		this.teamsPerClub.put(pricingTeam.team.club.id, 1);
		this.teams = new ArrayList<>();
		this.teams.add(pricingTeam);
		this.conflictingCliques = new HashSet<>();
		this.objectiveNoGamma = 0;
		this.minLevel = pricingTeam.team.level;
		this.maxLevel = pricingTeam.team.level;
		pricingTeam.clique = this;
	}

	public int getMinLevel() {
		return minLevel;
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	public int size() {
		return teams.size();
	}

	public HashMap<Integer, Integer> getTeamsPerClub() {
		return teamsPerClub;
	}

	public ArrayList<PricingTeam> getTeams() {
		return teams;
	}

	public double getObjectiveInclGamma() {
		return objectiveNoGamma - gamma;
	}

	public double getObjectiveNoGamma() {
		return objectiveNoGamma;
	}

	public double getGamma() {
		return gamma;
	}

	public void mergeClique(TeamClique clique, double distDelta) {

		conflictingCliques.addAll(clique.conflictingCliques);

		for (TeamClique tc : clique.conflictingCliques) {
			assert tc.conflictingCliques.contains(clique);
			tc.conflictingCliques.remove(clique);
			tc.conflictingCliques.add(this);
		}

		for (PricingTeam ptNew : clique.teams) {
			ptNew.clique = this;
			Integer n = teamsPerClub.get(ptNew.team.club.id);
			if (n == null) teamsPerClub.put(ptNew.team.club.id, 1);
			else teamsPerClub.put(ptNew.team.club.id, n + 1);
		}

		teams.addAll(clique.teams);
		objectiveNoGamma += distDelta;
		objectiveNoGamma += clique.objectiveNoGamma;
		gamma += clique.gamma;

		minLevel = Math.min(minLevel, clique.minLevel);
		maxLevel = Math.max(maxLevel, clique.maxLevel);
	}

//	public void mergeClique(TeamClique clique) {
//		int timeDelta = 0;
//		int distDelta = 0;
//		conflictingCliques.addAll(clique.conflictingCliques);
//
//		for (TeamClique tc : clique.conflictingCliques) {
//			assert tc.conflictingCliques.contains(clique);
//			tc.conflictingCliques.remove(clique);
//			tc.conflictingCliques.add(this);
//		}
//
//		for (PricingTeam ptNew : clique.teams) {
//			ptNew.clique = this;
//			Integer n = teamsPerClub.get(ptNew.team.club.id);
//			if(n==null) teamsPerClub.put(ptNew.team.club.id, 1);
//			else teamsPerClub.put(ptNew.team.club.id, n+1);
//			for (PricingTeam ptPresent : teams) {
//				timeDelta += ptNew.team.getTravelTimeTo(ptPresent.team);
//				distDelta += ptNew.team.getTravelDistTo(ptPresent.team);
//			}
//		}
//
//		teams.addAll(clique.teams);
//		objectiveNoGamma += toObjectiveDelta(timeDelta, distDelta);
//		objectiveNoGamma += clique.objectiveNoGamma;
//		gamma += clique.gamma;
//
//		minLevel = Math.min(minLevel, clique.minLevel);
//		maxLevel = Math.max(maxLevel, clique.maxLevel);
//	}

	public static boolean debugIntraWeight(TeamClique clique) {
		double intraDist = 0;
		for (int i = 0; i < clique.size() - 1; i++) {
			for (int j = i + 1; j < clique.size(); j++) {
				intraDist += 2 * clique.teams.get(i).team.getWeightedDistTimeTo(clique.teams.get(j).team);
			}
		}
		if (Math.abs(intraDist - clique.objectiveNoGamma) > Constants.EPS) {
			System.err.println("TeamClique objective " + clique.objectiveNoGamma + " should be " + intraDist);
			return false;
		} else {
			return true;
		}
	}

	public static boolean debugInterWeight(TeamClique c1, TeamClique c2, double interDist) {

		double dist = 0;
		for (int i = 0; i < c1.teams.size(); i++) {
			for (int j = 0; j < c2.teams.size(); j++) {
				dist += 2 * c1.teams.get(i).team.getWeightedDistTimeTo(c2.teams.get(j).team);
			}
		}
		if (Math.abs(interDist - dist) > Constants.EPS) {
			System.err.println("TeamClique interdist " + interDist + " should be " + dist);
			return false;
		} else {
			return true;
		}
	}

	public void updateGamma(double[] gammas) {
		gamma = 0;
		for (PricingTeam team : teams) {
			gamma += gammas[team.team.id];
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TeamClique[");
		boolean first = true;
		for (PricingTeam team : teams) {
			if (!first) sb.append(", ");
			else first = false;
			sb.append(team.team.id);
		}
		sb.append(", confl=" + nConflictingCliquesInLeague + ", obj=" + objectiveNoGamma + ", gamma=" + gamma + ", cost=" + getObjectiveInclGamma() + "]");
		return sb.toString();
	}

	public static String toCliqueString(ArrayList<TeamClique> cliques) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < cliques.size(); i++) {
			if (i != 0) sb.append(" ");
			TeamClique clique = cliques.get(i);
			sb.append(PricingTeam.toSimpleCliqueString(clique.getTeams()));
			sb.append("\\");
			ArrayList<TeamClique> cantBeList = new ArrayList<>(clique.conflictingCliques);
			for (int j = 0; j < cantBeList.size(); j++) {
				TeamClique tc = cantBeList.get(j);
				sb.append(PricingTeam.toSimpleCliqueString(tc.getTeams()));
			}
		}
		sb.append("]");
		return sb.toString();
	}

	private double toObjectiveDelta(int time, int distance) {
		return 2 * (problem.weightTravelTime * time + problem.weightTravelDist * distance);
	}
}

package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.core.model.Team;
import be.kuleuven.stgp.mip.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Jan on 24-2-2016.
 */
public class PricingLeague {

	private final Problem problem;
	private final CliqueManager cliqueManager;
	private int minLevel;
	private int maxLevel;
	private int size;
	private final HashMap<Integer, Integer> teamsPerClub;
	public final ArrayList<TeamClique> presentCliques;            // present presentCliques in this leagues
	public final ArrayList<TeamClique> idleCliques;                // idle cliques which can be in the same league with the present presentCliques
	public final ArrayList<TeamClique> conflictCliques;            // idle cliques which can not be in the same league with the present presentCliques
	private double objective;
	boolean infeasible;

	public PricingLeague(Problem problem, CliqueManager cliqueManager, TeamClique firstClique) {
		this.problem = problem;
		this.cliqueManager = cliqueManager;
		this.presentCliques = new ArrayList<>();
		this.presentCliques.add(firstClique);
		this.teamsPerClub = new HashMap<>();
		for (Map.Entry<Integer, Integer> clubN : firstClique.getTeamsPerClub().entrySet()) {
			teamsPerClub.put(clubN.getKey(), clubN.getValue());
		}
		this.idleCliques = new ArrayList<>();
		this.conflictCliques = new ArrayList<>();
		this.objective = firstClique.getObjectiveInclGamma();

		firstClique.nConflictingCliquesInLeague = 0;
		for (int i = 0; i < cliqueManager.getTeamCliques().size(); i++) {
			TeamClique mbtc = cliqueManager.getTeamCliques().get(i);
			if (mbtc != firstClique) {
				if (firstClique.conflictingCliques.contains(mbtc)) {
					conflictCliques.add(mbtc);
					mbtc.nConflictingCliquesInLeague = 1;
				} else {
					idleCliques.add(mbtc);
					mbtc.nConflictingCliquesInLeague = 0;
				}
			}
		}

		this.size = firstClique.size();
		this.maxLevel = firstClique.getMinLevel() + problem.maxLevelDiff;
		this.minLevel = firstClique.getMaxLevel() - problem.maxLevelDiff;

		if (problem.minLeagueSize <= firstClique.size()) {
			this.infeasible = false;
		} else {
			this.infeasible = true;
		}
	}

	public double getObjective() {
		return objective;
	}

	/* checkers ---------------------------------------------------------------------------------------------------- */

	public boolean canAddLevelClubTimeDistSize(TeamClique clique) {
		assert idleCliques.contains(clique);
		assert !conflictCliques.contains(clique);
		assert canAddTimeDist(clique);    // this check is now included in pre processed conflicts
		return canAddSize(clique.size()) && canAddLevel(clique) && canAddSameClub(clique);
	}

	public boolean canAddSameClub(TeamClique clique) {
		for (Map.Entry<Integer, Integer> clubN : clique.getTeamsPerClub().entrySet()) {
			Integer presentN = teamsPerClub.get(clubN.getKey());
			if (presentN != null && (presentN + clubN.getValue()) > problem.maxTeamSameClub) {
				return false;
			}
		}
		return true;
	}

	public boolean canAddTimeDist(TeamClique clique) {
		/* this check is now included in preprocessed fix(i,j,false) calls */
		for (int i = 0; i < presentCliques.size(); i++) {
			TeamClique localClique = presentCliques.get(i);
			for (int j = 0; j < localClique.getTeams().size(); j++) {
				PricingTeam localTeam = localClique.getTeams().get(j);
				for (int k = 0; k < clique.getTeams().size(); k++) {
					PricingTeam newTeam = clique.getTeams().get(k);
					if (localTeam.team.getTravelTimeTo(newTeam.team) > problem.maxTravelTime) return false;
					if (localTeam.team.getTravelDistTo(newTeam.team) > problem.maxTravelDist) return false;
				}
			}
		}
		return true;
	}

//	public boolean canAddDivision(Team team) {
//		return division == team.division;
//	}
//
//	public boolean canAddDivision(Team teamA, Team teamB) {
//		return division == teamA.division && division == teamB.division;
//	}

	public boolean canAddLevel(TeamClique clique) {
		assert clique.getMinLevel() + problem.maxLevelDiff >= clique.getMaxLevel();
		if (clique.getMinLevel() < minLevel) return false;
		if (clique.getMaxLevel() > maxLevel) return false;
		return true;
	}

	public boolean canAddSize(int n) {
		return problem.maxLeagueSize >= size + n;
	}

	public boolean canRemoveSize(int n) {
		return problem.minLeagueSize <= size - n;
	}

//	public double deltaIfAdd(TeamClique clique) {
//
//		int timeDelta = 0;
//		int distDelta = 0;
//		for (int i = 0; i < presentCliques.size(); i++) {
//			TeamClique presentClique = presentCliques.get(i);
//			if(presentClique!=clique) {
//				for (int j = 0; j < presentClique.size(); j++) {
//					PricingTeam presentTeam = presentClique.getTeams().get(j);
//					for (int k = 0; k < clique.size(); k++) {
//						PricingTeam newTeam = clique.getTeams().get(k);
//						timeDelta += presentTeam.getTravelTimeTo(newTeam);
//						distDelta += presentTeam.getTravelDistTo(newTeam);
//					}
//				}
//			}
//		}
//
//		return toObjectiveDelta(timeDelta, distDelta) + clique.getObjectiveInclGamma();

	public double deltaIfAdd(TeamClique clique) {

		int distDelta = 0;
		for (int i = 0; i < presentCliques.size(); i++) {
			TeamClique presentClique = presentCliques.get(i);
			if (presentClique != clique) {
				distDelta += cliqueManager.getInterDistDelta(presentClique, clique);
				assert TeamClique.debugInterWeight(presentClique, clique, cliqueManager.getInterDistDelta(presentClique, clique));
			}
		}

		return distDelta + clique.getObjectiveInclGamma();
	}

	public double deltaIfRemove(TeamClique clique) {
		return -deltaIfAdd(clique);
//		int distDelta = 0;
//		for (int i = 0; i < presentCliques.size(); i++) {
//			TeamClique presentClique = presentCliques.get(i);
//			if (presentClique != clique) {
//				distDelta += cliqueManager.getInterDistDelta(presentClique, clique);
//				assert TeamClique.debugInterWeight(presentClique, clique, cliqueManager.getInterDistDelta(presentClique, clique));
//			}
//		}
//
//		return distDelta + clique.getObjectiveInclGamma();
	}

	private double toObjectiveDelta(int time, int distance) {
		return 2 * (problem.weightTravelTime * time + problem.weightTravelDist * distance);
	}

	/* manipulate -------------------------------------------------------------------------------------------------- */

	public void addClique(TeamClique clique) {
		addClique(clique, deltaIfAdd(clique));
	}

	public void addClique(TeamClique clique, double objectiveDelta) {
		assert TeamClique.debugIntraWeight(clique);
		assert presentCliques.size() > 0;
		assert clique.size() > 0;
		assert debugObjective();
		assert debugTeamsInSameLeague();
		assert debugPresentIdleForbidden();
//		assert debugSizeClubsLevelsTimeDist();
		assert clique.nConflictingCliquesInLeague == 0;
		int newMaxLevel = clique.getMinLevel() + problem.maxLevelDiff;
		int newMinLevel = clique.getMaxLevel() - problem.maxLevelDiff;
		maxLevel = Math.min(maxLevel, newMaxLevel);
		minLevel = Math.max(minLevel, newMinLevel);
		objective += objectiveDelta;
		assert idleCliques.contains(clique);
		assert !presentCliques.contains(clique);
		assert size <= problem.maxLeagueSize;
		idleCliques.remove(clique);
		presentCliques.add(clique);
		for (int i = 0; i < cliqueManager.getTeamCliques().size(); i++) {
			TeamClique divClique = cliqueManager.getTeamCliques().get(i);
			if (clique != divClique && clique.conflictingCliques.contains(divClique)) {
				divClique.nConflictingCliquesInLeague++;
				if (divClique.nConflictingCliquesInLeague == 1) {
					// divClique has a conflict with the added clique, thus it should not be in the present cliques
					assert !presentCliques.contains(divClique) : toStateString();
					// divClique has one conflict, thus with only with the added clique, thus before it was not forbidden
					assert !conflictCliques.contains(divClique) : toStateString();
					// divClique had no conflicts before thus it should be in the idle cliques
					assert idleCliques.contains(divClique) : toStateString();
					idleCliques.remove(divClique);
					conflictCliques.add(divClique);
				}
			}
		}
		for (Map.Entry<Integer, Integer> clubN : clique.getTeamsPerClub().entrySet()) {
			Integer prevN = teamsPerClub.get(clubN.getKey());
			if (prevN == null) teamsPerClub.put(clubN.getKey(), clubN.getValue());
			else teamsPerClub.put(clubN.getKey(), prevN + clubN.getValue());
		}
		size += clique.size();
		checkInfeasibleSize();
		assert debugPresentIdleForbidden();
//		assert debugSizeClubsLevelsTimeDist();
		assert debugObjective();
		assert debugTeamsInSameLeague();
	}

	public void removeClique(TeamClique clique, double objectiveDelta) {
		assert !idleCliques.contains(clique);
		assert presentCliques.contains(clique);
		assert debugObjective();
		assert debugPresentIdleForbidden();
		assert clique.nConflictingCliquesInLeague == 0;

		presentCliques.remove(clique);
		idleCliques.add(clique);
		for (int i = 0; i < cliqueManager.getTeamCliques().size(); i++) {
			TeamClique divisionClique = cliqueManager.getTeamCliques().get(i);
			if (clique.conflictingCliques.contains(divisionClique)) {
				assert divisionClique.nConflictingCliquesInLeague > 0;
				divisionClique.nConflictingCliquesInLeague--;
				if (divisionClique.nConflictingCliquesInLeague == 0) {
					idleCliques.add(divisionClique);
					conflictCliques.remove(divisionClique);
				}
			}
		}

		// levels
		int minPresentLevel = Integer.MAX_VALUE;
		int maxPresentLevel = Integer.MIN_VALUE;

		for (int i = 0; i < presentCliques.size(); i++) {
			TeamClique c = presentCliques.get(i);
			minPresentLevel = Math.min(minPresentLevel, c.getMinLevel());
			maxPresentLevel = Math.max(maxPresentLevel, c.getMaxLevel());
		}
		assert minPresentLevel + problem.maxLevelDiff >= maxPresentLevel;
		minLevel = maxPresentLevel - problem.maxLevelDiff;
		maxLevel = minPresentLevel + problem.maxLevelDiff;

		objective += objectiveDelta;
		for (Map.Entry<Integer, Integer> clubN : clique.getTeamsPerClub().entrySet()) {
			Integer prevN = teamsPerClub.get(clubN.getKey());
			assert prevN != null;
			teamsPerClub.put(clubN.getKey(), prevN - clubN.getValue());
		}

		size -= clique.size();
		checkInfeasibleSize();

		assert debugPresentIdleForbidden();
		assert debugObjective();
		assert debugTeamsInSameLeague();
	}

	private void checkInfeasibleSize() {
		boolean newInfeasible = size < problem.minLeagueSize || size > problem.maxLeagueSize;
		if (newInfeasible != infeasible) {
			infeasible = newInfeasible;
		}
	}

	public boolean debugPresentIdleForbidden() {
		String info = "";
		boolean ok = true;
		// duplicates?
		int allDivSize = cliqueManager.getTeamCliques().size();
		int listSizes = presentCliques.size() + idleCliques.size() + conflictCliques.size();
		if (allDivSize != listSizes) {
			info += "Total list sizes " + listSizes + " while there are " + allDivSize + " division cliques";
			ok = false;
		}
		// all division teams in 1 list?
		for (int i = 0; i < cliqueManager.getTeamCliques().size(); i++) {
			TeamClique clique = cliqueManager.getTeamCliques().get(i);
			int inLists = 0;
			if (presentCliques.contains(clique)) inLists++;
			if (idleCliques.contains(clique)) inLists++;
			if (conflictCliques.contains(clique)) inLists++;
			if (inLists == 0) {
				info += clique + " is not in a list\n";
				ok = false;
			} else if (inLists > 1) {
				info += clique + " is not in 1 list but " + inLists + " (present: " + presentCliques.contains(clique) + ", idle: " + idleCliques.contains(clique) + ", forbidden: " + conflictCliques.contains(clique) + ")\n";
				ok = false;
			}
		}

		// count conflicts
		int[] nConflicts = new int[cliqueManager.getTeamCliques().size()];
		for (int i = 0; i < cliqueManager.getTeamCliques().size(); i++) {
			TeamClique divisionClique = cliqueManager.getTeamCliques().get(i);
			for (int j = 0; j < presentCliques.size(); j++) {
				TeamClique presentClique = presentCliques.get(j);
				if (presentClique.conflictingCliques.contains(divisionClique)) {
					nConflicts[i]++;
				}
			}
		}
		for (int i = 0; i < cliqueManager.getTeamCliques().size(); i++) {
			TeamClique divisionClique = cliqueManager.getTeamCliques().get(i);
			if (divisionClique.nConflictingCliquesInLeague != nConflicts[i]) {
				info += "wrong nConflicts, " + divisionClique.nConflictingCliquesInLeague + " should be " + nConflicts[i] + "\n";
				ok = false;
			}
		}

		// check present list
		for (int i = 0; i < presentCliques.size(); i++) {
			TeamClique presentClique = presentCliques.get(i);
			if (presentClique.nConflictingCliquesInLeague != 0) {
				info += "present clique: " + presentClique + ".nConflictingCliques=" + presentClique.nConflictingCliquesInLeague + "\n";
				ok = false;
			}
		}

		// check idle list
		for (int i = 0; i < idleCliques.size(); i++) {
			TeamClique idleClique = idleCliques.get(i);
			if (idleClique.nConflictingCliquesInLeague != 0) {
				info += "idle clique: " + idleClique + ".nConflictingCliques=" + idleClique.nConflictingCliquesInLeague + "\n";
				ok = false;
			}
			for (int j = 0; j < presentCliques.size(); j++) {
				TeamClique presentClique = presentCliques.get(j);
				if (presentClique.conflictingCliques.contains(idleClique)) {
					info += "idle clique: " + idleClique + " conflicts with present clique: " + presentClique + "\n";
					ok = false;
				}
			}
		}

		// check forbidden list
		for (int i = 0; i < conflictCliques.size(); i++) {
			TeamClique forbiddenClique = conflictCliques.get(i);
			if (forbiddenClique.nConflictingCliquesInLeague <= 0) {
				info += "forbidden clique: " + forbiddenClique + ".nConflictingCliques=" + forbiddenClique.nConflictingCliquesInLeague + "\n";
				ok = false;
			}
			boolean hasConflicts = false;
			for (int j = 0; j < presentCliques.size(); j++) {
				TeamClique presentClique = presentCliques.get(j);
				if (presentClique.conflictingCliques.contains(forbiddenClique)) {
					hasConflicts = true;
					break;
				}
			}
			if (!hasConflicts) {
				info += "forbidden clique: " + forbiddenClique + " has no conflicts with any present clique" + "\n";
				ok = false;
			}
		}

		if (!ok) {
			System.err.println(info);
			System.err.println(toStateString());
		}

		return ok;
	}

	public boolean containsRealClique() {
		for (int i = 0; i < presentCliques.size(); i++) {
			if (presentCliques.get(i).getTeams().size() > 1) return true;
		}
		return false;
	}

	public boolean debugObjective() {
		boolean cliqueObjOk = true;
		ArrayList<Team> teams = new ArrayList<>();
		for (int i = 0; i < presentCliques.size(); i++) {
			TeamClique c = presentCliques.get(i);
			double cliqueObjNoGamma = 0;
			double cliqueGamma = 0;
			for (int j = 0; j < c.size(); j++) {
				PricingTeam teamA = c.getTeams().get(j);
				teams.add(teamA.team);
				cliqueGamma += cliqueManager.getGamma(teamA);
				for (int k = j + 1; k < c.size(); k++) {
					PricingTeam teamB = c.getTeams().get(k);
					cliqueObjNoGamma += 2 * teamA.getWeightedDistTimeTo(teamB);
				}
			}
			double cliqueObj = cliqueObjNoGamma - cliqueGamma;
			if (Math.abs(cliqueObjNoGamma - c.getObjectiveNoGamma()) >= Constants.EPS) {
				System.err.println("Clique ObjNoGamma: " + c.getObjectiveNoGamma() + ", should be: " + cliqueObjNoGamma);
				cliqueObjOk = false;
			}
			if (Math.abs(cliqueGamma - c.getGamma()) >= Constants.EPS) {
				System.err.println("Clique Gamma: " + c.getGamma() + ", should be: " + cliqueGamma);
				cliqueObjOk = false;
			}
			if (Math.abs(cliqueObj - c.getObjectiveInclGamma()) >= Constants.EPS) {
				System.err.println("Clique ObjInclGamma: " + c.getObjectiveInclGamma() + ", should be: " + cliqueObj);
				cliqueObjOk = false;
			}
		}
		double obj = 0;
		for (int i = 0; i < teams.size(); i++) {
			Team teamA = teams.get(i);
			obj -= cliqueManager.getGamma(teamA.id);
			for (int j = i + 1; j < teams.size(); j++) {
				Team teamB = teams.get(j);
				int time = teamA.getTravelTimeTo(teamB);
				int dist = teamA.getTravelDistTo(teamB);
				obj += toObjectiveDelta(time, dist);
			}
		}
		boolean objOk = (Math.abs(obj - objective)) < Constants.EPS;
		if (!objOk) {
			System.err.println("Cost: " + objective + ", should be: " + obj);
			System.err.println(toPresentString());
		}
		return cliqueObjOk && objOk;
	}

	public boolean debugSizeClubsLevelsTimeDist() {
		ArrayList<Team> teams = new ArrayList<>();
		for (int i = 0; i < presentCliques.size(); i++) {
			TeamClique c = presentCliques.get(i);
			for (PricingTeam team : c.getTeams()) {
				teams.add(team.team);
			}
		}
		String info = "";
		// size
		if (teams.isEmpty()) {
			return false;
		}
		boolean sizeOk = true;
		if (teams.size() < problem.minLeagueSize) {
			sizeOk = false;
			info += "size too small, ";
		}
		if (teams.size() > problem.maxLeagueSize) {
			sizeOk = false;
			info += "size too big, ";
		}

		// club, level, time, dist

		boolean clubsOk = true;
		boolean levelOk = true;
		boolean timeOk = true;
		boolean distOk = true;

		int[] fromSameClubCount = new int[problem.clubs.length];
		int minLevel = Integer.MAX_VALUE;
		int maxLevel = Integer.MIN_VALUE;

		for (int i = 0; i < teams.size(); i++) {
			Team t = teams.get(i);
			// club count
			fromSameClubCount[t.club.id]++;
			if (clubsOk && fromSameClubCount[t.club.id] > problem.maxTeamSameClub) {
				clubsOk = false;
				info += "too many teams from same club, ";
			}
			// levels
			minLevel = Math.min(minLevel, t.level);
			maxLevel = Math.max(maxLevel, t.level);
			if (levelOk && (maxLevel - minLevel) > problem.maxLevelDiff) {
				info += "too hig level diff, ";
			}
			// time, dist
			for (int j = i + 1; j < teams.size(); j++) {
				if (timeOk && t.getTravelTimeTo(t) > problem.maxTravelTime) {
					info += "time not ok, ";
					timeOk = false;
				}
				if (distOk && t.getTravelDistTo(t) > problem.maxTravelDist) {
					info += "dist not ok, ";
					distOk = false;
				}
			}
		}

		if (sizeOk && clubsOk && levelOk && timeOk && distOk) {
			return true;
		} else {
			System.out.println(info);
			return false;
		}
	}

	public boolean debugTeamsInSameLeague() {
		ArrayList<PricingTeam> teams = new ArrayList<>();
		HashSet<PricingTeam> teamsSet = new HashSet<>();
		for (int i = 0; i < presentCliques.size(); i++) {
			TeamClique c = presentCliques.get(i);
			for (PricingTeam team : c.getTeams()) {
				teamsSet.add(team);
				teams.add(team);
			}
		}

		boolean ok = true;
		String info = "";

		for (int i = 0; i < teams.size(); i++) {
			PricingTeam a = teams.get(i);
			// must be in same league
			for (PricingTeam b : a.clique.getTeams()) {
				if (!teamsSet.contains(b)) {
					info += "team " + a.team.id + " and " + b.team.id + " should be in same league, ";
					ok = false;
				}
			}

			// cant be in same league?
//			for(PricingTeam b : a.conflict.getTeams()){
//				if (b!=a && teamsSet.contains(b)) {
//					info += "team " + a.team.id + " and " + b.team.id + " can't be in same league, ";
//					ok = false;
//				}
//			}
		}
		if (!ok) System.err.println(info + "\n" + toStateString());

		return ok;
	}

	private double calcObjNoGamma(){
		double objNoGamma = objective;
		for(TeamClique tc : presentCliques){
			objNoGamma += tc.getGamma();
		}
		return objNoGamma;
	}

	public String toPresentTeamsString() {
		StringBuilder sb = new StringBuilder();
		ArrayList<PricingTeam> teams = new ArrayList<>();
		for(TeamClique tc : presentCliques) teams.addAll(tc.getTeams());
		sb.append("PresentTeams[");
		for (int i = 0; i < teams.size(); i++) {
			PricingTeam pt = teams.get(i);
			sb.append("\n\t\t").append(pt);
		}
		sb.append("\n\t]");
		return sb.toString();
	}

	public String toPresentDetailString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PresentCliques[");
		for (int i = 0; i < presentCliques.size(); i++) {
			TeamClique tc = presentCliques.get(i);
			sb.append("\n\t\t").append(tc);
		}
		sb.append("\n\t]");
		return sb.toString();
	}

	public String toPresentString() {
		return "PresentCliques" + TeamClique.toCliqueString(presentCliques);
	}

	public String toIdleString() {
		return "IdleCliques" + TeamClique.toCliqueString(idleCliques);
	}

	public String toForbiddenString() {
		return "ForbiddenCliques" + TeamClique.toCliqueString(conflictCliques);
	}

	public String toAllString() {
		return "League\n\t" + toPresentString() + "\n\t" + toIdleString() + "\n\t" + toForbiddenString();
	}

	public String toStateString() {
		return "----------- state -----------\n"
				+ toAllString() + "\n"
				+ cliqueManager + "\n"
				+ "-----------------------------";
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("League[obj=").append(objective).append(", objNoGamma=").append(calcObjNoGamma()).append("]");
		sb.append("\n\t").append(toPresentTeamsString());
//		sb.append("\n\t").append(toPresentString());
		sb.append("\n\t").append(toPresentDetailString());
		sb.append("\n\t").append(toIdleString());
		sb.append("\n\t").append(toForbiddenString());
		return sb.toString();
	}

}

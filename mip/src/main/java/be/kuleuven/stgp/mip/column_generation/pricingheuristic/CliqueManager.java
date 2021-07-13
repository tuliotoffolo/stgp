package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.util.*;

import java.util.*;

/**
 * Created by Jan on 1-3-2016.
 */
public class CliqueManager {

	private static final boolean DEBUG = false;

	private boolean solving = false;
	private final Problem problem;
	private final PricingTeam[] pricingTeams;
	private final ArrayList<TeamClique> teamCliques;
	private double[] gamma;

	private final double[][] cliqueTravelDistDelta;

	private HashMap<Integer, HashSet<Integer>> fixedTrueEdges = null;
	private HashMap<Integer, HashSet<Integer>> fixedFalseEdges = null;

	public CliqueManager(Problem problem) {
		this.problem = problem;
		this.pricingTeams = new PricingTeam[problem.teams.length];
		this.teamCliques = new ArrayList<>();
		this.cliqueTravelDistDelta = new double[problem.teams.length][problem.teams.length];

		for (int i = 0; i < problem.teams.length; i++) {
			PricingTeam pricingTeam = new PricingTeam(problem.teams[i]);
			pricingTeams[i] = pricingTeam;
			teamCliques.add(pricingTeam.clique);
		}

		if (DEBUG) {
			fixedTrueEdges = new HashMap<>();
			fixedFalseEdges = new HashMap<>();
		}

		initCliqueTravelTimeDeltas();
		fixPreprocessedConflicts();
	}

	public double getInterDistDelta(TeamClique tc1, TeamClique tc2) {
		return cliqueTravelDistDelta[tc1.id][tc2.id];
	}

	public double getGamma(int teamId) {
		return gamma[teamId];
	}

	public double getGamma(PricingTeam team) {
		return gamma[team.team.id];
	}

	public TeamClique getTeamClique(Team team) {
		return pricingTeams[team.id].clique;
	}

	public TeamClique getTeamClique(int teamId) {
		return pricingTeams[teamId].clique;
	}

	public ArrayList<TeamClique> getTeamCliques() {
		return teamCliques;
	}

	public void setSolving(boolean solving) {
		assert this.solving != solving;
		this.solving = solving;
	}

	public void fix(int i, int j, boolean value) {
		assert !solving;

		PricingTeam teamI = pricingTeams[i];
		PricingTeam teamJ = pricingTeams[j];

		if (DEBUG) {
			assert !(value && ((fixedFalseEdges.get(i) != null && fixedFalseEdges.get(i).contains(j)) || (fixedFalseEdges.get(j) != null && fixedFalseEdges.get(j).contains(i))));
			assert !(!value && ((fixedTrueEdges.get(i) != null && fixedTrueEdges.get(i).contains(j)) || (fixedTrueEdges.get(j) != null && fixedTrueEdges.get(j).contains(i))));
		}

		// don't set an already fixed value
//		assert teamI.clique != teamJ.clique : "called fix(" + i + "," + j + "," + value + ") while fix(" + i + "," + j + ",true) was already called";
//		assert teamI.conflict != teamJ.conflict : "called fix(" + i + "," + j + "," + value + ") while fix(" + i + "," + j + ",false) was already called";
		assert !(value && teamI.clique.conflictingCliques.contains(teamJ.clique)) : "called fix(" + i + "," + j + "," + value + ") while fix(" + i + "," + j + ",false) was already called";
		assert !(!value && teamI.clique == teamJ.clique) : "called fix(" + i + "," + j + "," + value + ") while fix(" + i + "," + j + ",true) was already called";
		assert TeamClique.debugIntraWeight(teamI.clique);
		assert TeamClique.debugIntraWeight(teamJ.clique);
		assert TeamClique.debugInterWeight(teamI.clique, teamJ.clique, getInterDistDelta(teamI.clique, teamJ.clique));

		if (value) {
			// null -> true
			if (teamI.clique != teamJ.clique) {
				TeamClique cliqueI = teamI.clique;
				TeamClique cliqueJ = teamJ.clique;
				// merge the presentCliques
				teamCliques.remove(cliqueJ);
				double interDistIJ = getInterDistDelta(cliqueI, cliqueJ);
				cliqueI.mergeClique(cliqueJ, interDistIJ);
				assert TeamClique.debugIntraWeight(cliqueI);
				// update cliqueTravelTimes
				for (int l = 0; l < teamCliques.size(); l++) {
					TeamClique clique = teamCliques.get(l);
					if (clique.id != cliqueI.id) {
						double edgeDelta = getInterDistDelta(cliqueJ, clique);
						assert TeamClique.debugInterWeight(cliqueJ, clique, edgeDelta);
						cliqueTravelDistDelta[cliqueI.id][clique.id] += edgeDelta;
						cliqueTravelDistDelta[clique.id][cliqueI.id] += edgeDelta;
						assert TeamClique.debugInterWeight(cliqueI, clique, getInterDistDelta(cliqueI, clique));
					}
				}
			}
			if (DEBUG) {
				HashSet<Integer> fixedIset = fixedTrueEdges.get(i);
				if (fixedIset == null) {
					fixedIset = new HashSet<>();
					fixedTrueEdges.put(i, fixedIset);
				}
				fixedIset.add(j);
			}
		} else {
			// null -> false
//			if (teamI.conflict != teamJ.conflict) {
//				// merge the conflictCliques
//				conflictCliques.get(division.id).remove(teamJ.conflict);
//				teamI.conflict.mergeClique(teamJ.conflict);
//			}
			teamI.clique.conflictingCliques.add(teamJ.clique);
			teamJ.clique.conflictingCliques.add(teamI.clique);
			if (DEBUG) {
				HashSet<Integer> fixedIset = fixedFalseEdges.get(i);
				if (fixedIset == null) {
					fixedIset = new HashSet<>();
					fixedFalseEdges.put(i, fixedIset);
				}
				fixedIset.add(j);
			}
		}

		if (DEBUG) System.out.println("fix(" + i + "," + j + "," + value + ")");

		assert debug();

	}

	public void updateGamma(double[] gamma) {
		assert solving;
		this.gamma = gamma;
		for (int j = 0; j < teamCliques.size(); j++) {
			teamCliques.get(j).updateGamma(gamma);
		}
	}

	public void clear() {
		// clique lists
		teamCliques.clear();
		// pricingTeams
		for (int i = 0; i < pricingTeams.length; i++) {
			PricingTeam pt = pricingTeams[i];
			pt.clear();
			teamCliques.add(pt.clique);
		}
		// debug fixed
		if (DEBUG) {
			fixedTrueEdges = new HashMap<>();
			fixedFalseEdges = new HashMap<>();
		}
		// preprocessed conflicts
		initCliqueTravelTimeDeltas();
		fixPreprocessedConflicts();
		solving = false;
	}

	private void fixPreprocessedConflicts() {
		for (Pair<Team, Team> pair : problem.getIncompatibleTeams()) {
			fix(pair.first.id, pair.second.id, false);
		}
	}

	private void initCliqueTravelTimeDeltas() {
		// init cliqueTravelTimeDeltas
		for (int i = 0; i < problem.teams.length - 1; i++) {
			for (int j = i + 1; j < problem.teams.length; j++) {
				cliqueTravelDistDelta[i][j] = 2 * problem.teams[i].getWeightedDistTimeTo(problem.teams[j]);
				cliqueTravelDistDelta[j][i] = cliqueTravelDistDelta[i][j];
				assert TeamClique.debugInterWeight(teamCliques.get(i), teamCliques.get(j), getInterDistDelta(teamCliques.get(i), teamCliques.get(j)));
			}
		}
	}

	public boolean debug() {

		boolean ok = true;

		// make sure that each team is included in only one teamClique
		HashSet<PricingTeam> teamsInTeamClique = new HashSet<>();
		for (TeamClique tc : teamCliques) {
			for (PricingTeam team : tc.getTeams()) {
				if (teamsInTeamClique.contains(team)) {
					System.err.println(team + " included in more than one TeamClique");
					ok = false;
				} else {
					teamsInTeamClique.add(team);
				}
				if (team.clique != tc) {
					System.err.println(tc + " includes " + team + " but this team points to " + team.clique);
					ok = false;
				}
			}
		}
//		for (ConflictClique cc : conflictCliquesArr) {
//			for (PricingTeam team : cc.getTeams()) {
//				if (teamsInConflictClique.contains(team)) {
//					System.err.println(team + " included in more than one ConflictClique");
//					ok = false;
//				} else {
//					teamsInConflictClique.add(team);
//				}
//				if (team.conflict != cc) {
//					System.err.println(cc + " includes " + team + " but this team points to " + team.conflict);
//					ok = false;
//				}
//			}
//		}

		// all teams are found in a team and conflict clique?
		if (teamsInTeamClique.size() != problem.teams.length) {
			System.err.println((problem.teams.length - teamsInTeamClique.size()) + " teams were not included in a TeamClique");
			ok = false;
		}
//		if (teamsInConflictClique.size() != problem.teams.length) {
//			System.err.println((problem.teams.length - teamsInConflictClique.size()) + " teams were not included in a ConflictClique");
//			ok = false;
//		}

//		// check the conflict list for every TeamClique
//		for (TeamClique tc : teamCliquesArr) {
//			HashSet<PricingTeam> conflictingTeams = new HashSet<>();
//			for (PricingTeam tcTeam : tc.getTeams()) {
//				conflictingTeams.addAll(tcTeam.conflict.getTeams());
//				// is the clique from this conflicting team included in the conflictCliques list?
//				for (PricingTeam conflictTeam : tcTeam.conflict.getTeams()) {
//					if (tcTeam != conflictTeam) {    // the team itself is included in this conflict clique, just ignore
//						if (!tc.conflictingCliques.contains(conflictTeam.clique)) {
//							System.err.println(tc + " includes " + tcTeam + " which has a conflict with " + conflictTeam + " but his clique " + conflictTeam.clique + " is not present in the conflictingTeamCliques list");
//							ok = false;
//						}
//					}
//				}
//			}
//			for (TeamClique conflicting : tc.conflictingCliques) {
//				// this conflicting teamClique should be in the full teamClique list
//				if (!teamCliquesArr.contains(conflicting)) {
//					System.err.println(tc + ".conflictingCliques includes " + conflicting + " which is not in the full teamClique list");
//					ok = false;
//				}
//			}
//		}

		if (!ok) {
			System.err.println(this);
		}

		return ok;
	}

	public void printReproduceStateCode(String cliqueManagerName){
		if(!DEBUG) throw new IllegalStateException("Can't print reproduce code when DEBUG=false");

		// print gamma
		StringBuilder sb = new StringBuilder();
		sb.append("double[] gamma={");
		for (int i = 0; i < gamma.length; i++) {
			if(i!=0) sb.append(", ");
			sb.append(gamma[i]);
		}
		sb.append("};");

		System.out.println(sb.toString());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CliqueManager[\n");
		sb.append("\tTeamCliques:    ");
		sb.append("\t").append(TeamClique.toCliqueString(teamCliques)).append("\n");
//		sb.append("\tConflictCliques:");
//		for (int i = 0; i < conflictCliques.size(); i++) {
//			ArrayList<ConflictClique> list = conflictCliques.get(i);
//			sb.append("\t").append(ConflictClique.toCliqueString(list)).append("\n");
//		}
		sb.append("]");
		return sb.toString();
	}
}

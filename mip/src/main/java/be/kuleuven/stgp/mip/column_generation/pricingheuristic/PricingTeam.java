package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import be.kuleuven.stgp.core.model.Team;

import java.util.ArrayList;

/**
 * Created by Jan on 1-3-2016.
 */
public class PricingTeam {

	public final Team team;
	public TeamClique clique;

	public PricingTeam(Team team) {
		this.team = team;
		this.clique = new TeamClique(this);
	}

	public void clear() {
		this.clique = new TeamClique(this);
	}

//	public int getTravelDistTo(PricingTeam team) {
//		return this.team.getTravelDistTo(team.team);
//	}
//
//	public int getTravelTimeTo(PricingTeam team) {
//		return this.team.getTravelTimeTo(team.team);
//	}

	public int getWeightedDistTimeTo(PricingTeam team) {
		return this.team.getWeightedDistTimeTo(team.team);
	}

	@Override
	public String toString() {
		return "PricingTeam[" +
				"id=" + team.id +
				", level=" + team.level +
				", club=" + team.club.id +
				']';
	}

	public static String toSimpleCliqueString(ArrayList<PricingTeam> teams) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int j = 0; j < teams.size(); j++) {
			PricingTeam pt = teams.get(j);
			if (j != 0) sb.append(",");
			sb.append(pt.team.id);
		}
		sb.append(")");
		return sb.toString();
	}
}

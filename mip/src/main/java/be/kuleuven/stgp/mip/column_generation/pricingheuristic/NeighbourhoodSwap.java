//package be.kuleuven.stgp.mip.column_generation.pricingheuristic;
//
//import be.kuleuven.stgp.core.model.Team;
//
//import java.util.Random;
//
///**
// * Created by Jan on 25-2-2016.
// */
//public class NeighbourhoodSwap extends Neighbourhood {
//
//	public NeighbourhoodSwap(Random RANDOM) {
//		super(RANDOM);
//	}
//
//	@Override
//	public Move getBestImprove(PricingLeague league) {
//
//		if (league.teams.size() < 2) return null;
//
//		SwapMove bestMove = new SwapMove();
//
//		for (int i = 0; i < league.teams.size(); i++) {
//			Team candidateToRemove = league.teams.get(i);
//			double removeDelta = league.deltaIfRemove(candidateToRemove);
//			league.removeTeam(candidateToRemove, removeDelta);
//
//			for (int j = 0; j < league.idleTeams.size(); j++) {
//				Team candidateToAdd = league.idleTeams.get(j);
//				if (candidateToRemove.id != candidateToAdd.id) {
//					if (league.canAddLevelClubTimeDistSize(candidateToAdd)) {
//						double addDelta = league.deltaIfAdd(candidateToAdd);
//						double delta = addDelta + removeDelta;
//						if (delta < bestMove.delta) {
//							bestMove.teamToRemove = candidateToRemove;
//							bestMove.teamToAdd = candidateToAdd;
//							bestMove.removeDelta = removeDelta;
//							bestMove.addDelta = addDelta;
//							bestMove.delta = delta;
//						}
//					}
//				}
//			}
//			double reAddDelta = -league.deltaIfAdd(candidateToRemove);
//			assert removeDelta == reAddDelta : removeDelta + "   " + reAddDelta;
//
//			league.addTeam(candidateToRemove, -removeDelta);
//		}
//
//		return bestMove.teamToRemove != null ? bestMove : null;
//	}
//
//	@Override
//	public Move getFirstImprove(PricingLeague league) {
////		if (league.teams.size() < 2) return null;
////
////		for (int i = 0; i < league.teams.size(); i++) {
////			Team candidateToRemove = league.teams.get(i);
////			double removeDelta = league.deltaIfRemove(candidateToRemove);
////			league.removeTeam(candidateToRemove, removeDelta);
////
////			for (int j = 0; j < league.idleTeams.size(); j++) {
////				Team candidateToAdd = league.idleTeams.get(j);
////				if (candidateToRemove.id != candidateToAdd.id) {
////					if (league.canAddLevelClubTimeDistSize(candidateToAdd)) {
////						double addDelta = league.deltaIfAdd(candidateToAdd);
////						double delta = addDelta + removeDelta;
////						if (delta < 0) {
////							league.addTeam(candidateToRemove, -removeDelta);
////							return new SwapMove(candidateToRemove, removeDelta, candidateToAdd, addDelta);
////						}
////					}
////				}
////			}
////			assert removeDelta == -league.deltaIfAdd(candidateToRemove) : removeDelta + "   " + (-league.deltaIfAdd(candidateToRemove));
////
////			league.addTeam(candidateToRemove, -removeDelta);
////		}
//
//		return null;
//	}
//
//	@Override
//	public Move getFirstFeasible(PricingLeague league) {
////		if (league.teams.size() < 2) return null;
////
////		for (int i = 0; i < league.teams.size(); i++) {
////			Team candidateToRemove = league.teams.get(i);
////			double removeDelta = league.deltaIfRemove(candidateToRemove);
////			league.removeTeam(candidateToRemove, removeDelta);
////
////			for (int j = 0; j < league.idleTeams.size(); j++) {
////				Team candidateToAdd = league.idleTeams.get(j);
////				if (candidateToRemove.id != candidateToAdd.id) {
////					if (league.canAddLevelClubTimeDistSize(candidateToAdd)) {
////						double addDelta = league.deltaIfAdd(candidateToAdd);
////						double delta = addDelta + removeDelta;
////						league.addTeam(candidateToRemove, -removeDelta);
////						return new SwapMove(candidateToRemove, removeDelta, candidateToAdd, addDelta);
////					}
////				}
////			}
////			assert removeDelta == -league.deltaIfAdd(candidateToRemove) : removeDelta + "   " + (-league.deltaIfAdd(candidateToRemove));
////
////			league.addTeam(candidateToRemove, -removeDelta);
////		}
//
//		return null;
//	}
//
//	public class SwapMove extends Move {
//
//		private Team teamToRemove;
//		private Team teamToAdd;
//		private double removeDelta;
//		private double addDelta;
//		private double delta;
//
//		public SwapMove() {
//			clear();
//		}
//
//		public SwapMove(Team teamToRemove, double removeDelta, Team teamToAdd, double addDelta) {
//			this.teamToRemove = teamToRemove;
//			this.teamToAdd = teamToAdd;
//			this.removeDelta = removeDelta;
//			this.addDelta = addDelta;
//			this.delta = addDelta + removeDelta;
//		}
//
//		@Override
//		public void execute(PricingLeague league) {
//			league.removeTeam(teamToRemove, removeDelta);
//			league.addTeam(teamToAdd, addDelta);
//		}
//
//		@Override
//		public double getDelta() {
//			return delta;
//		}
//
//		public void clear() {
//			teamToRemove = null;
//			teamToAdd = null;
//			delta = Double.POSITIVE_INFINITY;
//		}
//	}
//}

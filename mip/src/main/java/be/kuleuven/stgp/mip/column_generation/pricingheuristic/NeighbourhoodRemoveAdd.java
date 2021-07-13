package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import java.util.Random;

/**
 * Created by Jan on 10-3-2016.
 */
public class NeighbourhoodRemoveAdd extends Neighbourhood {

	public NeighbourhoodRemoveAdd(Random RANDOM) {
		super(RANDOM);
	}

	@Override
	public Move getBestImprove(PricingLeague league, double accept) {

		if (league.presentCliques.size() < 2) return null;

		RemoveAddMove bestMove = new RemoveAddMove();

		// best accept
		for (int i = 0; i < league.presentCliques.size(); i++) {
			TeamClique candidate = league.presentCliques.get(i);
			double delta = league.deltaIfRemove(candidate);
			if (delta < bestMove.removeDelta && RANDOM.nextDouble() < accept) {
				bestMove.removeClique = candidate;
				bestMove.removeDelta = delta;
			}
		}
		if (bestMove.removeClique == null) return null;
		league.removeClique(bestMove.removeClique, bestMove.removeDelta);

		// first accept
//		TeamClique removeCandidate = league.presentCliques.get(RANDOM.nextInt(league.presentCliques.size()));
//		bestMove.removeClique = removeCandidate;
//		bestMove.removeDelta = league.deltaIfRemove(removeCandidate);
//		if (bestMove.removeClique == null) return null;
//		league.removeClique(bestMove.removeClique, bestMove.removeDelta);

		// best accept
		for (int i = 0; i < league.idleCliques.size(); i++) {
			TeamClique candidate = league.idleCliques.get(i);
			if (candidate != bestMove.removeClique && league.canAddLevelClubTimeDistSize(candidate)) {
				double delta = league.deltaIfAdd(candidate);
				if (delta < bestMove.addDelta && RANDOM.nextDouble() < accept) {
					bestMove.addClique = candidate;
					bestMove.addDelta = delta;
					bestMove.delta = bestMove.removeDelta + bestMove.addDelta;
				}
			}
		}

		league.addClique(bestMove.removeClique, -bestMove.removeDelta);

		return bestMove.addClique != null ? bestMove : null;
	}

	@Override
	public Move getFirstImprove(PricingLeague league) {
		return null;
	}

	@Override
	public Move getFirstFeasible(PricingLeague league) {
		return null;
	}

	public class RemoveAddMove extends Move {

		private TeamClique removeClique;
		private TeamClique addClique;
		private double removeDelta;
		private double addDelta;
		private double delta;

		public RemoveAddMove() {
			clear();
		}

		@Override
		public void execute(PricingLeague league) {
			league.removeClique(removeClique, removeDelta);
			league.addClique(addClique, addDelta);
		}

		@Override
		public double getDelta() {
			return delta;
		}

		public void clear() {
			removeClique = null;
			addClique = null;
			delta = Double.POSITIVE_INFINITY;
		}
	}
}

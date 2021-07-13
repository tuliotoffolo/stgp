package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import java.util.Collections;
import java.util.Random;

/**
 * Created by Jan on 25-2-2016.
 */
public class NeighbourhoodRemove extends Neighbourhood {

	public NeighbourhoodRemove(Random RANDOM) {
		super(RANDOM);
	}

	@Override
	public Move getBestImprove(PricingLeague league, double accept) {

		if (!league.canRemoveSize(1)) return null;

		RemoveMove bestMove = new RemoveMove();

		for (int i = 0; i < league.presentCliques.size(); i++) {
			TeamClique candidate = league.presentCliques.get(i);
			if (league.canRemoveSize(candidate.size())) {
				double delta = league.deltaIfRemove(candidate);
				if (delta < bestMove.delta && RANDOM.nextDouble() < accept) {
					bestMove.clique = candidate;
					bestMove.delta = delta;
				}
			}
		}

		return bestMove.clique != null ? bestMove : null;
	}

	//	@Override
	public Move getFirstImprove(PricingLeague league) {
		if (!league.canRemoveSize(1)) return null;

		Collections.shuffle(league.presentCliques, RANDOM);

		for (int i = 0; i < league.presentCliques.size(); i++) {
			TeamClique candidate = league.presentCliques.get(i);
			double delta = league.deltaIfRemove(candidate);
			if (delta < 0) {
				return new RemoveMove(candidate, delta);
			}
		}

		return null;
	}

	//	@Override
	public Move getFirstFeasible(PricingLeague league) {
		if (!league.canRemoveSize(1)) return null;

		Collections.shuffle(league.presentCliques, RANDOM);

		for (int i = 0; i < league.presentCliques.size(); i++) {
			TeamClique candidate = league.presentCliques.get(i);
			double delta = league.deltaIfRemove(candidate);
			return new RemoveMove(candidate, delta);
		}

		return null;
	}

	public class RemoveMove extends Move {

		private TeamClique clique;
		private double delta;

		public RemoveMove() {
			clear();
		}

		public RemoveMove(TeamClique clique, double delta) {
			this.clique = clique;
			this.delta = delta;
		}

		@Override
		public void execute(PricingLeague league) {
			league.removeClique(clique, delta);
		}

		@Override
		public double getDelta() {
			return delta;
		}

		public void clear() {
			clique = null;
			delta = Double.POSITIVE_INFINITY;
		}
	}
}

package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import java.util.Collections;
import java.util.Random;

/**
 * Created by Jan on 25-2-2016.
 */
public class NeighbourhoodAdd extends Neighbourhood {

	public NeighbourhoodAdd(Random RANDOM) {
		super(RANDOM);
	}

	@Override
	public Move getBestImprove(PricingLeague league, double accept) {

		if (!league.canAddSize(1)) return null;

		AddMove bestMove = new AddMove();

		for (int i = 0; i < league.idleCliques.size(); i++) {
			TeamClique candidate = league.idleCliques.get(i);
			if (league.canAddLevelClubTimeDistSize(candidate)) {
				double delta = league.deltaIfAdd(candidate);
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
		if (!league.canAddSize(1)) return null;

		Collections.shuffle(league.idleCliques, RANDOM);

		for (int i = 0; i < league.idleCliques.size(); i++) {
			TeamClique candidate = league.idleCliques.get(i);
			if (league.canAddLevelClubTimeDistSize(candidate)) {
				double delta = league.deltaIfAdd(candidate);
				if (delta < 0) {
					return new AddMove(candidate, delta);
				}
			}
		}

		return null;
	}

	//	@Override
	public Move getFirstFeasible(PricingLeague league) {
		if (!league.canAddSize(1)) return null;

		Collections.shuffle(league.idleCliques, RANDOM);

		for (int i = 0; i < league.idleCliques.size(); i++) {
			TeamClique candidate = league.idleCliques.get(i);
			if (league.canAddLevelClubTimeDistSize(candidate)) {
				double delta = league.deltaIfAdd(candidate);
				return new AddMove(candidate, delta);
			}
		}

		return null;
	}

	public class AddMove extends Move {

		private TeamClique clique;
		private double delta;

		public AddMove() {
			clear();
		}

		public AddMove(TeamClique clique, double delta) {
			this.clique = clique;
			this.delta = delta;
		}

		@Override
		public void execute(PricingLeague league) {
			league.addClique(clique, delta);
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

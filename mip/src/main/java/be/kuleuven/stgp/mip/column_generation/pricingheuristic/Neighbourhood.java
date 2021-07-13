package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import be.kuleuven.stgp.core.model.Problem;

import java.util.Random;

/**
 * Created by Jan on 25-2-2016.
 */
public abstract class Neighbourhood {

	protected final Random RANDOM;

	public Neighbourhood(Random RANDOM) {
		this.RANDOM = RANDOM;
	}

	public abstract Move getBestImprove(PricingLeague league, double accept);
	public abstract Move getFirstImprove(PricingLeague league);
	public abstract Move getFirstFeasible(PricingLeague league);

}

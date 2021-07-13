package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

/**
 * Created by Jan on 25-2-2016.
 */
public abstract class Move {

	public abstract void execute(PricingLeague league);
	public abstract double getDelta();

}

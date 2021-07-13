package be.kuleuven.stgp.heuristic.move;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.heuristic.model.BestFit;
import be.kuleuven.stgp.heuristic.model.MtSolution;

import java.util.Random;

/**
 * Created by Jan on 10-12-2015.
 */
public abstract class Move {

	protected final Problem PROBLEM;
	protected final Random RANDOM;
	protected final BestFit BESTFIT;

	protected Move(Problem problem, Random random){
		this.PROBLEM = problem;
		this.RANDOM = random;
		this.BESTFIT = new BestFit(problem, random);
	}

	public abstract MtSolution getNeighbour(MtSolution solution);

}

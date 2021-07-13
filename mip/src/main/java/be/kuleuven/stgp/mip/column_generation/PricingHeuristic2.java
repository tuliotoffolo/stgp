package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.stgp.core.model.solution.*;

import java.util.*;

/**
 * This inteface represents the Pricing problem of the Column Generation.
 *
 * @author Tulio Toffolo
 */
public interface PricingHeuristic2 {

    /**
     * Gets the solutions (leagues) generated for the pricing problem (all
     * guaranteed to have negative reduced cost).
     *
     * @return list with the solutions (leagues) generated for the pricing
     * problem (all guaranteed to have negative reduced cost).
     */
    List<League> getLeagues();

    /**
     * Gets the costs of the solutions generated for the pricing problem (all
     * guaranteed to be negative). Important: the size of the returned list must
     * coincide with the size of the list returned by getLeagues().
     *
     * @return list with the costs of the solutions generated for the pricing
     * problem (all guaranteed to be negative).
     */
    List<Double> getLeaguesCosts();

    /**
     * Solves the pricing problem. The parameter indicates the pricing problem
     * to be solved (including dual costs, etc.).
     *
     * @param problem array with dual values.
     * @param nThreads maximum number of threads that the method may use.
     * @return true if a reduced cost column is obtained and false otherwise.
     */
    boolean solve(PricingProblem problem, int nThreads);
}

package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.stgp.core.model.solution.*;

import java.util.*;

/**
 * This inteface represents the Pricing problem of the Column Generation.
 *
 * @author Tulio Toffolo
 */
public interface PricingHeuristic {

    /**
     * Fixes variable x(i,j) to value (either 0 or 1).
     * <p>
     * A fixation in this context means either: (1) teams i and j must be in the
     * <b>same league</b> or (2) teams i and j must be in <b>different
     * leagues</b>.
     *
     * @param i     index of first team
     * @param j     index of second team
     * @param value true if teams i and j must be in the <b>same league</b> and
     *              false if teams i and j must be in <b>different leagues</b>.
     */
    void fix(int i, int j, boolean value);

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
     * Solves the pricing problem. The parameter indicates the dual costs
     * associated to each team, i.e. an array gamma[number_teams] where each
     * position indicates the dual cost of selecting one team.
     *
     * @param gamma  array with dual values.
     * @param cutoff the maximum allowed objective value of a pricing solution
     *               that is a reduced cost column.
     * @return true if a reduced cost column is obtained and false otherwise.
     */
    boolean solve(double gamma[], double cutoff);

    /**
     * Solves the pricing problem. The parameter indicates the dual costs
     * associated to each team, i.e. an array gamma[number_teams] where each
     * position indicates the dual cost of selecting one team. Additionally,
     * this method allows the user
     *
     * @param gamma    array with dual values.
     * @param nThreads maximum number of threads that the method may use.
     * @return true if a reduced cost column is obtained and false otherwise.
     */
    //boolean solve(double gamma[], int nThreads);

    /**
     * Resets the state of the pricing problem, removing all fixations.
     */
    void reset();
}

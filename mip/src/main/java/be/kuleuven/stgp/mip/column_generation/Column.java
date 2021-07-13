package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.codes.mp.*;
import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.mip.util.*;

/**
 * This class represents a Column within the Column Generation algorithm.
 *
 * @author Tulio Toffolo
 */
public class Column {

    public final League league;

    private MPVar var;
    private int iter;
    private double objective;
    private double value;

    /**
     * Instantiates a new Column.
     *
     * @param league    the league related to this column (or variable)
     * @param objective the objective value for the pricing problem
     */
    public Column(League league, double objective) {
        this.league = league;
        this.objective = objective;
    }

    /**
     * Instantiates a new Column.
     *
     * @param problem   the problem
     * @param teams     array  teams
     * @param objective the objective
     */
    public Column(Problem problem, boolean teams[], double objective) {
        this.league = new League(problem);
        this.objective = objective;

        // importing selected teams
        for (int i = 0; i < teams.length; i++)
            if (teams[i])
                league.add(problem.teams[i]);

        //assert league.validate(System.err) : "oops, invalid league";
    }

    /**
     * Private constructor used for cloning.
     *
     * @param column the column to be cloned.
     */
    private Column(Column column) {
        this.league = column.league;

        this.var = null;
        this.iter = column.iter;
        this.objective = column.objective;
        this.value = column.value;
    }

    @Override
    public Column clone() {
        return new Column(this);
    }

    /**
     * Checks if a team is include in this column.
     *
     * @param team team to be checked
     * @return true if team is in this column and false otherwise
     */
    public boolean contains(Team team) {
        return league.contains(team);
    }

    /**
     * Checks if a team is include in this column.
     *
     * @param teamId id of the team to be checked
     * @return true if team is in this column and false otherwise
     */
    public boolean contains(int teamId) {
        return league.contains(teamId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Column column = ( Column ) o;
        return league.equals(column.league);
    }

    /**
     * Gets iter.
     *
     * @return the iter
     */
    public int getIter() {
        return iter;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * Gets var.
     *
     * @return the var
     */
    public MPVar getVar() {
        return var;
    }

    @Override
    public int hashCode() {
        return league != null ? league.hashCode() : 0;
    }

    /**
     * Is valid boolean.
     *
     * @param duals the lambdas
     * @return the boolean
     */
    public boolean isValid(BranchNode node, double[] duals) {
        //assert league.validate(System.err) : "invalid league...";

        // checking that column has negative reduced cost
        double objective = league.getObjective();
        for (Team team : league)
            objective -= duals[team.id];
        assert Math.abs(objective - this.objective) < Constants.EPS : "invalid objective value";

        // checking that column is feasible considering the fixations
        for (PairInt pair : node.fixedZero)
            assert !(contains(pair.getFirst()) && contains(pair.getSecond())) : String.format("column is invalid: teams %d and %d should not be together", pair.getFirst(), pair.getSecond());
        for (PairInt pair : node.fixedOne)
            assert contains(pair.getFirst()) == contains(pair.getSecond()) : String.format("column is invalid: teams %d and %d should be either together", pair.getFirst(), pair.getSecond());

        return objective < -Constants.EPS;
    }

    /**
     * Sets iter.
     *
     * @param iter the iter
     */
    public void setIter(int iter) {
        this.iter = iter;
    }

    /**
     * Sets value.
     *
     * @param value the value
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Sets var.
     *
     * @param var the var
     */
    public void setVar(MPVar var) {
        this.var = var;
    }
}

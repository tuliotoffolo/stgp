package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.codes.mp.*;
import be.kuleuven.codes.mp.solvers.*;
import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.mip.column_generation.pricingheuristic.*;
import be.kuleuven.stgp.mip.util.*;
import ilog.cplex.*;

import java.util.*;
import java.util.LinkedList;

/**
 * This class is responsible for representing and solving the Pricing problem of
 * the Column Generation.
 *
 * @author Tulio Toffolo
 */
public class Pricing {

    public final int id;
    public final Problem problem;

    private MPModel model;
    private MPSolver solver = null;
    private MPObjective objective;

    /***
     * MPVars X, Y, with x[teams] and y[teams][teams]
     */
    private MPVar x[], y[][];

    private List<MPLinConstr> constrs = new ArrayList<>();

    private List<MPLinConstr> fixedConstrs = new LinkedList<>();
    private List<MPVar> fixedVars = new LinkedList<>();

    private long iters, lastImprovingIter;

    private PricingHeuristic heuristic = null;
    private boolean heuristicSolution = false;

    private int heuristicCounter = 0, mipCounter = 0;

    /**
     * Instantiates a new Pricing Problem.
     *
     * @param id      the id of the pricing problem
     * @param problem the problem
     */
    public Pricing(int id, Problem problem) {
        this.id = id;
        this.problem = problem;
        //this.heuristic = Jan: create here your PricingHeuristic, example: new PricingHeuristic(problem, this);
        this.heuristic = new PricingHeuristicImpl(problem);

        model = new MPModel(MPObjective.MINIMIZE, problem.name);

        createMPVars();
        createConstraints();
        createValidInequalities();

        objective = model.getObjective();
    }

    /***
     * Builds and returns the list of negative reduced gammaCost obtained in the
     * last solve. (an empty list if such column does not exist)
     *
     * @return list with negative reduced gammaCost columns obtained in the last
     * (empty list if such column does not exist)
     */
    public List<Column> buildColumns() {
        List<Column> columns = new ArrayList<>();

        if (heuristicSolution) {
            int nColumns = Math.min(Parameters.get().populateLimit, heuristic.getLeagues().size());
            for (int l = 0; l < nColumns; l++) {
                columns.add(new Column(heuristic.getLeagues().get(l), heuristic.getLeaguesCosts().get(l)));
            }
        }

        else {
            int nColumns = Math.min(Parameters.get().populateLimit, solver.getSolutions().size());
            for (int s = 0; s < nColumns; s++) {
                // creating alpha array
                boolean alpha[] = new boolean[problem.teams.length];

                for (int i = 0; i < x.length; i++)
                    if (Math.round(solver.getValue(x[i], s)) == 1.0)
                        alpha[i] = true;

                // getting objective
                double obj = solver.getObjValue(s);

                columns.add(new Column(problem, alpha, obj));
            }
        }

        return columns;
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object.
     */
    public Pricing clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Gets objective value of the last solve
     *
     * @return the objective value of the last solve
     */
    public Double getObjValue() {
        return heuristicSolution && heuristic != null
          ? heuristic.getLeaguesCosts().get(0)
          : solver != null ? solver.getObjValue() : Double.MAX_VALUE;
    }

    /**
     * Checks if current pricing has a heuristic solution.
     *
     * @return true if the solutions were originated by the heuristic and false
     * otherwise
     */
    public boolean hasHeuristicSolution() {
        return heuristicSolution;
    }

    /***
     * Updates the objective function and runs the solver to get a new
     * solutionStr.
     *
     * @param gamma      the dual value of the partition constraint in the
     *                   master problem
     * @param maxThreads the max threads
     * @return true in case of success and 0 otherwise
     */
    public boolean solve(long finishTimeMillis, double gamma[], double cutoff, int maxThreads) {
        if (heuristic != null && (heuristicSolution = heuristic.solve(gamma, cutoff))) {
            heuristicCounter++;
            return true;
        }

        mipCounter++;

        if (Parameters.get().limitPricingIters) {
            iters = 0;
            lastImprovingIter = Long.MAX_VALUE - Parameters.get().maxItersPricing;
        }

        // updating objective
        objective.clear();

        // adding x_{i} variables to objective function
        for (int i = 0; i < problem.teams.length; i++) {
            objective.addTerm(-gamma[i], x[i]);
        }

        // adding y_{ij} variables to objective function
        for (int i = 0; i < problem.teams.length; i++) {
            Team teamI = problem.teams[i];

            for (int j = i + 1; j < problem.teams.length; j++) {
                Team teamJ = problem.teams[j];

                if (Math.abs(teamI.level - teamJ.level) <= problem.maxLevelDiff
                  && teamI.getTravelDistTo(teamJ) <= problem.maxTravelDist
                  && teamI.getTravelTimeTo(teamJ) <= problem.maxTravelTime) {
                    objective.addTerm(2 * teamI.getWeightedDistTimeTo(teamJ), y[i][j]);
                }
            }
        }

        if (solver == null) {
            //solver = new SolverCplex(model, false);
            solver = new SolverGurobi(model, false);
            model.setSolver(solver);
        }
        else {
            model.updateSolver();
        }

        // solver options
        //solver.setParam(MPSolver.BooleanParam.NumericalEmphasis, true);
        solver.setParam(MPSolver.DoubleParam.CutUp, cutoff - Constants.EPS);
        solver.setParam(MPSolver.IntParam.Threads, Math.max(1, maxThreads));
        solver.setParam(MPSolver.DoubleParam.TimeLimit, Math.max(1, finishTimeMillis - System.currentTimeMillis()) / 1000.0);
        solver.setParam(MPSolver.DoubleParam.MIPGap, .5);
        //solver.writeModel("pricing.lp");

        if (Parameters.get().populate) {
            solver.setParam(MPSolver.IntParam.PopulateLim, Parameters.get().populateLimit);
            return solver.populate();
        }
        else {
            return solver.solve();
        }
    }

    /**
     * Fixes a variable.
     *
     * @param i     the first team's id
     * @param j     the second team's id
     * @param value the value (1 if both teams must be value and 0 if they must
     *              be assigned to different leagues)
     */
    public void fix(int i, int j, boolean value) {
        if (heuristic != null) heuristic.fix(i, j, value);

        if (!value) {
            y[i][j].setBounds(0.0, 0.0);
            fixedVars.add(y[i][j]);
        }
        else {
            MPLinExpr lhs = new MPLinExpr();
            lhs.addTerm(1, x[i]);
            lhs.addTerm(-1, x[j]);
            fixedConstrs.add(model.addEq(lhs, 0, String.format("fix_%d_%d", i, j)));
        }
    }

    /**
     * Resets the pricing, i.e. removes all fixations.
     */
    public void reset() {
        for (MPVar var : fixedVars)
            var.setBounds(0.0, 1.0);
        for (MPLinConstr constr : fixedConstrs)
            model.delete(constr);

        fixedVars.clear();
        fixedConstrs.clear();
        if (heuristic != null) heuristic.reset();
        heuristicSolution = false;
    }

    /**
     * Creates the variables of the pricing problem.
     */
    private void createMPVars() {
        // creating x_{il} variables
        x = new MPVar[problem.teams.length];
        for (int i = 0; i < problem.teams.length; i++) {
            x[i] = model.addBinVar(String.format("x(%d)", i));
        }

        // creating y_{ij} variables
        y = new MPVar[problem.teams.length][problem.teams.length];
        for (int i = 0; i < problem.teams.length; i++) {
            Team teamI = problem.teams[i];

            for (int j = i + 1; j < problem.teams.length; j++) {
                Team teamJ = problem.teams[j];

                if (Math.abs(teamI.level - teamJ.level) <= problem.maxLevelDiff
                  && teamI.getTravelDistTo(teamJ) <= problem.maxTravelDist
                  && teamI.getTravelTimeTo(teamJ) <= problem.maxTravelTime) {
                    y[i][j] = model.addBinVar(String.format("y(%d,%d)", i, j));
                }
                else {
                    y[i][j] = model.addIntVar(0, 0, String.format("y(%d,%d)", i, j));
                }
            }
        }
    }

    /**
     * Creates the constraints of the pricing problem.
     */
    private void createConstraints() {
        // minimum and maximum number of teams per league
        {
            MPLinExpr lhs = new MPLinExpr();
            for (int i = 0; i < problem.teams.length; i++)
                lhs.addTerm(1., x[i]);
            model.addGe(lhs, problem.minLeagueSize, "min_league_size");
            model.addLe(lhs, problem.maxLeagueSize, "max_league_size");
        }

        // maximum number of teams per league
        for (Club club : problem.clubs) {
            if (club.teams.size() > problem.maxTeamSameClub) {
                MPLinExpr lhs = new MPLinExpr();
                for (Team team : club.teams) {
                    lhs.addTerm(1., x[team.id]);
                }
                model.addLe(lhs, problem.maxTeamSameClub, String.format("max_teams_per_club(%d)", club.id));
            }
        }

        // activation of variables *Y*
        for (int i = 0; i < problem.teams.length; i++) {
            for (int j = i + 1; j < problem.teams.length; j++) {
                MPLinExpr lhs = new MPLinExpr();
                lhs.addTerm(1., x[i]);
                lhs.addTerm(1., x[j]);
                lhs.addTerm(-1., y[i][j]);

                model.addLe(lhs, 1, String.format("y_activation(%d,%d)", i, j));
            }
        }
    }

    /**
     * Creates and adds additional constraints (valid inequalities) for the
     * pricing problem.
     */
    private void createValidInequalities() {
        // *Y* and *X* direct coupling
        {
            MPLinExpr lhs = new MPLinExpr();
            for (int i = 0; i < problem.teams.length; i++) {
                for (int j = i + 1; j < problem.teams.length; j++) {
                    lhs.addTerm(1.0, y[i][j]);
                }
            }
            model.addGe(lhs, (problem.minLeagueSize - 1) * problem.minLeagueSize / 2, String.format("min_league_size_cut"));
        }

        //// minimum and maximum number of teams per league (cut using *Y* variables)
        for (Team team : problem.teams) {
            MPLinExpr lhs = new MPLinExpr();
            for (int i = 0; i < team.id; i++)
                lhs.addTerm(1., y[i][team.id]);
            for (int j = team.id + 1; j < problem.teams.length; j++)
                lhs.addTerm(1., y[team.id][j]);

            MPLinExpr min = new MPLinExpr();
            min.addTerm(problem.minLeagueSize - 1, x[team.id]);

            MPLinExpr max = new MPLinExpr();
            max.addTerm(problem.maxLeagueSize - 1, x[team.id]);

            model.addGe(lhs, min, String.format("min_league_size_cut(%d)", team.id));
            model.addLe(lhs, max, String.format("max_league_size_cut(%d)", team.id));
        }

        // y_{ij} + y_{jk} < y_{ik} + 1
        //for (int i = 0; i < problem.teams.length; i++) {
        //    for (int j = i + 1; j < problem.teams.length; j++) {
        //        for (int k = j + 1; k < problem.teams.length; k++) {
        //            IloLinearNumExpr lhs = cplex.linearNumExpr();
        //            lhs.addTerm(1., y[i][j]);
        //            lhs.addTerm(1., y[j][k]);
        //            lhs.addTerm(-1., y[i][k]);
        //            cplex.addLe(lhs, 1, String.format("y_triangle(%d,%d,%d)", i, j, k));
        //        }
        //    }
        //}
    }


    /**
     * The class Node callback.
     *
     * @author Tulio Toffolo
     */
    static class NodeCallback extends IloCplex.NodeCallback {

        Pricing pricing;

        /**
         * Instantiates a new Node callback.
         *
         * @param pricing the pricing
         */
        public NodeCallback(Pricing pricing) {
            this.pricing = pricing;
        }

        public void main() {
            pricing.iters++;
            if (pricing.lastImprovingIter + Parameters.get().itersColumnLife < pricing.iters)
                abort();
        }
    }

    /**
     * The class Incumbent callback.
     *
     * @author Tulio Toffolo
     */
    static class IncumbentCallback extends IloCplex.IncumbentCallback {

        Pricing pricing;

        /**
         * Instantiates a new Incumbent callback.
         *
         * @param pricing the pricing
         */
        public IncumbentCallback(Pricing pricing) {
            this.pricing = pricing;
        }

        public void main() {
            pricing.lastImprovingIter = pricing.iters;
        }
    }
}

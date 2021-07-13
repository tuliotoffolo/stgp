package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.codes.mp.*;
import be.kuleuven.codes.mp.solvers.*;
import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.core.util.*;
import be.kuleuven.stgp.mip.util.*;
import ilog.concert.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * This class implements the Column Generation algorithm.
 *
 * @author Tulio Toffolo
 */
public class ColumnGeneration {

    public static AtomicLong masterRuntime = new AtomicLong(0);

    public final Problem problem;
    public final MPModel model;

    public BranchNode currentNode;

    private MPSolver solver = null;
    private Pricing pricing;

    private int varIters = 0;
    private MPObjective objective;

    private MPVar y;
    private List<MPVar> artificials = new ArrayList<>();
    private List<Column> lambdas = new LinkedList<>();

    private boolean hasSolution = false;
    private double objValue = Double.MAX_VALUE;

    int lambdaCount = 0;

    /***
     * lambdaConstrs[team]
     */
    private MPLinConstr[] lambdaConstrs;
    private MPLinConstr maxLambdasConstr;
    private double dualConstant;

    /**
     * Instantiates a new Column generation.
     *
     * @param problem the problem
     */
    public ColumnGeneration(Problem problem) {
        this.problem = problem;
        this.pricing = new Pricing(0, problem);

        model = new MPModel(MPObjective.MINIMIZE, problem.name);

        createMPVars();
        createObjective();
        createConstraints();
    }

    /**
     * Instantiates a new Column generation.
     *
     * @param problem         the problem
     * @param initialSolution the initial solution
     */
    public ColumnGeneration(Problem problem, Solution initialSolution) {
        this(problem);

        // adding initial solution to formulation
        if (initialSolution != null)
            buildColumnsFromSolution(initialSolution);
    }

    /**
     * Instantiates a new Column generation.
     *
     * @param columnGeneration the column generation
     */
    public ColumnGeneration(ColumnGeneration columnGeneration) {
        this.problem = columnGeneration.problem;
        this.pricing = new Pricing(0, problem); // TODO: use a clone() instead of reusing the same object

        model = new MPModel(MPObjective.MINIMIZE, problem.name);

        createMPVars();
        createObjective();
        createConstraints();

        // copying columns to new column generation
        for (Column lambda : columnGeneration.lambdas)
            addLambda(lambda.clone());
    }

    /***
     * Adds a lambda (column) to the master problem.
     *
     * @param lambda Lambda (column) to be added to the master problem (*pos* in
     *               the formulation).
     */
    public void addLambda(Column lambda) {
        hasSolution = false;
        MPColumn col = new MPColumn(lambda.league.getObjective());

        // updating lambda constraints
        for (Team team : lambda.league)
            col.add(lambdaConstrs[team.id], 1.0);
        col.add(maxLambdasConstr, 1.0);

        MPVar var = model.addNumVar(0.0, 1.0, col, "lambda(%d)", lambdaCount++);
        lambda.setVar(var);
        lambda.setIter(varIters);
        lambdas.add(lambda);
    }

    /***
     * Build lambda (columns) from initial solution
     *
     * @param solution initial solution
     * @return the resulting list of columns
     */
    public void buildColumnsFromSolution(Solution solution) {
        for (League league : solution)
            addLambda(new Column(league, 0.0));
    }

    /**
     * @return
     */
    public ColumnGeneration clone() {
        return new ColumnGeneration(this);
    }

    /**
     * Returns the columns encapsulated in an unmodifiable list.
     *
     * @return the columns
     */
    public List<Column> getColumns() {
        return Collections.unmodifiableList(lambdas);
    }

    public double getDualConstant() {
        assert hasSolution;
        double[] duals = solver.getDuals(new MPConstr[]{ maxLambdasConstr });
        return duals != null ? duals[0] : 0;
    }

    /***
     * Produces (and returns) an array with the dual values for the lambda_y
     * constraints: duals[projects]
     *
     * @return an array duals[projects] with the dual values for the lambda_y
     * constraints.
     */
    public double[] getDualLambdas() {
        assert hasSolution;
        return solver.getDuals(lambdaConstrs);
    }

    /***
     * Gets the objective value of the last solve.
     *
     * @return the objective value of the last solve.
     * @throws IloException
     */
    public double getObjValue() {
        return objValue;
    }

    /**
     * Returns the pricing solver.
     *
     * @return the pricing solver.
     */
    public Pricing getPricing() {
        return pricing;
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
        pricing.fix(i, j, value);
    }

    /***
     * Checks if the artificial variables are not in the basis; if so, remove
     * the variables and return false. Otherwise, trueis returned.
     *
     * @return true if any artificial variable is in the basis and false
     * otherwise.
     * @throws IloException
     */
    public boolean hasArtificialVars() {
        if (artificials.isEmpty())
            return false;

        if (!hasSolution)
            return true;

        //if (model.getStatus() != IloCplex.Status.Optimal)
        //    return true;

        // check if the artificial variables are in the basis
        boolean result = false;
        for (MPVar var : artificials)
            if (solver.getValue(var) > Constants.EPS)
                return true;

        return false;
    }

    /***
     * Removes the unused artificial variables from the formulation.
     *
     * @throws IloException
     */
    public void removeArtificialVars() {
        //assert hasSolution;
        //hasSolution = false;
        //
        //// listing useless artificial variables
        //List<MPVar> uselessArtificialVars = new LinkedList<>();
        //Iterator<MPVar> it = artificials.iterator();
        //while (it.hasNext()) {
        //    MPVar var = it.next();
        //    if (solver.getValue(var) <= Constants.EPS) {
        //        uselessArtificialVars.add(var);
        //        it.remove();
        //    }
        //}

        // removing useless artificial variables
        //for (MPVar var : uselessArtificialVars)
        //    model.setVarBounds(var, 0.0, 0.0);
        //model.delete(var);
    }

    /***
     * Removes expires lambdas from the formulation.
     */
    public void removeExpiredLambdas() {
        if (Parameters.get().removeColumns && hasSolution) {
            int count = 0;
            List<MPVar> expiredVars = new LinkedList<>();

            Iterator<Column> iterator = lambdas.iterator();
            while (iterator.hasNext()) {
                Column lambda = iterator.next();
                if (solver.getValue(lambda.getVar()) > Constants.EPS) {
                    lambda.setIter(varIters);
                }
                else if (lambda.getIter() < varIters - Parameters.get().itersColumnLife) {
                    //TODO: get reduced gammaCost of variable in the framework
                    //if (model.getReducedCost(lambda.getVar()) > Parameters.get().columnMinReducedCost) {
                    //    iterator.remove();
                    //    expiredVars.add(lambda.getVar());
                    //    count++;
                    //}
                }
            }

            expiredVars.forEach(model::delete);

            //if (count > 0)
            //    Util.safePrintf(output, "%d columns removed...\n", count);
        }
    }

    /**
     * Removes all columns (lambdas) from the master problem
     */
    public void reset() {
        currentNode = null;
        pricing.reset();

        for (Column c : lambdas)
            model.delete(c.getVar());
        lambdas.clear();

        hasSolution = false;
        lambdaCount = 0;
        varIters = 0;
        objValue = Double.MAX_VALUE;
    }

    /***
     * Optimize current master problem.
     *
     * @return true if the solve is successful (and the optimal solutionStr is
     * returned), and false otherwise.
     * @throws ExecutionException   the execution exception
     * @throws InterruptedException the interrupted exception
     */
    public boolean solve(long finishTimeMillis) throws ExecutionException, InterruptedException {
        return solve(finishTimeMillis, 1, null);
    }

    /***
     * Optimize current master problem.
     *
     * @param output the output
     * @return true if the solve is successful (and the optimal solutionStr is
     * returned), and false otherwise.
     * @throws ExecutionException   the execution exception
     * @throws InterruptedException the interrupted exception
     */
    public boolean solve(long finishTimeMillis, int nThreads, PrintStream output) throws ExecutionException, InterruptedException {
        //solver = new SolverCplex(model, false);
        solver = new SolverGurobi(model, false);
        //solver.setParam(MPSolver.BooleanParam.NumericalEmphasis, true);
        //solver.setParam(MPSolver.IntParam.Threads, nThreads);
        model.setSolver(solver);

        long initialTime = System.currentTimeMillis();
        //solver.setParam(MPSolver.BooleanParam.NumericalEmphasis, true);
        //solver.setParam(MPSolver.IntParam.RootAlg, 2);

        boolean hasArtificial = !artificials.isEmpty();
        boolean stop = false;

        PriorityQueue<PairDouble<Integer>> heap = new PriorityQueue<>();
        heap.add(new PairDouble<>(Double.MAX_VALUE, pricing.id));

        Util.safePrintln(output, "-----------------------------------------------------------------------------------------");
        Util.safePrintln(output, "   Iter        Objective         Pricing       Columns      Src      Total         Time  ");
        Util.safePrintln(output, "-----------------------------------------------------------------------------------------");

        int iter = 0;
        while (!stop) {
            if (System.currentTimeMillis() > finishTimeMillis)
                return false;

            varIters++;
            double prevObjValue = getObjValue();

            //solver.writeModel("master-0.lp");
            model.updateSolver();
            //solver.writeModel("master-1.lp");

            long masterSolvingStartingTimeMillis = System.currentTimeMillis();
            if (!solver.solve()) {
                Util.safePrintln(output, "No solution found for master problem (...)");
                Util.safePrintln(output, "-----------------------------------------------------------------------------------------");
                masterRuntime.addAndGet(System.currentTimeMillis() - masterSolvingStartingTimeMillis);
                return false;
            }
            else {
                hasSolution = true;
                masterRuntime.addAndGet(System.currentTimeMillis() - masterSolvingStartingTimeMillis);
            }

            this.objValue = solver.getObjValue();
            final double dualLambdas[] = getDualLambdas();
            final double dualConstant = getDualConstant();

            if (prevObjValue - getObjValue() < Constants.EPS)
                varIters--;
            else
                removeExpiredLambdas();

            if (hasArtificial) {
                if (!hasArtificialVars()) {
                    removeArtificialVars();
                    hasArtificial = false;

                    Util.safePrintln(output, "\n   All artificial variables are zero. Reoptimizing...\n");

                    heap.clear();
                    heap.add(new PairDouble<>(Double.MAX_VALUE, pricing.id));

                    continue;
                }
                removeArtificialVars();
            }

            int total = 0, blocks = 0;
            ArrayList<PairDouble<Integer>> done = new ArrayList<>();
            int currentProject = 0;

            List<Future<Integer>> executions = new ArrayList<>();
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            while (!heap.isEmpty()) {

                currentProject = heap.remove().snd;

                final int currentPricing = currentProject;

                //executions.add(executor.submit(() -> {
                int nColumns = 0;
                if (pricing.solve(finishTimeMillis, dualLambdas, dualConstant, nThreads)) {
                    List<Column> columns = pricing.buildColumns();
                    for (Column col : columns) {
                        assert col.isValid(currentNode, dualLambdas) : "invalid column returned by " + (pricing.hasHeuristicSolution() ? "heuristic" : "mip");
                        addLambda(col);
                        nColumns++;
                    }

                    done.add(new PairDouble<>(pricing.getObjValue(), currentPricing));
                }
                else {
                    done.add(new PairDouble<>(0.0, currentPricing));
                }

                if (System.currentTimeMillis() > finishTimeMillis)
                    throw new InterruptedException("time limit reached");

                //return nColumns;
                //}));

                if (nColumns > 0) {
                    blocks++;
                    total += nColumns;
                }
            }
            executor.shutdownNow();

            heap.addAll(done);
            done.clear();

            stop = total == 0;

            Util.safePrintf(output, "%7d   %14s   %13s    %6d - %-3d    %3s    %7d   %9.1fs\n",
              ++iter,
              Math.abs(getObjValue()) < 1e9 ? String.format("%14.4f", getObjValue()) : String.format("%14.0f", getObjValue()),
              stop || hasArtificial ? 0.0 : Math.abs(heap.peek().fst) < 1e7 ? String.format("%12.4f", heap.peek().fst) : String.format("%12.0f", heap.peek().fst),
              total, blocks,
              pricing.hasHeuristicSolution() ? " H " : "MIP",
              lambdas.size(),
              (System.currentTimeMillis() - initialTime) / 1000.0);
        }
        Util.safePrintln(output, "-----------------------------------------------------------------------------------------");

        //  solution from solver
        for (Column lambda : lambdas)
            lambda.setValue(solver.getValue(lambda.getVar()));

        return !hasArtificialVars();
    }

    /**
     * Creates the (artificial) variables for the master problem.
     */
    private void createMPVars() {
        for (int i = 0; i < problem.teams.length; i++) {
            artificials.add(model.addNumVar(0.0, 1.0, 10e6, "artificial(%d)", i));
        }
    }

    /**
     * Creates the objective function and assigns it to the {@link #objective}
     * variable.
     */
    private void createObjective() {
        objective = model.getObjective();
    }

    /**
     * Creates the constraints of the master problem.
     */
    private void createConstraints() {
        // creating lambda_y constraints
        lambdaConstrs = new MPLinConstr[problem.teams.length];
        for (int i = 0; i < lambdaConstrs.length; i++) {
            lambdaConstrs[i] = model.addEq(artificials.get(i), 1.0, String.format("team_cover(%d)", i));
        }

        // creating maximum number of leagues constraint
        maxLambdasConstr = model.addLe(new MPLinExpr(0), Math.floor(( double ) problem.teams.length / ( double ) problem.minLeagueSize), "max_leagues");
    }


    /***
     * Create a formulation (for testing purposes only)
     *
     * @param args the args
     * @throws Exception the exception
     */
    public static void main(String args[]) throws Exception {
        Locale.setDefault(new Locale("en-US"));
        long startTimeMillis = System.currentTimeMillis();
        long timeLimitMillis = startTimeMillis + 3600 * 1000;

        Problem problem = new Problem(args[0]);
        //MIP_1 mip1 = new MIP_1(problem);
        //Solution initialSolution = mip1.solve(5 * 1000);

        System.out.println();

        //System.out.printf("Initial solution: %d\n", initialSolution.getObjective());
        //System.out.printf("Current runtime:  %.2f\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);

        // creating and saving CG
        ColumnGeneration cg = new ColumnGeneration(problem);
        cg.solve(timeLimitMillis, Runtime.getRuntime().availableProcessors(), System.out);

        System.out.printf("Root bound...: %.2f\n", cg.getObjValue());
        System.out.printf("Total runtime: %.2f seconds\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);
    }
}

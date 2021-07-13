package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.core.util.*;
import be.kuleuven.stgp.mip.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class implements the Branch-and-price algorithm.
 *
 * @author Tulio Toffolo
 */
public class BranchAndPrice {

    public final static Queue<ColumnGeneration> cgs = new ConcurrentLinkedQueue<>();
    public final static long startTimeMillis = System.currentTimeMillis();

    public final Problem problem;
    public final int initialUB;

    private Solution bestSolution = null;
    private long timeLimitMillis;

    private PriorityQueue<BranchNode> nodesHeap = new PriorityQueue<>();

    /**
     * Instantiates a new Branch-and-price.
     *
     * @param problem the problem
     */
    public BranchAndPrice(Problem problem) {
        this(problem, Integer.MAX_VALUE);
    }

    /**
     * Instantiates a new Branch-and-price providing an initial upper bound.
     *
     * @param problem   the problem
     * @param initialUB the initial upper bound
     */
    public BranchAndPrice(Problem problem, int initialUB) {
        this.problem = problem;
        this.initialUB = initialUB;
    }

    /**
     * Instantiates a new Branch-and-price providing an initial solution.
     *
     * @param problem         the problem
     * @param initialSolution the initial solution
     */
    public BranchAndPrice(Problem problem, Solution initialSolution) {
        this(problem, initialSolution != null ? initialSolution.getObjective() : Integer.MAX_VALUE);
        this.bestSolution = initialSolution;
    }

    /**
     * Gets current runtime.
     *
     * @return the current runtime
     */
    public double getRuntime() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000.0;
    }

    /**
     * Solves the problem with the Branch-and-price.
     *
     * @param timeLimitSeconds the time limit (in seconds)
     * @param output           the output stream (for log purposes)
     * @return the best solution obtained
     * @throws ExecutionException   if any error occurs during the execution
     * @throws InterruptedException if one of the execution's threads is
     *                              interrupted
     */
    public Solution solve(int timeLimitSeconds, PrintStream output) throws ExecutionException, InterruptedException {
        this.timeLimitMillis = System.currentTimeMillis() + timeLimitSeconds * 1000;

        // creating ColumnGeneration objects (one for each thread)
        for (int i = 0; i < Math.max(2, ThreadPool.get().getMaxThreads()); i++)
            cgs.add(new ColumnGeneration(problem, bestSolution));

        // opening the root node
        openNode(null, output);

        // opening remaining nodes
        while (!nodesHeap.isEmpty()) {
            openNode(nodesHeap.poll(), output);

            if (System.currentTimeMillis() > timeLimitMillis) {
                Util.safePrintln(output, "\nTime limit reached: solution is not guaranteed optimal!");
                break;
            }
        }

        if (bestSolution != null)
            bestSolution.validate(System.err);

        return bestSolution;
    }

    /**
     * Solves a node, possibly generating up to two new nodes.
     *
     * @param parent the node to be opened (solved)
     * @param output the output stream (for log purposes)
     * @throws ExecutionException   if any error occurs during the execution
     * @throws InterruptedException if one of the execution's threads is
     *                              interrupted
     */
    protected void openNode(BranchNode parent, PrintStream output) throws ExecutionException, InterruptedException {
        // printing message to output
        if (parent != null)
            Util.safePrintf(output, "%7.1fs    opening node %d (best lb = %.2f)\n", getRuntime(), parent.id, parent.lowerBound);
        else
            Util.safePrintf(output, "%7.1fs    solving root node\n", getRuntime());

        // trying to prune the parent node
        if (bestSolution != null && parent != null && Math.ceil(parent.lowerBound - Constants.EPS) >= bestSolution.getObjective()) {
            Util.safePrintf(output, "%7.1fs        pruned children by parent cost, %.2f >= %d\n", getRuntime(), parent.lowerBound, bestSolution.getObjective());
            return;
        }

        // generating children nodes
        List<BranchNode> children;
        if (parent == null) {
            BranchNode node = new BranchNode(problem);
            node.solve(timeLimitMillis, Parameters.get().nThreads, cgs.peek());
            children = Arrays.asList(node);
        }
        else {
            children = parent.openChildrenNodes(timeLimitMillis, cgs, Parameters.get().strongBranching, Parameters.get().printPricing ? output : null);
        }

        // adding children nodes to the heap (unless those that can be pruned)
        for (BranchNode node : children) {

            // if node is infeasible
            if (!node.isFeasible()) {
                Util.safePrintf(output, "%7.1fs        node %3d :: infeasible\n", getRuntime(), node.id);
                continue;
            }

            Solution solution = node.getSolution();

            // if an (improving) integer solution is returned
            if (solution != null && (bestSolution == null || solution.getObjective() < bestSolution.getObjective())) {
                Util.safePrintf(output, "%7.1fs        node %3d :: improved best solution, %d %s\n", getRuntime(), node.id, solution.getObjective(), bestSolution != null ? "< " + bestSolution.getObjective() : "");
                bestSolution = solution.clone();
            }

            // pruning integer node with high objective
            else if (solution != null) {
                Util.safePrintf(output, "%7.1fs        node %3d :: pruned integer node with cost %d >= %d\n", getRuntime(), node.id, solution.getObjective(), bestSolution.getObjective());
            }

            // pruning fractional node with high lower bound
            else if (bestSolution != null && Math.ceil(node.lowerBound - Constants.EPS) >= bestSolution.getObjective()) {
                Util.safePrintf(output, "%7.1fs        node %3d :: pruned node with lower bound %.2f >= %d\n", getRuntime(), node.id, node.lowerBound, bestSolution.getObjective());
            }

            // adding the node to the heap
            else {
                Util.safePrintf(output, "%7.1fs        node %3d :: added to the heap with lb %.2f\n", getRuntime(), node.id, node.lowerBound);
                nodesHeap.add(node);
            }
        }
    }


    /**
     * Main method
     *
     * @param args the command line arguments
     * @throws Exception if any error occurs during the execution
     */
    public static void main(String args[]) throws Exception {
        Locale.setDefault(new Locale("en-US"));
        long startTimeMillis = System.currentTimeMillis();

        //System.in.read();

        Problem problem = new Problem(args[0]);
        Solution initialSolution = null;
        //if (new File(args[0].replace(".prob", ".sol")).exists()) {
        //    initialSolution = new Solution(problem);
        //    initialSolution.read(args[0].replace(".prob", ".sol"));
        //    System.out.printf("Loaded initial solution with cost %d (as %d)...\n", initialSolution.getObjective(), initialSolution.getObjective() + 1);
        //    initialSolution.objective++;
        //}

        System.out.printf("Starting Branch-and-price algorithm with up to %d threads...\n\n", ThreadPool.get().getMaxThreads());

        // generating initial candidate solution
        //MIP_1 mip1 = new MIP_1(problem);
        //Solution initialSolution = mip1.solve(500 * 1000);
        //System.out.println();
        //System.out.printf("Initial solution: %d\n", initialSolution.getObjective());
        //System.out.printf("Current runtime:  %.2f\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);

        // creating and saving CG
        BranchAndPrice bp = new BranchAndPrice(problem, initialSolution);
        Solution solution = bp.solve(Integer.parseInt(args[2]), System.out);

        System.out.println();
        if (solution != null && solution.validate()) {
            System.out.printf("Solution cost: %d\n", solution.getObjective());
            solution.write(args[1]);
        }
        System.out.printf("Total runtime: %.2f\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);
        System.out.println();
        System.out.printf("Total master problem runtime: %.2f\n", ( double ) ColumnGeneration.masterRuntime.get() / 1000.0);

        Parameters.get().writeJson();
        ThreadPool.get().shutdownNow();
    }
}

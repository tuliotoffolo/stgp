package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.core.util.*;
import be.kuleuven.stgp.mip.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * This class implements the Branch-and-price algorithm (parallel version).
 *
 * @author Tulio Toffolo
 */
public class BranchAndPriceParallel {

    public static long startTimeMillis = System.currentTimeMillis();
    public static final ConcurrentLinkedDeque<ColumnGeneration> cgs = new ConcurrentLinkedDeque<>();

    public final Problem problem;
    public final int initialUB;

    private long finishTimeMillis;

    private volatile Solution bestSolution = null;
    private volatile double rootLb = 0, lb = 0;
    private volatile int ub = Integer.MAX_VALUE;

    private final TreeMap<Double, Integer> nodes = new TreeMap<>();

    private final TreeMap<Integer, PriorityQueue<BranchNode>> levels = new TreeMap<>();
    private final PriorityBlockingQueue<BranchNode> nodesHeap = new PriorityBlockingQueue<>();

    private final AtomicInteger nNodes = new AtomicInteger(0);
    private final AtomicInteger nWorkers = new AtomicInteger(0);

    private double lastPrint;

    /**
     * Instantiates a new Branch-and-price.
     *
     * @param problem the problem
     */
    public BranchAndPriceParallel(Problem problem) {
        this(problem, Integer.MAX_VALUE);
    }

    /**
     * Instantiates a new Branch-and-price providing an initial upper bound.
     *
     * @param problem   the problem
     * @param initialUB the initial upper bound
     */
    public BranchAndPriceParallel(Problem problem, int initialUB) {
        this.problem = problem;
        this.initialUB = initialUB;
        this.ub = initialUB;
    }

    /**
     * Instantiates a new Branch-and-price providing an initial solution.
     *
     * @param problem         the problem
     * @param initialSolution the initial solution
     */
    public BranchAndPriceParallel(Problem problem, Solution initialSolution) {
        this(problem, initialSolution != null ? initialSolution.getObjective() : Integer.MAX_VALUE);
        this.bestSolution = initialSolution;
    }

    /**
     * Gets the best lower bound.
     *
     * @return the best lower bound.
     */
    public double getLb() {
        return lb;
    }

    /**
     * Gets the root lower bound given by the linear relaxation.
     *
     * @return the root lower bound given by the linear relaxation.
     */
    public double getRootLb() {
        return rootLb;
    }

    /**
     * Gets current runtime (in seconds).
     *
     * @return the current runtime (in seconds)
     */
    public double getRuntime() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000.0;
    }

    /**
     * Solves the problem with the Branch-and-price.
     *
     * @param finishTimeMillis the time in which the execution must end (the
     *                         value will be compare with System.currentTimeMillis())
     * @param output           the output stream (for log purposes)
     * @return the best solution obtained
     * @throws ExecutionException   if any error occurs during the execution
     * @throws InterruptedException if one of the execution's threads is
     *                              interrupted
     */
    public Solution solve(long finishTimeMillis, PrintStream output) throws ExecutionException, InterruptedException {
        this.finishTimeMillis = finishTimeMillis;

        System.out.printf("-------------------------------------------------------------------------------------------\n");
        System.out.printf("%9s %8s %7s %8s %7s %12s %12s %10s %8s   %s\n", "Runtime", "Nodes", "Heap", "ID", "Lvl", "NodeObj", "Objective", "BestUB", "Gap", "");
        System.out.printf("-------------------------------------------------------------------------------------------\n");

        // creating extra column generations classes (one for each thread)
        for (int i = 0; i < Math.max(2, ThreadPool.get().getMaxThreads()); i++)
            cgs.add(new ColumnGeneration(problem, bestSolution));

        // opening the root node
        try {
            openNode(finishTimeMillis, null, null);
            rootLb = lb;
        }
        catch (InterruptedException e) {
            if (System.currentTimeMillis() < finishTimeMillis)
                e.printStackTrace();
            System.out.printf("--------------------------------------------------------------------------------------------\n");
            System.out.printf("Time limit reached: solution is not guaranteed optimal!\n");
            ThreadPool.get().shutdownNow();
            return null;
        }
        catch (Exception | Error e) {
            e.printStackTrace();
            return null;
        }

        // opening remaining nodes in parallel
        AtomicInteger currentLevel = new AtomicInteger(0);
        ExecutorService threads = Executors.newFixedThreadPool(Parameters.get().nThreads);
        while (nNodes.get() > 0 || nWorkers.get() > 0) {
            while (nNodes.get() > 0) {
                ThreadPool.get().acquire(Parameters.get().strongBranching * 2);

                BranchNode node = Parameters.get().cyclicBFS ? getNodeCyclic(currentLevel.get()) : getNode();
                if (node == null) break;
                currentLevel.set(node.level + 1);

                if (getRuntime() - lastPrint >= 5)
                    printStatus("", node, "");

                nWorkers.incrementAndGet();
                threads.submit(() -> {
                    try {
                        if (!openNode(finishTimeMillis, node, null)) {
                            ThreadPool.get().release(Parameters.get().strongBranching * 2);
                        }
                    }
                    catch (InterruptedException e) {
                        if (System.currentTimeMillis() < finishTimeMillis)
                            e.printStackTrace();
                    }
                    catch (Exception | Error e) {
                        e.printStackTrace();
                    }
                    finally {
                        nWorkers.decrementAndGet();
                    }
                });

                if (System.currentTimeMillis() > finishTimeMillis) {
                    System.out.printf("--------------------------------------------------------------------------------------------\n");
                    System.out.printf("Time limit reached: solution is not guaranteed optimal!\n");
                    threads.shutdownNow();
                    ThreadPool.get().shutdownNow();
                    return bestSolution;
                }
            }

            if (System.currentTimeMillis() > finishTimeMillis) {
                System.out.printf("--------------------------------------------------------------------------------------------\n");
                System.out.printf("Time limit reached: solution is not guaranteed optimal!\n");
                threads.shutdownNow();
                ThreadPool.get().shutdownNow();
                return bestSolution;
            }

            Thread.sleep(50);
        }

        if (bestSolution != null)
            bestSolution.validate(System.err);

        System.out.printf("--------------------------------------------------------------------------------------------\n");
        System.out.printf("Optimal solution obtained!\n");

        assert nodes.isEmpty();

        return bestSolution;
    }

    /**
     * This method simply prints information in a standard format to stdout.
     *
     * @param pre   2 characters string to be printed in the beginning of the
     *              line.
     * @param extra text to be printed in the end of the line.
     */
    public void printStatus(String pre, BranchNode node, String extra) {
        if (System.currentTimeMillis() > finishTimeMillis)
            return;

        lastPrint = getRuntime();
        int nodes = BranchNode.nextId.get();

        String timeStr = lastPrint >= 3600.0 ? String.format("%7.2fm", lastPrint / 60) : String.format("%7.1fs", lastPrint);
        String nodeObj = node.isInteger() ? String.format("%8d***", node.getSolution().getObjective()) : String.format("%11.2f", node.lowerBound);
        String lbStr = lb == 0 ? "-" : String.format("%.2f", lb);
        String ubStr = ub >= Integer.MAX_VALUE ? "-" : String.format("%d", ub);
        String gapStr = ub >= Integer.MAX_VALUE || lb == 0 ? "-" : String.format("%.2f%%", 100 * (( double ) ub - lb) / ( double ) ub);

        System.out.printf("%9s %8s %7s %8s %7s %12s %12s %10s %8s   %s\n", timeStr, nodes, nNodes.get(), node.id, node.level, nodeObj, lbStr, ubStr, gapStr, extra);
    }

    /**
     * Solves a node, possibly generating up to two new nodes.
     *
     * @param parent the node to be opened (solved)
     * @param output the output stream (for log purposes)
     * @return true if the node is actually opened and false otherwise.
     * @throws ExecutionException   if any error occurs during the execution
     * @throws InterruptedException if one of the execution's threads is
     *                              interrupted
     */
    private boolean openNode(long finishTimeMillis, BranchNode parent, PrintStream output) throws ExecutionException, InterruptedException {
        boolean result = false;

        // printing message to output
        if (parent != null)
            Util.safePrintf(output, "%7.1fs    opening node %d (lb=%.2f, min=%.2f) - level %d\n", getRuntime(), parent.id, parent.lowerBound, lb, parent.level);
        else
            Util.safePrintf(output, "%7.1fs    solving root node\n", getRuntime());

        // trying to prune the parent node
        if (bestSolution != null && parent != null && Math.ceil(parent.lowerBound - Constants.EPS) >= bestSolution.getObjective()) {
            Util.safePrintf(output, "%7.1fs        pruned children by parent cost, %.2f >= %d (level %d)\n", getRuntime(), parent.lowerBound, bestSolution.getObjective(), parent.level);
        }
        else {
            // generating children nodes
            List<BranchNode> children;
            if (parent == null) {
                BranchNode node = new BranchNode(problem);
                node.solve(finishTimeMillis, ThreadPool.get().getMaxThreads(), cgs.peek(), Parameters.get().printPricing ? System.out : null);
                children = Arrays.asList(node);
                setLB(node);
            }
            else {
                children = parent.openChildrenNodes(finishTimeMillis, cgs, Parameters.get().strongBranching, Parameters.get().printPricing ? System.out : null);
                result = true;
            }

            // adding children nodes to the heap (unless those that can be pruned)
            for (BranchNode node : children) {

                // if node is infeasible
                if (!node.isFeasible()) {
                    Util.safePrintf(output, "%7.1fs        node %3d :: infeasible (level %d)\n", getRuntime(), node.id, node.level);
                    continue;
                }

                Solution solution = node.getSolution();

                // if an (improving) integer solution is returned
                if (solution != null && (bestSolution == null || solution.getObjective() < bestSolution.getObjective())) {
                    Util.safePrintf(output, "%7.1fs        node %3d :: improved best solution, %d %s (level %d)\n", getRuntime(), node.id, solution.getObjective(), bestSolution != null ? "< " + bestSolution.getObjective() : "", node.level);
                    setUB(node);
                }

                // pruning integer node with high objective
                else if (solution != null) {
                    Util.safePrintf(output, "%7.1fs        node %3d :: pruned integer node with cost %d >= %d (level %d)\n", getRuntime(), node.id, solution.getObjective(), bestSolution.getObjective(), node.level);
                }

                // pruning fractional node with high lower bound
                else if (bestSolution != null && Math.ceil(node.lowerBound - Constants.EPS) >= bestSolution.getObjective()) {
                    Util.safePrintf(output, "%7.1fs        node %3d :: pruned node with lower bound %.2f >= %d (level %d)\n", getRuntime(), node.id, node.lowerBound, bestSolution.getObjective(), node.level);
                }

                // adding the node to the heap
                else {
                    Util.safePrintf(output, "%7.1fs        node %3d :: added to the heap with lb=%.2f (level %d)\n", getRuntime(), node.id, node.lowerBound, node.level);
                    if (Parameters.get().cyclicBFS)
                        addNodeCyclic(node);
                    else
                        addNode(node);
                }
            }
        }

        if (parent != null) setLB(parent);
        return result;
    }

    /**
     * Adds a node to the heap.
     */
    private synchronized void addNode(BranchNode node) {
        nodesHeap.add(node);
        nNodes.incrementAndGet();

        int occurrences = 0;
        Integer value = nodes.get(node.lowerBound);
        if (value != null) occurrences = value;

        nodes.put(node.lowerBound, occurrences + 1);
        //printStatus("", node, "");
    }

    /**
     * Adds a node to one of the cyclic heaps, while taking concurrency among
     * threads into account. Note that this method is called only if the CBFS
     * (cyclic best first search) strategy is selected.
     */
    private synchronized void addNodeCyclic(BranchNode node) {
        if (!levels.containsKey(node.level))
            levels.put(node.level, new PriorityQueue<>());

        levels.get(node.level).add(node);
        nNodes.incrementAndGet();

        int occurrences = 0;
        Integer value = nodes.get(node.lowerBound);
        if (value != null) occurrences = value;

        nodes.put(node.lowerBound, occurrences + 1);
        //printStatus("", node, "");
    }

    /**
     * Gets the next node to be processed.
     *
     * @return the next node to be processed
     */
    private synchronized BranchNode getNode() {
        BranchNode node = nodesHeap.poll();
        if (node != null) {
            nNodes.decrementAndGet();
        }

        return node;
    }

    /**
     * Gets the next node to be processed while taking concurrency among threads
     * into account. Note that this method is called only if the CBFS (cyclic
     * best first search) strategy is selected.
     *
     * @param currentLevel the current level of the tree to be processed
     * @return the next node to be processed
     */
    private synchronized BranchNode getNodeCyclic(int currentLevel) {
        if (levels.isEmpty()) return null;

        BranchNode node;
        while (!levels.containsKey(currentLevel)) {
            Integer nextLevel = levels.higherKey(currentLevel);
            currentLevel = nextLevel == null ? 0 : nextLevel;
        }

        PriorityQueue<BranchNode> heap = levels.get(currentLevel);
        node = heap.poll();
        if (node != null)
            nNodes.decrementAndGet();

        if (heap.isEmpty())
            levels.remove(currentLevel);

        return node;
    }

    /**
     * Updates the best lower bound known so far.
     *
     * @param node node containing the lower bound candidate
     */
    private synchronized void setLB(BranchNode node) {
        Integer value = nodes.get(node.lowerBound);
        if (value != null) {
            if (value == 1)
                nodes.remove(node.lowerBound);
            else
                nodes.put(node.lowerBound, value - 1);
        }

        double newlb = Math.min(ub, Math.min(node.lowerBound, nodes.isEmpty() ? Double.MAX_VALUE : nodes.firstKey()));
        if (newlb > lb + Constants.EPS) {
            lb = newlb;
            printStatus("", node, "lb");
        }
    }

    /**
     * Updates the best solution and upper bound known so far.
     *
     * @param node node containing the new best solution candidate
     */
    private synchronized void setUB(BranchNode node) {
        if (bestSolution == null || node.getSolution().getObjective() < bestSolution.getObjective()) {
            ub = Math.min(ub, node.getSolution().getObjective());
            bestSolution = node.getSolution();

            // removing invalid old nodes
            if (Parameters.get().cyclicBFS) {
                int count = 0;
                for (PriorityQueue<BranchNode> heap : levels.values()) {
                    heap.removeIf(c -> Math.round(c.lowerBound - Constants.EPS) >= ub);
                    count += heap.size();
                }
                nNodes.set(count);
            }
            else {
                nodesHeap.removeIf(c -> Math.round(c.lowerBound - Constants.EPS) >= ub);
                nNodes.set(nodesHeap.size());
            }

            // removing old lower bounds
            NavigableMap<Double, Integer> headmap = nodes.tailMap(ub - 1 + Constants.EPS, false);
            headmap.clear();

            printStatus("", node, "solution");
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
        long timeLimitMillis = System.currentTimeMillis() + Integer.parseInt(args[2]) * 1000;

        Problem problem = new Problem(args[0]);
        Solution initialSolution = null;
        //if (new File(args[0].replace(".prob", ".sol")).exists()) {
        //    initialSolution = new Solution(problem);
        //    initialSolution.read(args[0].replace(".prob", ".sol"));
        //    System.out.printf("Loaded initial solution with cost %d (as %d)...\n", initialSolution.getObjective(), initialSolution.getObjective() + 1);
        //    initialSolution.objective++;
        //}


        // generating initial candidate solution
        //MIP_1 mip1 = new MIP_1(problem);
        //Solution initialSolution = mip1.solve(500 * 1000);
        //System.out.println();
        //System.out.printf("Initial solution: %d\n", initialSolution.getObjective());
        //System.out.printf("Current runtime:  %.2f\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);

        // creating and saving CG

        System.out.printf("Executing branch-and-price on instance %s\n", problem.name);
        System.out.println();
        System.out.printf("Maximum cols / iter.: %d\n", Parameters.get().populateLimit);
        System.out.printf("Maximum threads.....: %d\n", Parameters.get().nThreads);
        System.out.printf("Strong branching....: %d\n", Parameters.get().strongBranching);
        System.out.printf("Tree-search strategy: %s\n", Parameters.get().cyclicBFS ? "Cyclic BFS" : "BFS");
        if (Parameters.get().removeColumns) {
            System.out.println();
            System.out.printf("Columns iter life...: %d\n", Parameters.get().itersColumnLife);
            System.out.printf("Remove col. min-cost: %.2f\n", Parameters.get().columnMinReducedCost);
        }

        BranchAndPriceParallel bp = new BranchAndPriceParallel(problem, initialSolution);
        Solution solution = bp.solve(timeLimitMillis, System.out);
        System.out.println();

        System.out.printf("Root bound...: %.2f\n", bp.getRootLb());
        System.out.printf("Best bound...: %.2f\n", bp.getLb());
        if (solution != null && solution.validate(System.err)) {
            System.out.printf("Solution cost: %d\n", solution.getObjective());
            solution.write(args[1]);
        }
        System.out.printf("Total nodes..: %d\n", BranchNode.nextId.get());
        System.out.printf("Total runtime: %.2f\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);

        System.out.println();
        System.out.printf("Total master problem runtime: %.2f\n", ( double ) ColumnGeneration.masterRuntime.get() / 1000.0);

        Parameters.get().writeJson();
        ThreadPool.get().shutdownNow();
        System.exit(0);
    }
}

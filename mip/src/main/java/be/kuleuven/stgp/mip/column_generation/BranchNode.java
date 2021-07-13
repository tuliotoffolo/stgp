package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.mip.util.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * This class represents a Node of the Branch-and-price search tree.
 *
 * @author Tulio Toffolo
 */
public class BranchNode implements Comparable<BranchNode> {

    public static AtomicInteger nextId = new AtomicInteger(0);

    public final Problem problem;
    public final Random random;

    public double lowerBound;
    public int id, level;

    private boolean feasible = false;
    private Solution solution = null;

    private LinkedList<FracVar> vars = new LinkedList<>();

    private LinkedList<League> leagues = new LinkedList<>();
    protected LinkedList<PairInt> fixedOne = new LinkedList<>();
    protected LinkedList<PairInt> fixedZero = new LinkedList<>();

    //private List<Clique> cliques = new LinkedList<>();
    //private Clique[] teamCliques;
    //private int[] teamConflicts;

    /**
     * Empty constructor (for the root node).
     */
    public BranchNode(Problem problem) {
        this.problem = problem;
        this.random = new Random(0);
        this.lowerBound = Double.MAX_VALUE;
        this.id = nextId.getAndIncrement();
        this.level = 0;

        //this.teamCliques = new Clique[problem.teams.length];
        //this.teamConflicts = new int[problem.teams.length];
    }

    /**
     * Private constructor used for cloning purposes.
     *
     * @param node
     */
    private BranchNode(BranchNode node) {
        this.problem = node.problem;
        this.random = new Random(node.random.nextInt());
        this.lowerBound = node.lowerBound;
        this.id = nextId.getAndIncrement();
        this.level = node.level;

        this.leagues = new LinkedList<>(node.leagues);
        this.fixedOne = new LinkedList<>(node.fixedOne);
        this.fixedZero = new LinkedList<>(node.fixedZero);

        //this.teamCliques = new Clique[node.teamCliques.length];
        //this.teamConflicts = node.teamConflicts.clone();
        //
        //for (Clique clique : node.cliques) {
        //    Clique newClique = new Clique(clique);
        //    this.cliques.add(newClique);
        //    for (Team team : newClique)
        //        this.teamCliques[team.id] = newClique;
        //}
    }

    @Override
    public BranchNode clone() {
        return new BranchNode(this);
    }

    @Override
    public int compareTo(BranchNode node) {
        int result;

        result = Double.compare(lowerBound, node.lowerBound);
        if (result != 0) return result;

        result = Integer.compare(vars.size(), node.vars.size());
        if (result != 0) return result;

        result = -Integer.compare(fixedOne.size(), node.fixedOne.size());
        if (result != 0) return result;

        return Integer.compare(id, node.id);
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
        if (value) {
            // adding the teams to the right groups or joining them
            //if (teamCliques[i] == null && teamCliques[j] == null) {
            //    Clique newClique = new Clique();
            //    newClique.add(problem.teams[i]);
            //    newClique.add(problem.teams[j]);
            //    cliques.add(newClique);
            //    teamCliques[i] = newClique;
            //    teamCliques[j] = newClique;
            //}
            //else if (teamCliques[i] != null && teamCliques[j] == null) {
            //    teamCliques[i].add(problem.teams[j]);
            //    teamCliques[j] = teamCliques[i];
            //}
            //else if (teamCliques[i] == null && teamCliques[j] != null) {
            //    teamCliques[j].add(problem.teams[i]);
            //    teamCliques[i] = teamCliques[j];
            //}
            //else if (teamCliques[i] != teamCliques[j]) {
            //    teamCliques[i].addAll(teamCliques[j]);
            //    for (Team team : teamCliques[j])
            //        teamCliques[team.id] = teamCliques[i];
            //    cliques.remove(teamCliques[j]);
            //}

            fixedOne.add(new PairInt(i, j));
        }
        else {
            //teamConflicts[i]++;
            //teamConflicts[j]++;
            fixedZero.add(new PairInt(i, j));
        }
    }

    /**
     * Gets a solution (if the node is integer). This method returns null if the
     * node has fractional solution.
     *
     * @return the solution, if the node is integer, and null otherwise
     */
    public Solution getSolution() {
        return solution;
    }

    /**
     * Checks if this node is feasible.
     *
     * @return true if the node is feasible and false otherwise
     */
    public boolean isFeasible() {
        return feasible;
    }

    /**
     * Checks if this node is fractional.
     *
     * @return true if the node is feasible and false otherwise
     */
    public boolean isFractional() {
        return feasible && solution == null;
    }

    /**
     * Checks if this node yields an integer solution.
     *
     * @return true if the node is integer and false otherwise
     */
    public boolean isInteger() {
        return solution != null;
    }

    /**
     * Creates the children nodes of this node and returns them in a list.
     *
     * @return the list of generated children nodes
     */
    private List<BranchNode> makeChildrenNodes(PairInt branchVar) {
        assert branchVar != null : "all variables in solution are integer.";

        BranchNode leftNode = clone();
        BranchNode rightNode = clone();

        leftNode.level++;
        rightNode.level++;

        // fixing variables
        leftNode.fix(branchVar.getFirst(), branchVar.getSecond(), true);
        rightNode.fix(branchVar.getFirst(), branchVar.getSecond(), false);

        // filtering columns for left node
        Iterator<League> iterLeft = leftNode.leagues.iterator();
        while (iterLeft.hasNext()) {
            League league = iterLeft.next();
            if (league.contains(branchVar.getFirst()) != league.contains(branchVar.getSecond()))
                iterLeft.remove();
        }

        // filtering columns for right node
        Iterator<League> iterRight = rightNode.leagues.iterator();
        while (iterRight.hasNext()) {
            League league = iterRight.next();
            if (league.contains(branchVar.getFirst()) && league.contains(branchVar.getSecond()))
                iterRight.remove();
        }

        return Arrays.asList(leftNode, rightNode);
    }

    /**
     * Creates the children nodes and solve them. A strong branching strategy is
     * used, and executed in parallel.
     *
     * @return the list of generated children (solved) nodes
     */
    public List<BranchNode> openChildrenNodes(long finishTimeMillis, Queue<ColumnGeneration> cgs, int nBranches, PrintStream output) throws ExecutionException, InterruptedException {
        LinkedList<FracVar> vars = new LinkedList<>(this.vars);
        List<List<BranchNode>> childrenNodes = new Vector<>(nBranches);

        CountDownLatch countDownLatch = new CountDownLatch(nBranches * 2);
        ExecutorService executor = ThreadPool.get();

        int nThreads = 0;
        for (int i = 0; i < nBranches; i++) {
            if (vars.isEmpty()) break;
            PairInt candidate = i == 0 ? vars.pollFirst() : vars.remove(random.nextInt(vars.size()));

            List<BranchNode> children = makeChildrenNodes(candidate);
            childrenNodes.add(children);

            for (BranchNode node : children) {
                nThreads++;
                try {
                    executor.submit(() -> {
                        ColumnGeneration cg = cgs.poll();
                        try {
                            node.solve(finishTimeMillis, 1, cg, output);
                        }
                        catch (InterruptedException e) {
                            if (System.currentTimeMillis() < finishTimeMillis)
                                e.printStackTrace();
                        }
                        catch (Exception | Error e) {
                            e.printStackTrace();
                        }
                        finally {
                            ThreadPool.get().release();
                            cgs.add(cg);
                            countDownLatch.countDown();
                        }
                    });
                }
                catch (Exception e) {
                    if (System.currentTimeMillis() < finishTimeMillis)
                        e.printStackTrace();
                }
            }
        }

        if (System.currentTimeMillis() > finishTimeMillis)
            throw new InterruptedException("time limit reached");

        if (nThreads < nBranches * 2) {
            for (int i = 0; i < nThreads - (nBranches * 2); i++)
                countDownLatch.countDown();
            ThreadPool.get().release(nBranches * 2 - nThreads);
        }

        if (System.currentTimeMillis() > finishTimeMillis)
            throw new InterruptedException("time limit reached");

        countDownLatch.await();

        if (childrenNodes.size() > 1)
            childrenNodes.sort((a, b) -> {
                int nFracA = (a.get(0).isFractional() ? 1 : 0) + (a.get(1).isFractional() ? 1 : 0);
                int nFracB = (b.get(0).isFractional() ? 1 : 0) + (b.get(1).isFractional() ? 1 : 0);
                if (nFracA != nFracB) return Integer.compare(nFracA, nFracB);

                boolean aInt = a.get(0).isInteger() || a.get(1).isInteger();
                boolean bInt = b.get(0).isInteger() || b.get(1).isInteger();
                if (aInt && !bInt) return -1;
                if (bInt && !aInt) return 1;

                double aMin = Math.min(a.get(0).lowerBound, a.get(1).lowerBound);
                double bMin = Math.min(b.get(0).lowerBound, b.get(1).lowerBound);
                if (Math.abs(aMin - bMin) < Constants.EPS)
                    return -Double.compare(aMin, bMin);

                double aMax = Math.max(a.get(0).lowerBound, a.get(1).lowerBound);
                double bMax = Math.max(b.get(0).lowerBound, b.get(1).lowerBound);
                return -Double.compare(aMax, bMax);
            });

        return childrenNodes.get(0);
    }

    /**
     * Solves the node.
     *
     * @return true if the node is solved and false otherwise (it returns false
     * if the node is infeasible, for instance)
     * @throws InterruptedException if any interruption occurs during the
     *                              execution
     * @throws ExecutionException   if any error occurs during the execution
     */
    public boolean solve(long finishTimeMillis, int nThreads, ColumnGeneration cg) throws InterruptedException, ExecutionException {
        return feasible = solve(finishTimeMillis, nThreads, cg, null);
    }

    /**
     * Solves the node.
     *
     * @param output the output stream (for log purposes)
     * @return true if the node is solved and false otherwise (it returns false
     * if the node is infeasible, for instance)
     * @throws InterruptedException if any interruption occurs during the
     *                              execution
     * @throws ExecutionException   if any error occurs during the execution
     */
    public boolean solve(long finishTimeMillis, int nThreads, ColumnGeneration cg, PrintStream output) throws InterruptedException, ExecutionException {
        cg.reset();
        cg.currentNode = this;

        // adding known columns and fixations to column generation
        for (League league : leagues)
            cg.addLambda(new Column(league, 0));
        for (PairInt entry : fixedOne)
            cg.fix(entry.getFirst(), entry.getSecond(), true);
        for (PairInt entry : fixedZero)
            cg.fix(entry.getFirst(), entry.getSecond(), false);

        // solving column generation for this node
        boolean solved = cg.solve(finishTimeMillis, nThreads, output);
        if (!solved || cg.hasArtificialVars()) {
            solution = null;
            lowerBound = Double.MAX_VALUE;
            return feasible = false;
        }

        lowerBound = cg.getObjValue();
        Collection<Column> columns = cg.getColumns();

        // storing all columns as leagues
        leagues.clear();
        columns.forEach(c -> leagues.add(c.league));

        // converting solution to (t,l) format
        Map<PairInt, Double> varsMap = new TreeMap<>();
        for (Column col : columns) {
            if (col.getValue() > Constants.EPS && col.getValue() < 1 - Constants.EPS) {
                for (Team teamI : col.league.teams) {
                    Set<Team> nextTeams = col.league.teams.tailSet(teamI, false);
                    for (Team teamJ : nextTeams) {
                        assert teamI.id < teamJ.id : "error in the teams order";
                        PairInt key = new PairInt(teamI.id, teamJ.id);

                        Double previousValue = varsMap.get(key);
                        varsMap.put(key, (previousValue == null ? 0 : previousValue) + col.getValue());
                    }
                }
            }
        }

        // filtering vars with value = 1 (case with multiple columns with the same teams)
        Iterator<Map.Entry<PairInt, Double>> iter = varsMap.entrySet().iterator();
        while (iter.hasNext()) {
            double value = iter.next().getValue();
            if (value > 1 - Constants.EPS) {
                assert value < 1.0 + Constants.EPS : "value is crazy high!";
                iter.remove();
            }
        }

        // is the solution integer? if so, return it!
        if (varsMap.isEmpty()) {
            solution = new Solution(cg.problem);
            for (Column col : columns)
                if (col.getValue() > Constants.EPS)
                    solution.addLeague(col.league.clone(solution));
            assert solution.validate() : "invalid solution returned";
        }
        // if not, then store the fractional variables
        else {
            vars.clear();
            for (Map.Entry<PairInt, Double> entry : varsMap.entrySet())
                vars.add(new FracVar(entry.getKey().getFirst(), entry.getKey().getSecond(), entry.getValue()));
            vars.sort(this::compareVars);
        }

        return feasible = true;
    }

    /**
     * Compares two variable entries, returning -1 if the first is smaller, 0 if
     * they are equal and +1 if the second is smaller. This method is used to
     * select the pair of teams (variable) to branch on.
     *
     * @param first  first entry
     * @param second second entry
     * @return -1 if first is smaller than second, 0 if they are equal and +1 if
     * first is grated than second
     */
    private int compareVars(FracVar first, FracVar second) {
        int result;

        result = -Integer.compare(problem.teams[first.getFirst()].getWeightedDistTimeTo(problem.teams[first.getSecond()]),
          problem.teams[second.getFirst()].getWeightedDistTimeTo(problem.teams[second.getSecond()]));
        if (result != 0) return result;

        double sizeFirst = Math.min(1 - first.getValue(), first.getValue());
        //sizeFirst += teamCliques[first.getFirst()] != null ? teamCliques[first.getFirst()].size() : 0;
        //sizeFirst += teamCliques[first.getSecond()] != null ? teamCliques[first.getSecond()].size() : 0;

        double sizeSecond = Math.min(1 - second.getValue(), second.getValue());
        //sizeSecond += teamCliques[second.getFirst()] != null ? teamCliques[second.getFirst()].size() : 0;
        //sizeSecond += teamCliques[second.getSecond()] != null ? teamCliques[second.getSecond()].size() : 0;

        result = -Double.compare(sizeFirst, sizeSecond);
        if (result != 0) return result;

        //int conflictsFirst = teamConflicts[first.getFirst()] + teamConflicts[first.getSecond()];
        //int conflictsSecond = teamConflicts[second.getFirst()] + teamConflicts[second.getSecond()];
        //
        //result = Integer.compare(conflictsFirst, conflictsSecond);
        //if (result != 0) return result;

        return result;
    }


    private static class FracVar extends PairInt {

        public final double value;

        public FracVar(int first, int second, double value) {
            super(first, second);
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }

    private static class Clique extends TreeSet<Team> {

        public Clique() {
            super();
        }

        public Clique(Clique clique) {
            super(clique);
        }
    }
}

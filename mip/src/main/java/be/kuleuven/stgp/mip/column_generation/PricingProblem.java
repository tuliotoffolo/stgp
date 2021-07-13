package be.kuleuven.stgp.mip.column_generation;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.util.*;

import java.util.*;

/**
 * This class represents the Pricing problem of the Column Generation.
 *
 * @author Tulio Toffolo
 */
public class PricingProblem {

    public final Problem problem;

    public ArrayList<Node> nodes;

    private HashMap<PairInt, Integer> distances = new HashMap<>();
    private int codCounter;
    private double cutoff;

    public PricingProblem(Problem problem, double duals[], double cutoff) {
        this.problem = problem;
        this.cutoff = cutoff;
        this.nodes = new ArrayList<>(problem.teams.length);
        this.codCounter = problem.teams.length;

        // adding nodes
        for (int i = 0; i < problem.teams.length; i++)
            nodes.add(new Node(this, problem.teams[i], nodes.size(), codCounter++));

        // adding dual costs
        for (int i = 0; i < problem.teams.length; i++)
            nodes.get(i).dualCost = duals[i];

        // adding distances
        for (int i = 0; i < problem.teams.length; i++)
            for (int j = i + 1; j < problem.teams.length; j++)
                if (problem.teams[i].isCompatible(problem.teams[j]))
                    distances.put(new PairInt(i, j), problem.teams[i].getTravelDistTo(problem.teams[j]));


    }

    public PricingProblem(PricingProblem pricingProblem, double duals[], double cutoff) {
        this.problem = pricingProblem.problem;
        this.cutoff = cutoff;
        this.nodes = new ArrayList<>(pricingProblem.nodes.size());
        this.codCounter = pricingProblem.codCounter;

        // adding nodes
        for (Node node : pricingProblem.nodes)
            nodes.add(new Node(this, node, nodes.size(), node.cod));

        // adding dual costs
        for (Node node : pricingProblem.nodes) {
            node.dualCost = 0;
            for (Team team : node.teams)
                node.dualCost += duals[team.id];
        }

        // adding distances
        distances.putAll(pricingProblem.distances);
    }

    public void disconnectNodes(Node nodeI, Node nodeJ) {
        distances.remove(getNodePairInt(nodeI, nodeJ));
    }

    public double getCutoff() {
        return cutoff;
    }

    public int getDist(int i, int j) {
        return getDist(nodes.get(i), nodes.get(j));
    }

    public int getDist(Node nodeI, Node nodeJ) {
        Integer dist = distances.get(new PairInt(nodeI.cod, nodeJ.cod));
        if (dist == null) return Integer.MAX_VALUE;
        return dist;
    }

    public double getNodeCost(int i) {
        return nodes.get(i).getCost();
    }

    public void mergeNodes(int i, int j) {
        Node n = new Node(this, nodes.get(i), nodes.get(j), nodes.size(), codCounter++);

        for (Node node : nodes) {
            if (node.id == i || node.id == j) continue;

            Integer distI = distances.remove(getNodePairInt(node, nodes.get(i)));
            Integer distJ = distances.remove(getNodePairInt(node, nodes.get(i)));

            if (distI != null && distJ != null)
                distances.put(getNodePairInt(node, n), distI + distJ);
        }

        Node nodeI = nodes.remove(i);
        Node nodeJ = nodes.remove(j);
    }

    public void setCutoff(double cutoff) {
        this.cutoff = cutoff;
    }

    public static PairInt getNodePairInt(Node nodeI, Node nodeJ) {
        return new PairInt(Math.min(nodeI.cod, nodeJ.cod), Math.max(nodeI.cod, nodeJ.cod));
    }

    public static class Node {

        public final PricingProblem problem;
        public final int cod;
        public int id;

        public int internalDistance;
        public double dualCost;

        public final TreeSet<Club> clubs = new TreeSet<>();
        public final TreeSet<Integer> levels = new TreeSet<>();
        public final ArrayList<Team> teams = new ArrayList<>();

        public Node(PricingProblem pricingProblem, Team team, int id, int cod) {
            this.problem = pricingProblem;
            this.id = id;
            this.cod = cod;
            this.internalDistance = 0;

            clubs.add(team.club);
            levels.add(team.level);
            teams.add(team);
        }

        public Node(PricingProblem pricingProblem, Node node, int id, int cod) {
            this.problem = pricingProblem;
            this.id = id;
            this.cod = cod;
            this.internalDistance = node.internalDistance;

            clubs.addAll(node.clubs);
            levels.addAll(node.levels);
            teams.addAll(node.teams);
        }

        public Node(PricingProblem pricingProblem, Node nodeI, Node nodeJ, int id, int cod) {
            this.problem = pricingProblem;
            this.id = id;
            this.cod = cod;
            this.internalDistance = nodeI.internalDistance + nodeJ.internalDistance + pricingProblem.getDist(nodeI, nodeJ);

            clubs.addAll(nodeI.clubs);
            clubs.addAll(nodeJ.clubs);
            levels.addAll(nodeI.levels);
            levels.addAll(nodeJ.levels);
            teams.addAll(nodeI.teams);
            teams.addAll(nodeJ.teams);

            // merging dual costs
            this.dualCost = nodeI.dualCost + nodeJ.dualCost;
        }

        public double getCost() {
            return internalDistance + dualCost;
        }

        public int getSize() {
            return teams.size();
        }

        public int getTravelDistTo(Node node) {
            return problem.getDist(id, node.id);
        }

        public boolean isCompatible(Node node) {
            boolean levelCompatible = levels.last() - node.levels.first() <= 1 && node.levels.last() - levels.first() <= 1;
            return problem.getDist(this, node) < Integer.MAX_VALUE && levelCompatible;
        }
    }
}

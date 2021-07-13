package be.kuleuven.stgp.mip.mip;

import be.kuleuven.codes.mp.*;
import be.kuleuven.codes.mp.solvers.*;
import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.mip.util.*;
import gurobi.*;

import java.io.*;
import java.util.*;

public class MIP_1 {

    public final Problem problem;
    MPModel model;

    int maxL;

    MPVar x[][];
    MPVar y[][];
    MPVar z[];

    public MIP_1(Problem problem) {
        this.problem = problem;
        maxL = problem.teams.length / problem.minLeagueSize;

        model = new MPModel(problem.name);
        createMPVars();
        createConstraints();
        createValidInequalities();
        createSymBreakConstraints();

        //setBranchingPriority();
    }

    public double getLb() {
        return model.getSolver().getBestBound();
    }

    public double getRootLb() {
        return model.getSolver().getRootBound();
    }

    public Solution solve(long runtimeMillis) {
        // creating solver
        //MPSolver solver = new SolverLocalSolver(model);
        MPSolver solver = new SolverGurobi(model);
        model.setSolver(solver);

        // running solver
        solver.setParam(MPSolver.IntParam.Threads, 1);
        solver.setParam(MPSolver.DoubleParam.TimeLimit, runtimeMillis / 1000);
        //model.write("temp.lp");
        solver.solve();

        // converting solution from solver
        double[] solverSol = solver.getSolution();
        Solution solution = new Solution(problem);
        for (int t = 0; t < x.length; t++) {
            for (int l = 0; l < x[t].length; l++) {
                if (x[t][l] != null && solverSol[x[t][l].getIndex()] > Constants.EPS) {
                    while (l >= solution.getNLeagues())
                        solution.addLeague(new League(problem));
                    solution.add(t, l);
                }
            }
        }

        System.out.printf("\nObjective = %.2f\n", solver.getObjValue());
        return solution;
    }

    private void createMPVars() {
        // creating x_{il} variables
        x = new MPVar[problem.teams.length][maxL];
        for (int l = 0; l < maxL; l++) {
            for (int i = 0; i < problem.teams.length; i++) {
                x[i][l] = model.addBinVar(String.format("x(%d,%d)", i, l));
            }
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
                    y[i][j] = model.addBinVar(2 * teamI.getWeightedDistTimeTo(teamJ), String.format("y(%d,%d)", i, j));
                }
                else {
                    y[i][j] = model.addNumVar(0., 0., String.format("y(%d,%d)", i, j));
                }
            }
        }

        // creating z_{l} variables
        z = new MPVar[maxL];
        for (int l = 0; l < maxL; l++) {
            z[l] = model.addBinVar(String.format("z(%d)", l));
        }
    }

    private void createConstraints() {
        // every team must be assigned to exactly one league
        for (int i = 0; i < problem.teams.length; i++) {
            MPLinExpr lhs = new MPLinExpr();
            for (int l = 0; l < maxL; l++)
                lhs.addTerm(1., x[i][l]);

            model.addConstr(lhs, '=', 1, String.format("team_assignment(%d)", i));
        }

        // minimum and maximum number of teams per league
        for (int l = 0; l < maxL; l++) {
            MPLinExpr lhs = new MPLinExpr();
            for (int i = 0; i < problem.teams.length; i++)
                lhs.addTerm(1., x[i][l]);

            MPLinExpr min = new MPLinExpr();
            min.addTerm(problem.minLeagueSize, z[l]);

            MPLinExpr max = new MPLinExpr();
            max.addTerm(problem.maxLeagueSize, z[l]);

            model.addConstr(lhs, '>', min, String.format("min_league_size(%d)", l));
            model.addConstr(lhs, '<', max, String.format("max_league_size(%d)", l));
        }

        // maximum number of teams per league
        for (int l = 0; l < maxL; l++) {
            for (Club club : problem.clubs) {
                MPLinExpr lhs = new MPLinExpr();
                int counter = 0;
                for (Team team : club.teams) {
                    lhs.addTerm(1., x[team.id][l]);
                    counter++;
                }
                if (counter > 0) {
                    MPLinExpr max = new MPLinExpr();
                    max.addTerm(problem.maxTeamSameClub, z[l]);
                    model.addConstr(lhs, '<', max, String.format("max_teams_per_club(c%d,%d)", club.id, l));
                }
            }
        }

        // activation of variables *Y*
        for (int i = 0; i < problem.teams.length; i++) {
            for (int j = i + 1; j < problem.teams.length; j++) {

                for (int l = 0; l < maxL; l++) {
                    MPLinExpr lhs = new MPLinExpr();
                    lhs.addTerm(1., x[i][l]);
                    lhs.addTerm(1., x[j][l]);
                    lhs.addTerm(-1., y[i][j]);

                    model.addConstr(lhs, '<', 1, String.format("y_activation(%d,%d,%d)", i, j, l));
                }
            }
        }
    }

    private void createValidInequalities() {
        // *X* < *Z* constraints
        for (int i = 0; i < problem.teams.length; i++) {
            for (int l = 0; l < maxL; l++) {
                model.addConstr(x[i][l], '<', z[l], String.format("x_z(%d,%d)", i, l));
            }
        }

        // minimum and maximum number of teams per league (cut using *Y* variables)
        for (Team team : problem.teams) {
            MPLinExpr lhs = new MPLinExpr();
            for (int i = 0; i < team.id; i++)
                if (y[i][team.id] != null)
                    lhs.addTerm(1., y[i][team.id]);
            for (int j = team.id + 1; j < problem.teams.length; j++)
                if (y[team.id][j] != null)
                    lhs.addTerm(1., y[team.id][j]);

            model.addConstr(lhs, '>', problem.minLeagueSize - 1, String.format("min_league_size_cut(%d)", team.id));
            model.addConstr(lhs, '<', problem.maxLeagueSize - 1, String.format("max_league_size_cut(%d)", team.id));
        }

        // triangle inequalities, for each triple (i,j,k)
        //for (int i = 0; i < problem.teams.length; i++) {
        //    for (int j = i + 1; j < problem.teams.length; j++) {
        //        for (int k = j + 1; k < problem.teams.length; k++) {
        //            MPLinExpr lhs = new MPLinExpr();
        //            lhs.addTerm(1.0, y[i][j]);
        //            lhs.addTerm(1.0, y[i][k]);
        //            lhs.addTerm(-1.0, y[j][k]);
        //            model.addConstr(lhs, '<', 1, "triangular(%d,%d,%d)", i, j, k);
        //
        //            lhs = new MPLinExpr();
        //            lhs.addTerm(1.0, y[i][j]);
        //            lhs.addTerm(1.0, y[j][k]);
        //            lhs.addTerm(-1.0, y[i][k]);
        //            model.addConstr(lhs, '<', 1, "triangular(%d,%d,%d)", j, i, k);
        //
        //            lhs = new MPLinExpr();
        //            lhs.addTerm(1.0, y[i][k]);
        //            lhs.addTerm(1.0, y[j][k]);
        //            lhs.addTerm(-1.0, y[i][j]);
        //            model.addConstr(lhs, '<', 1, "triangular(%d,%d,%d)", k, i, j);
        //        }
        //    }
        //}
    }

    private void createSymBreakConstraints() {
        // symmetry breaking constraints
        for (int l = 1; l < maxL; l++) {
            model.addConstr(z[l - 1], '>', z[l], String.format("sym_breaking(%d)", l));
        }

        // symmetry breaking constraints (2)
        //for (int l = 1; l < maxL; l++) {
        //    MPLinExpr lhs = new MPLinExpr();
        //    for (Team team : problem.teams)
        //        lhs.addTerm(1., x[team.id][l - 1]);
        //
        //    MPLinExpr rhs = new MPLinExpr();
        //    for (Team team : problem.teams)
        //        rhs.addTerm(1., x[team.id][l]);
        //
        //    model.addConstr(lhs, '>', rhs, String.format("sym_breaking_2(%d)", l));
        //}
    }

    /*
    private void setBranchingPriority()  {
        // priorities for Z variables
        int priorities[] = new int[z.length];
        Arrays.fill(priorities, 10);
        model.set(GRB.IntAttr.BranchPriority, z, priorities);

        // priorities for X variables
        priorities = new int[maxL];
        Arrays.fill(priorities, 100);
        for (int i = 0; i < problem.teams.length; i++)
            model.set(GRB.IntAttr.BranchPriority, x[i], priorities);
    }
    */

    public static void main(String args[]) throws IOException, GRBException {
        Locale.setDefault(new Locale("en-US"));
        long startTimeMillis = System.currentTimeMillis();

        Problem problem = new Problem(args[0]);
        MIP_1 mip = new MIP_1(problem);
        //MIP_2 mip = new MIP_2(problem);

        System.out.println();
        Solution solution = mip.solve(Integer.parseInt(args[2]) * 1000);

        //System.out.printf("Root bound...: %.2f\n", mip.getRootLb());
        //System.out.printf("Best bound...: %.2f\n", mip.getLb());
        if (solution.validate(System.out)) {
            solution.write(args[1]);
            System.out.printf("Solution cost: %d\n", solution.getObjective());
        }

        System.out.printf("Total runtime: %.2f\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);
    }
}

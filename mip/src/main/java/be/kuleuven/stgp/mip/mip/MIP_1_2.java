package be.kuleuven.stgp.mip.mip;

import be.kuleuven.codes.mp.*;
import be.kuleuven.codes.mp.solvers.*;
import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.mip.column_generation.*;
import be.kuleuven.stgp.mip.util.*;
import gurobi.*;

import java.io.*;
import java.util.*;

public class MIP_1_2 {

    public final Problem problem;
    MPModel model;

    int maxL;

    MPVar x[][];
    MPVar y[][][];

    public MIP_1_2(Problem problem) {
        this.problem = problem;
        maxL = problem.teams.length / problem.minLeagueSize;

        model = new MPModel(problem.name);
        createMPVars();
        createConstraints();
        createValidInequalities();
        createSymBreakConstraints();

        //setBranchingPriority();
    }

    public Solution solve(long timeLimitMillis) {
        // creating solver
        //MPSolver solver = new SolverCplex(model);
        MPSolver solver = new SolverGurobi(model);
        model.setSolver(solver);

        // running solver
        solver.setParam(MPSolver.IntParam.Threads, Parameters.get().nThreads / 4);
        solver.setParam(MPSolver.DoubleParam.TimeLimit, timeLimitMillis / 1000);
        //model.writeTxt("temp.lp");
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
                x[i][l] = model.addVar(0., 1., 0., 'B', String.format("x(%d,%d)", i, l));
            }
        }

        // creating y_{ij} variables
        y = new MPVar[problem.teams.length][problem.teams.length][maxL];
        for (int l = 0; l < maxL; l++) {
            for (int i = 0; i < problem.teams.length; i++) {
                Team teamI = problem.teams[i];

                for (int j = i + 1; j < problem.teams.length; j++) {
                    Team teamJ = problem.teams[j];

                    if (Math.abs(teamI.level - teamJ.level) <= problem.maxLevelDiff
                      && teamI.getTravelDistTo(teamJ) <= problem.maxTravelDist
                      && teamI.getTravelTimeTo(teamJ) <= problem.maxTravelTime) {
                        y[i][j][l] = model.addVar(0., 1., 2 * teamI.getWeightedDistTimeTo(teamJ), 'B', String.format("y(%d,%d,%d)", i, j, l));
                    }
                    else {
                        y[i][j][l] = model.addVar(0., 0., 0., 'B', String.format("y(%d,%d,%d)", i, j, l));
                    }
                }
            }
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
        for (int i = 0; i < problem.teams.length; i++) {
            for (int l = 0; l < maxL; l++) {
                MPLinExpr lhs = new MPLinExpr();
                for (int j = 0; j < i; j++)
                    lhs.addTerm(1., y[j][i][l]);
                for (int j = i + 1; j < problem.teams.length; j++)
                    lhs.addTerm(1., y[i][j][l]);

                MPLinExpr min = new MPLinExpr();
                min.addTerm(problem.minLeagueSize - 1, x[i][l]);

                MPLinExpr max = new MPLinExpr();
                max.addTerm(problem.maxLeagueSize - 1, x[i][l]);

                model.addConstr(lhs, '>', min, String.format("min_league_size(%d,%d)", i, l));
                model.addConstr(lhs, '<', max, String.format("max_league_size(%d,%d)", i, l));
            }
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
                if (counter > problem.maxTeamSameClub)
                    model.addConstr(lhs, '<', problem.maxTeamSameClub, String.format("max_teams_per_club(c%d,%d)", club.id, l));
            }
        }

        // activation of variables *Y*
        for (int i = 0; i < problem.teams.length; i++) {
            for (int j = i + 1; j < problem.teams.length; j++) {
                for (int l = 0; l < maxL; l++) {
                    MPLinExpr lhs = new MPLinExpr();
                    lhs.addTerm(1., x[i][l]);
                    lhs.addTerm(1., x[j][l]);
                    lhs.addTerm(-1., y[i][j][l]);

                    model.addConstr(lhs, '<', 1, String.format("y_activation(%d,%d,%d)", i, j, l));
                }
            }
        }
    }

    private void createValidInequalities() {
        // *Y* and *Z* direct coupling
        //for (Team team : problem.teams) {
        //    for (int j = team.id + 1; j < problem.teams.length; j++) {
        //        Team team2 = problem.teams[j];
        //        for (int l = 0; l < maxL; l++) {
        //            model.addConstr(y[team.id][team2.id][l], '<', z[l], String.format("y_z(%d,%d,%d)", team.id, team2.id, l));
        //        }
        //    }
        //}

        // *Y* and *X* direct coupling
        for (Team team : problem.teams) {
            for (int j = team.id + 1; j < problem.teams.length; j++) {
                Team team2 = problem.teams[j];
                for (int l = 0; l < maxL; l++) {
                    model.addConstr(y[team.id][team2.id][l], '<', x[team.id][l], String.format("y_x(%d,%d,%d)", team.id, team2.id, l));
                    model.addConstr(y[team.id][team2.id][l], '<', x[team2.id][l], String.format("y_x(%d,%d,%d)", team2.id, team.id, l));
                }
            }
        }
    }

    private void createSymBreakConstraints() {
        // symmetry breaking constraints
        //for (int l = 1; l < maxL; l++) {
        //    model.addConstr(z[l - 1], '>', z[l], String.format("sym_breaking(%d)", l));
        //}

        // symmetry breaking constraints (2)
        for (int l = 1; l < maxL; l++) {
            MPLinExpr lhs = new MPLinExpr();
            for (Team team : problem.teams)
                lhs.addTerm(1., x[team.id][l - 1]);

            MPLinExpr rhs = new MPLinExpr();
            for (Team team : problem.teams)
                rhs.addTerm(1., x[team.id][l]);

            model.addConstr(lhs, '>', rhs, String.format("sym_breaking_2(%d)", l));
        }
    }

    /*
    private void setBranchingPriority()  {
        // priorities for X variables
        int priorities[] = new int[maxL];
        Arrays.fill(priorities, 10);

        for (int i = 0; i < problem.teams.length; i++)
            model.set(GRB.IntAttr.BranchPriority, x[i], priorities);
    }
    */
    public static void main(String args[]) throws IOException, GRBException {
        Locale.setDefault(new Locale("en-US"));
        long startTimeMillis = System.currentTimeMillis();

        Problem problem = new Problem(args[0]);
        //MIP_1 mip = new MIP_1(problem);
        MIP_1_2 mip = new MIP_1_2(problem);

        System.out.println();
        Solution solution = mip.solve(Integer.parseInt(args[2]) * 1000);
        if (solution.validate(System.err)) {
            solution.write(args[1]);
            System.out.printf("Solution cost: %d\n", solution.getObjective());
        }

        System.out.printf("Total runtime: %.2f\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);
    }
}

package be.kuleuven.stgp.mip.mip;

import be.kuleuven.codes.mp.*;
import be.kuleuven.codes.mp.solvers.*;
import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.mip.util.*;
import gurobi.*;

import java.io.*;
import java.util.*;

public class MIP_2 {

    public final Problem problem;
    MPModel model;

    int maxL;

    MPVar y[][];

    public MIP_2(Problem problem) {
        this.problem = problem;
        maxL = problem.teams.length / problem.minLeagueSize;

        model = new MPModel(problem.name);
        createMPVars();
        createConstraints();
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

        // running solver
        solver.setParam(MPSolver.IntParam.Threads, 1);
        solver.setParam(MPSolver.DoubleParam.TimeLimit, runtimeMillis / 1000);
        //solver.writeModel("temp.lp");
        solver.solve();

        // converting solution from solver
        Solution solution = new Solution(problem);
        try {
            double[] solverSol = solver.getSolution();
            for (int i = 0; i < problem.teams.length; i++) {
                if (solution.getLeague(i) != null)
                    continue;

                League league = new League(problem);
                Team teamI = problem.teams[i];
                league.add(teamI);

                for (int j = i + 1; j < problem.teams.length; j++) {
                    Team teamJ = problem.teams[j];
                    if (solverSol[y[i][j].getIndex()] > Constants.EPS)
                        league.add(teamJ);
                }

                solution.addLeague(league);
            }
        }
        catch (Exception ignore) {}

        System.out.println();
        return solution;
    }

    private void createMPVars() {
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
    }

    private void createConstraints() {
        // triangle inequalities, for each triple (i,j,k)
        for (int i = 0; i < problem.teams.length; i++) {
            for (int j = i + 1; j < problem.teams.length; j++) {
                for (int k = j + 1; k < problem.teams.length; k++) {
                    MPLinExpr lhs = new MPLinExpr();
                    lhs.addTerm(1.0, y[i][j]);
                    lhs.addTerm(1.0, y[i][k]);
                    lhs.addTerm(-1.0, y[j][k]);
                    model.addConstr(lhs, '<', 1, "triangular(%d,%d,%d)", i, j, k);

                    lhs = new MPLinExpr();
                    lhs.addTerm(1.0, y[i][j]);
                    lhs.addTerm(1.0, y[j][k]);
                    lhs.addTerm(-1.0, y[i][k]);
                    model.addConstr(lhs, '<', 1, "triangular(%d,%d,%d)", j, i, k);

                    lhs = new MPLinExpr();
                    lhs.addTerm(1.0, y[i][k]);
                    lhs.addTerm(1.0, y[j][k]);
                    lhs.addTerm(-1.0, y[i][j]);
                    model.addConstr(lhs, '<', 1, "triangular(%d,%d,%d)", k, i, j);
                }
            }
        }

        // minimum and maximum number of teams per league
        for (Team team : problem.teams) {
            MPLinExpr lhs = new MPLinExpr();
            for (int i = 0; i < team.id; i++)
                lhs.addTerm(1., y[i][team.id]);
            for (int j = team.id + 1; j < problem.teams.length; j++)
                lhs.addTerm(1., y[team.id][j]);

            model.addConstr(lhs, '>', problem.minLeagueSize - 1, "min_league_size(%d)", team.id);
            model.addConstr(lhs, '<', problem.maxLeagueSize - 1, "max_league_size(%d)", team.id);
        }

        // maximum number of teams per league
        for (Team team : problem.teams) {
            MPLinExpr lhs = new MPLinExpr();
            int counter = 0;

            for (Team sameClubTeam : team.club.teams) {
                if (sameClubTeam == team) continue;
                lhs.addTerm(1., sameClubTeam.id > team.id ? y[team.id][sameClubTeam.id] : y[sameClubTeam.id][team.id]);
                counter++;
            }

            if (counter > 0)
                model.addConstr(lhs, '<', problem.maxTeamSameClub - 1, "max_teams_per_club(%d)", team.id);
        }
    }


    public static void main(String args[]) throws IOException, GRBException {
        Locale.setDefault(new Locale("en-US"));
        long startTimeMillis = System.currentTimeMillis();

        Problem problem = new Problem(args[0]);
        MIP_2 mip = new MIP_2(problem);

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

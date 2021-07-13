package be.kuleuven.stgp.mip.mip;

import be.kuleuven.stgp.core.model.*;
import gurobi.*;

import java.util.*;

public class QuadMIP_1 {

    public final Problem problem;
    GRBModel model;

    int maxL;

    GRBVar x[][];
    GRBVar y[][];
    GRBVar z[];

    public QuadMIP_1(Problem problem) throws GRBException {
        this.problem = problem;
        maxL = problem.teams.length / problem.minLeagueSize;

        model = new GRBModel(new GRBEnv());
        createVariables();
        createObjective();
        createConstraints();
        createValidInequalities();
        createSymBreakConstraints();

        setBranchingPriority();
    }

    private void createVariables() throws GRBException {
        // creating x_{il} variables
        x = new GRBVar[problem.teams.length][maxL];
        for (int l = 0; l < maxL; l++) {
            for (int i = 0; i < problem.teams.length; i++) {
                x[i][l] = model.addVar(0., 1., 0., 'B', String.format("x(%d,%d)", i, l));
            }
        }

        // creating z_{l} variables
        z = new GRBVar[maxL];
        for (int l = 0; l < maxL; l++) {
            z[l] = model.addVar(0., 1., 0., 'B', String.format("z(%d)", l));
        }

        model.update();
    }

    private void createObjective() throws GRBException {
        GRBQuadExpr expr = new GRBQuadExpr();
        for (int l = 0; l < maxL; l++) {
            for (int i = 0; i < problem.teams.length; i++) {
                Team teamI = problem.teams[i];

                for (int j = i + 1; j < problem.teams.length; j++) {
                    Team teamJ = problem.teams[j];

                    if (true || (Math.abs(teamI.level - teamJ.level) < problem.maxLevelDiff)) {
                        int distance = 2 * problem.travelDists[teamI.club.id][teamJ.club.id];
                        expr.addTerm(distance, x[i][l], x[j][l]);
                    }
                }
            }
        }

        model.setObjective(expr);
        model.update();
    }

    private void createConstraints() throws GRBException {
        // every team must be assigned to exactly one league
        for (int i = 0; i < problem.teams.length; i++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int l = 0; l < maxL; l++)
                lhs.addTerm(1., x[i][l]);

            model.addConstr(lhs, '=', 1, String.format("team_assignment(%d)", i));
        }

        // minimum and maximum number of teams per league
        for (int l = 0; l < maxL; l++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int i = 0; i < problem.teams.length; i++)
                lhs.addTerm(1., x[i][l]);

            GRBLinExpr min = new GRBLinExpr();
            min.addTerm(problem.minLeagueSize, z[l]);

            GRBLinExpr max = new GRBLinExpr();
            max.addTerm(problem.maxLeagueSize, z[l]);

            model.addConstr(lhs, '>', min, String.format("min_league_size(%d)", l));
            model.addConstr(lhs, '<', max, String.format("max_league_size(%d)", l));
        }

        // pair of prohibited teams in the same league
        //for (int i = 0; i < problem.teams.length; i++) {
        //    Team teamI = problem.teams[i];
        //    for (int j = i + 1; j < problem.teams.length; j++) {
        //        Team teamJ = problem.teams[j];
        //
        //        if (!(Math.abs(teamI.level - teamJ.level) < problem.maxLevelDiff && teamI.category == teamJ.category)) {
        //            for (int l = 0; l < maxL; l++) {
        //                GRBLinExpr expr = new GRBLinExpr();
        //                expr.addTerm(1.0, x[i][l]);
        //                expr.addTerm(1.0, x[j][l]);
        //                model.addConstr(expr, '<', 1, String.format("forbidden_pair(%d,%d,%d)", i, j, l));
        //            }
        //        }
        //    }
        //}

        model.update();
    }

    private void createValidInequalities() throws GRBException {
        // *X* < *Z* constraints
        for (int i = 0; i < problem.teams.length; i++) {
            for (int l = 0; l < maxL; l++) {
                model.addConstr(x[i][l], '<', z[l], String.format("x_z(%d,%d)", i, l));
            }
        }

        model.update();
    }

    private void createSymBreakConstraints() throws GRBException {
        // symmetry breaking constraints
        for (int l = 1; l < maxL; l++) {
            model.addConstr(z[l - 1], '>', z[l], String.format("sym_breaking(%d)", l));
        }

        // symmetry breaking constraints (2)
        for (int l = 1; l < maxL; l++) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (Team team : problem.teams)
                lhs.addTerm(1., x[team.id][l - 1]);

            GRBLinExpr rhs = new GRBLinExpr();
            for (Team team : problem.teams)
                rhs.addTerm(1., x[team.id][l]);

            model.addConstr(lhs, '>', rhs, String.format("sym_breaking_2(%d)", l));
        }
    }

    private void setBranchingPriority() throws GRBException {
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
}

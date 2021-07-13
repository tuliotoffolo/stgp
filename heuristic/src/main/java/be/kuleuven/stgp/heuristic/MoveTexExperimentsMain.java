package be.kuleuven.stgp.heuristic;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;

import java.io.*;

/**
 * Created by Jan on 25-1-2016.
 */
public class MoveTexExperimentsMain {

    public static void main(String[] args) throws IOException {
        long startTimeMillis = System.currentTimeMillis();
        String inputPath = args[0];
        String outputPath = args[1];
        int seconds = new Integer(args[2]);

        Problem problem = MovetexMain.readProblem(inputPath);
        Solver solver = new Solver(SolverListener.PRINT_LISTENER);
        Solution solution = solver.solve(problem, seconds);

        if (solution.validate(System.err)) {
            System.out.printf("Solution cost: %d\n", solution.getObjective());
            System.out.printf("Total runtime: %.2f seconds\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);
            solution.write(outputPath);
        }
    }
}

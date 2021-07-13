package be.kuleuven.stgp.mip;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;
import be.kuleuven.stgp.mip.mip.*;
import gurobi.*;

import java.io.*;
import java.util.*;

public class Main {

    static long startTimeMillis = System.currentTimeMillis();

    public static void main(String args[]) throws IOException, GRBException {
        Locale.setDefault(new Locale("en-US"));

        Problem problem = new Problem(args[0]);
        //MIP_1 mip = new MIP_1(problem);
        MIP_1_2 mip = new MIP_1_2(problem);

        Solution solution = mip.solve(Integer.parseInt(args[2]) * 1000);
        solution.validate(System.err);

        System.out.println();
        System.out.printf("Solution cost: %d\n", solution.getObjective());
        System.out.printf("Total runtime: %.2f\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);
        solution.write(args[1]);
    }
}

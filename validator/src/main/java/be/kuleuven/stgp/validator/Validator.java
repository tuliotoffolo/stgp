package be.kuleuven.stgp.validator;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.model.solution.*;

import java.io.*;

public class Validator {

    public static void main(String args[]) throws IOException {
        if (args.length != 2) {
            System.out.printf("Usage: java -jar stgp-validator.jar <problem_file> <solution_file>\n\n");
            return;
        }

        try {
            Problem problem = new Problem(args[0]);
            Solution solution = new Solution(problem, args[1]);
            if (solution.validate(System.out)) {
                //System.out.printf("Solution is valid and has objective value %d.\n\n", solution.getObjective());
                System.out.printf("%20s %10d.\n\n", solution.getObjective());
            }
            else {
                System.out.printf("Solution is NOT valid!\n\n");
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.printf("There was an error reading the problem and/or the solution file.\n");
            System.out.printf("Please double-check the files.\n\n");
        }
        catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            //System.out.printf("The solution contain errors.\n");
            System.out.printf("Solution is NOT valid!\n\n");
        }
        catch (Exception e) {
            System.out.printf("There was an error reading the problem and/or the solution file.\n");
            System.out.printf("Please double-check the files.\n\n");
        }
    }
}
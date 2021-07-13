package be.kuleuven.stgp.mip.io;

import be.kuleuven.stgp.core.model.*;

import java.io.*;
import java.util.*;

public class InstanceExporter {

    public static void main(String args[]) throws IOException {
        Locale.setDefault(new Locale("en-US"));

        Problem problem = new Problem(args[0]);
        problem.writeTxt(args[1]);

        //PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(args[1])));
        //writer.printf("min = %d\n", problem.minLeagueSize);
        //writer.printf("max = %d\n", problem.maxLeagueSize);
        //writer.printf("T = %d\n", problem.teams.length);
        //
        //writer.printf("Dij = [ \n");
        //for (int i = 0; i < problem.teams.length; i++) {
        //    writer.printf("");
        //    for (int j = 0; j < problem.teams.length; j++) {
        //        writer.printf("%d ", problem.teams[i].getTravelDistTo(problem.teams[j]));
        //    }
        //    writer.printf("\n");
        //}
        //writer.printf("]\n");
        //
        ////writer.printf("Tij = [ \n");
        ////for (int i = 0; i < problem.teams.length; i++) {
        ////    writer.printf("    [ ");
        ////    for (int j = 0; j < problem.teams.length; j++) {
        ////        writer.printf("%d, ", problem.teams[i].getTravelTimeTo(problem.teams[j]));
        ////    }
        ////    writer.printf("\n");
        ////}
        ////writer.printf("]\n");
        //
        //writer.close();
    }
}
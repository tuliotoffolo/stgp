package be.kuleuven.stgp.core.model.solution;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.util.*;
import com.google.gson.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * This class represents a Solution for the problem.
 *
 * @author Tulio Toffolo
 */
public class Solution implements Iterable<League> {

    public final Problem problem;

    public int objective;
    private List<League> leagues = new ArrayList<>();
    private League leagueTeam[];

    /**
     * Instantiates a new Solution.
     *
     * @param problem the problem
     */
    public Solution(Problem problem) {
        this.problem = problem;
        this.leagueTeam = new League[problem.teams.length];
    }

    /**
     * Instantiates a new Solution.
     *
     * @param problem the problem
     * @param inPath  the path to read the solution from
     * @throws IOException if any error occurs while reading the solution
     */
    public Solution(Problem problem, String inPath) throws IOException {
        this.problem = problem;
        this.leagueTeam = new League[problem.teams.length];
        read(inPath);
    }

    /**
     * Wrapper that adds a team to a league while updating the objective
     * function
     *
     * @param team   the team
     * @param league the league
     */
    public void add(int team, int league) {
        objective -= leagues.get(league).getObjective();
        leagues.get(league).add(problem.teams[team]);
        objective += leagues.get(league).getObjective();
    }

    /**
     * Adds a league to the solution.
     *
     * @param league the league
     */
    public void addLeague(League league) {
        leagues.add(league);
        objective += league.getObjective();
    }

    /**
     * Clones solution.
     *
     * @return the solution
     */
    public Solution clone() {
        Solution copy = new Solution(problem);
        for (League league : leagues)
            copy.addLeague(league.clone(copy));

        return copy;
    }

    /**
     * Returns league with index l
     *
     * @param l league index
     * *
     * @return league with index l
     */
    public League getLeague(int l) {
        return leagues.get(l);
    }

    /**
     * Gets the number of leagues in the solution.
     *
     * @return the number of leagues
     */
    public int getNLeagues() {
        return leagues.size();
    }

    /**
     * Gets objective value.
     *
     * @return objective value
     */
    public int getObjective() {
        return objective;
    }

    @Override
    public Iterator<League> iterator() {
        return leagues.iterator();
    }

    /**
     * Reads a solution from a file.
     *
     * @param inPath the input file path
     */
    public void read(String inPath) throws IOException {
        JsonObject json = null;

        try {
            json = ( JsonObject ) new JsonParser().parse(Files.newBufferedReader(Paths.get(inPath)));
        }
        catch (Exception ignore) {
            readSol(inPath, System.out);
            return;
        }

        if (json != null && json.isJsonObject())
            readJson(inPath, System.out);
        else
            readSol(inPath, System.out);
    }

    /**
     * Reads a solution from a json file.
     *
     * @param inPath the input file path
     * @param output the output stream (for log purposes)
     * @throws IOException if any error occurs while reading the solution
     */
    public void readJson(String inPath, PrintStream output) throws IOException {
        reset();


        // reading number of leagues and calculated costs
        try {
            JsonObject json = ( JsonObject ) new JsonParser().parse(Files.newBufferedReader(Paths.get(inPath)));
            int fileObjective = json.get("objective").getAsInt();

            JsonArray jsonLeagues = json.get("leagues").getAsJsonArray();
            for (int i = 0; i < jsonLeagues.size(); i++) {
                JsonArray jsonLeague = jsonLeagues.get(i).getAsJsonObject().get("teams").getAsJsonArray();
                League league = new League(problem);
                for (JsonElement jsonTeam : jsonLeague) {
                    int cod = jsonTeam.getAsInt();
                    Team team = problem.getTeamFromCod(cod);
                    if (team != null) {
                        league.add(team);
                    }
                    else {
                        Util.safePrintf(output, "Invalid team code: %d\n", cod);
                        //throw new IllegalArgumentException(String.format("Invalid team code: %d\n", cod));
                    }
                }
                addLeague(league);
            }

            if (fileObjective != objective) {
                Util.safePrintf(output, "Invalid objective value: %d != %d\n", fileObjective, objective);
                //objective = fileObjective;
                //throw new IllegalArgumentException(String.format("Invalid objective value: %d != %d\n", fileObjective, objective));
            }
        }
        catch (NullPointerException e) {
            throw new IllegalArgumentException("Solution file is invalid.");
        }
    }

    /**
     * Reads a solution from a file.
     *
     * @param inPath the input file path
     * @param output the output stream (for log purposes)
     * @throws IOException if any error occurs while reading the solution
     */
    public void readSol(String inPath, PrintStream output) throws IOException {
        reset();
        BufferedReader reader = Files.newBufferedReader(Paths.get(inPath));

        // reading number of leagues and calculated costs

        try {
            SimpleTokenizer token = new SimpleTokenizer(reader.readLine(), ";");
            int nLeagues = token.nextInt();
            int fileObjective = token.nextInt();

            for (int i = 0; i < nLeagues; i++) {
                token = new SimpleTokenizer(reader.readLine(), ";");

                League league = new League(problem);
                int nTeams = token.nextInt();
                for (int j = 0; j < nTeams; j++) {
                    int cod = token.nextInt();
                    Team team = problem.getTeamFromCod(cod);
                    if (team != null) {
                        league.add(team);
                    }
                    else {
                        Util.safePrintf(output, "Invalid team code: %d\n", cod);
                        //throw new IllegalArgumentException(String.format("Invalid team code: %d\n", cod));
                    }
                }

                addLeague(league);
            }

            if (fileObjective != objective) {
                objective = fileObjective;
                //Util.safePrintf(output, "Invalid objective value: %d != %d\n", fileObjective, objective);
                //throw new IllegalArgumentException(String.format("Invalid objective value: %d != %d\n", fileObjective, objective));
            }
        }
        catch (NullPointerException e) {
            throw new IllegalArgumentException("Solution file is invalid.");
        }

        reader.close();
    }

    /**
     * Resets the solution, i.e. removes all leagues and clear the objective
     * function.
     */
    public void reset() {
        objective = 0;
        leagues.clear();
        for (int i = 0; i < leagueTeam.length; i++)
            leagueTeam[i] = null;
    }

    /**
     * Validates the solution considering objective value and constraints.
     *
     * @return true if solution is valid and false otherwise
     */
    public boolean validate() {
        return validate(null);
    }

    /**
     * Validates the solution considering objective value and constraints.
     *
     * @param output the output stream (for log purposes)
     * @return true if solution is valid and false otherwise
     */
    public boolean validate(PrintStream output) {
        boolean valid = true;

        // checking team allocations
        boolean allocs[] = new boolean[problem.teams.length];
        for (League league : leagues) {
            for (Team team : league) {
                if (allocs[team.id]) {
                    valid = false;
                    Util.safePrintf(output, "Team %d (%s) is assigned to more than one league\n", team.cod, team.name);
                }
                allocs[team.id] |= true;
            }
        }

        // checking if all teams are assigned to first league
        for (int t = 0; t < problem.teams.length; t++) {
            if (!allocs[t]) {
                Team team = problem.teams[t];
                valid = false;
                Util.safePrintf(output, "Team %d (%s) is not allocated to any league\n", team.cod, team.name);
            }
        }

        // check whether intra-league constraints are respected
        for (League league : leagues)
            valid &= league.validate(output);

        // checking total objective
        int calcObj = 0;
        for (League league : leagues)
            calcObj += league.getObjective();
        if (objective != calcObj && valid) {
            valid = false;
            Util.safePrintf(output, "Objective value mismatch: %d vs %d\n", calcObj, objective);
        }

        return valid;
    }

    /**
     * Writes solution to file.
     *
     * @param outPath the output path
     * @throws IOException if any error occurs while writing solution to file
     */
    public void write(String outPath) throws IOException {
        writeJson(outPath);
    }

    /**
     * Writes solution to file using json format.
     *
     * @param outPath the output path
     * @throws IOException if any error occurs while writing solution to file
     */
    public void writeJson(String outPath) throws IOException {
        JsonObject json = new JsonObject();
        int time = 0, dist = 0;
        for (League league : leagues) {
            time += league.getTotalTravelTime();
            dist += league.getTotalTravelDist();
        }

        // printing parameters
        json.addProperty("problem", problem.name);
        json.addProperty("objective", objective);
        json.addProperty("time", time);
        json.addProperty("dist", dist);
        json.addProperty("nLeagues", leagues.size());

        JsonArray jsonLeagues = new JsonArray();
        for (League league : leagues) {
            JsonObject jsonLeague = new JsonObject();
            jsonLeague.addProperty("time", league.getTotalTravelTime());
            jsonLeague.addProperty("dist", league.getTotalTravelDist());
            jsonLeague.addProperty("nTeams", league.size());

            JsonArray jsonTeams = new JsonArray();
            for (Team team : league)
                jsonTeams.add(team.id);
            jsonLeague.add("teams", jsonTeams);

            jsonLeagues.add(jsonLeague);
        }
        json.add("leagues", jsonLeagues);

        PrintWriter printer = new PrintWriter(Files.newBufferedWriter(Paths.get(outPath)));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        printer.printf(gson.toJson(json));
        printer.close();
    }

    /**
     * Write solution to file.
     *
     * @param outPath the output path
     * @throws IOException if any error occurs while writing solution to file
     */
    public void writeSol(String outPath) throws IOException {
        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outPath)));

        writer.printf("%d;%d\n", leagues.size(), objective);
        for (League league : leagues) {
            writer.printf("%d", league.size());
            for (Team team : league)
                writer.printf(";%d", team.cod);
            writer.printf("\n");
        }

        writer.close();
    }
}

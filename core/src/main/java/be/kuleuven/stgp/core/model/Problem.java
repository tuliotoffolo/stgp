package be.kuleuven.stgp.core.model;

import be.kuleuven.stgp.core.util.*;
import com.google.gson.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * The class Problem.
 *
 * @author Tulio Toffolo
 */
public class Problem {

    public final String name;

    public final Club clubs[];
    public final Team teams[];

    public final int travelDists[][], travelTimes[][], weightedDistTime[][];
    public final int maxLevelDiff;
    public final int minLeagueSize, maxLeagueSize;
    public final int maxTeamSameClub, maxTravelDist, maxTravelTime;
    public final int weightTravelDist, weightTravelTime;

    private final List<Pair<Team, Team>> incompatibleTeams = new LinkedList<>();
    private final Map<Integer, Club> mapClubs = new HashMap<>();
    private final Map<Integer, Team> mapTeams = new HashMap<>();

    /**
     * Private constructor used by the Builder subclass.
     *
     * @param name             the name of the problem
     * @param nClubs           number of clubs
     * @param nTeams           number of teams
     * @param maxLevelDiff     maximum level difference among teams in the same
     *                         league
     * @param maxTeamSameClub  maximum number of teams from the same club
     * @param maxTravelDist    maximum travel distance
     * @param maxTravelTime    maximum travel time
     * @param minLeagueSize    minimum league size
     * @param maxLeagueSize    maximum league size
     * @param weightTravelDist weight for the travel distance in the objective
     *                         function
     * @param weightTravelTime weight for the travel time in the objective
     *                         function
     */
    private Problem(String name, int nClubs, int nTeams, int maxLevelDiff, int minLeagueSize, int maxLeagueSize, int maxTeamSameClub, int maxTravelDist, int maxTravelTime, int weightTravelDist, int weightTravelTime) {
        this.name = name;
        this.clubs = new Club[nClubs];
        this.teams = new Team[nTeams];
        this.travelDists = new int[nClubs][nClubs];
        this.travelTimes = new int[nClubs][nClubs];
        this.weightedDistTime = new int[nClubs][nClubs];
        this.maxLevelDiff = maxLevelDiff;
        this.minLeagueSize = minLeagueSize;
        this.maxLeagueSize = maxLeagueSize;
        this.maxTeamSameClub = maxTeamSameClub;
        this.maxTravelDist = maxTravelDist;
        this.maxTravelTime = maxTravelTime;
        this.weightTravelDist = weightTravelDist;
        this.weightTravelTime = weightTravelTime;

        // updating list of non-compatible teams
        for (int i = 0; i < teams.length; i++)
            for (int j = i + 1; j < teams.length; j++)
                if (!teams[i].isCompatible(teams[j]))
                    incompatibleTeams.add(new Pair<>(teams[i], teams[j]));
    }

    /**
     * Instantiates a new Problem.
     *
     * @param inPath the in path
     * @throws IOException the io exception
     */
    public Problem(String inPath) throws IOException {
        this.name = Paths.get(inPath).getFileName().toString().replace(".prob", "");

        JsonObject json = null;
        boolean newFormat;

        // checking if input file is a valid JSON file
        try {
            JsonParser parser = new JsonParser();
            json = ( JsonObject ) parser.parse(Files.newBufferedReader(Paths.get(inPath)));
            newFormat = json.isJsonObject();
        }
        catch (Exception ignore) {
            newFormat = false;
        }

        if (newFormat) {
            // reading parameters
            JsonObject jsonParams = json.get("params").getAsJsonObject();
            minLeagueSize = jsonParams.get("minLeagueSize").getAsInt();
            maxLeagueSize = jsonParams.get("maxLeagueSize").getAsInt();
            maxLevelDiff = jsonParams.get("maxLevelDiff").getAsInt();
            maxTeamSameClub = jsonParams.get("maxTeamSameClub").getAsInt();
            maxTravelDist = jsonParams.get("maxTravelDist").getAsInt();
            maxTravelTime = jsonParams.get("maxTravelTime").getAsInt();
            weightTravelDist = jsonParams.get("weightTravelDist").getAsInt();
            weightTravelTime = jsonParams.get("weightTravelTime").getAsInt();

            // reading clubs
            JsonArray jsonClubs = json.get("clubs").getAsJsonArray();
            clubs = new Club[jsonClubs.size()];
            for (int i = 0; i < jsonClubs.size(); i++) {
                JsonObject jsonClub = jsonClubs.get(i).getAsJsonObject();
                int id = jsonClub.get("id").getAsInt();
                int cod = jsonClub.get("cod").getAsInt();
                String name = jsonClub.get("name").getAsString();
                int nFields = jsonClub.get("nFields").getAsInt();
                double latitude = jsonClub.get("latitude").getAsDouble();
                double longitude = jsonClub.get("longitude").getAsDouble();

                assert clubs[id] == null : "multiple clubs with the same id";
                if (id != i)
                    System.err.println("clubs are not sorted in the json file");

                clubs[id] = new Club(this, id, cod, name, nFields, latitude, longitude);
                mapClubs.put(cod, clubs[id]);
            }

            // reading teams
            JsonArray jsonTeams = json.get("teams").getAsJsonArray();
            teams = new Team[jsonTeams.size()];
            for (int i = 0; i < jsonTeams.size(); i++) {
                JsonObject jsonTeam = jsonTeams.get(i).getAsJsonObject();
                int id = jsonTeam.get("id").getAsInt();
                int cod = jsonTeam.get("cod").getAsInt();
                String clubName = jsonTeam.get("club").getAsString();
                int clubCod = jsonTeam.get("clubCod").getAsInt();
                int level = jsonTeam.get("level").getAsInt();

                Club club = mapClubs.get(clubCod);
                assert club.name.equals(clubName) : "invalid club name for team " + club.name + " (" + cod + ")";

                assert teams[id] == null : "multiple clubs with the same id";
                if (id != i)
                    System.err.println("teams are not sorted in the json file");

                teams[id] = new Team(this, id, cod, clubName, club, level);
                mapTeams.put(cod, teams[id]);
                club.addTeam(teams[i]);
            }

            // reading matrices with distances and times
            travelDists = new int[clubs.length][clubs.length];
            travelTimes = new int[clubs.length][clubs.length];
            weightedDistTime = new int[clubs.length][clubs.length];
            JsonArray jsonMatrix = json.get("timeDistMatrix").getAsJsonArray();
            for (int i = 0; i < jsonMatrix.size(); i++) {
                JsonObject jsonCell = jsonMatrix.get(i).getAsJsonObject();

                int club1Cod = jsonCell.get("src").getAsInt();
                int club2Cod = jsonCell.get("dest").getAsInt();
                Club club1 = mapClubs.get(club1Cod);
                Club club2 = mapClubs.get(club2Cod);

                int time = jsonCell.get("time").getAsInt();
                int dist = jsonCell.get("dist").getAsInt();

                travelDists[club1.id][club2.id] = dist;
                travelDists[club2.id][club1.id] = dist;
                travelTimes[club1.id][club2.id] = time;
                travelTimes[club2.id][club1.id] = time;
                weightedDistTime[club1.id][club2.id] = dist * weightTravelDist + time * weightTravelTime;
                weightedDistTime[club2.id][club1.id] = dist * weightTravelDist + time * weightTravelTime;
            }
        }

        else {

            BufferedReader reader = Files.newBufferedReader(Paths.get(inPath));
            SimpleTokenizer token;
            String line;

            // reading minimum and maximum league size
            token = new SimpleTokenizer(reader.readLine(), ": ");
            assert token.nextToken().equals("+MinMaxLeagueSize");
            minLeagueSize = token.nextInt();
            maxLeagueSize = token.nextInt();

            // reading maximum level difference value
            token = new SimpleTokenizer(reader.readLine(), ": ");
            assert token.nextToken().equals("+MaxLevelDiff");
            maxLevelDiff = token.nextInt();

            // reading maximum number of teams from the same club
            token = new SimpleTokenizer(reader.readLine(), ": ");
            assert token.nextToken().equals("+maxTeamSameClub");
            maxTeamSameClub = token.nextInt();

            // reading maximum travel distance and time
            token = new SimpleTokenizer(reader.readLine(), ": ");
            assert token.nextToken().equals("+maxTravelDistTime");
            maxTravelDist = token.nextInt();
            maxTravelTime = token.nextInt();

            // reading weights
            token = new SimpleTokenizer(reader.readLine(), ": ");
            assert token.nextToken().equals("+Weights");
            weightTravelDist = token.nextInt();
            weightTravelTime = token.nextInt();

            // skipping empty line(s)
            while ((line = reader.readLine()).isEmpty()) ;

            // reading number of clubs
            token = new SimpleTokenizer(line, ":");
            assert token.nextToken().equals("+Clubs");
            int nClubs = token.nextInt();

            // creating clubs
            clubs = new Club[nClubs];
            for (int i = 0; i < nClubs; i++) {
                token = new SimpleTokenizer(reader.readLine(), ";");

                int cod = token.nextInt();
                String name = token.nextToken();
                int nFields = token.nextInt();
                double latitude = token.nextDouble();
                double longitude = token.nextDouble();
                clubs[i] = new Club(this, i, cod, name, nFields, latitude, longitude);

                mapClubs.put(cod, clubs[i]);
            }

            // skipping empty line(s)
            while ((line = reader.readLine()).isEmpty()) ;

            // reading number of teams
            token = new SimpleTokenizer(line, ":");
            assert token.nextToken().equals("+Teams");
            int nTeams = token.nextInt();

            // creating teams
            teams = new Team[nTeams];
            for (int i = 0; i < nTeams; i++) {
                token = new SimpleTokenizer(reader.readLine(), ";");

                int cod = token.nextInt();
                String name = token.nextToken();
                int codClub = token.nextInt();
                Club club = mapClubs.get(codClub);
                int codCategory = token.nextInt();
                int level = token.nextInt();
                teams[i] = new Team(this, i, cod, name, club, level);

                mapTeams.put(cod, teams[i]);
                club.addTeam(teams[i]);
            }

            // skipping empty line(s)
            while ((line = reader.readLine()).isEmpty()) ;

            // reading number of non-zeros in matrices
            token = new SimpleTokenizer(line, ":");
            assert token.nextToken().equals("+Distances");
            int nDistances = token.nextInt();

            // reading matrices with distances and times
            travelDists = new int[nClubs][nClubs];
            travelTimes = new int[nClubs][nClubs];
            weightedDistTime = new int[nClubs][nClubs];
            for (int i = 0; i < nDistances; i++) {
                token = new SimpleTokenizer(reader.readLine(), ";");

                int cod1 = token.nextInt();
                int cod2 = token.nextInt();
                Club club1 = mapClubs.get(cod1);
                Club club2 = mapClubs.get(cod2);

                int time = token.nextInt();
                int dist = token.nextInt();

                travelDists[club1.id][club2.id] = dist;
                travelDists[club2.id][club1.id] = dist;
                travelTimes[club1.id][club2.id] = time;
                travelTimes[club2.id][club1.id] = time;
                weightedDistTime[club1.id][club2.id] = dist * weightTravelDist + time * weightTravelTime;
                weightedDistTime[club2.id][club1.id] = dist * weightTravelDist + time * weightTravelTime;
            }

            reader.close();
        }

        // updating list of non-compatible teams
        for (int i = 0; i < teams.length; i++)
            for (int j = i + 1; j < teams.length; j++)
                if (!teams[i].isCompatible(teams[j]))
                    incompatibleTeams.add(new Pair<>(teams[i], teams[j]));
    }

    /**
     * Gets club from cod.
     *
     * @param cod the cod
     * @return the club from cod
     */
    public Club getClubFromCod(int cod) {
        return mapClubs.get(cod);
    }

    /**
     * Gets list of incompatible pairs of teams.
     */
    public List<Pair<Team, Team>> getIncompatibleTeams() {
        return Collections.unmodifiableList(incompatibleTeams);
    }

    /**
     * Gets team from cod.
     *
     * @param cod the cod
     * @return the team from cod
     */
    public Team getTeamFromCod(int cod) {
        return mapTeams.get(cod);
    }

    /**
     * Writes problem to a file.
     *
     * @param outPath the output file path
     * @throws IOException if any error occurs while writing the file
     */
    public void write(String outPath) throws IOException {
        writeJson(outPath);
    }

    /**
     * Writes problem to a json file.
     *
     * @param outPath the output file path
     * @throws IOException if any error occurs while writing the file
     */
    public void writeJson(String outPath) throws IOException {
        JsonObject json = new JsonObject();

        // printing general information
        JsonObject jsonInfo = new JsonObject();
        jsonInfo.addProperty("name", name);
        jsonInfo.addProperty("nClubs", clubs.length);
        jsonInfo.addProperty("nTeams", teams.length);
        json.add("info", jsonInfo);

        // printing parameters
        JsonObject jsonParams = new JsonObject();
        jsonParams.addProperty("minLeagueSize", minLeagueSize);
        jsonParams.addProperty("maxLeagueSize", maxLeagueSize);
        jsonParams.addProperty("maxLevelDiff", maxLevelDiff);
        jsonParams.addProperty("maxTeamSameClub", maxTeamSameClub);
        jsonParams.addProperty("maxTravelDist", maxTravelDist);
        jsonParams.addProperty("maxTravelTime", maxTravelTime);
        jsonParams.addProperty("weightTravelDist", weightTravelDist);
        jsonParams.addProperty("weightTravelTime", weightTravelTime);
        json.add("params", jsonParams);

        // printing clubs
        JsonArray jsonClubs = new JsonArray();
        for (Club club : clubs) {
            JsonObject jsonClub = new JsonObject();
            jsonClub.addProperty("id", club.id);
            jsonClub.addProperty("cod", club.cod);
            jsonClub.addProperty("name", club.name);
            jsonClub.addProperty("nFields", club.nFields);
            jsonClub.addProperty("latitude", club.latitude);
            jsonClub.addProperty("longitude", club.longitude);
            jsonClubs.add(jsonClub);
        }
        json.add("clubs", jsonClubs);

        // printing teams
        JsonArray jsonTeams = new JsonArray();
        for (Team team : teams) {
            JsonObject jsonTeam = new JsonObject();
            jsonTeam.addProperty("id", team.id);
            jsonTeam.addProperty("cod", team.cod);
            jsonTeam.addProperty("name", team.name);
            jsonTeam.addProperty("club", team.club.name);
            jsonTeam.addProperty("clubCod", team.club.cod);
            //jsonTeam.addProperty("category", team.division.name);
            //jsonTeam.addProperty("categoryCod", team.division.cod);
            jsonTeam.addProperty("level", team.level);
            jsonTeams.add(jsonTeam);
        }
        json.add("teams", jsonTeams);

        // printing matrix with distances and times
        JsonArray jsonMatrix = new JsonArray();
        for (int i = 0; i < clubs.length; i++) {
            Club clubI = clubs[i];
            for (int j = i + 1; j < clubs.length; j++) {
                Club clubJ = clubs[j];

                if (clubI.getTravelTimeTo(clubJ) == 0 && clubJ.getTravelTimeTo(clubJ) == 0)
                    continue;

                JsonObject jsonCell = new JsonObject();
                jsonCell.addProperty("src", clubI.cod);
                jsonCell.addProperty("dest", clubJ.cod);
                jsonCell.addProperty("time", clubI.getTravelTimeTo(clubJ));
                jsonCell.addProperty("dist", clubI.getTravelDistTo(clubJ));
                jsonMatrix.add(jsonCell);
            }
        }
        json.add("timeDistMatrix", jsonMatrix);

        // printing matrix with times
        //JsonArray jsonTimeMatrix = new JsonArray();
        //for (Club clubI : clubs) {
        //    JsonArray jsonTimeArray = new JsonArray();
        //    for (Club clubJ : clubs)
        //        jsonTimeArray.add(clubI.getTravelTimeTo(clubJ));
        //
        //    jsonTimeMatrix.add(jsonTimeArray);
        //}
        //json.add("timeClubsMatrix", jsonTimeMatrix);

        // printing matrix with distances
        //JsonArray jsonDistMatrix = new JsonArray();
        //for (Club clubI : clubs) {
        //    JsonArray jsonDistArray = new JsonArray();
        //    for (Club clubJ : clubs)
        //        jsonDistArray.add(clubI.getTravelDistTo(clubJ));
        //
        //    jsonDistMatrix.add(jsonDistArray);
        //}
        //json.add("distClubsMatrix", jsonDistMatrix);

        PrintWriter printer = new PrintWriter(Files.newBufferedWriter(Paths.get(outPath)));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        printer.printf(gson.toJson(json));
        printer.close();
    }

    /**
     * Writes problem to a file.
     *
     * @param outPath the output file path
     * @throws IOException if any error occurs while writing the file
     */
    public void writeTxt(String outPath) throws IOException {
        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(outPath)));

        // printing minimum and maximum league size
        writer.printf("+MinMaxLeagueSize: %d %d\n", minLeagueSize, maxLeagueSize);

        // printing maximum level difference value
        writer.printf("+MaxLevelDiff: %d\n", maxLevelDiff);

        // printing maximum number of teams from the same club
        writer.printf("+maxTeamSameClub: %d\n", maxTeamSameClub);

        // printing maximum travel distance and time
        writer.printf("+maxTravelDistTime: %d %d\n", maxTravelDist, maxTravelTime);

        // printing weights
        writer.printf("+Weights: %.0f %.0f\n", weightTravelDist, weightTravelTime);

        writer.println();

        // filtering divisions
        //List<Division> categoriesFiltered = new LinkedList<>();
        //for (Division division : divisions)
        //    if (!division.teams.isEmpty())
        //        categoriesFiltered.add(division);
        //categoriesFiltered.sort((a, b) -> Integer.compare(a.cod, b.cod));

        // printing divisions
        //writer.printf("+Categories: %d\n", categoriesFiltered.size());
        //for (Division division : categoriesFiltered)
        //    writer.printf("%d;%d;%d;%d\n", division.cod, division.maxTeamSameClub, division.maxTravelTime, division.maxTravelDist);
        //
        //writer.println();

        // filtering clubs
        List<Club> clubsFiltered = new LinkedList<>();
        for (Club club : clubs)
            if (!club.teams.isEmpty())
                clubsFiltered.add(club);
        clubsFiltered.sort((a, b) -> Integer.compare(a.cod, b.cod));

        // printing club
        writer.printf("+Clubs: %d\n", clubsFiltered.size());
        for (Club club : clubsFiltered)
            writer.printf("%d;%s;%d;%f;%f\n", club.cod, club.name, club.nFields, club.latitude, club.longitude);

        writer.println();

        // printing teams
        List<Team> teamsFiltered = new LinkedList<>(Arrays.asList(teams));
        teamsFiltered.sort((a, b) -> Integer.compare(a.cod, b.cod));
        writer.printf("+Teams: %d\n", teamsFiltered.size());
        for (Team team : teamsFiltered)
            writer.printf("%d;%s;%d;%d\n", team.cod, team.name, team.club.cod, team.level);

        writer.println();

        // printing matrix with distances and times
        int sizeMatrix = (clubsFiltered.size() * (clubsFiltered.size() - 1)) / 2;
        writer.printf("+Distances: %d\n", sizeMatrix);
        for (int i = 0; i < clubsFiltered.size(); i++) {
            Club clubI = clubsFiltered.get(i);
            for (int j = i + 1; j < clubsFiltered.size(); j++) {
                Club clubJ = clubsFiltered.get(j);
                writer.printf("%d;%d;%d;%d\n", clubI.cod, clubJ.cod, clubI.getTravelTimeTo(clubJ), clubI.getTravelDistTo(clubJ));
            }
        }

        writer.close();
    }


    /**
     * Builds a new problem.
     *
     * @author Jan Christiaens
     */
    public static class Builder {

        private String problemName = null;
        private Integer nClubs = null;
        private Integer nDivisions = null;
        private Integer nTeams = null;
        private Integer maxLevelDiff = null;
        private Integer minLeagueSize = null;
        private Integer maxLeagueSize = null;
        private Integer maxTeamSameClub = null;
        private Integer maxTravelDist = null;
        private Integer maxTravelTime = null;
        private Integer weightTravelDist = null;
        private Integer weightTravelTime = null;

        private Problem problem = null;

        /**
         * Sets problem name.
         *
         * @param problemName the problem name
         * @return the builder
         */
        public Builder setProblemName(String problemName) {
            this.problemName = problemName;
            return this;
        }

        /**
         * Sets clubs.
         *
         * @param nClubs the n clubs
         * @return the builder
         */
        public Builder setnClubs(Integer nClubs) {
            this.nClubs = nClubs;
            return this;
        }

        /**
         * Sets divisions.
         *
         * @param nDivisions the n divisions
         * @return the builder
         */
        public Builder setnDivisions(Integer nDivisions) {
            this.nDivisions = nDivisions;
            return this;
        }

        /**
         * Sets number of teams.
         *
         * @param nTeams the number of teams
         * @return the builder
         */
        public Builder setnTeams(Integer nTeams) {
            this.nTeams = nTeams;
            return this;
        }

        /**
         * Sets maximum level diff allowed.
         *
         * @param maxLevelDiff the max level diff
         * @return the builder
         */
        public Builder setMaxLevelDiff(Integer maxLevelDiff) {
            this.maxLevelDiff = maxLevelDiff;
            return this;
        }

        /**
         * Sets min league size.
         *
         * @param minLeagueSize the min league size
         * @return the builder
         */
        public Builder setMinLeagueSize(Integer minLeagueSize) {
            this.minLeagueSize = minLeagueSize;
            return this;
        }

        /**
         * Sets max league size.
         *
         * @param maxLeagueSize the max league size
         * @return the builder
         */
        public Builder setMaxLeagueSize(Integer maxLeagueSize) {
            this.maxLeagueSize = maxLeagueSize;
            return this;
        }

        /**
         * Sets max number of  teams form the same club.
         *
         * @param maxTeamSameClub the max number of teams form the same club.
         * @return the builder
         */
        public Builder setMaxTeamSameClub(Integer maxTeamSameClub) {
            this.maxTeamSameClub = maxTeamSameClub;
            return this;
        }

        /**
         * Sets max travel dist.
         *
         * @param maxTravelDist the max travel dist
         * @return the builder
         */
        public Builder setMaxTravelDist(Integer maxTravelDist) {
            this.maxTravelDist = maxTravelDist;
            return this;
        }

        /**
         * Sets max travel time.
         *
         * @param maxTravelTime the max travel time
         * @return the builder
         */
        public Builder setMaxTravelTime(Integer maxTravelTime) {
            this.maxTravelTime = maxTravelTime;
            return this;
        }

        /**
         * Sets weight travel dist.
         *
         * @param weightTravelDist the weight travel dist
         * @return the builder
         */
        public Builder setWeightTravelDist(Integer weightTravelDist) {
            this.weightTravelDist = weightTravelDist;
            return this;
        }

        /**
         * Sets weight travel time.
         *
         * @param weightTravelTime the weight travel time
         * @return the builder
         */
        public Builder setWeightTravelTime(Integer weightTravelTime) {
            this.weightTravelTime = weightTravelTime;
            return this;
        }

        /**
         * Initializes a new problem using the builder.
         *
         * @return the builder
         */
        public Builder initProblem() {
            if (problemName == null)
                throw new IllegalStateException("problemName not set");
            if (nClubs == null)
                throw new IllegalStateException("nClubs not set");
            if (nDivisions == null)
                throw new IllegalStateException("nDivisions not set");
            if (nTeams == null)
                throw new IllegalStateException("nTeams not set");
            if (maxLevelDiff == null)
                throw new IllegalStateException("maxLevelDiff not set");
            if (minLeagueSize == null)
                throw new IllegalStateException("minLeagueSize not set");
            if (maxLeagueSize == null)
                throw new IllegalStateException("maxLeagueSize not set");
            if (maxTeamSameClub == null)
                throw new IllegalStateException("maxTeamSameClub not set");
            if (maxTravelDist == null)
                throw new IllegalStateException("maxTravelDist not set");
            if (maxTravelTime == null)
                throw new IllegalStateException("maxTravelTime not set");
            if (weightTravelDist == null)
                throw new IllegalStateException("weightTravelDist not set");
            if (weightTravelTime == null)
                throw new IllegalStateException("weightTravelTime not set");

            problem = new Problem(problemName, nClubs, nTeams, maxLevelDiff, minLeagueSize, maxLeagueSize,
              maxTeamSameClub, maxTravelDist, maxTravelTime, weightTravelDist, weightTravelTime);

            return this;
        }

        /**
         * Creates a club and returns it.
         *
         * @param id        the id  of the club
         * @param stam      the stam of the club
         * @param name      the name of the club
         * @param nFields   the number of fields of the club
         * @param latitude  the latitude coordinate
         * @param longitude the longitude coordinate
         * @return the club
         */
        public Club createClub(int id, int stam, String name, int nFields, double latitude, double longitude) {
            if (problem == null)
                throw new IllegalStateException("initialize the problem before using this method");
            if (problem.clubs[id] != null)
                throw new IllegalArgumentException("club " + id + " already set");
            Club club = new Club(problem, id, stam, name, nFields, latitude, longitude);
            problem.clubs[id] = club;
            return club;
        }

        /**
         * Creates a team and returns it.
         *
         * @param id    the id of the team
         * @param cod   the cod of the team
         * @param name  the name of the team
         * @param club  the club of the team
         * @param level the level of the team
         * @return the team
         */
        public Team createTeam(int id, int cod, String name, Club club, int level) {
            if (problem == null)
                throw new IllegalStateException("initialize the problem before using this method");
            if (problem.teams[id] != null)
                throw new IllegalArgumentException("teams " + id + " already set");
            Team team = new Team(problem, id, cod, name, club, level);
            problem.teams[id] = team;
            club.teams.add(team);
            return team;
        }

        /**
         * Sets the traverl time between two clubs.
         *
         * @param i    the first club id
         * @param j    the second club id
         * @param time the time
         * @return the builder
         */
        public Builder setTime(int i, int j, int time) {
            if (problem == null)
                throw new IllegalStateException("initialize the problem before using this method");
            problem.travelTimes[i][j] = time;
            return this;
        }

        /**
         * Sets the distance between two clubs.
         *
         * @param i    the first club id
         * @param j    the second club id
         * @param dist the distance
         * @return the builder
         */
        public Builder setDist(int i, int j, int dist) {
            if (problem == null)
                throw new IllegalStateException("initialize the problem before using this method");
            problem.travelDists[i][j] = dist;
            return this;
        }

        /**
         * Gets problem.
         *
         * @return the problem
         */
        public Problem getProblem() {
            return problem;
        }

    }
}

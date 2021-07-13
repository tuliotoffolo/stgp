package be.kuleuven.stgp.core.model;

/**
 * The class Team.
 *
 * @author Tulio Toffolo
 */
public class Team implements Comparable<Team> {

    public final Problem problem;

    public final int id, cod, level;
    public final Club club;
    public final String name;

    /**
     * Instantiates a new Team.
     *
     * @param problem  the problem
     * @param id       the id of the team
     * @param cod      the cod of the team
     * @param name     the name of the team
     * @param club     the club of the team
     * @param level    the level of the team
     */
    public Team(Problem problem, int id, int cod, String name, Club club, int level) {
        this.problem = problem;
        this.id = id;
        this.cod = cod;
        this.name = name;
        this.club = club;
        this.level = level;
    }

    @Override
    public int compareTo(Team team) {
        return Integer.compare(id, team.id);
    }

    /**
     * Gets travel distance to another team.
     *
     * @param team the other team
     * @return the travel distance to another team
     */
    public int getTravelDistTo(Team team) {
        return club.getTravelDistTo(team.club);
    }

    /**
     * Gets travel time to another team.
     *
     * @param team the other team
     * @return the travel time to another team
     */
    public int getTravelTimeTo(Team team) {
        return club.getTravelTimeTo(team.club);
    }

    /**
     * Gets weighted distance and time to another team.
     *
     * @param team the other team
     * @return the weighted distance and time to another team
     */
    public int getWeightedDistTimeTo(Team team) {
        return problem.weightedDistTime[club.id][team.club.id];
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * Checks whether this team is compatible with another, i.e. if both can be
     * in the same league.
     *
     * @param team team to check
     * @return true if this team can be in the same league of the team passed as
     * argument and false otherwise
     */
    public boolean isCompatible(Team team) {
        return Math.abs(level - team.level) <= problem.maxLevelDiff
          && getTravelDistTo(team) <= problem.maxTravelDist
          && getTravelTimeTo(team) <= problem.maxTravelTime;
    }
}

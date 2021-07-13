package be.kuleuven.stgp.core.model;

import java.util.*;

/**
 * The class Division.
 *
 * @author Tulio Toffolo
 */
public class Division {

    public final Problem problem;

    public final int id, cod;
    public final String name;
    public final int maxTeamSameClub, maxTravelDist, maxTravelTime;

    public List<Team> teams = new ArrayList<>();

    /**
     * Instantiates a new Division.
     *
     * @param problem         the problem
     * @param id              the id of the division
     * @param cod             the cod of the division
     * @param name            the name of the division
     * @param maxTeamSameClub the maximum number of teams from the same club
     * @param maxTravelTime   the maximum travel time allowed
     * @param maxTravelDist   the maximum travel distance allowed
     */
    public Division(Problem problem, int id, int cod, String name, int maxTeamSameClub, int maxTravelTime, int maxTravelDist) {
        this.problem = problem;
        this.id = id;
        this.cod = cod;
        this.name = name;
        this.maxTeamSameClub = maxTeamSameClub;
        this.maxTravelTime = maxTravelTime;
        this.maxTravelDist = maxTravelDist;
    }

    /**
     * Instantiates a new Division.
     *
     * @param problem         the problem
     * @param id              the id of the division
     * @param cod             the cod of the division
     * @param maxTeamSameClub the maximum number of teams from the same club
     * @param maxTravelTime   the maximum travel time allowed
     * @param maxTravelDist   the maximum travel distance allowed
     */
    public Division(Problem problem, int id, int cod, int maxTeamSameClub, int maxTravelTime, int maxTravelDist) {
        this(problem, id, cod, "u" + cod, maxTeamSameClub, maxTravelTime, maxTravelDist);
    }

    /**
     * Adds a team to this division.
     *
     * @param team the team
     */
    protected void addTeam(Team team) {
        teams.add(team);
    }
}

package be.kuleuven.stgp.core.model;

import java.util.*;

/**
 * This class represents a Club.
 *
 * @author Tulio Toffolo
 */
public class Club implements Comparable<Club>, Iterable<Team> {

    public final Problem problem;

    public final int id, cod, nFields;
    public final double latitude, longitude;
    public final String name;

    public List<Team> teams = new ArrayList<>();

    /**
     * Instantiates a new Club.
     *
     * @param problem   the problem
     * @param id        the id of the club
     * @param cod       the cod of the club
     * @param name      the name of the club
     * @param nFields   the number of fields that the club possess
     * @param latitude  the latitude coordinate of the club
     * @param longitude the longitude coordinate of the club
     */
    public Club(Problem problem, int id, int cod, String name, int nFields, double latitude, double longitude) {
        this.problem = problem;
        this.id = id;
        this.cod = cod;
        this.name = name;
        this.nFields = nFields;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public int compareTo(Club club) {
        return Integer.compare(cod, club.cod);
    }

    /**
     * Gets the travel distance from this club to another club.
     *
     * @param club the other club
     * @return the travel distance from this club to the other club
     */
    public int getTravelDistTo(Club club) {
        return problem.travelDists[id][club.id];
    }

    /**
     * Gets the travel time from this club to another club.
     *
     * @param club the other club
     * @return the travel time from this club to the other club
     */
    public int getTravelTimeTo(Club club) {
        return problem.travelTimes[id][club.id];
    }

    /**
     * Gets weighted distance and time to another club.
     *
     * @param club the other club
     * @return the weighted distance and time to another club
     */
    public int getWeightedDistTimeTo(Club club) {
        return problem.weightedDistTime[id][club.id];
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public Iterator<Team> iterator() {
        return teams.iterator();
    }

    /**
     * Adds a team to the club.
     *
     * @param team the team
     */
    protected void addTeam(Team team) {
        teams.add(team);
    }
}

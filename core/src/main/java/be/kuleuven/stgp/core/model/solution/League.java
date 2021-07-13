package be.kuleuven.stgp.core.model.solution;

import be.kuleuven.stgp.core.model.*;
import be.kuleuven.stgp.core.util.*;

import java.io.*;
import java.util.*;

/**
 * This class represents a league.
 *
 * @author Tulio Toffolo
 */
public class League implements Iterable<Team> {

    public final Problem problem;
    public NavigableSet<Team> teams = new TreeSet<>();

    //private boolean[] teamsArray;
    private int objective, totalTravelDist, totalTravelTime;

    /**
     * Instantiates a new League.
     *
     * @param problem the problem
     */
    public League(Problem problem) {
        this.problem = problem;
        //this.teamsArray = new boolean[problem.teams.length];
    }

    /**
     * Adds a team to a league.
     *
     * @param teamToAdd the team to add
     */
    public void add(Team teamToAdd) {
        for (Team team : teams) {
            objective += team.getWeightedDistTimeTo(teamToAdd) * 2;
            totalTravelDist += team.getTravelDistTo(teamToAdd) * 2;
            totalTravelTime += team.getTravelTimeTo(teamToAdd) * 2;
        }

        teams.add(teamToAdd);
        //teamsArray[teamToAdd.id] = true;
    }

    /**
     * Clones this league.
     *
     * @param solution the solution
     * @return the league
     */
    public League clone(Solution solution) {
        League copy = new League(problem);

        for (Team team : teams)
            copy.teams.add(team);
        copy.objective = objective;
        copy.totalTravelDist = totalTravelDist;
        copy.totalTravelTime = totalTravelTime;

        return copy;
    }

    /**
     * Checks if a team is assigned to this league.
     *
     * @param team the team
     * @return true if the team is assigned to this league and false otherwise
     */
    public boolean contains(Team team) {
        return teams.contains(team);
        //return teamsArray[team.id];
    }

    /**
     * Checks if a team is assigned to this league.
     *
     * @param teamId the team's id
     * @return true if the team is assigned to this league and false otherwise
     */
    public boolean contains(int teamId) {
        return teams.contains(problem.teams[teamId]);
        //return teamsArray[teamId];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        League league = ( League ) o;
        return teams.equals(league.teams);
    }

    /**
     * Gets total objective coefficient (or cost) of this league.
     *
     * @return the objective coefficient (or cost)
     */
    public int getObjective() {
        return objective;
    }

    /**
     * Gets total travel distance.
     *
     * @return the total travel dist
     */
    public int getTotalTravelDist() {
        return totalTravelDist;
    }

    /**
     * Gets total travel time.
     *
     * @return the total travel time
     */
    public int getTotalTravelTime() {
        return totalTravelTime;
    }

    @Override
    public int hashCode() {
        return teams.hashCode();
    }

    @Override
    public Iterator<Team> iterator() {
        return teams.iterator();
    }

    /**
     * Removes a team from the league.
     *
     * @param teamToRemove the team to remove
     * @return true if the team is removed and false if the team was not
     * assigned to this league
     */
    public boolean remove(Team teamToRemove) {
        if (teams.remove(teamToRemove)) {
            for (Team team : teams) {
                objective -= team.getWeightedDistTimeTo(teamToRemove) * 2;
                totalTravelDist -= team.getTravelDistTo(teamToRemove) * 2;
                totalTravelTime -= team.getTravelTimeTo(teamToRemove) * 2;
            }
            //teamsArray[teamToRemove.id] = false;
            return true;
        }
        return false;
    }

    /**
     * Gets the number of teams.
     *
     * @return the n teams
     */
    public int size() {
        return teams.size();
    }

    /**
     * Validates this league, considering costs and constraints.
     *
     * @param output the output stream (for log purpose)
     * @return true if the league is valid and false otherwise
     */
    public boolean validate(PrintStream output) {
        boolean valid = true;

        if (teams.size() < problem.minLeagueSize) {
            valid = false;
            Util.safePrintf(output, "League has less teams than the minimum required.\n");
        }
        if (teams.size() > problem.maxLeagueSize) {
            valid = false;
            Util.safePrintf(output, "League has more teams than the maximum allowed.\n");
        }

        for (Team teamI : teams) {
            NavigableSet<Team> nextTeams = teams.tailSet(teamI, false);
            for (Team teamJ : nextTeams) {
                // teams must have the same category
                // maximum distance
                if (teamI.getTravelDistTo(teamJ) > problem.maxTravelDist) {
                    valid = false;
                    Util.safePrintf(output, "Maximum distance constraint not respected among teams %d and %d.\n", teamI.cod, teamJ.cod);
                }

                // maximum travel time
                if (teamI.getTravelTimeTo(teamJ) > problem.maxTravelTime) {
                    valid = false;
                    Util.safePrintf(output, "Maximum travel time constraint not respected among teams %d and %d.\n", teamI.cod, teamJ.cod);
                }

                // maximum level difference
                if (Math.abs(teamI.level - teamJ.level) > problem.maxLevelDiff) {
                    valid = false;
                    Util.safePrintf(output, "Teams %d and %d are from different category but are allocated in the same league.\n", teamI.cod, teamJ.cod);
                }
            }
        }

        Set<Club> clubSet = new HashSet<>();
        for (Team team : teams)
            clubSet.add(team.club);

        // maximum number of teams from the same club
        for (Club club : clubSet) {
            int counter = 0;
            for (Team team : teams) {
                if (club == team.club)
                    counter++;
            }
            if (counter > problem.maxTeamSameClub) {
                valid = false;
                Util.safePrintf(output, "Too many teams from club %d in the same league..\n", club.cod);
            }
        }

        return valid;
    }
}

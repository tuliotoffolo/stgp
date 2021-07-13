//package be.kuleuven.stgp.mip.column_generation.pricingheuristics;
//
//import be.kuleuven.stgp.core.model.*;
//import be.kuleuven.stgp.core.model.solution.*;
//import be.kuleuven.stgp.mip.column_generation.*;
//import be.kuleuven.stgp.mip.util.*;
//
//import java.util.*;
//
//public class SimpleConstructive implements PricingHeuristic {
//
//    public final Problem problem;
//    public final Pricing pricing;
//
//    public List<League> leagues = new ArrayList<>();
//    public List<Double> leaguesCosts = new ArrayList<>();
//
//    private List<Clique> cliques = new LinkedList<>();
//    private List<Clique> teamCliques = new ArrayList<>();
//
//    private final Clique nil = new Clique();
//
//
//    public SimpleConstructive(Problem problem, Pricing pricing) {
//        this.problem = problem;
//        this.pricing = pricing;
//
//        for (Team team : problem.teams)
//            teamCliques.add(nil);
//        reset();
//    }
//
//
//    @Override
//    public void fix(int i, int j, boolean value) {
//        Clique cliqueI = teamCliques.get(i);
//        Clique cliqueJ = teamCliques.get(j);
//
//        if (value) {
//            // adding the teams to the right groups or joining them
//            if (cliqueI == nil && cliqueJ == nil) {
//                Clique newClique = new Clique();
//                newClique.add(problem.teams[i]);
//                newClique.add(problem.teams[j]);
//                cliques.add(newClique);
//                teamCliques.set(i, newClique);
//                teamCliques.set(j, newClique);
//            }
//            else if (cliqueI != nil && cliqueJ == nil) {
//                cliqueI.add(problem.teams[j]);
//                teamCliques.set(j, cliqueI);
//            }
//            else if (cliqueI == nil && cliqueJ != nil) {
//                cliqueJ.add(problem.teams[i]);
//                teamCliques.set(i, cliqueJ);
//            }
//            else if (cliqueI != cliqueJ) {
//                cliqueI.addAll(cliqueJ);
//                for (Team team : cliqueJ.group)
//                    teamCliques.set(team.id, cliqueI);
//                cliques.remove(cliqueJ);
//            }
//        }
//
//        else {
//            // adding conflict to cliqueI
//            if (cliqueI == nil) {
//                Clique newClique = new Clique();
//                newClique.add(problem.teams[i]);
//                teamCliques.set(i, newClique);
//            }
//
//            // adding conflict to cliqueJ
//            if (cliqueJ == nil) {
//                Clique newClique = new Clique();
//                newClique.add(problem.teams[j]);
//                teamCliques.set(j, newClique);
//            }
//
//            teamCliques.get(i).conflict.add(problem.teams[j]);
//            teamCliques.get(j).conflict.add(problem.teams[i]);
//        }
//
//        //if (value == 0) {
//        //    forbidTeams.get(i).add(problem.teams[j]);
//        //    forbidTeams.get(j).add(problem.teams[i]);
//        //}
//        //else {
//        //    groupedTeams.get(i).add(problem.teams[j]);
//        //    groupedTeams.get(j).add(problem.teams[i]);
//        //}
//    }
//
//    @Override
//    public List<League> getLeagues() {
//        return leagues;
//    }
//
//    @Override
//    public List<Double> getLeaguesCosts() {
//        return leaguesCosts;
//    }
//
//    @Override
//    public boolean solve(double gamma[]) {
//        cliques.forEach(c -> c.updateCost(gamma));
//        leagues.clear();
//        leaguesCosts.clear();
//
//        for (Team firstTeam : problem.teams) {
//            if (gamma[firstTeam.id] < 0) continue;
//
//            League league = makeLeague(gamma, firstTeam);
//            if (!league.validate(null)) continue;
//
//            double cost = league.getTotalTravelDist();
//            for (Team team : league)
//                cost -= gamma[team.id];
//
//            if (cost < -Constants.EPS) {
//                leagues.add(league);
//                leaguesCosts.add(cost);
//            }
//        }
//
//        return !leagues.isEmpty();
//    }
//
//    @Override
//    public void reset() {
//        cliques.clear();
//        for (int i = 0; i < problem.teams.length; i++)
//            teamCliques.set(i, nil);
//
//        for (int i = 0; i < problem.teams.length; i++) {
//            Team teamI = problem.teams[i];
//
//            for (int j = i + 1; j < problem.teams.length; j++) {
//                Team teamJ = problem.teams[j];
//
//                if (!(Math.abs(teamI.level - teamJ.level) <= problem.maxLevelDiff
//                  && teamI.division.equals(teamJ.division)
//                  && teamI.getTravelDistTo(teamJ) <= teamI.division.maxTravelDist
//                  && teamI.getTravelTimeTo(teamJ) <= teamI.division.maxTravelTime)) {
//                    fix(i, j, false);
//                }
//            }
//        }
//    }
//
//
//    private League makeLeague(double gamma[], Team selectedTeam) {
//        Set<Team> candidates = new LinkedHashSet<>(Arrays.asList(problem.teams));
//        League league = new League(problem);
//        double gammaSum = 0;
//
//        do {
//            // adding the selected team (or teams) and updating candidate list
//            if (teamCliques.get(selectedTeam.id) == nil) {
//                gammaSum -= gamma[selectedTeam.id];
//                league.add(selectedTeam);
//                candidates.remove(selectedTeam);
//            }
//            else {
//                for (Team groupTeam : teamCliques.get(selectedTeam.id).group) {
//                    gammaSum -= gamma[groupTeam.id];
//                    league.add(groupTeam);
//                }
//                candidates.removeAll(teamCliques.get(selectedTeam.id).conflict);
//                candidates.removeAll(teamCliques.get(selectedTeam.id).group);
//            }
//
//            // resetting best candidate variables
//            selectedTeam = null;
//            double bestCost = Double.MAX_VALUE;
//
//            // selecting the best candidate
//            Iterator<Team> iterator = candidates.iterator();
//            while (iterator.hasNext()) {
//                Team candidate = iterator.next();
//
//                if (league.size() + teamCliques.get(candidate.id).group.size() > problem.maxLeagueSize) {
//                    iterator.remove();
//                    continue;
//                }
//
//                // summing the cost
//                double cost = teamCliques.get(candidate.id) == nil ? -gamma[candidate.id] : teamCliques.get(candidate.id).getCost();
//                for (Team existingTeam : league) {
//                    if (teamCliques.get(candidate.id) == nil) {
//                        cost += existingTeam.getTravelDistTo(candidate) + candidate.getTravelDistTo(existingTeam);
//                    }
//                    else {
//                        for (Team groupTeam : teamCliques.get(candidate.id).group)
//                            cost += existingTeam.getTravelDistTo(groupTeam) + groupTeam.getTravelDistTo(existingTeam);
//                    }
//                }
//
//                if ((cost > 0 || gammaSum + league.getTotalTravelDist() + cost > 0) && league.size() >= problem.minLeagueSize) {
//                    iterator.remove();
//                    continue;
//                }
//
//                if (cost < bestCost) {
//                    bestCost = cost;
//                    selectedTeam = candidate;
//                }
//            }
//        }
//        while (selectedTeam != null && league.size() >= problem.minLeagueSize);
//
//        gammaSum += league.getTotalTravelDist();
//        return league;
//    }
//
//    private static class Clique {
//
//        final Set<Team> conflict = new TreeSet<>();
//        final Set<Team> group = new TreeSet<>();
//        double gammaCost = 0;
//        double travelCost = 0;
//
//        void add(Team team) {
//            for (Team existingTeam : group)
//                travelCost += existingTeam.getTravelDistTo(team) + team.getTravelDistTo(existingTeam);
//            group.add(team);
//        }
//
//        void addAll(Clique clique) {
//            conflict.addAll(clique.conflict);
//            group.addAll(clique.group);
//        }
//
//        double getCost() {
//            return gammaCost + travelCost;
//        }
//
//        void updateCost(double[] gamma) {
//            gammaCost = 0;
//            for (Team team : group)
//                gammaCost -= gamma[team.id];
//        }
//    }
//}

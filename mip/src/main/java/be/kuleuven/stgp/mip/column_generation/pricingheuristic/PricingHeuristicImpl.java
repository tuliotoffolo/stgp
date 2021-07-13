package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import be.kuleuven.stgp.core.model.Problem;
import be.kuleuven.stgp.core.model.Team;
import be.kuleuven.stgp.core.model.solution.League;
import be.kuleuven.stgp.mip.column_generation.PricingHeuristic;
import be.kuleuven.stgp.mip.util.Constants;

import java.util.*;

/**
 * Created by Jan on 24-2-2016.
 */
public class PricingHeuristicImpl implements PricingHeuristic {

	private final Problem problem;
	private final PricingTeam[] pricingTeams;

	private final ArrayList<League> leagues = new ArrayList<>();
	private final ArrayList<Double> costs = new ArrayList<>();
	private final ArrayList<PrimitivePricingLeague> convertedPricingLeagues = new ArrayList<>();
	private final HashSet<HashSet<Team>> primitiveLeagueSets = new HashSet<>();
	private final Random random = new Random(0);

	private final CliqueManager cliqueManager;

	private double cutoff;

	public PricingHeuristicImpl(Problem problem) {
		this.problem = problem;
		this.pricingTeams = new PricingTeam[problem.teams.length];
		this.cliqueManager = new CliqueManager(problem);

		for (int i = 0; i < problem.teams.length; i++) {
			PricingTeam pricingTeam = new PricingTeam(problem.teams[i]);
			TeamClique teamClique = new TeamClique(pricingTeam);
			pricingTeams[i] = pricingTeam;
		}

//		synchronized (this) {
//			try {
//				wait(15000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}

	}

	@Override
	public void fix(int i, int j, boolean value) {
		cliqueManager.fix(i, j, value);
	}

	@Override
	public List<League> getLeagues() {
		return leagues;
	}

	@Override
	public List<Double> getLeaguesCosts() {
		return costs;
	}

	@Override
	public boolean solve(double[] gamma, double cutoff) {
		this.cutoff = cutoff;

		// ../data/small7.prob small7.sol 300
		// ../data/flanders-u17.prob flanders-u17.sol 300

//		if(1==1) return false;

		leagues.clear();
		costs.clear();
		convertedPricingLeagues.clear();
		primitiveLeagueSets.clear();
		cliqueManager.setSolving(true);
		cliqueManager.updateGamma(gamma);

//		simmulatedAnnealing();
		fixedIterations();

		Collections.sort(convertedPricingLeagues);
		for (int i = 0; i < convertedPricingLeagues.size(); i++) {
			PrimitivePricingLeague cpl = convertedPricingLeagues.get(i);
			leagues.add(cpl.toLeague());
			costs.add(cpl.cost);
		}

		cliqueManager.setSolving(false);

		return !leagues.isEmpty();
	}

	@Override
	public void reset() {
//		System.err.println("Reset");
		leagues.clear();
		costs.clear();
		convertedPricingLeagues.clear();
		primitiveLeagueSets.clear();
		cliqueManager.clear();
	}

	private final NeighbourhoodAdd neighAdd = new NeighbourhoodAdd(random);
	private final NeighbourhoodRemove neighRemove = new NeighbourhoodRemove(random);
	private final NeighbourhoodRemoveAdd neighRemoveAdd = new NeighbourhoodRemoveAdd(random);
//	private final NeighbourhoodSwap neighSwap = new NeighbourhoodSwap(random);

	/* Simulated annealing ----------------------------------------------------------------------------------------- */

//	private void addAndRemoveSA() {
//
////		final Neighbourhood[] neighbourhoods = {neighAdd, neighRemove, neighSwap};
//		final Neighbourhood[] neighbourhoods = {neighAdd, neighRemove};
////		final Neighbourhood[] neighbourhoods = {neighAdd, neighSwap};
//
//		for (int i = 0; i < 10; i++) {
//
//			// SA settings
//			final double INITIAL_TEMP = 10000;
//			final double FINAL_TEMP = 9999;
//			final double ITERATIONS = 50;
//			final double coolingRate = getCooling(ITERATIONS, INITIAL_TEMP, FINAL_TEMP);
//
//			// Set initial temp
//			double temp = INITIAL_TEMP;
//
//			// Initialize intial solution
//			Team firstTeam = problem.teams[random.nextInt(problem.teams.length)];
//			PricingLeague league = new PricingLeague(problem, cliqueManager, firstTeam);
//
//			Double bestObj = null;
//
//			// Loop until system has cooled
//			while (temp > FINAL_TEMP) {
//				// Create new neighbour tour
//				Move move = neighbourhoods[random.nextInt(neighbourhoods.length)].getBestImprove(league);
////			Move move = neighbourhoods[random.nextInt(neighbourhoods.length)].getFirstImprove(league);
////			Move move = neighbourhoods[random.nextInt(neighbourhoods.length)].getFirstFeasible(league);
//
//				if (move != null) {
//
//					double currentObjective = league.getObjective();
//					double neighbourObjective = league.getObjective() + move.getDelta();
//
//					// Decide if we should accept the neighbour
//					if (acceptanceProbability(currentObjective, neighbourObjective, temp) > random.nextDouble()) {
//						move.execute(league);
////						if(bestObj==null || bestObj>neighbourObjective){
////							assert isEqual(neighbourObjective, league.getObjective()) : neighbourObjective + "  " + league.getObjective();
////							bestObj = neighbourObjective;
//						eval(league);
////						}
//					}
//				}
//
//				// Cool system
//				temp *= coolingRate;
//			}
//
//		}
//
//	}

//	private void simmulatedAnnealing() {
//
//		// ../data/flanders-u17.prob flanders-u17.sol 300
//
////		final Neighbourhood[] neighbourhoods = {neighAdd, neighRemove, neighSwap};
//		final Neighbourhood[] neighbourhoods = {neighAdd, neighRemove};
////		final Neighbourhood[] neighbourhoods = {neighAdd, neighSwap};
//
//		// SA settings
//		final double INITIAL_TEMP = 100;
//		final double FINAL_TEMP = 1;
//		final double ITERATIONS = 1000;
//		final double COOLING_RATE = getCooling(ITERATIONS, INITIAL_TEMP, FINAL_TEMP);
//
//		final int MAX_FULL_ITERATIONS = 50;
//		final int MAX_RUNS_WITHOUT_ANY_FEASIBLE = 1000;
//		final int MAX_RUN_NO_IMP = 50;
//
//		int runsWithoutAnyFeasible = 0;
//		int totalRuns = 0;
//		int cliqueIndex = 0;
//
//		ArrayList<TeamClique> cliques = new ArrayList(cliqueManager.getTeamCliques());
//		Collections.shuffle(cliques, random);
//		Collections.sort(cliques, (c1,c2) -> -Double.compare(c1.getGamma(), c2.getGamma()));
//
//		StringBuilder sb = new StringBuilder();
//
//		while (totalRuns < MAX_FULL_ITERATIONS || (convertedPricingLeagues.isEmpty() && runsWithoutAnyFeasible < MAX_RUNS_WITHOUT_ANY_FEASIBLE)) {
//
//			boolean foundFeasible = false;
//			int noEvals = 0;
//
//			// Set initial temp
//			double temp = INITIAL_TEMP;
//
//			// Initialize intial solution
//			Team firstTeam = problem.teams[random.nextInt(problem.teams.length)];
//			TeamClique firstClique = cliqueManager.getTeamClique(firstTeam);
////			TeamClique firstClique = cliques.get(cliqueIndex);
////			cliqueIndex++;
////			if(cliqueIndex==cliques.size()) cliqueIndex = 0;
//
//			PricingLeague league = new PricingLeague(problem, cliqueManager, firstClique);
//			assert league.debugObjective();
//
//			Double bestObj = null;
//
//			// Loop until system has cooled
//			while (temp > FINAL_TEMP) {
//				double accept = convertedPricingLeagues.size()==0 ? 0.9 : 0.9;
////				accept = random.nextDouble()/10+0.9;
//
//				Move move;
//				if(random.nextBoolean()) move = neighbourhoods[random.nextInt(neighbourhoods.length)].getBestImprove(league, accept);
//				else move = neighbourhoods[random.nextInt(neighbourhoods.length)].getFirstImprove(league);
//
//				boolean feasibleBefore = !league.infeasible;
//				if (move != null) {
//
//					double currentObjective = league.getObjective();
//					double neighbourObjective = league.getObjective() + move.getDelta();
//
//					// Decide if we should accept the neighbour
//					if (acceptanceProbability(currentObjective, neighbourObjective, temp) > random.nextDouble()) {
//						assert league.debugObjective();
//						move.execute(league);
//						assert !feasibleBefore || !league.infeasible : "before: " + feasibleBefore + "  after: " + !league.infeasible + "   " + move.getClass().getSimpleName();
//						if(eval(league)){
//							noEvals = 0;
//							foundFeasible = true;
//						}else{
//							noEvals++;
//						}
//					}
//				}
//
//				// Cool system
//				temp *= COOLING_RATE;
//
////				if(noEvals==MAX_RUN_NO_IMP){
////					break;
////				}
//			}
//
//			if (convertedPricingLeagues.isEmpty()) {
//				runsWithoutAnyFeasible++;
//			}
//
//			if(!foundFeasible){
//				sb.append("run " + totalRuns + " -> " + evalString(league)).append("\n");
//			}
//
//			totalRuns++;
//		}
//
//		//if(convertedPricingLeagues.isEmpty()){
////			cliqueManager.printReproduceStateCode("cm");
////			System.out.println(sb.toString());
////		}
////		System.out.println("Found " + convertedPricingLeagues.size() + " solutions\ttotalRuns: " + totalRuns + "\twithoutAnyFeasible: " + runsWithoutAnyFeasible);
//	}

	private double getCooling(double iterations, double initialTemp, double finalTemp) {
		// at this cooling rate, the temp at it-1 will == 1
		// thus at it it will be < 1
		return Math.pow(finalTemp / initialTemp, 1d / (iterations - 1));
	}

	// Calculate the acceptance probability
	public double acceptanceProbability(double energy, double newEnergy, double temperature) {
		// If the new solution is better, accept it
		if (newEnergy < energy) {
			return 1.0;
		}
		// If the new solution is worse, calculate an acceptance probability
		return Math.exp((energy - newEnergy) / temperature);
	}

	/* without meta heuristic -------------------------------------------------------------------------------------- */

	private void fixedIterations() {

		// ../data/flanders-u17.prob flanders-u17.sol 300

//		final Neighbourhood[] neighbourhoods = {neighAdd, neighRemove, neighSwap};
//		final Neighbourhood[] neighbourhoods = {neighAdd, neighRemove};
		final Neighbourhood[] neighbourhoods = {neighAdd, neighRemove, neighRemoveAdd};
//		final Neighbourhood[] neighbourhoods = {neighAdd, neighSwap};

		final int MAX_FULL_ITERATIONS = 50;
		final int MAX_RUNS_WITHOUT_ANY_FEASIBLE = 5000;
		final int MAX_RUN_IMPROVEMENTS = 50;
		final int MAX_RUN_NO_IMP = 100000;

		int runsWithoutAnyFeasible = 0;
		int totalRuns = 0;
		int cliqueIndex = 0;

		ArrayList<TeamClique> cliques = new ArrayList(cliqueManager.getTeamCliques());
		Collections.shuffle(cliques, random);
		Collections.sort(cliques, (c1,c2) -> -Double.compare(c1.getGamma(), c2.getGamma()));

//		StringBuilder sb = new StringBuilder();

		while (totalRuns < MAX_FULL_ITERATIONS || (convertedPricingLeagues.isEmpty() && runsWithoutAnyFeasible < MAX_RUNS_WITHOUT_ANY_FEASIBLE)) {

			int noEvals = 0;
			boolean foundFeasible = false;

			Team firstTeam = problem.teams[random.nextInt(problem.teams.length)];
//			TeamClique firstClique = cliqueManager.getTeamClique(firstTeam);
			TeamClique firstClique = cliques.get(cliqueIndex);
			cliqueIndex++;
			if(cliqueIndex==cliques.size()) cliqueIndex = 0;

			PricingLeague league = new PricingLeague(problem, cliqueManager, firstClique);
			assert league.debugObjective();

			// Loop until system has cooled
			for (int j = 0; j < MAX_RUN_IMPROVEMENTS; j++) {
				double accept = convertedPricingLeagues.size()==0 ? 0.8 : 0.5;
//				accept = random.nextDouble()/10+0.9;

				Move move;
				move = neighbourhoods[random.nextInt(neighbourhoods.length)].getBestImprove(league, accept);
//				if(random.nextBoolean()) move = neighbourhoods[random.nextInt(neighbourhoods.length)].getBestImprove(league, accept);
//				else move = neighbourhoods[random.nextInt(neighbourhoods.length)].getFirstImprove(league);
//				else move = neighbourhoods[random.nextInt(neighbourhoods.length)].getFirstFeasible(league);

//				Move bestAddMove = neighAdd.getBestImprove(league);
//				Move bestRemoveMove = neighRemove.getBestImprove(league);
//				Move bestSwapMove = neighSwap.getBestImprove(league);
//				Move move = null;
//				if (bestAddMove != null) move = bestAddMove;
//				if (bestRemoveMove != null && (move == null || move.getDelta() > bestRemoveMove.getDelta()))
//					move = bestRemoveMove;
//				if (bestSwapMove != null && (move == null || move.getDelta() > bestSwapMove.getDelta()))
//					move = bestSwapMove;

				boolean feasibleBefore = !league.infeasible;
				if (move != null) {
					assert league.debugObjective();
					move.execute(league);
					assert !feasibleBefore || !league.infeasible : "before: " + feasibleBefore + "  after: " + !league.infeasible + "   " + move.getClass().getSimpleName();
					if(eval(league)){
						noEvals = 0;
						foundFeasible = true;
					}else{
						noEvals++;
					}
				}

				if(noEvals==MAX_RUN_NO_IMP){
					break;
				}
			}

			if (convertedPricingLeagues.isEmpty()) {
				runsWithoutAnyFeasible++;
			}

//			if(!foundFeasible){
//				sb.append("run " + totalRuns + " -> " + evalString(league)).append("\n");
//			}

			totalRuns++;
		}

//		if(convertedPricingLeagues.isEmpty()){
////			cliqueManager.printReproduceStateCode("cm");
////			System.out.println(sb.toString());
//		}
//		System.out.println("Found " + convertedPricingLeagues.size() + " solutions\ttotalRuns: " + totalRuns + "\twithoutAnyFeasible: " + runsWithoutAnyFeasible);
	}

	public CliqueManager getCliqueManager(){
		return cliqueManager;
	}

	private boolean eval(PricingLeague pl) {
		if (pl.infeasible) {

		} else if (pl.getObjective() >= cutoff - Constants.EPS) {
			assert pl.debugSizeClubsLevelsTimeDist();
			assert pl.debugTeamsInSameLeague();
			assert pl.debugObjective();
		} else {
			HashSet<Team> primitiveSolution = new HashSet<>();
			for(TeamClique clique : pl.presentCliques){
				for(PricingTeam p : clique.getTeams())
					primitiveSolution.add(p.team);
			}

			if (!primitiveLeagueSets.contains(primitiveSolution)) {
				primitiveLeagueSets.add(primitiveSolution);
				convertedPricingLeagues.add(new PrimitivePricingLeague(primitiveSolution, pl.getObjective()));
			}
			assert pl.debugSizeClubsLevelsTimeDist();
			assert pl.debugTeamsInSameLeague();
			assert pl.debugObjective();

			return true;
		}

		return false;
	}

	private String evalString(PricingLeague pl) {
		if (pl.infeasible) {
			return "pl.infeasible=true, " + pl;
		} else if (pl.getObjective() >= -Constants.EPS) {
			return "pl.objective=" + pl.getObjective();
		} else {
			return "pl accepted";
		}
	}

	private class PrimitivePricingLeague implements Comparable<PrimitivePricingLeague> {

		private final double cost;
		private final HashSet<Team> primitiveSolution;

		public PrimitivePricingLeague(HashSet<Team> primitiveSolution, double cost) {
			this.cost = cost;
			this.primitiveSolution = primitiveSolution;
		}

		@Override
		public int compareTo(PrimitivePricingLeague o) {
			return Double.compare(cost, o.cost);
		}

		public League toLeague() {
			League l = new League(problem);
				for(Team team : primitiveSolution){
					l.add(team);
				}
			return l;
		}
	}

}

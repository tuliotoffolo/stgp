//package be.kuleuven.stgp.heuristic;
//
//import be.kuleuven.stgp.core.model.Problem;
//import be.kuleuven.stgp.core.model.solution.Solution;
//import be.kuleuven.stgp.pricingheuristic.model.Constructive;
//import be.kuleuven.stgp.pricingheuristic.model.MtSolution;
//import be.kuleuven.stgp.pricingheuristic.move.Move;
//import be.kuleuven.stgp.pricingheuristic.move.RuinAndRecreateMove;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Random;
//
///**
// * Created by Jan on 23-10-2015.
// */
//public class Solver {
//
//	public static final int DELAY_MS = 100;
//	public static final int PROB_SIZE = 500;
//
//	private final Random random = new Random(0);
//	private final SolverListener listener;
//	private final ArrayList<Move> moves = new ArrayList<>();
//
//	public Solver(SolverListener listener) {
//		this.listener = listener;
//	}
//
//	public Solution solve(Problem problem) {
//
//		/* create moves */
//		createMoves(problem);
//
//		/* meta settings ----------------------------------- */
//
////		int MAX_IDLE = 1000000;
////		int L = 10000;
//
//		int MAX_IDLE = 100000;
//		int L = 1000;
//
//		/* create initial solution ------------------------- */
//		MtSolution solution = Constructive.create(problem, random);
//		MtSolution bestSolution = new MtSolution(solution);
//		listener.improved(solution.convert());
//
//		solution.convert().validate(System.err);
//
//		/* [meta] init ------------------------------------- */
//		int idle = 0;
//		int count = 0;
//		double bound = solution.objective();
//
//		/* loop -------------------------------------------- */
//		while (true) {
//
//			double oldDist = solution.objective();
//			// move
//			Move move = moves.get(random.nextInt(moves.size()));
//			MtSolution neighbour = move.getNeighbour(solution);
//			double newDist = neighbour.objective();
//
//			// [meta] accept?
//			if (newDist < oldDist || newDist < bound) {
//				solution = neighbour;
//				if (newDist < bestSolution.objective() && bestSolution.isFeasible()) {
//					idle = 0;
//					bestSolution = new MtSolution(solution);
//					listener.improved(bestSolution.convert());
//				}
//			} else {
//				idle++;
//			}
//
//			// [meta] update
//			count++;
//			if (count == L) {
//				count = 0;
//				bound = solution.objective();
//			}
//
//			// stop?
//			if (idle >= MAX_IDLE) {
//				break;
//			}
//		}
//
//		/* finished ---------------------------------------- */
//
//		return bestSolution.convert();
//	}
//
//	private void createMoves(Problem problem) {
//		moves.clear();
//		moves.add(new RuinAndRecreateMove(problem, random));
//	}
//
//}

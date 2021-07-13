package be.kuleuven.stgp.mip.column_generation.pricingheuristic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeSet;

/**
 * Created by Jan on 29-2-2016.
 */
public class TreeSetHashSetTest {

	public static void main(String[] args){

		// -Xmx100M
		// output:
		//    HashSets: 2347000
		//    TreeSets: 2281000

		boolean TREE_SET = false;

		Random random = new Random(0);

		int listSize = 100;
		int maxValue = 200;

		if(TREE_SET) {
			ArrayList<TreeSet<Integer>> lists = new ArrayList<>();
			while(true) {
				TreeSet<Integer> l = new TreeSet<>();
				for (int j = 0; j < listSize; j++) {
					l.add(random.nextInt(maxValue));
					lists.add(l);
				}
				if(lists.size()%1000==0) System.out.println("TreeSets: " + lists.size());
			}
		}else {
			ArrayList<HashSet<Integer>> lists = new ArrayList<>();
			while(true) {
				HashSet<Integer> l = new HashSet<>(listSize, 0.95f);
				for (int j = 0; j < listSize; j++) {
					l.add(random.nextInt(maxValue));
					lists.add(l);
				}
				if(lists.size()%1000==0) System.out.println("HashSets: " + lists.size());
			}
		}

	}
}

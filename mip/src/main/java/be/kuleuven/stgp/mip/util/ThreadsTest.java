package be.kuleuven.stgp.mip.util;

import java.util.Random;

/**
 * Created by Jan on 9-3-2016.
 */
public class ThreadsTest {

	private final Random random = new Random(0);

	public ThreadsTest() {

		Threads threads = new Threads(16);
		final int nodes = 20;
		final int threadsPerNode = 2;

		// 100 branches
		for (int i = 0; i < nodes; i++) {
			final String branchName = "Node " + (i+1);
			// create runnables
			Runnable[] runnables = new Runnable[threadsPerNode];
			for (int j = 0; j < threadsPerNode; j++) {
				runnables[j] = ()-> executeSomeLongMethod();
			}
			// execute them
			threads.executeThreads(branchName, runnables);
		}
	}

	public void executeSomeLongMethod(){
		int minMill = 2000;
		int maxMill = 10000;
		int mill = random.nextInt(maxMill-minMill) + minMill;
		Object dummyObj = new Object();
		synchronized (dummyObj){
			try {
				dummyObj.wait(mill);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) {
		new ThreadsTest();
	}

}

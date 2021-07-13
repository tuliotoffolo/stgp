package be.kuleuven.stgp.mip.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Created by Jan on 9-3-2016.
 */
public class Threads {

	private final Semaphore semaphore;
	private final long createTime = System.currentTimeMillis();

	public Threads(int maxThreads) {
		semaphore = new Semaphore(maxThreads);
	}

	public void executeAndJoinThreads(String info, final Runnable... runnable) {
		try {
			// wait for n threads to be available
			semaphore.acquire(runnable.length);

			// we got them, start the runnables
			CountDownLatch latch = new CountDownLatch(runnable.length);

			// wait for n threads to be available
			log(info + " is waiting...");
			semaphore.acquire(runnable.length);
			log(info + " can start...");
			// we got them, start the runnables
			for (int i = 0; i < runnable.length; i++) {
				Runnable r = runnable[i];
				final String rInfo = info + " [" + (i+1) + "/" + runnable.length + "]";
				new Thread(() -> {
					// execute the runnable
					log("\t\t" + rInfo + " started");
					r.run();
					// finished
					log("\t\t" + rInfo + " finished");
					semaphore.release();
					latch.countDown();
				}).start();
			}

			// join
			latch.await();
			log("\t\t" + info + " all threads finished...");

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void executeThreads(String info, final Runnable... runnable) {
		try {
			// wait for n threads to be available
			log(info + " is waiting...");
			semaphore.acquire(runnable.length);
			log(info + " can start...");
			// we got them, start the runnables
			for (int i = 0; i < runnable.length; i++) {
				Runnable r = runnable[i];
				final String rInfo = info + " [" + (i+1) + "/" + runnable.length + "]";
				new Thread(() -> {
					// execute the runnable
					log("\t\t" + rInfo + " started");
					r.run();
					// finished
					log("\t\t" + rInfo + " finished");
					semaphore.release();
				}).start();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void log(String str){
		long now = System.currentTimeMillis();
		double millis = (double)(now-createTime)/1000;
		System.out.println(String.format("[%.3f sec] %s", millis, str));
	}
}

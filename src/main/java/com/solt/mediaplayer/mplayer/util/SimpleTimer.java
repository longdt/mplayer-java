/**
 * 
 */
package com.solt.mediaplayer.mplayer.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author user
 *
 */
public class SimpleTimer {
	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "SimpleTimer-" + threadNumber.getAndIncrement());
			if (!t.isDaemon()) {
				t.setDaemon(true);
			}
			if (t.getPriority() != Thread.NORM_PRIORITY) {
				t.setPriority(Thread.NORM_PRIORITY);
			}
			return t;
		}
	});
	
	public static void schedule(Runnable task, long delay) {
		executor.schedule(task, delay, TimeUnit.MILLISECONDS);
	}

}

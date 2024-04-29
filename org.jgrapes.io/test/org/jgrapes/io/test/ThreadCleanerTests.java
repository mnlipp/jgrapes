package org.jgrapes.io.test;

import org.jgrapes.io.util.ThreadCleaner;
import org.junit.Test;

public class ThreadCleanerTests {

    protected boolean terminated;

    private void startThread() {
        Object referent = new Object();
        Thread thread = new Thread() {

            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        interrupt();
                    }
                }
                terminated = true;
            }

        };
        thread.start();
        ThreadCleaner.watch(referent, thread);
    }

    @Test(timeout = 10000)
    public void test() {
        startThread();
        while (!terminated) {
            System.gc();
        }
    }

}

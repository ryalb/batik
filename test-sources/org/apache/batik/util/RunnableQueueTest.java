/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.util;

import org.apache.batik.test.AbstractTest;
import org.apache.batik.test.DefaultTestReport;
import org.apache.batik.test.TestReport;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class RunnableQueueTest extends AbstractTest {

    public int nThreads;
    public int activeThreads;
    public Random rand;
    public RunnableQueue rq;

    /**
     * Constructor
     * @param nThreads number of runnables to queue
     * @param sync     Should requests be made synchronously (from
     *                 different threads).
     */
    public RunnableQueueTest(int nThreads, boolean sync) {
        this.nThreads = nThreads;
    }
        
        /**
         * Returns this Test's name
         */
        public String getName() {
            return "RunnableQueue Stress Test";
        }

    /**
     * This method will only throw exceptions if some aspect
     * of the test's internal operation fails.
     */
    public TestReport runImpl() throws Exception {
        rq = RunnableQueue.createRunnableQueue();

        List l = new ArrayList(nThreads);
        rand = new Random(2345);

        // Two switch flickers to make things interesting...
        l.add(new SwitchFlicker());
        l.add(new SwitchFlicker());

        for (int i=0; i<nThreads; i++) {
            Runnable rqRable = new RQRable(i, rand.nextInt(50)+1);
            l.add(new TPRable(rq, i, rand.nextBoolean(),
                              rand.nextInt(500)+1, 20, rqRable));
        }

        synchronized (this) {
            ThreadPounder tp = new ThreadPounder(l);
            tp.start();
            activeThreads = nThreads;
            while (activeThreads != 0) {
                wait();
            }
        }

        System.exit(0);
        return null;
    }

    public class SwitchFlicker implements Runnable {
        public void run() {
            boolean suspendp, waitp;
            int time;
            while (true) {
                try {
                    synchronized (rand) {
                        suspendp = rand.nextBoolean();
                        waitp = rand.nextBoolean();
                        time  = rand.nextInt(500);
                    }
                    if (suspendp) {
                        // 1/2 of the time suspend, 1/2 time wait, 1/2 the
                        // time don't
                        rq.suspendExecution(waitp);
                        System.out.println("Suspended - " + 
                                           (waitp?"Wait":"Later"));
                        Thread.sleep(time/10);
                    } else {
                        // 1/2 the time resume
                        rq.resumeExecution();
                        System.out.println("Resumed");
                        Thread.sleep(time);
                    }
                } catch(InterruptedException ie) { }
            }
        }
    }

    public class TPRable implements Runnable {
        RunnableQueue rq;
        int           idx;
        boolean       invokeAndWait;
        long          repeatDelay;
        int           count;
        Runnable      rqRable;

        TPRable(RunnableQueue rq, int idx, 
                boolean invokeAndWait,
                long    repeatDelay, int count,
                Runnable rqRable) {
            this.rq            = rq;
            this.idx           = idx;
            this.invokeAndWait = invokeAndWait;
            this.repeatDelay   = repeatDelay;
            this.count         = count;
            this.rqRable       = rqRable;
        }

        public void run() {
            try {
                while (count-- != 0) {
                    if (invokeAndWait) {
                        System.out.println("     InvW #" + idx);
                        rq.invokeAndWait(rqRable);
                        System.out.println("Done InvW #" + idx);
                    } else {
                        synchronized (rqRable) {
                            System.out.println("     InvL #" + idx);
                            rq.invokeLater(rqRable);
                            System.out.println("Done InvL #" + idx);
                            rqRable.wait();
                        }
                    }
                    if (repeatDelay < 0) 
                        break;
                    Thread.sleep(repeatDelay);
                }
            } catch (InterruptedException ie) {
            }
            synchronized(RunnableQueueTest.this) {
                activeThreads--;
                RunnableQueueTest.this.notify();
            }
        }
    }

    public static class RQRable implements Runnable {
        int  idx;
        long dur;

        RQRable(int idx, long dur) {
            this.idx = idx;
            this.dur = dur;
        }

        public void run() {
            try {
                System.out.println("      B Rable #" + idx);
                Thread.sleep(dur);
                System.out.println("      E Rable #" + idx);
                synchronized (this) {
                    notify();
                }
            } catch (InterruptedException ie) { }
        }
    }

    public static void main(String []args) {
        RunnableQueueTest rqt = new RunnableQueueTest(20, false);
        try {
            rqt.runImpl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

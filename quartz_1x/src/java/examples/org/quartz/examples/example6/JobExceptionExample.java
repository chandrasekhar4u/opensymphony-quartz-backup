/*
 * Copyright (c) 2005 by OpenSymphony
 * All rights reserved.
 * 
 */
package org.quartz.examples.example6;

import java.util.Date;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleTrigger;
import org.quartz.helpers.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * 
 * This job demonstrates how Quartz can handle JobExecutionExceptions that are
 * thrown by jobs.
 * 
 * @author Bill Kratzer
 */
public class JobExceptionExample {

	public void run() throws Exception {
		Log log = LogFactory.getLog(JobExceptionExample.class);

		log.info("------- Initializing ----------------------");

		// First we must get a reference to a scheduler
		SchedulerFactory sf = new StdSchedulerFactory();
		Scheduler sched = sf.getScheduler();

		log.info("------- Initialization Complete ------------");

		log.info("------- Scheduling Jobs -------------------");

		// jobs can be scheduled before start() has been called

		// get a "nice round" time a few seconds in the future...
		long ts = TriggerUtils.getNextGivenSecondDate(null, 15).getTime();

		// badJob1 will run every three seconds
		// this job will throw an exception and refire
		// immediately
		JobDetail job = new JobDetail("badJob1", "group1", BadJob1.class);
		SimpleTrigger trigger = new SimpleTrigger("trigger1", "group1",
				new Date(ts), null, SimpleTrigger.REPEAT_INDEFINITELY, 3000L);
		Date ft = sched.scheduleJob(job, trigger);
		log.info(job.getFullName() + " will run at: " + ft + " and repeat: "
				+ trigger.getRepeatCount() + " times, every "
				+ trigger.getRepeatInterval() / 1000 + " seconds");

		// badJob2 will run every three seconds
		// this job will throw an exception and never
		// refire
		job = new JobDetail("badJob2", "group1", BadJob2.class);
		trigger = new SimpleTrigger("trigger2", "group1", new Date(ts), null,
				SimpleTrigger.REPEAT_INDEFINITELY, 3000L);
		ft = sched.scheduleJob(job, trigger);
		log.info(job.getFullName() + " will run at: " + ft + " and repeat: "
				+ trigger.getRepeatCount() + " times, every "
				+ trigger.getRepeatInterval() / 1000 + " seconds");

		log.info("------- Starting Scheduler ----------------");

		// jobs don't start firing until start() has been called...
		sched.start();

		log.info("------- Started Scheduler -----------------");

		try {
			// sleep for thirty seconds
			Thread.sleep(30L * 1000L);
		} catch (Exception e) {
		}

		log.info("------- Shutting Down ---------------------");

		sched.shutdown(true);

		log.info("------- Shutdown Complete -----------------");

		SchedulerMetaData metaData = sched.getMetaData();
		log.info("Executed " + metaData.numJobsExecuted() + " jobs.");
	}

	public static void main(String[] args) throws Exception {

		JobExceptionExample example = new JobExceptionExample();
		example.run();
	}

}
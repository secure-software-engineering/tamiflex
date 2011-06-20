package de.bodden.tamiflex.reporting.rt;

import static de.bodden.tamiflex.reporting.rt.Entry.Status.ATTEMPTED;
import static de.bodden.tamiflex.reporting.rt.Entry.Status.FAILED;
import static de.bodden.tamiflex.reporting.rt.Entry.Status.SUCCEEDED;

public class Entry {
	
	public static enum Status { ATTEMPTED, SUCCEEDED, FAILED }
	
	protected Status status;
	
	protected final Thread thread;
	
	protected final String payload;

	private final int perThreadTimeStamp;

	private final StackTraceElement[] stackTrace;
	
	public Entry(int perThreadTimeStamp, StackTraceElement[] stackTrace, String payload) {
		this.perThreadTimeStamp = perThreadTimeStamp;
		this.stackTrace = stackTrace;
		this.payload = payload;
		this.thread = Thread.currentThread();
		this.status = ATTEMPTED;
	}
	
	@Override
	public String toString() {
		return status + ";" + thread.getId()+"-"+thread.getName() + ";" + payload;
	}
	
	public boolean matchesEarlierEntry(Entry earlierEntry) {
		if(earlierEntry.perThreadTimeStamp>=perThreadTimeStamp) 
			throw new IllegalArgumentException("not an earlier entry!");
		if(earlierEntry.thread != thread) {
			throw new IllegalArgumentException("not an earlier entry! (different threads)");
		}
		return earlierEntry.status == ATTEMPTED && stackTrace.length == earlierEntry.stackTrace.length && payload.equals(earlierEntry.payload); 
	}

	public void markAsSucceeded() {
		if(!successUnknown()) throw new IllegalStateException("status already set!");
		status = SUCCEEDED;
	}

	public void markAsFailed() {
		if(!successUnknown()) throw new IllegalStateException("status already set!");
		status = FAILED;
	}
	
	public boolean successUnknown() {
		return status == ATTEMPTED;
	}

	public int getStackDepth() {
		return stackTrace.length;
	}
	
	public StackTraceElement[] getStackTrace() {
		return stackTrace;
	}
}

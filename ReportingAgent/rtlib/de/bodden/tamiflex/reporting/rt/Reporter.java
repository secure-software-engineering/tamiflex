package de.bodden.tamiflex.reporting.rt;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Reporter {
	
	private final static boolean PRINT_STACK_TRACES = false; 
	private final static int NUM_STACK_FRAMES = 7; 

	static class Data {
		Set<String> threadIDs = new HashSet<String>();
		int numSuccessfulCalls, numFailedCalls;
		Map<String,Set<String>> kindToArguments = new HashMap<String, Set<String>>();
		Set<List<String>> stackTraces = new HashSet<List<String>>();
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if(PRINT_STACK_TRACES)
				for(List<String> trace: stackTraces) {
					for(String frame: trace) {
						sb.append(frame+"\n");
					}
					sb.append("\n");
				}
			sb.append("Thread IDs:   "+threadIDs+"\n");
			sb.append("Success rate: "+(numSuccessfulCalls+0.0)/(numSuccessfulCalls+numFailedCalls)*100+"% ("+numSuccessfulCalls+" succeeded, "+numFailedCalls+ " failed)\n");
			for(Map.Entry<String,Set<String>> entry: kindToArguments.entrySet()) {
				sb.append(entry.getKey());
				sb.append("\n");
				for(String arguments: entry.getValue()) {
					sb.append("    ");
					sb.append(arguments);
					sb.append("\n");
				}
			}			
			return sb.toString();
		}
	}
	
	public static void generateReport(List<Entry> allEntries, PrintWriter out) {
		Map<String,Data> callSiteToData = new HashMap<String, Data>();
		for (Entry entry : allEntries) {
			String payload = entry.payload;
			
			String[] items = payload.split(";");
			String method = items[0];
			String line = items[1];
			
			String callSite = method+":"+line;			
			Data data = callSiteToData.get(callSite);
			if(data==null) {
				data = new Data();
				callSiteToData.put(callSite, data);
			}

			data.threadIDs.add(entry.thread.getName()+"-"+entry.thread.getId());

			boolean successful = entry.status == Entry.Status.SUCCEEDED;
			if(successful) data.numSuccessfulCalls++; else data.numFailedCalls++;
			
			String kind = items[2];
			
			Set<String> arguments = data.kindToArguments.get(kind);
			if(arguments==null) {
				arguments = new HashSet<String>();
				data.kindToArguments.put(kind,arguments);
			}
			
			String args = payload.substring(payload.lastIndexOf(kind)+kind.length());
			arguments.add(args);
			
			StackTraceElement[] stackTrace = entry.getStackTrace();
			List<String> partialTrace = new LinkedList<String>(); 
			for(int i=1;i<NUM_STACK_FRAMES;i++) {
				if(i<stackTrace.length) {
					StackTraceElement frame = stackTrace[i];
					partialTrace.add("    " + frame.getClassName()+"."+frame.getMethodName()+":"+frame.getLineNumber());
				}				
			}
			data.stackTraces.add(partialTrace);
		}	
		
		List<String> callSites = new ArrayList<String>(callSiteToData.keySet());
		Collections.sort(callSites);
		
		out.println("Report for "+callSites.size()+" call sites.");
		out.println();
		out.println();
		
		for (String callSite : callSites) {
			out.println(callSite);
			out.println();
			out.println(callSiteToData.get(callSite));
			out.println();
			out.println("Remarks:");
			out.println();			
			out.println("================================================================================");
		}
	}

}

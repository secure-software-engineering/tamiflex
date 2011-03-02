package de.bodden.tamiflex.reporting.rt;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Reporter {

	static class Data {
		Set<String> threadIDs = new HashSet<String>();
		int numSuccessfulCalls, numFailedCalls;
		Map<String,Set<String>> kindToArguments = new HashMap<String, Set<String>>();
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Thread IDs:   "+threadIDs+"\n");
			sb.append("Success rate: "+(numSuccessfulCalls+0.0)/(numSuccessfulCalls+numFailedCalls)+"% ("+numSuccessfulCalls+" succeeded, "+numFailedCalls+ " failed)\n");
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

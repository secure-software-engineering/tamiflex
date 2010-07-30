import java.util.ArrayList;
import java.util.Arrays;

import soot.CompilationDeathException;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.jimple.toolkits.reflection.ReflectiveCallsInliner;
import soot.options.Options;
import soot.rtlib.DefaultHandler;
import soot.rtlib.IUnexpectedReflectiveCallHandler;
import soot.rtlib.OpaquePredicate;
import soot.rtlib.SootSig;
import soot.rtlib.UnexpectedReflectiveCall;


public class ReflInliner {
	
	public static void main(String[] args) {
		PackManager.v().getPack("wjpp").add(new Transform("wjpp.inlineReflCalls", new ReflectiveCallsInliner(true)));		
		Scene.v().addBasicClass(Object.class.getName());
		Scene.v().addBasicClass(SootSig.class.getName(),SootClass.BODIES);
		Scene.v().addBasicClass(UnexpectedReflectiveCall.class.getName(),SootClass.BODIES);
		Scene.v().addBasicClass(IUnexpectedReflectiveCallHandler.class.getName(),SootClass.BODIES);
		Scene.v().addBasicClass(DefaultHandler.class.getName(),SootClass.BODIES);
		Scene.v().addBasicClass(OpaquePredicate.class.getName(),SootClass.BODIES);
		ArrayList<String> argList = new ArrayList<String>(Arrays.asList(args));
		argList.add("-w");
		argList.add("-p");
		argList.add("cg");
		argList.add("enabled:false");
		
		G.v().out.println("TamiFlex Booster");
		try {
			soot.Main.main(argList.toArray(new String[0]));
		} catch(CompilationDeathException e) {
			G.v().out.println("\nERROR: "+e.getMessage()+"\n");
			G.v().out.println("The command-line options are described at:\n" +
					"http://www.sable.mcgill.ca/soot/tutorial/usage/index.html");
			if(Options.v().verbose()) {
				throw e;
			} else {
				G.v().out.println("Use -verbose to see stack trace.");
			}
			G.v().out.println();
			usage();
		}
	}

	private static void usage() {
		G.v().out.println(Options.v().getUsage());
	}

}

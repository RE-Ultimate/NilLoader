package nilloader;

import java.util.ArrayList;
import java.util.List;
import nilloader.api.NilLogger;
import nilloader.impl.log.AdHocLogImpl;
import nilloader.impl.log.NilLogImpl;

/**
 * Entrypoint to NilLoader's logging abstraction, which will try to detect and delegate to the best
 * available logging API in the current environment.
 */
public class NilLogManager {
	private static final NilLogImpl IMPL;
	
	static List<Runnable> initLogs = new ArrayList<>();
	
	static {
		/*
		 * Attempting to load the application logger config can (through very long dependency chain
		 * accidents involving custom appenders and formatters) load application classes
		 * prematurely, preventing them from being correctly patched.
		 * 
		 * So, give up. Always use the "adhoc" logger impl.
		 */
		IMPL = new AdHocLogImpl("NilLoader");
	}
	
	public static NilLogger getLogger(String name) {
		return new NilLogger(IMPL.fork(name));
	}
	
}

package com.ibm.airlock.engine;


/**
 * Created by DenisV on 8/31/16.
 */
public class ScriptExecutionTimeoutException extends Error {

	private static final long serialVersionUID = 1L;

	public ScriptExecutionTimeoutException() {
        super("Script execution timeout");
    }
}

package com.ibm.airlock.engine;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class StreamsScriptInvoker extends ScriptInvoker
{
//	final String EVENT_TEMPLATE = "event";
//	final String EVENTS_TEMPLATE = "events";
//	final String RESULTS_TEMPLATE = "results";
//	final String CACHE_TEMPLATE = "cache";

    Context globalRhino;
    Scriptable sharedScope;
    Map<String, String> savedObjects;
    String savedFunctions;
    @Override
	public ScriptInvoker clone() {
		try {
			return new StreamsScriptInvoker(savedObjects, savedFunctions);
		} catch (InvokerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public StreamsScriptInvoker(Map<String, String> objects, String functions) throws InvokerException
	{
		super(objects);
		this.savedFunctions = functions;
		this.savedObjects = objects;
		if (scriptObjects == null)
			scriptObjects = new TreeMap<String, String>();

//		if (!scriptObjects.containsKey(CACHE_TEMPLATE))
//			scriptObjects.put(CACHE_TEMPLATE, "{}");
//		if (!scriptObjects.containsKey(EVENT_TEMPLATE))
//			scriptObjects.put(EVENT_TEMPLATE, "{'name' : 'event_name' }");
//		if (!scriptObjects.containsKey(EVENTS_TEMPLATE))
//			scriptObjects.put(EVENTS_TEMPLATE, "{}");
//		if (!scriptObjects.containsKey(RESULTS_TEMPLATE))
//			scriptObjects.put(RESULTS_TEMPLATE, "{}");

		StringBuilder sb =  new StringBuilder();
		for (Map.Entry<String, String> e : scriptObjects.entrySet())
		{
			sb.append("\nvar ");
			sb.append(e.getKey());
			sb.append(" = ");
			sb.append(e.getValue());
			sb.append(";");
		}

 		new SafeContextFactory().makeContext();
		globalRhino = Context.enter();

        try {
			sharedScope = globalRhino.initStandardObjects();

			if (functions != null)
				globalRhino.evaluateString(sharedScope, functions, "<functions>", 1, null);

			String str = sb.toString();
			globalRhino.evaluateString(sharedScope, str, "<objects>", 1, null);
        }
        catch (Throwable e){
        	throw new InvokerException("Javascript shared scope initialization error: " + e.getMessage());
        }
        finally {
        //	Context.exit();
        }
    }
/*
	// each filter evaluation requires an event name and returns true/false/error
	@Deprecated
	public Output evaluateFilter(String query, String eventName)
	{
	   	if (query == null || query.isEmpty())
	   		return new Output(Result.TRUE);
	   	if (invalidName(eventName))
	   		return new Output(Result.ERROR, "invalid event name " + eventName);

       try {

			String overrideEvent = "var event = {'name':'" + eventName + "'};";
			query = overrideEvent + query;
			Object result = globalRhino.evaluateString(sharedScope, query, "JavaScript", 1, null);

			int out = -1;
			if (result instanceof Boolean)
				out = Context.toBoolean(result) ? 1 : 0;

			if (out == 0)
				return new Output(Result.FALSE);
			if (out == 1)
				return new Output(Result.TRUE);

			return new Output(Result.ERROR, "Script result is not boolean");
		}
       catch (ScriptExecutionTimeoutException e) {
    	   return new Output(Result.ERROR, "Javascript timeout: " + e.getMessage());
       }
       catch (Throwable e) {
    	   return new Output(Result.ERROR, "Javascript error: " + e.getMessage());
       }
       finally {
    	   //Context.exit();
       }
	}*/

	public Output evaluateFilter(String query, JSONObject event)
	{
	   	if (query == null || query.isEmpty())
	   		return new Output(Result.TRUE);

       try {

			String overrideEvent = "var event = " + event.write() + ";";
			query = overrideEvent + query;
			Object result = globalRhino.evaluateString(sharedScope, query, "JavaScript", 1, null);

			int out = -1;
			if (result instanceof Boolean)
				out = Context.toBoolean(result) ? 1 : 0;

			if (out == 0)
				return new Output(Result.FALSE);
			if (out == 1)
				return new Output(Result.TRUE);

			return new Output(Result.ERROR, "Script result is not boolean");
		}
       catch (ScriptExecutionTimeoutException e) {
    	   return new Output(Result.ERROR, "Javascript timeout: " + e.getMessage());
       }
       catch (Throwable e) {
    	   return new Output(Result.ERROR, "Javascript error: " + e.getMessage());
       }
       finally {
    	   //Context.exit();
       }
	}
	static Set<Character> invalid = new TreeSet<Character>(Arrays.asList('\b', '\f', '\n', '\r', '\t', '\0', '\'', '\"'));
	public static boolean invalidName(String name)
	{
		if (name == null || name.isEmpty())
			return true;

		for (int i = 0; i < name.length(); ++i)
			if (invalid.contains(name.charAt(i)))
				return true;

		return false;
	}
	// each processor evaluation requires a computed eventObject and returns a result JSON
	// the processor is assumed to changed the content of the 'results' object
	public JSONObject evaluateProcessor(String processor, String eventObject) throws InvokerException
	{
		 String str=null;
		 if (processor == null || processor.isEmpty())
      		return new JSONObject();

        try {
			String overrideEvents = "var result = {}; var cache = {}; var events = " + eventObject + ";";
			String exec = overrideEvents + processor + "; JSON.stringify(result); ";
			Object result = globalRhino.evaluateString(sharedScope, exec, "JavaScript", 1, null);

            str = Context.toString(result);
            return new JSONObject(str);
        }
        catch (ScriptExecutionTimeoutException e){
        		throw new InvokerException("Javascript timeout: " + e.getMessage());
        }
        catch (Throwable e){
        		e.printStackTrace();
        		System.out.println("str ="  + str);
        		throw new InvokerException("Javascript error: " + e.getMessage());
        }
        finally {
         //   Context.exit();
        }

	}

	@Override
	public Output evaluate(String query) {
		return null;
	}
	@Override
	public JSONObject evaluateConfiguration(String configString) throws InvokerException {
		return null;
	}
	@Override
	public JSONObject evaluateRuleAndConfiguration(String rule, String configString) throws InvokerException {
		return null;
	}

	@Override
	public JSONObject evaluateBothRuleAndConfiguration(String rule, String configString) throws InvokerException {
		return null;
	}


}

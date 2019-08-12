package com.ibm.airlock.engine;
/*
import android.util.Log;

import java.util.Map;
import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSValue;

public class AndroidScriptInvoker extends ScriptInvoker
{
	StringBuilder b = new StringBuilder();

	AndroidScriptInvoker(Map<String, String> scriptObjects)
	{
		super(scriptObjects);

		// add items such as profile, context etc as strings to the JS binding
		for (Map.Entry<String, String> e : scriptObjects.entrySet())
		{
			b.append("\nvar ");
			b.append(e.getKey());
			b.append('=');
			b.append(e.getValue());
			b.append(';');
		}
	}

	@Override
	Result evaluate(String query)
	{
			StringBuilder sb = new StringBuilder(b.toString());
			sb.append('(');
			sb.append(query);
			sb.append(')');

			JSContext jsContext = new JSContext();
			JSValue jsValue = jsContext.evaluateScript(sb.toString());

			if (jsValue.isBoolean()) {
				Log.i("RETURN VALUE", jsValue.toString());
				return (jsValue.toBoolean()) ? Result.TRUE : Result.FALSE;
			}
			return Result.ERROR; // "rule does not return true or false
	}

}
*/

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

//import com.weather.airlock.sdk.util.Constants;
//import android.support.annotation.VisibleForTesting;
//import android.util.Log;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.ibm.airlock.Constants;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by DenisV on 8/22/16.
 */
public class RhinoScriptInvoker extends ScriptInvoker
{
	// function definitions and device context objects are added into the shared scope.
	// all queries will use a new scope that refers to the shared scope
    Scriptable sharedScope;
    JSONObject usrContext;
 // SCOPING commented out - using a global context allowing a rule to access variables defined by other rules
    Context globalRhino;

    public ScriptInvoker clone() {
    	
    	ScriptInvoker toRet;
		try {
			toRet = new RhinoScriptInvoker(this.usrContext);
			return toRet;
		} catch (JSONException | InvokerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }
    public RhinoScriptInvoker(JSONObject userContext) throws JSONException, InvokerException 
	{
    	super(new TreeMap<String, String>()); // scriptObjects
    	this.usrContext = userContext;
 		String functions = null;
 		boolean validations = false;

		@SuppressWarnings("unchecked")
		Set<String> keys = userContext.keySet();
		for (String key :  keys)
		{
			if (key.equals(Constants.JSON_FIELD_JAVASCRIPT_UTILITIES))
				functions = userContext.getString(key);
			else  if (key.equals(Constants.JSON_FIELD_VALIDATION_MODE))
				validations = true;
			else
			{
				JSONObject json = getJSONObject(userContext, key);
				scriptObjects.put(key, json.toString());
			}
		}

		if (functions == null)
			throw new InvokerException("missing javascript utility functions in user context");

		init(functions, validations);
	}

    // verify that a key points to a JSONObject
    static public JSONObject getJSONObject(JSONObject parent, String key) throws JSONException
    {
    	JSONObject item;
		try {
			item = parent.getJSONObject(key);
		}
		catch (JSONException e) {
			throw new JSONException("key " + key + " does not refer to a JSONObject");
		}
		return item;
    }
    public RhinoScriptInvoker(Map<String, String> scriptObjects, String functions, boolean validations) throws InvokerException
    {
        super(scriptObjects); // unused
    	init(functions, validations);
    }
   void init(String functions, boolean validations) throws InvokerException
    {
        StringBuilder ruleEngineContextBuffer =  new StringBuilder();
        if (validations)
        	ruleEngineContextBuffer.append("var " + Constants.JSON_FIELD_VALIDATION_MODE + " = true;");
 
        // add items such as profile, context etc as strings to the JS binding
        prepareContextObjects(ruleEngineContextBuffer, scriptObjects);
 
		// create and enter safe execution context
        //  Context rhino = safeContextFactory.makeContext().enter();
 		new SafeContextFactory().makeContext();
		globalRhino = Context.enter();

        try {
        	// can seal it with initStandardObjects(null, true) and sealObject()
			sharedScope = globalRhino.initStandardObjects();
			globalRhino.evaluateString(sharedScope, functions, "<functions>", 1, null);
			String str = ruleEngineContextBuffer.toString();
			globalRhino.evaluateString(sharedScope, str, "<context>", 1, null);
        }
        catch (Throwable e){
        	throw new InvokerException("Javascript shared scope initialization error: " + e.getMessage());
        }
        finally {
        //	Context.exit();
        }
    }

   protected void prepareContextObjects(StringBuilder ruleEngineContextBuffer, Map<String, String> scriptObject)
   {
	   // create_frozen_object creates an object. in validation mode it also freezes and alerts.
       for (Map.Entry<String, String> e : scriptObject.entrySet())
       {
			String key = e.getKey();
			String value = e.getValue();

			ruleEngineContextBuffer.append("\ncreate_frozen_object(\"");
			ruleEngineContextBuffer.append(key);
			ruleEngineContextBuffer.append("\", ");
			ruleEngineContextBuffer.append(value);
			ruleEngineContextBuffer.append(");");
       }
   }

    @Override
    public Output evaluate(String query)
    {
    	if (query == null || query.isEmpty())
    		 return new Output(Result.TRUE);

    	// SCOPING commented out, allowing a rule to access variables defined by another rule
		// create and enter safe execution context
        // new SafeContextFactory().makeContext();
        // Context rhino = Context.enter();

        try {
        	// Scriptable scope = rhino.newObject(sharedScope);
        	// scope.setPrototype(sharedScope);
        	// scope.setParentScope(null);
        	
            // Object result = rhino.evaluateString(scope, query, "JavaScript", 1, null);
        	 Object result = globalRhino.evaluateString(sharedScope, query, "JavaScript", 1, null);

            // read execution result
            int out = -1;
            if (result instanceof Boolean)
            {
            	out = Context.toBoolean(result) ? 1 : 0;
            }

/*
            // optional: evaluate null/undefined result as false rather than error
            else if (result == null || result instanceof org.mozilla.javascript.Undefined)
            	out = 0;

            // optional: allow script to return the numbers 0/1 and the strings true/false
            else if (result instanceof Double)
            {
              	double d = Context.toNumber(result);
              	if (d == 0.) out = 0;
              	if (d == 1.) out = 1;
            }
            else
            {
            	String str = Context.toString(result);
            	if (str != null)
            	{
	            	if (str.equalsIgnoreCase("false")) out = 0;
	            	if (str.equalsIgnoreCase("true"))  out = 1;
            	}
            }
*/

            if (out == 0)
            	return new Output(Result.FALSE);
            if (out == 1)
            	return new Output(Result.TRUE);
  
            return new Output(Result.ERROR, "Script result is not boolean");

        }
        catch (ScriptExecutionTimeoutException e){
        	return new Output(Result.ERROR, "Javascript timeout: " + e.getMessage());
        }
        catch (Throwable e)
        {
        	if (debugMode)
        	{
        		System.out.println("JavaScript error: " + e.getMessage());
        		System.out.println("Script line #1 ==>  " + query);
        	}
        	return new Output(Result.ERROR, "Javascript error: " + e.getMessage());
        }
        finally {
        //    Context.exit();
        }
    }

	@Override
    public JSONObject evaluateConfiguration(String configString) throws InvokerException
    {
      	if (configString == null || configString.isEmpty())
      		return new JSONObject();

       	// SCOPING commented out, allowing a rule to access variables defined by another rule
		// create and enter safe execution context
        // new SafeContextFactory().makeContext();
        // Context rhino = Context.enter();

        try {
        	//Scriptable scope = globalRhino.newObject(sharedScope);
        	//scope.setPrototype(sharedScope);
        	//scope.setParentScope(null);

        	String exec = "var result_ = " + configString + "; JSON.stringify(result_); ";
           // Object result = rhino.evaluateString(scope, exec, "JavaScript", 1, null);
        	Object result = globalRhino.evaluateString(sharedScope, exec, "JavaScript", 1, null);

            String str = Context.toString(result);
            return new JSONObject(str);
  
        }
        catch (ScriptExecutionTimeoutException e){
        	throw new InvokerException("Javascript timeout: " + e.getMessage());
        }
        catch (Throwable e){
        	throw new InvokerException("Javascript error: " + e.getMessage());
        }
        finally {
         //   Context.exit();
        }
    }

	// evaluate rule and configuration together. This allows the configuration to act on variables introduced in the rule
	@Override
	public JSONObject evaluateRuleAndConfiguration(String rule, String configString) throws InvokerException
	{
		// SCOPING commented out, allowing a rule to access variables defined by another rule
		//new SafeContextFactory().makeContext();
       // Context rhino = Context.enter();

		String prefix = "Javascript rule error: ";
        try {
        	//Scriptable scope = rhino.newObject(sharedScope);
        	//scope.setPrototype(sharedScope);
        	//scope.setParentScope(null);

          	if (rule != null && !rule.isEmpty())
          	{
          		//Object obj = rhino.evaluateString(scope, rule, "JavaScript trigger", 1, null);
          		Object obj = globalRhino.evaluateString(sharedScope, rule, "JavaScript trigger", 1, null);

	            int res = -1;
	            if (obj instanceof Boolean)
	            	res = Context.toBoolean(obj) ? 1 : 0;

	            if (res == 0)
	            	return null; // not triggered

	            if (res != 1)
	            	throw new Exception("configuration rule does not return boolean\n" + rule);
          	}
  
          	prefix = "Javascript configuration error: ";
          	String exec = "var result_ = " + configString + "; JSON.stringify(result_); ";
            //Object result = rhino.evaluateString(scope, exec, "JavaScript configuration", 1, null);
          	Object result = globalRhino.evaluateString(sharedScope, exec, "JavaScript configuration", 1, null);

            String str = Context.toString(result);
            return new JSONObject(str);
        }
        catch (ScriptExecutionTimeoutException e){
        	throw new InvokerException("Javascript timeout: " + e.getMessage());
        }
        catch (Throwable e){
        	throw new InvokerException(prefix + e.getMessage());
        }
        finally {
         //   Context.exit();
        }
	}

	// evaluate rule and (if there's no JavaScript error) always evaluate configuration.
	// This allows the configuration to check variables introduced in the rule even when the rule fails.
	@Override
	public JSONObject evaluateBothRuleAndConfiguration(String rule, String configString) throws InvokerException
	{
		// SCOPING commented out, allowing a rule to access variables defined by another rule
		//new SafeContextFactory().makeContext();
       // Context rhino = Context.enter();

		String prefix = "Javascript rule error: ";
        try {
        	//Scriptable scope = rhino.newObject(sharedScope);
        	//scope.setPrototype(sharedScope);
        	//scope.setParentScope(null);

          	if (rule != null && !rule.isEmpty())
          	{
          		@SuppressWarnings("unused")
          		//Object obj = rhino.evaluateString(scope, rule, "JavaScript trigger", 1, null);
				Object obj = globalRhino.evaluateString(sharedScope, rule, "JavaScript trigger", 1, null);
          	}
  
          	prefix = "Javascript configuration error: ";
          	String exec = "var result_ = " + configString + "; JSON.stringify(result_); ";
            //Object result = rhino.evaluateString(scope, exec, "JavaScript configuration", 1, null);
          	Object result = globalRhino.evaluateString(sharedScope, exec, "JavaScript configuration", 1, null);

            String str = Context.toString(result);
            return new JSONObject(str);
        }
        catch (ScriptExecutionTimeoutException e){
        	throw new InvokerException("Javascript timeout: " + e.getMessage());
        }
        catch (Throwable e){
        	throw new InvokerException(prefix + e.getMessage());
        }
        finally {
         //   Context.exit();
        }
	}
}
package com.ibm.airlock.engine_dev;

import java.util.Map;
import java.util.TreeMap;

import org.apache.wink.json4j.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.ibm.airlock.engine.SafeContextFactory;
import com.ibm.airlock.engine.ScriptExecutionTimeoutException;
import com.ibm.airlock.engine.ScriptInvoker;

public class TestScript  extends ScriptInvoker{

	public TestScript(Map<String, String> scriptObjects) {
		super(scriptObjects);
	}

	@Override
	public Output evaluate(String query) {

		new SafeContextFactory().makeContext();
       Context rhino = Context.enter();

        try {
            Scriptable scope = rhino.initStandardObjects();

            // TOO: initialize context once and reuse
           // rhino.evaluateString(scope, DISTANCE_FUNC, "<cmd>", 1, null);
            Object result = rhino.evaluateString(scope, query, "JavaScript", 1, null);

            System.out.println("Distance is: " + Context.toNumber(result));

        }catch (ScriptExecutionTimeoutException e){
        	return new Output(Result.ERROR, "Javascript timeout: " + e.getMessage());
        }
        catch (Throwable e){
        	return new Output(Result.ERROR, "Javascript error: " + e.getMessage());
        }finally {
            Context.exit();
        }
		return null;
	}
	
	public static void main(String[] args)
	{
		try {
			TreeMap<String,String> dummy = new TreeMap<String,String>();
			
			TestScript t = new TestScript(dummy);
			String query = "var1 = [50, -5]; var2 = [58, -3]; calcDistance(var1, var2, false);" ;

			t.evaluate(query);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public JSONObject evaluateConfiguration(String configString) {
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

	@Override
	public ScriptInvoker clone() {
		TreeMap<String,String> dummy = new TreeMap<String,String>();
		return new TestScript(dummy);
	}
}

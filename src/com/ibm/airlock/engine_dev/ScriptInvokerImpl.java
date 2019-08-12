package com.ibm.airlock.engine_dev;

import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.engine.ScriptInvoker;

public class ScriptInvokerImpl extends ScriptInvoker
{
	static ScriptEngineManager manager = new ScriptEngineManager();
	static ScriptEngine engine = manager.getEngineByName("JavaScript");

	Bindings binding = new SimpleBindings();

	ScriptInvokerImpl(Map<String, String> scriptObjects) throws RuntimeException
	{
		super(scriptObjects);

		// add items such as profile, context etc as strings to the JS binding
		StringBuilder b = new StringBuilder();
		for (Map.Entry<String, String> e : scriptObjects.entrySet())
		{
			String key = e.getKey();
			String stringKey = "_" + key;
			String json = e.getValue();

			b.append("var " + key + " = JSON.parse(" + stringKey + "); ");
			binding.put(stringKey, json);
		}

		// now parse them in a JavaScript snippet and add them to the binding as JSON objects
		try {
			engine.eval(b.toString(), binding);
		}
		catch (ScriptException e) {
			throw new RuntimeException(e.toString());
		}
	}

	@Override
	public Output evaluate(String query)
	{
		try {
			engine.eval("var _result = " + query + " ;", binding);
			Object o = binding.get("_result");

			if (o instanceof Boolean)
				return new Output(((Boolean) o) ? Result.TRUE : Result.FALSE);

			return new Output(Result.ERROR, "rule does not return true or false");
		}
		catch (ScriptException e)
		{
			return new Output(Result.ERROR, "rule exception: " + e.toString());
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
		// TODO Auto-generated method stub
		return null;
	}
}

package com.ibm.airlock.engine_dev;

import java.util.ArrayList;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.ScriptInvoker;

public class Configuration
{
	//----------------------------------------------
	public abstract static class Base
	{
		public abstract JSONObject evaluate(ScriptInvoker invoker) throws ScriptError, JSONException;
	}
	//----------------------------------------------
	public static class Rule extends Base
	{
		String trigger;
		String attributesString;

		public Rule(String trigger, String attributesString)
		{
			this.trigger = trigger;
			this.attributesString = attributesString;
		}

		@Override
		public JSONObject evaluate(ScriptInvoker invoker) throws ScriptError
		{
			ScriptInvoker.Output out = invoker.evaluate(trigger);

			if (out.result == ScriptInvoker.Result.FALSE)
				return null; // not triggered

			if (out.result == ScriptInvoker.Result.ERROR)
				throw new ScriptError("Error evaluating configuration trigger: " + trigger);

			try {
				return invoker.evaluateConfiguration(attributesString);
			}
			catch (Exception e)
			{
				throw new ScriptError("tError evaluating configuration values: " + attributesString);
			}
		}
	}
	//----------------------------------------------
	public static class Group extends Base
	{
		int maxRulesOn = 0; // 0 means all rules apply
		ArrayList<Base> rules;

		public Group(int maxRulesOn, ArrayList<Base> rules)
		{
			this.maxRulesOn = maxRulesOn;
			this.rules = rules;
		}
		@Override
		public JSONObject evaluate(ScriptInvoker invoker) throws ScriptError, JSONException
		{
			int successCount = 0;
			JSONObject out = new JSONObject();

			for (Base base : rules)
			{
				JSONObject oneRule = base.evaluate(invoker);
				if (oneRule == null)
					continue; // trigger did not apply

				Utilities.mergeJson(out, oneRule);

				if (maxRulesOn > 0 && ++successCount == maxRulesOn)
					break;
			}

			return out;
		}
	}
	//----------------------------------------------
	public static class ScriptError extends Exception
	{
		ScriptError(String str) {
			super(str);
		}
		private static final long serialVersionUID = 1L;
	}
	//----------------------------------------------
}

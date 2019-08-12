package com.ibm.airlock.utilities;


import org.apache.wink.json4j.JSONObject;
import com.ibm.airlock.admin.SchemaValidator;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.VerifyRule;

public class VerifyConfiguration
{
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: VerifyConfiguration propertiesFile");
			return;
		}

		try {

			Utilities.Props props = new Utilities.Props();
			props.load(args[0]);

			String schema = Utilities.readString(props.get("outputSchema"));
			String config = Utilities.readString(props.get("configuration"));
			String minimalContext = Utilities.readString(props.get("minimalContext"));
			String maximalContext = Utilities.readString(props.get("maximalContext"));
			String minVer = Utilities.readString(props.get("minVer"));
			String stage = Utilities.readString(props.get("stage"));
			String functions = Utilities.readString(props.get("functions"));
			String translations = Utilities.readString(props.get("translations"));
			String ajv = Utilities.readString(props.get("ajv"));
			String validator = Utilities.readString(props.get("validator"));
			boolean relaxed = props.get("relaxed").equals("true");
			// if context is maximal (all fields are present) perform extra validation


			System.out.println("evaluating configuration string...");
			Environment env = new Environment();
			env.setServerVersion("2");
			JSONObject evaluated = VerifyRule.fullConfigurationEvaluation("", config, minimalContext, maximalContext, functions, translations, minVer, stage, env);

			//JSONObject evaluated = VerifyRule.checkBothConfigurationAndTrigger("", config, context, functions, translations, maximalContext, "dummy", "dummy", env);
			//evaluated = VerifyRule.checkConfigurationWithTrigger("", config, context, functions, translations, minimalContext, "dummy", "dummy", env);
			String formatted = evaluated.write(true);

			System.out.println("evaluated configuration:");
			System.out.println(formatted);

			System.out.println("\nvalidating configuration against output schema...");
			System.out.println(relaxed ? "[relaxed mode]" : "[strict mode]");
			SchemaValidator.validation(validator, ajv, schema, evaluated.toString(), relaxed);
			System.out.println("validated ok");

			//long startTime = System.currentTimeMillis();
			//System.out.println("processing took " + (endTime - startTime) + " milliseconds");

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

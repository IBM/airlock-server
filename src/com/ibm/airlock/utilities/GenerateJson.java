package com.ibm.airlock.utilities;

import java.io.PrintWriter;

import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.admin.JsonGenerator;
import com.ibm.airlock.admin.Utilities;

public class GenerateJson
{
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: GenerateJson propertiesFile");
			return;
		}

		try {

			Utilities.Props props = new Utilities.Props();
			props.load(args[0]);

			String schema = Utilities.readString(props.get("inputSchema"));
			String prune = Utilities.readString(props.get("prune"));
			String generator = Utilities.readString(props.get("generator"));
			String jsf = Utilities.readString(props.get("jsf"));
			String stage = props.get("stage");
			String minAppVer = props.get("minAppVer");
			String generatedJsonPath = props.getOptional("generatedJson");
		
			String generated = JsonGenerator.gtest("test_prune_minver", generator, jsf, prune, schema, stage, minAppVer, InputSampleGenerationMode.MAXIMAL, 0.8) ;
			JSONObject json = new JSONObject(generated);
			String formatted = json.write(true);
			PrintWriter pw = new PrintWriter(generatedJsonPath + ".minver.json", "UTF-8");
			pw.println(formatted);
			pw.close();
			
			generated = JsonGenerator.gtest("test_prune_max", generator, jsf, prune, generated, stage, minAppVer, InputSampleGenerationMode.MAXIMAL, 0.8) ;
			json = new JSONObject(generated);
			formatted = json.write(true);
			pw = new PrintWriter(generatedJsonPath + ".max.json", "UTF-8");
			pw.println(formatted);
			pw.close();
			
			generated = JsonGenerator.gtest("test_generate", generator, jsf, prune, generated, stage, minAppVer, InputSampleGenerationMode.MAXIMAL, 0.8) ;
			json = new JSONObject(generated);
			formatted = json.write(true);
			pw = new PrintWriter(generatedJsonPath + ".generated.json", "UTF-8");
			pw.println(formatted);
			pw.close();

/*
			String generated = Utilities.readString(generatedJsonPath + ".max.json");
			generated = JsonGenerator.gtest("test_generate", generator, jsf, prune, generated, stage, minAppVer, InputSampleGenerationMode.MAXIMAL, 0.8) ;
			JSONObject json = new JSONObject(generated);
			String formatted = json.write(true);
			PrintWriter pw = new PrintWriter(generatedJsonPath + ".generated2.json", "UTF-8");
			pw.println(formatted);
			pw.close();
*/

/*			System.out.println("pruning schema...");
			String out = JsonGenerator.pruneSchema(prune, schema, stage, minAppVer);
			JSONObject json = new JSONObject(out);

			System.out.println("pruned schema:");
			String formatted = json.write(true);
			System.out.println(formatted);
			if (generatedJsonPath != null)
			{
				PrintWriter pw = new PrintWriter(generatedJsonPath + ".pruned.json", "UTF-8");
				pw.println(formatted);
				pw.close();
			}

			System.out.println("generating json...");
			String generated = JsonGenerator.generation(generator, jsf, prune, schema, stage, minAppVer, InputSampleGenerationMode.MAXIMAL, 0.8) ;
			JSONObject json2 = new JSONObject(generated);
			formatted = json2.write(true);
			System.out.println(formatted);
			if (generatedJsonPath != null)
			{
				PrintWriter pw = new PrintWriter(generatedJsonPath, "UTF-8");
				pw.println(formatted);
				pw.close();
			}
*/
			
/*
			String generated = JsonGenerator.gtest("prune1", generator, jsf, prune, schema, stage, minAppVer, InputSampleGenerationMode.MINIMAL, 0.8) ;
			JSONObject json2 = new JSONObject(generated);
			PrintWriter pw = new PrintWriter(generatedJsonPath + ".prune1.json", "UTF-8");
			pw.println(json2.write(true));
			pw.close();

			generated = JsonGenerator.gtest("prune2", generator, jsf, prune, generated, stage, minAppVer, InputSampleGenerationMode.MINIMAL, 0.8) ;
			json2 = new JSONObject(generated);
			
			pw = new PrintWriter(generatedJsonPath + ".prune2.json", "UTF-8");
			pw.println(json2.write(true));
			pw.close();
			generated = JsonGenerator.gtest("prune3", generator, jsf, prune, generated, stage, minAppVer, InputSampleGenerationMode.MINIMAL, 0.8) ;
			json2 = new JSONObject(generated);
			pw = new PrintWriter(generatedJsonPath + ".prune3.json", "UTF-8");
			pw.println(json2.write(true));
			pw.close();
*/

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

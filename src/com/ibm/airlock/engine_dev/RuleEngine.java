package com.ibm.airlock.engine_dev;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.script.Bindings;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.engine.Percentile;
import com.ibm.airlock.engine.Percentile.PercentileException;
import com.ibm.airlock.engine_dev.RuleParser.Lines;

public class RuleEngine
{
	final static String DESCRIBE = "description";
	final static String COMMENT = "#";

	//--------------------------------------------------------
	static class FeatureData
	{
		Rule rule;
		String description, feature, featureID;
		String minVersion, percentBitmap;
		String trigger; // user friendly rule string
		Percentile percentile;
		Group subGroup = null; // optional

		// JSON strings
		final static String FEATURE = "feature";
		final static String FEATURE_ID = "featureID";
		final static String RULE = "rule";
		final static String TRIGGER = "trigger";
		final static String MIN_VERSION = "minVersion";
		final static String PERCENT_BITMAP = "percentBitmap";
		final static String SUBGROUP = "subGroup";

		// text file strings
		final static String RULE_HEADER = "StartRule: ";
		final static String FEATURE_HEADER = FEATURE + ": ";
		final static String FEATURE_ID_HEADER = FEATURE_ID + ": ";
		final static String MINVERSION_HEADER = MIN_VERSION + ": ";
		final static String PERCENT_HEADER = PERCENT_BITMAP + ": ";
		final static String TRIGGER_HEADER = TRIGGER + ": ";
		final static String RULE_END = "EndRule";

		// add rule from programmatic construction
		FeatureData(String description, String feature, String featureID, String minVersion, String percentBitmap, Rule rule) throws IOException, PercentileException
		{
			this.description = description; this.feature = feature;	this.featureID = featureID;
			this.minVersion = minVersion; this.percentBitmap = percentBitmap;

			if (!percentBitmap.isEmpty())
				percentile = new Percentile(percentBitmap);

			this.rule = rule;
			StringWriter w = new StringWriter();
			rule.toText(w);
			trigger = w.toString();
		}
		// add rule from text string
		FeatureData(String description, String feature, String featureID, String minVersion, String percentBitmap, String trigger, RuleEngine.Flavour flavour) throws EngineException, PercentileException
		{
			this.description = description; this.feature = feature;	this.featureID = featureID;
			this.minVersion = minVersion; this.percentBitmap = percentBitmap;

			if (!percentBitmap.isEmpty())
				percentile = new Percentile(percentBitmap);

			this.trigger = trigger;
			if (flavour == RuleEngine.Flavour.JAVASCRIPT_RULES)
				rule = new Rule.JavaScript(trigger);
			else
				rule = RuleParser.parseRule(trigger);
		}
		public void setSubgroup(Group group)
		{
			subGroup = group;
		}

		FeatureData(JSONObject obj) throws JSONException, EngineException, PercentileException
		{
			description = obj.getString(DESCRIBE);
			feature = obj.getString(FEATURE);
			featureID = obj.getString(FEATURE_ID);
			minVersion = obj.getString(MIN_VERSION);
			percentBitmap = obj.getString(PERCENT_BITMAP);
			trigger = obj.getString(TRIGGER);

			if (!percentBitmap.isEmpty())
				percentile = new Percentile(percentBitmap);

			JSONObject jobj = obj.getJSONObject(RULE);
			rule = Rule.jsonLoader(jobj);

			JSONObject sobj = obj.optJSONObject(SUBGROUP); // optional
			if (sobj != null)
				subGroup = new Group(sobj);
		}

		public void toJSON(Writer w) throws IOException
		{
			Util.writePair("{", DESCRIBE, description, true, w);
			Util.writePair(",", FEATURE, feature, true, w);
			Util.writePair(",", FEATURE_ID, featureID, true, w);
			Util.writePair(",", MIN_VERSION, minVersion, true, w);
			Util.writePair(",", PERCENT_BITMAP, percentBitmap, true, w);
			Util.writePair(",", TRIGGER, trigger, true, w);

			Util.writeKey(",", RULE, w);
			w.write(": ");

			//Rule printRule = asParsed ? rule : new Rule.Unparsed(trigger);
			rule.toJSON(w);

			if (subGroup != null)
				subGroup.toJSON(w);

			w.write("}");
		}
		public void toText(Writer w) throws IOException
		{
			w.write(RULE_HEADER + description + "\n");
			w.write(FEATURE_HEADER + feature + "\n");
			w.write(FEATURE_ID_HEADER + featureID + "\n");
			w.write(MINVERSION_HEADER + minVersion + "\n");
			w.write(PERCENT_HEADER + percentBitmap + "\n");
			w.write(TRIGGER_HEADER + trigger + "\n");
	
			if (subGroup != null)
				subGroup.toText(w);

			w.write(RULE_END + "\n");
		}
	}
	//--------------------------------------------------------
	public static class Group
	{
		public enum Type{ REGULAR, MUTUAL_EXCLUSION }
		Type type;
		String description;
		ArrayList<FeatureData> ruleList = new ArrayList<FeatureData>();

		// JSON strings
		final static String GROUP_TYPE = "groupType";
		final static String GROUP_RULES = "groupRules";

		// text file strings
		final static String HEADER1 = "StartGroup: ";
		final static String HEADER2 = "GroupType: ";
		final static String GROUP_END = "EndGroup";

		public Group(String description, Type type)
		{
			this.description = description; this.type = type;
		}
		public void addFeature(FeatureData rf)
		{
			ruleList.add(rf);
		}

		Group(JSONObject obj) throws JSONException, EngineException, PercentileException
		{
			description = obj.getString(DESCRIBE);
			String str = obj.getString(GROUP_TYPE);
			type = Type.valueOf(str);

			JSONArray arr = obj.getJSONArray(GROUP_RULES);
			for (int i = 0; i < arr.length(); ++i)
			{
				JSONObject jobj = arr.getJSONObject(i);
				FeatureData rf = new FeatureData(jobj);
				addFeature(rf);
			}
		}

		void toJSON(Writer w) throws IOException
		{
			Util.writePair("{", DESCRIBE, description, true, w);
			Util.writePair(",", GROUP_TYPE, type.toString(), true, w);
			Util.writeKey(",", GROUP_RULES, w);
			w.write(": [ ");

			for (int i = 0; i < ruleList.size(); ++i)
			{
				if (i > 0)
					w.write(",\n");
				ruleList.get(i).toJSON(w);
			}
			w.write("]");
			w.write("}\n");
		}
		public void toText(Writer w) throws IOException
		{
			w.write(HEADER1 + description + "\n");
			w.write(HEADER2 + type.toString() + "\n");
			for (int i = 0; i < ruleList.size(); ++i)
			{
				ruleList.get(i).toText(w);
			}
			w.write(GROUP_END + "\n");
		}
	}
	//--------------------------------------------------------
	class UserProfile
	{
		Bindings binding = null; // used by JavaScript rules
		JSONObject json = null;  // used by all other rules
		String appVersion;
		int userRandomNumber;

		UserProfile(JSONObject json, String appVersion, int userRandomNumber)
		{
			this.json = json;
			this.appVersion = appVersion;
			this.userRandomNumber = userRandomNumber;
		}

		// for native rules, jsonString is converted to JSONObject
		// for JavaScript rules, jsonString is converted to a JavaScript object inside a Binding
		UserProfile(String jsonString, String appVersion, int userRandomNumber) throws JSONException
		{
			switch (flavour)
			{
			case NATIVE_RULES:     json = new JSONObject(jsonString);  break;
			case JAVASCRIPT_RULES: binding = Rule.JavaScript.createUserObject(jsonString);  break;
			}
			this.appVersion = appVersion;
			this.userRandomNumber = userRandomNumber;
		}
	}
	//--------------------------------------------------------

	public enum Flavour { NATIVE_RULES, JAVASCRIPT_RULES; }

	Flavour flavour;
	String version;
	String description;
	ArrayList<Group> groupList = new ArrayList<Group>();

	// JSON strings
	final static String VERSION = "version";
	final static String DESCRIPTION = "description";
	final static String FLAVOUR = "flavour";
	final static String GROUPS = "groups";

	// text file strings
	final static String HEADER1 = "EngineVersion: ";
	final static String HEADER2 = "Description: ";
	final static String HEADER3 = "Flavour: ";

	/* boolean saveParsedRules = false;
	public void saveRulesInParsedFolrm(boolean b)
	{
		saveParsedRules = b;
	}*/

	public RuleEngine(String description, Flavour flavour)
	{
		this.version = "1.0";
		this.description = description;
		this.flavour = flavour;
	}
	public RuleEngine(String description, Flavour flavour, String version)
	{
		this.version = version;
		this.description = description;
		this.flavour = flavour;
	}
	public RuleEngine(JSONObject obj) throws JSONException, EngineException, PercentileException
	{
		version = obj.getString(VERSION);
		description = obj.getString(DESCRIPTION);
		flavour = Flavour.valueOf(obj.getString(FLAVOUR));
		JSONArray arr = obj.getJSONArray(GROUPS);

		for (int i = 0; i < arr.length(); ++i)
		{
			JSONObject jobj = arr.getJSONObject(i);
			Group group = new Group(jobj);
			addGroup(group);
		}
	}

	public void addGroup(Group group)
	{
		groupList.add(group);
	}

	public TreeSet<String> calculateFeatures(String jsonString, String appVersion, int userRandomNumber) throws JSONException, PercentileException
	{		
		return doCalculate(new UserProfile(jsonString, appVersion, userRandomNumber));
	}
	public TreeSet<String> calculateFeatures(JSONObject jsonObject, String appVersion, int userRandomNumber) throws JSONException, PercentileException
	{
		return doCalculate(new UserProfile(jsonObject, appVersion, userRandomNumber));
	}

	private TreeSet<String> doCalculate(UserProfile profile) throws JSONException, PercentileException
	{
		TreeSet<String> out = new TreeSet<String>();

		// for (Group group : groupList) // for() is not thread safe
		for (int i = 0; i < groupList.size(); ++i)
		{
			Group group = groupList.get(i);
			doGroup(group, profile, out);
		}
		return out;
	}
	private void doGroup(Group group, UserProfile profile, TreeSet<String> out) throws JSONException, PercentileException
	{
		for (int i = 0; i < group.ruleList.size(); ++i)
		{
			FeatureData rf = group.ruleList.get(i);

			// check non-rule constraints
			boolean proceed = true;
			if (!rf.minVersion.isEmpty())
				proceed = rf.minVersion.compareTo(profile.appVersion) <= 0;

			if (proceed && rf.percentile != null)
				proceed = rf.percentile.isAccepted(profile.userRandomNumber);
			if (!proceed)
				continue;

			// check rule constraints
			Result r = rf.rule.evaluate(profile);

			if (r.getType() == Result.Type.ERROR)
			{
				System.out.println(r.getError());
				continue; 
			}

			if (r.getBool() == true)
			{
				out.add(rf.featureID);

				if (rf.subGroup != null)
					doGroup(rf.subGroup, profile, out);

				if (group.type == Group.Type.MUTUAL_EXCLUSION)
					return;
			}
		}
	}

	public void toJSON(Writer w) throws IOException
	{
		Util.writePair("{", VERSION, version, true, w);
		Util.writePair(",", DESCRIPTION, description, true, w);
		Util.writePair(",", FLAVOUR, flavour.toString(), true, w);
		Util.writeKey(",", GROUPS, w);
		w.write(": [\n");

		for (int i = 0; i < groupList.size(); ++i)
		{
			if (i > 0)
				w.write(",\n");
			groupList.get(i).toJSON(w);
		}
		w.write("]");
		w.write("}\n");
	}
	public void toText(Writer w) throws IOException
	{
		w.write(HEADER1 + version + "\n");
		w.write(HEADER2 + description + "\n");
		w.write(HEADER3 + flavour.toString() + "\n");
		for (int i = 0; i < groupList.size(); ++i)
		{
			Group group = groupList.get(i);
			group.toText(w);
		}
	}
	//------------------load from text file-----------------------------------------
	// TOOO: perhaps change from text to XML for easier viewing of hierarchy
	public RuleEngine(String filepath) throws IOException, EngineException
	{
		Path path = Paths.get(filepath);
		List<String> items = Files.readAllLines(path, Charset.forName("UTF-8"));

		items = cleanLines(items);
		Lines lines = new Lines(items);

		String head1 = lines.nextLine();
		String head2 = lines.nextLine();
		String head3 = lines.nextLine();

		version = trimPrefix(head1, RuleEngine.HEADER1);
		description = trimPrefix(head2, RuleEngine.HEADER2);
		String flav = trimPrefix(head3, RuleEngine.HEADER3);
		flavour = RuleEngine.Flavour.valueOf(flav);

		RuleEngine.Group group;
		while ((group = getGroup(lines, null)) != null)
		{
			this.addGroup(group);
		}
	}

	Group getGroup(Lines lines, String header) throws EngineException
	{
		if (header == null)
			header = lines.nextLine();
		if (header == null)
			return null;

		String description = trimPrefix(header, RuleEngine.Group.HEADER1);

		String str = lines.nextLine();
		String s = trimPrefix(str, RuleEngine.Group.HEADER2);
		RuleEngine.Group.Type type = RuleEngine.Group.Type.valueOf(s); // need to verify it's not null

		RuleEngine.Group group = new RuleEngine.Group(description, type);

		RuleEngine.FeatureData rf;
		while ((rf = getRule(lines)) != null)
		{
			group.addFeature(rf);
		}
		return group;
	}

	FeatureData getRule(Lines lines) throws EngineException
	{
		String header1 = lines.nextLine();
		if (header1 == null)
			throw new EngineException("missing rules in group");

		if (header1.startsWith(RuleEngine.Group.GROUP_END))
			return null;

		String header2 = lines.nextLine();
		String header3 = lines.nextLine();
		String header4 = lines.nextLine();

		String name = trimPrefix(header1, RuleEngine.FeatureData.RULE_HEADER);
		String feature = trimPrefix(header2, RuleEngine.FeatureData.FEATURE_HEADER);
		String featureID = trimPrefix(header3, RuleEngine.FeatureData.FEATURE_ID_HEADER);
		String minVersion = trimPrefix(header3, RuleEngine.FeatureData.MINVERSION_HEADER);
		String bitmap = trimPrefix(header3, RuleEngine.FeatureData.PERCENT_HEADER);
		String trigger = trimPrefix(header4, RuleEngine.FeatureData.TRIGGER_HEADER);
	
		RuleEngine.FeatureData rf;
		try 
		{
			rf = new RuleEngine.FeatureData(name, feature, featureID, minVersion, bitmap, trigger, flavour);
		}
		catch (Exception e)
		{
			throw new EngineException("Error parsing rule '" + trigger + "' : " + e.toString());
		}

		String next = lines.nextLine();
		if (next == null)
			throw new EngineException("missing rule ending");

		if (next.startsWith(RuleEngine.Group.HEADER1)) // optional subgroup under the feature
		{
			RuleEngine.Group group = getGroup(lines, next);
			rf.setSubgroup(group);
			next = lines.nextLine();
		}

		trimPrefix(next, RuleEngine.FeatureData.RULE_END); // verify rule ending
		return rf;
	}
	static String trimPrefix(String string, String prefix) throws EngineException
	{
		if (string == null || !string.startsWith(prefix))
			throw new EngineException("expected prefix " + prefix + ", found " + string); // found null

		return string.substring(prefix.length()).trim();
	}
	static ArrayList<String> cleanLines(List<String> lines)
	{
		ArrayList<String> out = new ArrayList<String>();
		for (String line : lines)
		{
			line = line.trim();
			if (line.isEmpty() || line.startsWith(COMMENT))
				continue;

			out.add(line);
		}
		return out;
	}

	//-----------------------------------------------------------
	public static void main(String[] args)
	{
		try {

			String folder = "C:\\Users\\yigald\\Desktop\\Weather\\";
			String docpath = folder + "jsondoc.txt";
			String engine_json = folder + "engine.txt";
			String engine_text = folder + "engine_print.txt";
			String feature_list = folder + "initFeaturesDb.txt";

			boolean from_json = false;
			boolean from_text = false;
			boolean from_feature_list = false;
			boolean javascript = true;

			RuleEngine engine;
			RuleEngine.Flavour flavour = javascript ? RuleEngine.Flavour.JAVASCRIPT_RULES :  RuleEngine.Flavour.NATIVE_RULES;

			if (from_feature_list)
			{
				JSONObject obj = readJson(feature_list);
				engine = SeasonMapper.loadEngineFromFeatureList(obj, flavour);

				FileWriter out = new FileWriter(engine_json);
				engine.toJSON(out);
				out.close();

				FileWriter out2 = new FileWriter(engine_text);
				engine.toText(out2);
				out2.close();
			}
			else if (from_json)
			{
				JSONObject obj = readJson(engine_json);
				engine = new RuleEngine(obj);

				FileWriter out2 = new FileWriter(engine_text);
				engine.toText(out2);
				out2.close();
			}
			else if (from_text)
			{
				engine = new RuleEngine(engine_text);
				FileWriter out = new FileWriter(engine_json);
				engine.toJSON(out);
				out.close();
			}
			else
			{
				Rule theRule;
				
				if (javascript)
				{
					//theRule = new Rule.JavaScript("$.tenants[0].tenantName == \"tenant3Nodes\"");
					//theRule = new Rule.JavaScript("Object.keys($).length >= 4");
					theRule = new Rule.JavaScript("$.containerIDRange.start > 4 && $.containerIDRange.start < 100 && $.containerIDRange.start < 200");
					//theRule = new Rule.JavaScript("$.blabla.start > 4");
					//theRule = new Rule.JavaScript("$.a == \"b\"");
				}
				else
				{
//					Rule rule1 = new Rule.Operation(new Rule.Field("containerIDRange/start", Result.Type.INT), 
					Rule rule1 = new Rule.Operation(new Rule.ShortField("containerIDRange.start"), 
						Rule.Operation.Type.GT, 
						new Rule.Int(4));

//					Rule rule2 = new Rule.Operation(new Rule.ShortField("availableContainers"), 
//						Rule.Operation.Type.CONTAINS, 
//						new Rule.Str("3010"));

					Rule rule2 = new Rule.Operation(new Rule.FieldCounter("availableContainers"), 
						Rule.Operation.Type.GT, 
						new Rule.Int(1));

					theRule = new Rule.And(rule1, rule2);
				}

				String bitmap = "";
				String minversion = "";

				RuleEngine.Group group = new RuleEngine.Group("my group", RuleEngine.Group.Type.REGULAR);
				group.addFeature(new FeatureData("my rule", "my feature", "my id", minversion, bitmap, theRule));

				engine = new RuleEngine("my engine", flavour);
				engine.addGroup(group);

				FileWriter out = new FileWriter(engine_json);
				engine.toJSON(out);
				out.close();

				FileWriter out2 = new FileWriter(engine_text);
				engine.toText(out2);
				out2.close();
			}

			String doc = readString(docpath);
			String appVersion = "";
			int userNum = 50;
			TreeSet<String> found = engine.calculateFeatures(doc, appVersion, userNum);
			System.out.println(found);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	static String readString(String filepath) throws Exception
	{
		Path path = Paths.get(filepath);
		byte[] data = Files.readAllBytes(path);
		return new String(data, "UTF-8");
	}
	static JSONObject readJson(String filepath) throws Exception
	{
		String str = readString(filepath);
		return new JSONObject(str);
	}
}

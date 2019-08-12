package tests.restapi.testdriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.admin.BaseAirlockItem;
import com.ibm.airlock.admin.Utilities;
import com.ibm.airlock.admin.serialize.S3DataSerializer;
import com.ibm.airlock.admin.streams.AddStreamsToSchema;
import com.ibm.airlock.admin.BaseAirlockItem.Type;
import com.ibm.airlock.engine.AirlockVersionComparator;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.FeatureAttributes;
import com.ibm.airlock.engine.Percentile;
import com.ibm.airlock.engine.ScriptInvoker;
import com.ibm.airlock.engine.StreamsScriptInvoker;
import com.ibm.airlock.engine.VerifyRule;
import com.ibm.airlock.engine.VerifyStream;
import com.ibm.airlock.engine.Version;
import com.ibm.airlock.engine.Visit;
import com.ibm.airlock.engine_dev.ClientEngine;
import com.ibm.airlock.engine_dev.Experiments.BranchInfo;
import com.ibm.airlock.engine_dev.Experiments;

import tests.com.ibm.qautils.RestClientUtils;


public class TestDriver
{
	static final String COMMENT = "#";
	static final String PARM_SEPARATOR = "\\s*;\\s*";
	static final String ITEM_SEPARATOR = "\\s*,\\s*";
	static final String LATEST = "latest";
	static final String NEW = "new";
	static final String BAD_LINE = "invalid instruction: ";
	static final String PROPERTIES_FILE = "TestDriver.properties";
	static final String ACTION_LOG = "log_";

	//--------------------------------------------
	Config config;
	Translate trans;
	File workingFolder;
	FileWriter actionLog;
	ArrayList<Instruction> instructions;
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss"); // not multiThreaded
	TestSuite testSuite = new TestSuite();
	//--------------------------------------------

	enum Action
	{
		CLEANUP,

		NEW_PRODUCT,
		NEW_SEASON,

		RESTORE_PRODUCT,
		RESTORE_SEASON,

// initialize a season in this order: user groups, strings, translations, utilities, input schema, features
		ADD_USER_GROUPS,
		ADD_STRINGS,
		ADD_TRANSLATIONS, // this will also replace existing translations
		ADD_UTILITIES,
		ADD_SCHEMA,
		ADD_FEATURES,

		IMPORT_FEATURE,
		EXPORT_FEATURE,
		COPY_FEATURE,

		CALCULATE_FEATURES,
		CALCULATE_RUNTIME,
		CALCULATE_BRANCH,
		DO_EXPERIMENT,
		PREPARE_EXPERIMENT,
		VERIFY_FEATURES,
		PRINT_ANALYTICS,
		UPDATE_ANALYTICS,
		RUN_STREAMS,

		UPDATE_FEATURE_TREE, // for changing a feature or configuration tree (nodes are matched by their name)
		ADD_ONE_FEATURE, // for adding one feature or configuration under a parent (feature children are added)
		UPDATE_UTILITY,
		UPGRADE_UTILITIES,
		UPDATE_STRINGS, // will overlay existing strings and add new ones
		GET_INPUT_SAMPLE,
		COMPARE_SAMPLES,
		FIND_PARENT,

		PRINT_PRECENTAGE, // print percent lists
		//USER_ROLLOUT, // check RollOut on/off for a specific user
		//VALIDATE_PERCENTAGE, // verify if a percentage tree is valid. optionally compare trees
		GENERATE_RANDOM_MAP,

		GET_LOCALES,
		ADD_LOCALE,
		NEW_STRINGS, // in Smartling format
		STRING_STATUS,
		TRANSLATION_SUMMARY,
		STRINGS_IN_USE,
		OVERRIDE_TRANSLATE,
		PREPARE_TRANSLATIONS,
		GET_TRANSLATIONS,
		SET_STRING_STATUS,
		GET_STRING_BY_STATUS,
		SET_TRANSLATION_LOG,
		GET_TRANSLATION_LOG,
		WAKEUP_TRANSLATIONS,

		SLEEP,
		SET_STAGE,
		DELETE_PRODUCT,
		DELETE_SEASON,
		DELETE_UTILITY,
		DELETE_FEATURE,
		DELETE_STRING,
		
		IMPORT_SEASON,
		CLONE_SEASON,
		CLONE_EXPERIMENTS,
		MERGE_SCHEMA,

		TEST_SUITE,
		TEST_START,
		TEST_END
		};

	static class Instruction
	{
		int line;
		boolean cleanup;
		String source;
		Action action;
		String[] parameters;
		Instruction(int line, String source, boolean cleanup, Action action, String[] parameters) {
			this.line = line; this.source = source; this.cleanup = cleanup; this.action = action; this.parameters = parameters;
		}
	}

	static class Config
	{
		String product;
		String productId;
		String seasonId;
		String branch;
		String url;
		String translationsUrl, testUrl, analyticsUrl;
		String stage;
		String sessionToken;
		String utilityId;
		String featureId;
		JSONObject testResults;
		String oktaUrl, oktaApp;
		String ssoProperties;
		String user, password;
		int sleepMs = 0;
		Environment env = new Environment(); // for future use
		Version version;
	}

	public static class Error
	{
		public int line;
		public String source, error;

		Error(int line, String source, String error) {
			this.line = line; this.source = source; this.error = error;
		}
		public String toString() {
			return "Error in line " + line + ", action: " + source + "\n" + error;
		}
	}
	public static class Result
	{
		public ArrayList<Error> errors = new ArrayList<Error>();
		public String productId, seasonId;

		public void AddError(Error e)
		{
			System.out.println(e);
			errors.add(e);
		}
		public String toString()
		{
			if (errors.isEmpty())
				return "OK";

			StringBuilder b = new StringBuilder();
			for (Error e : errors)
			{
				b.append(e.toString());
				b.append("\n");
			}
			return b.toString();
		}
	}

	@SuppressWarnings("serial")
	static class TrimProperies extends Properties
	{
		public String getProperty(String key)
		{
			String out = super.getProperty(key);
			return (out == null) ? null : out.trim();
		}
		public String getProperty(String key, String defaultValue)
		{
			String out = super.getProperty(key, defaultValue);
			return (out == null) ? null : out.trim();
		}
	}
	static class CalclateParms
	{
		String locale, minAppVer, randParm, groupsData, contextPath, outputPath, fallbackPath;
		boolean asExperiment, withRuntime;
		String asBranch;
	}
	static class TestSuite
	{
		String suitePath = null;
		JSONArray suite = null;
		JSONObject test = null;
		void startSuite(String path)
		{
			suitePath = path;
			suite = new JSONArray();
			test = null;
		}
		void startTest(String testName) throws Exception
		{
			if (suite == null)
				return;

			test = new JSONObject();
			test.put("testName", testName);
		}
		void endTest() throws Exception
		{
			if (suite == null || test == null)
				return;

			suite.add(test);

			// overlay existing output, appending new test to it
			String str = suite.write(true);
			Utilities.writeString(str, suitePath);
			test = null;
		}
		void addTestParms(CalclateParms parms, String stage) throws Exception
		{
			if (test == null)
				return;

			test.put("locale", parms.locale);
			test.put("minAppVer", parms.minAppVer);
			test.put("randomMap", parms.randParm);
			test.put("usergroups", parms.groupsData);
			test.put("context", parms.contextPath);
			test.put("output", parms.outputPath);
			if (parms.fallbackPath != null)
				test.put("fallback", parms.fallbackPath);
			test.put("stage", stage);
		}
		void addTestAnalytics(String analytics, String summary) throws Exception
		{
			if (test == null)
				return;

			test.put("analytics", analytics);
			if (summary != null)
				test.put("analyticSummary", summary);
		}
		void addTestStreams(String streamFile, String streamEventFile, String streamFunctions,
				String streamResults, String streamInContext, String streamOutContext) throws Exception
		{
			if (test == null)
				return;

			test.put("streamFile", streamFile);
			test.put("streamEventFile", streamEventFile);
			test.put("streamFunctions", streamFunctions);
			test.put("streamResults", streamResults);
			test.put("streamInContext", streamInContext);
			test.put("streamOutContext", streamOutContext);
		}
		void addTestConfig(Config config, String s3_url) throws Exception
		{
			if (suite == null || test == null)
				return;

			test.put("productId", config.productId);
			test.put("seasonId", config.seasonId);
			test.put("url", config.url);
			test.put("s3_url", s3_url);
		}
	}
	//--------------------------------------------

	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("usage: TestDriver TestDriver-instructions-ini-file");
			return;
		}

		TestDriver t = new TestDriver();
		Result res = t.runTest(args[0]);
		System.out.println(res.errors.isEmpty() ? "finished ok" : "finished");
	}

	public Result runTest(String instructionPath)
	{
		Result out = new Result();

		try {
			System.out.println("instructions file: " + instructionPath);
			workingFolder = new File(instructionPath).getParentFile();

			loadConfigFile();
			parseInstructions(Utilities.readString(instructionPath));
			initActionLog();
		}
		catch (Exception e)
		{
			out.AddError(new Error(0, "(SETUP)", e.toString()));
			return out;
		}

		if (instructions.isEmpty())
		{
			out.AddError(new Error(0, "(SETUP)", "no instructions found"));
			return out;
		}

		for (Instruction instruction : instructions)
		{
			// on error, skip non-cleanup instructions
			if (out.errors.size() > 0 && !instruction.cleanup)
				continue;

			System.out.println("instruction: " + instruction.source);
			try {
				runInstruction(instruction);
			}
			catch (Exception e)
			{
				out.AddError(new Error(instruction.line, instruction.source, e.toString()));
			}
		}

		try { actionLog.close(); }
		catch (Exception e)	{}

		out.productId = config.productId;
		out.seasonId = config.seasonId;
		return out;
	}

	void runInstruction(Instruction instruction) throws Exception
	{
		actionLog.write(dateFormat.format(new Date()));
		actionLog.write(" ");
		actionLog.write(instruction.source);
		actionLog.write("\n");

		switch (instruction.action)
		{
		case SET_STAGE:           setStage(instruction); break;
		case NEW_PRODUCT:         newProduct(instruction); break;
		case NEW_SEASON:          newSeason(instruction); break;

		case RESTORE_PRODUCT:     restoreProduct(instruction); break;
		case RESTORE_SEASON:      restoreSeason(instruction); break;

		case ADD_USER_GROUPS:     addUserGroups(instruction); break;
		case ADD_STRINGS:         trans.addStrings(instruction); break;
		case ADD_TRANSLATIONS:    trans.addTranslations(instruction); break;
		case ADD_UTILITIES:       addUtilities(instruction); break;
		case ADD_SCHEMA:          addSchema(instruction); break;
		case ADD_FEATURES:        addFeatures(instruction); break;

		case IMPORT_FEATURE:      importFeature(instruction); break;
		case EXPORT_FEATURE:      exportFeature(instruction); break;
		case COPY_FEATURE:        copyFeature(instruction); break;

		case PREPARE_EXPERIMENT:  prepareExperiment(instruction); break;
		case DO_EXPERIMENT:       calculateFeatures(instruction, true, false); break;
		case CALCULATE_FEATURES:  calculateFeatures(instruction, false, false); break;
		case CALCULATE_RUNTIME:   calculateFeatures(instruction, false, true); break;
		case CALCULATE_BRANCH:    calculateBranch(instruction); break;
		case RUN_STREAMS:         runStreams(instruction); break;

		case VERIFY_FEATURES:     verifyFeatures(instruction); break;
		case PRINT_ANALYTICS:     printAnalytics(instruction); break;
		case UPDATE_ANALYTICS:    updateAnalytics(instruction); break;

		case UPDATE_FEATURE_TREE: updateFeatureTree(instruction); break;
		case ADD_ONE_FEATURE:     addOneFeature(instruction); break;
		case UPDATE_UTILITY:      updateUtility(instruction); break;
		case UPGRADE_UTILITIES:   upgradeUtilities(instruction); break;
		case UPDATE_STRINGS:      trans.updateStrings(instruction); break;
		case GET_INPUT_SAMPLE:    getInputSample(instruction); break;
		case COMPARE_SAMPLES:     compareSamples(instruction); break;
		case FIND_PARENT:         findParent(instruction); break;

		case PRINT_PRECENTAGE:    printPercentage(instruction); break;
		case GENERATE_RANDOM_MAP: generateRandomMap(instruction); break;

		case GET_LOCALES:         trans.getLocales(instruction); break;
		case ADD_LOCALE:          trans.addLocale(instruction); break;
		case NEW_STRINGS:         trans.newStrings(instruction); break;
		case STRING_STATUS:       trans.stringStatus(instruction); break;
		case TRANSLATION_SUMMARY: trans.translationSummary(instruction); break;
		case STRINGS_IN_USE:      trans.stringsInUse(instruction); break;
		case OVERRIDE_TRANSLATE:  trans.overrideTranslate(instruction); break;
		case PREPARE_TRANSLATIONS:trans.prepareTranslations(instruction); break;
		case GET_TRANSLATIONS:    trans.getTranslations(instruction); break;
		case SET_STRING_STATUS:   trans.setStringStatus(instruction); break;
		case GET_STRING_BY_STATUS:trans.getStringByStatus(instruction); break;
		case SET_TRANSLATION_LOG: trans.setTranslationLog(instruction); break;
		case GET_TRANSLATION_LOG: trans.getTranslationLog(instruction); break;
		case WAKEUP_TRANSLATIONS: trans.wakeupTranslations(instruction); break;

		case SLEEP:               doSleep(instruction); break;
		case DELETE_PRODUCT:      deleteProduct(instruction); break;
		case DELETE_SEASON:       deleteSeason(instruction); break;
		case DELETE_UTILITY:      deleteUtility(instruction); break;
		case DELETE_FEATURE:      deleteFeature(instruction); break;
		case DELETE_STRING:       deleteString(instruction); break;

		case IMPORT_SEASON:       importSeason(instruction); break;
		case CLONE_SEASON:        cloneSeason(instruction); break;
		case CLONE_EXPERIMENTS:   cloneExperiments(instruction); break;
		case MERGE_SCHEMA:        mergeSchema(instruction); break;


		case TEST_SUITE:          suiteStart(instruction); break;
		case TEST_START:          testStart(instruction); break;
		case TEST_END:            testEnd(instruction); break;

		case CLEANUP: // not used on its own
		default:
			throw new Exception(instruction.action.toString() + " not implemented");
		}
	}

	void loadConfigFile() throws Exception
	{
		config = new Config(); // reset from previous test
		Properties properties = new TrimProperies();

		File path = new File(workingFolder, PROPERTIES_FILE);
		System.out.println("properties file: " + path.getAbsolutePath());

		properties.load(new InputStreamReader(new FileInputStream(path), "UTF-8"));
		System.out.println(properties.toString());
		System.out.println();

		// url = http://airlock-dev3-adminapi.eu-west-1.elasticbeanstalk.com/airlock/api
		String url = properties.getProperty("url");
		if (url == null)
			throw new Exception("url missing in config file");

		if (!url.endsWith("/"))
			url = url + "/";

		config.url = url + "admin/";
		config.testUrl = url +  "test/";
		config.translationsUrl = url +  "translations/";
		config.analyticsUrl = url +  "analytics/";

		config.product = properties.getProperty("product");
		config.productId = properties.getProperty("productId");
		config.seasonId = properties.getProperty("seasonId");

		config.productId = checkUUID(config.productId, "productId");
		config.seasonId = checkUUID(config.seasonId, "seasonId");

		config.stage = properties.getProperty("stage", "DEVELOPMENT");
		config.branch = properties.getProperty("branch", "MASTER");
		String seasonVersion = properties.getProperty("seasonVersion", "2.5");
		config.env.setServerVersion(seasonVersion);
		config.version = Version.find(seasonVersion);

		String sleepMs = properties.getProperty("sleepMs", "0");
		config.sleepMs = Integer.parseInt(sleepMs);

		config.sessionToken = properties.getProperty("sessionToken"); // can be null for unsecured servers

		config.oktaUrl = properties.getProperty("oktaUrl");
		config.oktaApp = properties.getProperty("oktaApp");
		config.user = properties.getProperty("user");
		config.password = properties.getProperty("password");
		config.ssoProperties = properties.getProperty("ssoProperties");
		oktaSignin();
		ssoSignin();

		trans = new Translate(this);
	}

	void initActionLog() throws IOException
	{
		String name = ACTION_LOG + dateFormat.format(new Date()) + ".txt";
		File file = new File(workingFolder, name);
		actionLog = new FileWriter(file);
	}

	void oktaSignin() throws Exception
	{
		if (config.oktaUrl == null || config.oktaApp == null || config.user == null || config.password == null)
			return;

		SessionStarter st = new SessionStarter(config.url, config.oktaUrl, config.oktaApp);
		config.sessionToken = st.getJWT(config.user, config.password);
		System.out.println("Airlock JWT:" + config.sessionToken);
	}

	void ssoSignin() throws Exception
	{
		if (config.ssoProperties == null || config.user == null || config.password == null)
			return;

		ConnectToSso sso = new ConnectToSso(config.ssoProperties);
		String ssoJwt = sso.getJWT(config.user, config.password);
		System.out.println("SSO JWT:" + ssoJwt);

		String url = sso.getSsoUrl(config.url, config.stage);
		RestClientUtils.RestCallResults results = RestClientUtils.sendGet(url, ssoJwt);
		if (results.code != 200)
			throw new Exception(results.message);

		config.sessionToken =  results.message; 
		System.out.println("Airlock JWT: " + config.sessionToken);
	}
	void parseInstructions(String instructionBody) throws Exception
	{
		instructions = new ArrayList<Instruction>();
		String[] lines = instructionBody.split("\n");

		for (int i = 1; i <= lines.length; ++i)
		{
			String line = lines[i-1].trim();
			if (line.isEmpty() || line.startsWith(COMMENT))
				continue;

			String[] items = line.split(PARM_SEPARATOR);
			if (items.length < 1)
				throw new Exception("invalid instruction on line " + i + ": " + line);

			boolean cleanup = false;
			Action action = Action.valueOf(items[0]);

			if (action == Action.CLEANUP)
			{
				if (items.length < 2)
					throw new Exception("invalid instruction on line " + i + ": " + line);
				action = Action.valueOf(items[1]);
				cleanup = true;
			}
			if (action == null)
				throw new Exception("invalid action on line " + i + ": " + items[0]);

			int skip = cleanup ? 2 : 1;
			String[] parms = new String[items.length - skip];
			for (int j = skip; j < items.length; ++j) {
				parms[j-skip] = items[j];
			}

			Instruction inst = new Instruction(i, line, cleanup, action, parms);
			instructions.add(inst);
		}
	}

	void setStage(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("stage was not provided");
		
		String stage = instruction.parameters[0];
		if (stage.equals("DEVELOPMENT") || stage.equals("PRODUCTION"))
			config.stage = stage;
		else
			throw new Exception("invalid stage " + stage);
	}
	void newProduct(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1 && instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires a product name and an optional Smartling ID ");

		config.product = instruction.parameters[0];
		JSONObject product = new JSONObject();
		product.put("name", config.product);
		product.put("codeIdentifier", config.product);
		if (instruction.parameters.length == 2)
			product.put("smartlingProjectId", instruction.parameters[1]);

		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(config.url+"/products", product.toString(), config.sessionToken);
		config.productId = parseResultId(res.message, config.product);
		System.out.println("created product " + config.product + ", productId = " + config.productId);
	}
	void newSeason(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("minVersion was not provided");
		if (config.productId == null)
			throw new Exception("product ID was not defined");

		String seasonMinVer = instruction.parameters[0];

		JSONObject season = new JSONObject();
		season.put("minVersion", seasonMinVer);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(config.url+"/products/" + config.productId + "/seasons/", season.toString(), config.sessionToken);

		config.seasonId = parseResultId(res.message, "new season");
		System.out.println("under product " + config.product + ": created season " + config.seasonId);
	}
	// add product with given id
	void restoreProduct(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2 && instruction.parameters.length != 3)
			throw new Exception(instruction.action.toString() + " requires parameters ( productId ; productName ; [ optional SmartlingID ] )");

		String id = instruction.parameters[0];
		if (!isUUID(id))
			throw new Exception("invalid id " + id);

		config.product = instruction.parameters[1];

		JSONObject product = new JSONObject();
		product.put("name", config.product);
		product.put("codeIdentifier", config.product);
		if (instruction.parameters.length == 3)
			product.put("smartlingProjectId", instruction.parameters[2]);

		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(config.url+"/products/" + id, product.toString(), config.sessionToken);
		config.productId = parseResultId(res.message, config.product);
		System.out.println("created product " + config.product + ", productId = " + config.productId);
	}
	// add season with given id
	void restoreSeason(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( seasonId ; minVersion )");

		if (config.productId == null)
			throw new Exception("product ID was not defined");

		String id = instruction.parameters[0];
		if (!isUUID(id))
			throw new Exception("invalid id " + id);

		String seasonMinVer = instruction.parameters[1];

		JSONObject season = new JSONObject();
		season.put("minVersion", seasonMinVer);
		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(config.url + "/products/" + config.productId + "/seasons/" + id, season.toString(), config.sessionToken);

		config.seasonId = parseResultId(res.message, "new season");
		System.out.println("under product " + config.product + ": created season " + config.seasonId);
	}

	void addUserGroups(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception(BAD_LINE + instruction.source);

		String[] items = instruction.parameters[0].split(ITEM_SEPARATOR);
		TreeSet<String> groups = new TreeSet<String>(Arrays.asList(items));
		addUserGroups(groups);
	}
	void addUserGroups(Set<String> groups) throws Exception
	{
		// add existing groups to new group set
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "/usergroups", config.sessionToken);
		JSONObject json = parseError(res.message, "groups");

		JSONArray currentGroups = json.getJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
		for (int i = 0; i < currentGroups.length(); ++i) {
			groups.add(currentGroups.getString(i));
		}

		if (groups.size() == currentGroups.length())
		{
			//System.out.println("new user groups not found, skipping group addition");
			return;
		}

		System.out.println("updating user groups: " + groups.toString());

		json.remove(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
		json.put(Constants.JSON_FIELD_INTERNAL_USER_GROUPS, new JSONArray(groups));

		try {
			RestClientUtils.RestCallResults res2 = RestClientUtils.sendPut( config.url + "/usergroups", json.toString(), config.sessionToken);
			parseError(res2.message, null);
		} catch (Exception e) {
			throw new Exception("Could not update user groups: " + e.getMessage());
		}
	}
	void handleMissingUserGroups(JSONObject obj) throws Exception
	{
		ScanGroups scan = new ScanGroups();
		scan.run(obj, null);
		addUserGroups(scan.groups);
	}

	void getInputSample(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 3)
			throw new Exception(instruction.action.toString() + " requires 3 parameters ( stage ; minAppVer ; output_file_path )");

		String parms = "?stage=" + instruction.parameters[0];
		if (config.version.i >= Version.v2_5.i)
			parms += "&minappversion=" + instruction.parameters[1];

		String path = composePath(instruction.parameters[2]);

		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "products/seasons/" + config.seasonId + "/inputSample" + parms, config.sessionToken);
		JSONObject sample = parseError(res.message, "input sample");
		String str = sample.write(true);
		System.out.println(str);
		Utilities.writeString(str, path);
	}

	void compareSamples(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( path_to_sample_1 ; path_to_sample_2 )");

		String path1 = composePath(instruction.parameters[0]);
		String path2 = composePath(instruction.parameters[1]);

		JSONObject sample1 = Utilities.readJson(path1);
		JSONObject sample2 = Utilities.readJson(path2);

		TreeSet<String> keys1 = getSampleKeys(sample1);
		TreeSet<String> keys2 = getSampleKeys(sample2);

		TreeSet<String> diff1 = getDiff(keys1, keys2);
		TreeSet<String> diff2 = getDiff(keys2, keys1);

		if (diff1.isEmpty() && diff2.isEmpty())
			System.out.println("keys are identical");
		else
		{
			if (diff1.size() > 0)
				System.out.println("keys in sample 1 but not in sample 2: " + diff1.toString());
			if (diff2.size() > 0)
				System.out.println("keys in sample 2 but not in sample 1: " + diff2.toString());
		}
	}
	void findParent(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("child name or id was not provided");

		String id = instruction.parameters[0];
		JSONObject features = getFeatures();

		// id may be a name, convert it
		JSONObject childJson = findName(features, id);
		String childId = getId(childJson);
		String childName = getName(childJson);

		JSONObject root = features.getJSONObject("root");
		Map<String,String> map = scanParents(root, true);

		String parentId = map.get(childId);
		String parentName = "<unknown>";

		if (parentId != null)
		{
			JSONObject parentJson = findName(features, parentId);
			parentName = getName(parentJson);
		}
		System.out.println("parent is: " + parentId + " (" + parentName + ")");
		System.out.println("child is:  " + childId + " (" + childName + ")");
	}

	void printPercentage(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("output csv was not provided");
		String path = composePath(instruction.parameters[0]);

		JSONObject features = getFeatures().getJSONObject(Constants.JSON_FIELD_ROOT);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		pw.println("Feature,Percent,EffectivePercent,Items");
		PercentPrinter printer = new PercentPrinter(pw);
		printer.run(features, null);
		pw.close();

		String out = sw.toString();
		System.out.println(out);
		Utilities.writeString(out, path);
	}

	class PercentPrinter extends Visit
	{
		PrintWriter pw;
		PercentPrinter(PrintWriter pw) {
			this.pw = pw;
		}

		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			BaseAirlockItem.Type type = getNodeType(obj);
			if (type != Type.FEATURE && type != Type.CONFIGURATION_RULE)
				return null;

			double percent = obj.optDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100.0);
			String b64 = obj.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP, "").trim();
			if (b64.isEmpty())
			{
				pw.println("\"" + getName(obj) + "\"," +  percent);
			}
			else
			{
				Percentile p = new Percentile(b64);
				pw.println("\"" + getName(obj) + "\"," +  percent + "," + p.getEffectivePercentage() + ",[" + p.printBits() + "]");
			}

			return null;
		}
	}

	// generate a random map for each feature (to simulate the device application data).
	// output should be kept in a separate folder and reused in calculateFeatures
	void generateRandomMap(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2 && instruction.parameters.length != 3)
			throw new Exception(instruction.action.toString() + " requires 2 or 3 parameters ( legacy-random-number; output-random-map [ ; input-random-map ] )");

		String srand = instruction.parameters[0];
		String outpath = composePath(instruction.parameters[1]);
		String inpath = (instruction.parameters.length == 3) ? composePath(instruction.parameters[2]) : null;

		int legacy = -1;
		try {
			legacy = Integer.parseInt(srand);
		}
		catch (Exception e)
		{}
		if (legacy < 0 || legacy > 99)
			throw new Exception("invalid legacy random number " + srand);

		Map<String,Integer> inMap;
		if (inpath == null)
			inMap = new TreeMap<String,Integer>();
		else
		{
			JSONObject json = Utilities.readJson(inpath);
			inMap =  ClientEngine.Json2Thresholds(json);
		}

		//JSONObject features = getFeatures();
		JSONObject features = getMasterAndBranches();
		Map<String,Integer> outMap = ClientEngine.calculateFeatureRandomNumbers(features, legacy, inMap);
		
		JSONObject json = ClientEngine.thresholds2Json(outMap);
		
		String str = json.write(true);
		System.out.println("Calculated randoms:");
		System.out.println(str);
		Utilities.writeString(str, outpath);
	}

	void addFeatures(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("features file was not provided");

		String path = composePath(instruction.parameters[0]);
		JSONObject features = Utilities.readJson(path);
		JSONObject root = features.getJSONObject("root");
		handleMissingUserGroups(root);
		JSONArray jsonArray = root.getJSONArray("features");
		copyFeatures(jsonArray, "ROOT");
	}

	void importFeature(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( parent name or id ; features_file_path )");

		String id = instruction.parameters[0];
		String path = composePath(instruction.parameters[1]);

		// id may be a name, convert it
		JSONObject features = getFeatures();
		JSONObject parentJson = findName(features, id);
		String parentId = getId(parentJson);
		String parentName = getName(parentJson);

		String body = Utilities.readString(path);
		// do we need this: handleMissingUserGroups(body's json);

		System.out.println("importing features to parent " + parentId + " (" + parentName + ")");

		try {
			RestClientUtils.RestCallResults res =
				RestClientUtils.sendPut(config.url +"/features/import/" + parentId, body, config.sessionToken);
			if (res.code != 200)
				parseError(res.message, null);
		}
		catch (Exception e) {
			throw new Exception("Failed to import features:\n" + e.getLocalizedMessage());
		}
	}
	void exportFeature(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( feature name or id ; output_file_path )");

		String id = instruction.parameters[0];
		String path = composePath(instruction.parameters[1]);

		// id may be a name, convert it
		JSONObject features = getFeatures();
		JSONObject json = findName(features, id);
		String fId = getId(json);
		String fName = getName(json);
		System.out.println("exporting feature " + fId + " (" + fName + ")");

		try {
			RestClientUtils.RestCallResults res =
				RestClientUtils.sendGet(config.url +"/products/seasons/features/" + fId, config.sessionToken);
			JSONObject obj = parseError(res.message, null);
			String body = obj.toString(true);
			Utilities.writeString(body, path);
		}
		catch (Exception e) {
			throw new Exception("Failed to export feature:\n" + e.getLocalizedMessage());
		}
	}

	void copyFeature(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( feature id ; new parent id )");

		String featureId = instruction.parameters[0];
		String parentId = instruction.parameters[1];
		if (!isUUID(featureId))
			throw new Exception("invalid feature id " + featureId);
		if (!isUUID(parentId))
			throw new Exception("invalid parent id " + parentId);

		try {
			RestClientUtils.RestCallResults res =
				RestClientUtils.sendPut(config.url +"/features/copy/" + featureId + "/" + parentId, "", config.sessionToken);
			if (res.code != 200)
				parseError(res.message, null);
		}
		catch (Exception e) {
			throw new Exception("Failed to copy feature:\n" + e.getLocalizedMessage());
		}
	}

	void addUtilities(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 3)
			throw new Exception(instruction.action.toString() + " requires 3 parameters ( stage ;  minAppVer; utility_file_path )");

		String parms = "?stage=" + instruction.parameters[0];

		if (config.version.i < Version.v2_5.i)
			parms += "&minAppVerion=" + instruction.parameters[1]; // V2 API uses minAppVerion instead of minAppVersion

		String path = composePath(instruction.parameters[2]);
		String utilityBody = Utilities.readString(path);

		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPost(config.url +"/products/seasons/" + config.seasonId + "/utilities" + parms, utilityBody, config.sessionToken);
			config.utilityId = parseResultId(res.message, null);
		}
		catch (Exception e) {
			throw new Exception("Failed to create utilities:\n" + e.getLocalizedMessage());
		}
	}
	void updateUtility(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 4)
			throw new Exception(instruction.action.toString() + " requires 4 parameters ( utilityId ; stage ; minAppVer; utility_file_path )");

		String id = instruction.parameters[0];
		if (id.equals(LATEST))
		{
			id = config.utilityId;
			if (id == null)
				throw new Exception("latest utilityId has not been initialized");
			System.out.println("updating latest utility " + id);
		}

		JSONObject funcJson = getUtilities();
		String modDate = getUtilityModDate(funcJson, id);

		String parms = "?stage=" + instruction.parameters[1] + "&lastmodified=" + modDate;
		if (config.version.i < Version.v2_5.i)
			parms += "&minAppVerion=" + instruction.parameters[2]; // V2 API uses minAppVerion instead of minAppVersion

		String path = composePath(instruction.parameters[3]);
		String utilityBody = Utilities.readString(path);

		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(config.url + "/products/seasons/utilities/" + id + parms, utilityBody, config.sessionToken);
			config.utilityId = parseResultId(res.message, null);
		}
		catch (Exception e) {
			throw new Exception("Failed to update utility:\n" + e.getLocalizedMessage());
		}
	}
	void deleteUtility(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing utility ID");

		String id = instruction.parameters[0];
		if (id.equals(LATEST))
		{
			id = config.utilityId;
			if (id == null)
				throw new Exception("latest utilityId has not been initialized");
			System.out.println("deleting latest utility " + id);
		}

		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(config.url + "/products/seasons/utilities/" + id, config.sessionToken);
		if (res.code != 200)
			parseError(res.message, "delete utility");
	}
	void upgradeUtilities(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing products.json file");

		String path = composePath(instruction.parameters[0]);
		JSONObject json = Utilities.readJson(path);

		ArrayList<String> items = new ArrayList<String>();
		JSONArray products = json.getJSONArray("products");
		for (int i = 0; i < products.size(); ++i)
		{
			JSONObject product = products.getJSONObject(i);
			JSONArray seasons  = product.getJSONArray("seasons");
			for (int j = 0; j < seasons.size(); ++j)
			{
				JSONObject season = seasons.getJSONObject(j);
				String id = season.getString("uniqueId");
				items.add(id);
			}
		}

		for (String seasonId : items)
		{
			System.out.println("season " + seasonId);
			RestClientUtils.RestCallResults res = RestClientUtils.sendPut(config.url+ "/products/seasons/" + seasonId + "/upgrade/utilities", "", config.sessionToken);
			if (res.code != 200)
				parseError(res.message, "update utilities");
		}
	}

	void addSchema(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("inputSchema file was not provided");

		String path = composePath(instruction.parameters[0]);
		String schemaBody = Utilities.readString(path);

		try {
			RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url+ "/products/seasons/" +config. seasonId + "/inputschema", config.sessionToken);
			JSONObject schemaJson = parseError(res.message, null);

			// accept both wrapped and unwrapped schemas in input
			JSONObject json = new JSONObject(schemaBody);
			if (json.containsKey("inputSchema"))
				json = json.getJSONObject("inputSchema");

			schemaJson.put("inputSchema", json); // replace current schema

			res = RestClientUtils.sendPut(config.url + "/products/seasons/" + config.seasonId + "/inputschema", schemaJson.toString(), config.sessionToken);
			parseError(res.message, null);
		}
		catch (Exception e) {
			throw new Exception("Failed to create inputSchema:\n" + e.getLocalizedMessage());
		}
	}

	void calculateFeatures(Instruction instruction, boolean asExperiment, boolean withRuntime) throws Exception
	{
		if (instruction.parameters.length != 6 && instruction.parameters.length != 7)
			throw new Exception(instruction.action.toString() + " requires 6 or 7 parameters ( locale; min-app-ver; feature-random-file; comma-separated-groups; context-file; output-file ; [fallbacks-file] )");

		boolean ver25 = config.version.i < Version.v3_0.i;
		if (ver25 && asExperiment)
			throw new Exception("can't perform experiments on 2.5 servers");

		CalclateParms parms = new CalclateParms();
		parms.locale = instruction.parameters[0];
		parms.minAppVer = instruction.parameters[1];
		parms.randParm = instruction.parameters[2];
		parms.groupsData = instruction.parameters[3];
		parms.contextPath = instruction.parameters[4];
		parms.outputPath = instruction.parameters[5];
		parms.fallbackPath = (instruction.parameters.length == 7) ? instruction.parameters[6] : null;
		parms.asExperiment = asExperiment;
		parms.withRuntime = withRuntime;
		parms.asBranch = null;
		doCalculate(parms);
	}
	void calculateBranch(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 7 && instruction.parameters.length != 8)
			throw new Exception(instruction.action.toString() + " requires 7 or 8 parameters ( branch-name; locale; min-app-ver; feature-random-file; comma-separated-groups; context-file; output-file ; [fallbacks-file] )");

		boolean ver25 = config.version.i < Version.v3_0.i;
		if (ver25)
			throw new Exception("can't calculate branch on 2.5 servers");

		CalclateParms parms = new CalclateParms();
		parms.asBranch = instruction.parameters[0];
		parms.locale = instruction.parameters[1];
		parms.minAppVer = instruction.parameters[2];
		parms.randParm = instruction.parameters[3];
		parms.groupsData = instruction.parameters[4];
		parms.contextPath = instruction.parameters[5];
		parms.outputPath = instruction.parameters[6];
		parms.fallbackPath = (instruction.parameters.length == 8) ? instruction.parameters[7] : null;
		parms.asExperiment = false;
		parms.withRuntime = false;
		doCalculate(parms);
	}

	void doCalculate(CalclateParms parms) throws Exception
	{
		testSuite.addTestParms(parms, config.stage);
		fixPaths(parms);

		// the random parameter can be a number to use on all features, or the path to a JSON map containing a different random for each feature
		Map<String,Integer> userFeatureRand = getRandomMap(parms.randParm);

		// optional fallbacks from a local file
		JSONObject fallbacks = (parms.fallbackPath == null) ? null :  Utilities.readJson(parms.fallbackPath);

		// user context
		String context = Utilities.readString(parms.contextPath);

		// user groups
		String[] items = parms.groupsData.split(ITEM_SEPARATOR);
		List<String> profileGroups = Arrays.asList(items);

		// functions
		JSONObject funcJson = getUtilities();
		String functions = filterUtilities(funcJson, parms.minAppVer);

		// translations
		String translations = getTranslations(parms.locale);

		// feature tree
		JSONObject features = 
				(parms.asBranch != null) ? getBranchFeatures(parms.asBranch) :
				(parms.asExperiment || parms.withRuntime) ? getMasterAndBranches() :
					getMasterAndBranches(); // getFeatures(); always get runtime, for a PROD version without DEV features

		// calculate result for this user context
		JSONObject contextObject = VerifyRule.createContextObject(context, functions, translations);

		// create output with generic "mx" feature name. removed since it plays havoc with addMissingDefaults() below
		// config.env.put(Environment.PRINT_GOLD, "true");
		ScriptInvoker.setDebugMode(true);
		if (parms.asExperiment)
			config.env.startTrace();

		config.testResults = parms.asExperiment ? Experiments.calculateExperimentsAndFeatures(features, contextObject, profileGroups, fallbacks, parms.minAppVer, userFeatureRand, config.env) :
				ClientEngine.calculateFeatures(features, contextObject, profileGroups, fallbacks, parms.minAppVer, userFeatureRand, config.env);

		// config.env.remove(Environment.PRINT_GOLD);
		ScriptInvoker.setDebugMode(false);
		if (parms.asExperiment)
		{
			String trace = new JSONArray(config.env.endTrace()).write(true);
			System.out.println("Experiment trace:\n" + trace);
		}

		JSONObject defaults = getAirlockDefaults(); // TODO: allow loading defaults from external file
//		JSONObject defaults = Utilities.readJson("C:/client/gil/AirlockDefaults.json");
//		Utilities.writeString(features.write(true), "C:/client/gil/outFeatures.json");
//		Utilities.writeString(config.testResults.write(true), "C:/client/gil/results1.json");

		addMissingDefaults(config.testResults, defaults);
//		Utilities.writeString(config.testResults.write(true), "C:/client/gil/results2.json");

//		JSONObject notifications = getNotifications();
//		JSONObject jj = ClientEngine.calculateNotifications(notifications, contextObject, profileGroups, parms.minAppVer, userFeatureRand, config.env);

		String str = config.testResults.write(true);
		System.out.println("Calculated features:");
		System.out.println(str);
		Utilities.writeString(str, parms.outputPath);
	}

	// the random parameter can be a number to use on all features, or the path to a JSON map containing a different random for each feature
	Map<String,Integer> getRandomMap(String randParm) throws Exception
	{
		Map<String,Integer> userFeatureRand = null;
		int random = -1;

		try {
			random = Integer.parseInt(randParm);
		}
		catch (Exception e)
		{}

		boolean legacy = (config.version.i <= Version.v2_1.i);
		if (random >= 0)
		{
			if (!legacy)
				throw new Exception("random user number cannot be used in versions above 2.1");

			userFeatureRand = new TreeMap<String,Integer>();
			userFeatureRand.put(ClientEngine.LegacyRandomKey, random);
		}
		else
		{
			if (legacy)
				throw new Exception("random user number must be used in version 2.1");

			JSONObject json = Utilities.readJson(composePath(randParm));
			userFeatureRand = ClientEngine.Json2Thresholds(json);
		}
		return userFeatureRand;
	}
	String getTranslations(String locale) throws Exception
	{
		String params = (config.version.i < Version.v2_1.i) ? "" : "?stage=" + config.stage;
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.translationsUrl + config.seasonId + "/translate/" + locale + params, config.sessionToken);
		JSONObject json = parseError(res.message, "translations");
		return json.getJSONObject("strings").toString();
	}

	void prepareExperiment(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 6)
			throw new Exception(instruction.action.toString() + " requires 6 parameters ( locale; min-app-ver; feature-random-file; comma-separated-groups; context-file; output-file )");

		CalclateParms parms = new CalclateParms();
		parms.locale = instruction.parameters[0];
		parms.minAppVer = instruction.parameters[1];
		parms.randParm = instruction.parameters[2];
		parms.groupsData = instruction.parameters[3];
		parms.contextPath = instruction.parameters[4];
		parms.outputPath = instruction.parameters[5];
		parms.fallbackPath = null;
		testSuite.addTestParms(parms, config.stage);
		fixPaths(parms);

		Map<String,Integer> userFeatureRand = getRandomMap(parms.randParm);

		// user context
		String context = Utilities.readString(parms.contextPath);

		// user groups
		String[] items = parms.groupsData.split(ITEM_SEPARATOR);
		List<String> profileGroups = Arrays.asList(items);

		// functions
		JSONObject funcJson = getUtilities();
		String functions = filterUtilities(funcJson, parms.minAppVer);

		// translations
		String translations = getTranslations(parms.locale);
		JSONObject contextObject = VerifyRule.createContextObject(context, functions, translations);

		JSONObject json = getMasterAndBranches();

		PrintWriter pw = new PrintWriter(parms.outputPath);
		pw.println("Original master and branches:");
		pw.println(json.write(true));

		config.env.startTrace();
		List<BranchInfo> list = Experiments.calculateExperiments(json, contextObject, profileGroups, parms.minAppVer, userFeatureRand, config.env);
		String trace =  new JSONArray(config.env.endTrace()).write(true);

		System.out.println("Experiment trace:\n" + trace);
		System.out.println("Experiments found:\n" + list);

		pw.println("===========================================");
		pw.println("Experiment trace:\n" + trace);
		pw.println("===========================================");
		pw.println("Experiments found:\n" + list);
		pw.println("===========================================");

		JSONObject runtimeTree = Experiments.calculateRuntimeTree(json, list);
		
		pw.println("Runtime tree:");
		pw.println(runtimeTree.write(true));
		pw.close();
	}
	void runStreams(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 6)
			throw new Exception(instruction.action.toString() + " requires 6 parameters ( streams-json; events-json; stream-functions; results-json; input-context-json; output-context-json )");

		String streamsFile = instruction.parameters[0];
		String eventsFile = instruction.parameters[1];
		String functionFile = instruction.parameters[2];
		String resultFile = instruction.parameters[3];
		String inContextFile = instruction.parameters[4];
		String outContextFile = instruction.parameters[5];
		testSuite.addTestStreams(streamsFile, eventsFile, functionFile, resultFile, inContextFile, outContextFile);

		JSONObject tmp = Utilities.readJson(composePath(streamsFile));
		JSONArray streams = tmp.getJSONArray("streams");
		tmp = Utilities.readJson(composePath(eventsFile));
		JSONArray globals = tmp.getJSONArray("events");
		String functions = Utilities.readString(composePath(functionFile));

		StreamsScriptInvoker invoker = new StreamsScriptInvoker(null, functions);
		JSONObject results = new JSONObject(); // output from all streams

		for (int i = 0; i < streams.length(); ++i)
		{
			JSONObject stream = streams.getJSONObject(i);
			String streamName = stream.getString("name");
			streamName = replace(streamName); // stream name in schema does not contains spaces & dots
			String filter = stream.getString("filter");
			String processor = stream.getString("processor");
			JSONObject result = VerifyStream.evaluateFilterAndProcessor(invoker, filter, processor, globals);
			results.put(streamName, result);
		}
		Utilities.writeString(results.write(true), composePath(resultFile));

		JSONObject context = Utilities.readJson(composePath(inContextFile));
		tmp = context.getJSONObject("context");
		tmp.put("streams", results);
		Utilities.writeString(context.write(true), composePath(outContextFile));
	}
	void verifyFeatures(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("gold file was not provided");

		if (config.testResults == null)
			throw new Exception("calculateFeatures was not performed");

		String path = composePath(instruction.parameters[0]);
		JSONObject gold = Utilities.readJson(path);

		// do we need smart compare?
		String calcString = config.testResults.write(true);
		String goldString = gold.write(true);
		if (calcString.equals(goldString))
			System.out.println("verify features results are OK");
		else
		{
			System.out.println("verify features results differ:");
			System.out.println(">>> GOLD RESULT:\n" + goldString);
			System.out.println(">> CALCULATED RESULT:\n" + calcString);
			throw new Exception("gold file does not match calculation");
		}
	}
	// this version creates the gold JSON first, then merges the missing default features into it
	void printAnalytics(Instruction instruction) throws Exception
 	{
		if (instruction.parameters.length != 3 && instruction.parameters.length != 4)
			throw new Exception(instruction.action.toString() + " requires 3 or 4 parameters  (context-file-path ; calculated-results-path ; analytics-file-path [; summary-file-path ] )");

		String context = composePath(instruction.parameters[0]);
		String results = composePath(instruction.parameters[1]);
		String output = instruction.parameters[2];
		String summary = (instruction.parameters.length == 4) ? instruction.parameters[3] : null;
		testSuite.addTestAnalytics(output, summary);

		JSONObject cntx = Utilities.readJson(context);
		JSONObject calculation = Utilities.readJson(results);
		TreeSet<String> contextWhitelist = getContextWhitelist(calculation);
		JSONObject contextAnalytics = FeatureAttributes.getWhitelistData(cntx, contextWhitelist);

		ReportAnalytics report = new ReportAnalytics(contextAnalytics, calculation);
		JSONObject croot = calculation.getJSONObject("root");
		report.run(croot, null);

/*		disabled, since the missing default features are already merged into the results
		JSONObject defaults =  getAirlockDefaults();
		JSONObject droot = defaults.getJSONObject("root");
		JSONObject features = report.out.getJSONObject(Constants.JSON_FEATURE_FIELD_FEATURES);
		addMissingToAnalytics(features, droot);
*/

		String str = report.out.write(true);
		System.out.println(str);
		Utilities.writeString(str, composePath(output));

		if (summary != null)
		{
			JSONObject report2 = summarize(report.out);
			Utilities.writeString(report2.write(true), composePath(summary));
		}
 	}

/*	void addMissingToAnalytics(JSONObject features, JSONObject defaults) throws Exception
	{
		if (getNodeType(defaults) == BaseAirlockItem.Type.FEATURE)
		{
			String name = getName(defaults);
			if (!features.containsKey(name))
			{
				JSONObject data = new JSONObject();
				data.put("featureIsReported", false);
				data.put("isON", false);
				features.put(name, data);
			}
		}
		JSONArray children = defaults.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
		if (children != null)
			for (int i = 0; i < children.size(); ++i)
				addMissingToAnalytics(features, children.getJSONObject(i));
	}
*/
	void addMissingDefaults(JSONObject calculation, JSONObject defaults) throws Exception
	{
		JSONObject croot = calculation.getJSONObject("root");
//		Map<String,JSONObject> calcMap = new HashMap<String, JSONObject>();
//		mapItem(croot, calcMap);
		ResultMap res = new ResultMap();
		res.run(croot, null);
		
		JSONObject droot = defaults.getJSONObject("root");
		addOneDefault("", droot, res.calcMap);
	}

	class ResultMap extends Visit
	{
		Map<String,JSONObject> calcMap = new HashMap<String, JSONObject>();

//		ResultMap() { visiting = FEATURES | CONFIGURATIONS; }

		@Override
		protected Object visit(JSONObject obj, Object state) throws Exception {
			String key = obj.optString(Constants.JSON_FIELD_NAME, "ROOT"); // already contains extended name
			//String key = getName(obj); // extended name
			calcMap.put(key, obj);
			return null;
		}
	}
/*	
	void mapItem(JSONObject in, Map<String, JSONObject> out) throws JSONException
	{
		String key = in.optString(Constants.JSON_FIELD_NAME, "ROOT");
		out.put(key, in);

		JSONArray array =  in.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
		if (array != null)
			for (int i = 0; i < array.length(); ++i)
			{
				mapItem(array.getJSONObject(i), out);
			}

		array =  in.optJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		if (array != null)
			for (int i = 0; i < array.length(); ++i)
			{
				mapItem(array.getJSONObject(i), out);
			}
		
		array =  in.optJSONArray(Constants.JSON_FEATURE_FIELD_ORDERING_RULES);
		if (array != null)
			for (int i = 0; i < array.length(); ++i)
			{
				mapItem(array.getJSONObject(i), out);
			}
	}
*/
	void addOneDefault(String parent, JSONObject defaultObj, Map<String,JSONObject> calcMap) throws Exception
	{
		String name = getName(defaultObj);
		if (!calcMap.containsKey(name))
		{
			JSONObject parentNode = calcMap.get(parent);
			if (parentNode != null)
			{
				JSONObject clone = (JSONObject) doClone(defaultObj, calcMap);
				JSONArray parentChildren = parentNode.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
				parentChildren.add(clone);
			}
		}

		JSONArray children = defaultObj.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
		if (children != null)
			for (int i = 0; i < children.size(); ++i)
				addOneDefault(name, children.getJSONObject(i), calcMap);
	}

	public Object doClone(Object obj, Map<String,JSONObject> calcMap) throws Exception
	{
		if (obj instanceof JSONObject)
		{
			JSONObject in = (JSONObject) obj;

			String name = getName(in);
			if (calcMap.containsKey(name))
				return null; // don't add children who are already in due to branch merge

			JSONObject out = new JSONObject(in); // shallow clone
			calcMap.put(name, out);

			if (getNodeType(out) == BaseAirlockItem.Type.FEATURE)
			{
				boolean isOn = in.getBoolean("defaultIfAirlockSystemIsDown");
				out.put("isON", isOn); // copy it from default
				out.put("resultTrace", "copied from defaults");
				String config = in.optString("defaultConfiguration");
				out.put("featureAttributes", config == null ? "{}" : config);
				// merge namespace.name
				out.put(Constants.JSON_FIELD_NAME, name);
				out.remove(Constants.JSON_FEATURE_FIELD_NAMESPACE);
			}

			JSONArray children = out.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
			if (children != null)
			{
				JSONArray newChildren = (JSONArray) doClone(children, calcMap);
				out.put(Constants.JSON_FEATURE_FIELD_FEATURES, newChildren);
			}
			return out;
		}

		if (obj instanceof JSONArray)
		{
			JSONArray in = (JSONArray) obj;
			JSONArray out = new JSONArray();
			for (int i = 0; i < in.length(); ++i)
			{
				Object item = doClone(in.get(i), calcMap);
				if (item != null)
					out.add(item);
			}
			return out;
		}

		// the rest are immmutable. null objects will be removed from parent array, but in this context it's ok
		return obj;
	}
	void copyOptional(JSONObject in, JSONObject out, String key) throws JSONException
	{
		Object obj = in.opt(key);
		if (obj != null)
			out.put(key, obj);
	}
	class ReportAnalytics extends Visit
	{
		JSONObject out = new JSONObject();
		JSONObject features = new JSONObject();

		ReportAnalytics(JSONObject contextAnalytics, JSONObject calculation) throws Exception
		{
			out.put(Constants.JSON_FEATURE_FIELD_FEATURES, features);
			out.put(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS, contextAnalytics);
			copyOptional(calculation, out, Constants.JSON_FIELD_EXPERIMENT_LIST);
			copyOptional(calculation, out, Constants.JSON_FIELD_EXPERIMENT);
			copyOptional(calculation, out, Constants.JSON_FIELD_VARIANT);
		}

		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			 // mx nodes will get their 'sent' status from a parent node
			// feature nodes will get their 'sent' status from "featureIsReported" attribute
			Boolean sent = (state == null) ? false : (Boolean) state;

			Type type = getNodeType(obj);
			JSONArray reordered = obj.optJSONArray(Constants.JSON_FIELD_REORDERED_CHILDREN);
			boolean hasReordered = reordered != null && !reordered.isEmpty(); // apppears in mx nodes too

			if (type == Type.FEATURE || hasReordered)
			{
				String name = obj.optString("name");
				if (name == null)
					return state; // skip the root

				if (type == Type.FEATURE)
					sent = obj.optBoolean("featureIsReported", false);

				JSONObject data = new JSONObject();
				if (hasReordered && sent)
					data.put(Constants.JSON_FIELD_REORDERED_CHILDREN, reordered);

				if (type == Type.FEATURE)
				{
					data.put("featureIsReported", sent);
					Boolean isON = obj.optBoolean("isON", false);
					data.put("isON", isON);

					if (isON)
					{
						JSONObject configNames = obj.optJSONObject("reportedConfigurationNames");
						JSONObject configValues = obj.optJSONObject("reportedConfigurationValues");
						JSONObject orderingNames = obj.optJSONObject("reportedOrderingNames");
						data.put("reportedConfigurationNames", configNames);
						data.put("reportedConfigurationValues", configValues);
						if (orderingNames != null)
							data.put("reportedOrderingNames", orderingNames);
					}
					if (sent)
					{
						JSONArray appliedReorderingRules = obj.optJSONArray("appliedReorderingRules");
						if (appliedReorderingRules != null && !appliedReorderingRules.isEmpty())
							data.put("appliedReorderingRules", appliedReorderingRules);
					}
				}

				features.put(name, data);
			}

			return sent; // percolate parent's sent status downwards, (reordering needs it but can't get it from mx nodes)
		}
	}
	
	@SuppressWarnings("unchecked")
	JSONObject summarize(JSONObject report) throws Exception
	{
		JSONObject out = new JSONObject();
		JSONArray array = report.optJSONArray(Constants.JSON_FIELD_EXPERIMENT_LIST);
		if (array != null)
		{
			final String exp = "EXPERIMENT_";
			final String var = "VARIANT_";
			for (int i = 0; i < array.size(); ++i)
			{
				String str = array.getString(i);
				if (str.startsWith(exp))
					out.put("experiment", str.substring(exp.length()));
				else if (str.startsWith(var))
					out.put("variant", str.substring(var.length()));
			}
		}

		JSONObject anal = report.optJSONObject("inputFieldsForAnalytics");
		if (anal != null)
		{
			Set<String> keys = anal.keySet();
			for (String key : keys)
			{
				String context = "context_" +  replace(key);
				out.put(context, anal.get(key));
			}
		}

		JSONObject features = report.optJSONObject("features");
		if (features != null)
		{
			Set<String> keys = features.keySet();
			for (String key : keys)
			{
				String feature = "feature_" +  replace(key);
				JSONObject obj = features.getJSONObject(key);
				boolean isReported = obj.optBoolean("featureIsReported", false);
				if (isReported)
				{
					boolean isOn = obj.optBoolean("isON", false);
					out.put(feature, isOn ? "On" : "Off");
				}

				JSONObject configNames = obj.optJSONObject("reportedConfigurationNames");
				if (configNames != null)
				{
					JSONArray filtered = new JSONArray();
					Set<String> configNameKeys = configNames.keySet();
					for (String configNameKey : configNameKeys)
					{
						boolean accepted = configNames.getBoolean(configNameKey);
						if (accepted)
							filtered.add(configNameKey);
					}
					out.put(feature + "_conf_appliedRules", filtered);
				}
				JSONObject configValues = obj.optJSONObject("reportedConfigurationValues");
				if (configValues != null)
				{
					Set<String> configKeys = configValues.keySet();
					for (String configKey : configKeys)
					{
						String config = feature + "_conf_" + replace(configKey);
						out.put(config, configValues.get(configKey));
					}
				}
				JSONObject orderingNames = obj.optJSONObject("reportedOrderingNames");
				if (orderingNames != null)
				{
					JSONArray filtered = new JSONArray();
					Set<String> configNameKeys = orderingNames.keySet();
					for (String configNameKey : configNameKeys)
					{
						boolean accepted = orderingNames.getBoolean(configNameKey);
						if (accepted)
							filtered.add(configNameKey);
					}
					out.put(feature + "_ordering_rules", filtered);
				}
			}
		}
		return out;
	}

	//------------------------------------------------
	String replace(String in)
	{
		 return in.replace('.', '_').replace(' ',  '_');
	}
	String getS3Path() throws Exception
	{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "/products/seasons/" + config.seasonId + "/defaults", config.sessionToken);
		JSONObject json = parseError(res.message, "season defaults");
		return json.getString(Constants.JSON_FIELD_DEV_STORAGE_PUBLIC_PATH);
	}
	String getQualifiedS3Path() throws Exception
	{
		// note: a double // in the URL will give an authorization error
		return getS3Path() + "seasons/" + config.productId + "/" + config.seasonId;
	}
	JSONObject getS3File(String name) throws Exception
	{
		String url = getQualifiedS3Path() + "/" + name;
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		return parseError(res.message, name);
	}
	JSONObject getMasterAndBranches() throws Exception
	{
		return getS3File("AirlockRuntime" + config.stage + ".json");
	}
	JSONObject getNotifications() throws Exception
	{
		return getS3File("AirlockNotifications" + config.stage + ".json");
	}
	TreeSet<String> getContextWhitelist(JSONObject calculation) throws Exception
	{
		boolean legacy = config.version.i < Version.v3_0.i;
		JSONArray array;
		if (legacy)
		{
			JSONObject json = getMasterAndBranches();
			array = json.optJSONArray(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS);
		}
		else //	get it from the results instead, where it was merged correctly
			array = calculation.optJSONArray(Constants.JSON_FIELD_INPUT_FIELDS_FOR_ANALYTICS);

		TreeSet<String> out = new TreeSet<String>();
		if (array != null)
			for (int i = 0; i < array.length(); ++i)
				out.add(array.getString(i));

		return out;
	}
	JSONObject getAirlockDefaults() throws Exception
	{
		return getS3File(Constants.AIRLOCK_DEFAULTS_FILE_NAME);
	}
	JSONObject getFullAirlockFeatures() throws Exception // can return more than getFeatures()
	{
		return getS3File(Constants.AIRLOCK_FEATURES_FILE_NAME);
	}
	//----------------------------------------------------------------------------------------------------------
	// analytics url
	void updateAnalytics(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( old_analytics_file ; old_features_file )");

		String anal = composePath(instruction.parameters[0]);
		String feat = composePath(instruction.parameters[1]);

		JSONObject oldAnalytics = Utilities.readJson(anal);
		JSONObject oldFeatures = Utilities.readJson(feat);
		JSONObject newAnalytics = getGlobalAnalytics("BASIC");
		JSONObject newFeatures = getFeatures(); // getFullAirlockFeatures() made no difference
		//Utilities.writeString(newFeatures.write(true), "c:/client/josemina/newfeatures.json");

		Map<String,String> id2Name = crossRef(oldFeatures, true);
		Map<String,String> name2Id = crossRef(newFeatures, false);
		
		JSONObject globalDataCollection = oldAnalytics.getJSONObject("globalDataCollection");
		JSONObject analyticsDataCollection = globalDataCollection.getJSONObject("analyticsDataCollection");
		JSONArray featuresAttributesForAnalytics = analyticsDataCollection.getJSONArray("featuresAttributesForAnalytics");

		JSONArray newAttr = new JSONArray();
		for (int i = 0; i < featuresAttributesForAnalytics.size(); ++i)
		{
			JSONObject obj = featuresAttributesForAnalytics.getJSONObject(i);
			String id = obj.getString("id");
			String newId =  replaceId(id, id2Name, name2Id);
			if (newId != null)
			{
				obj.put("id", newId);
				newAttr.add(obj);
			}
		}
		analyticsDataCollection.put("featuresAttributesForAnalytics", newAttr);

		JSONArray newArray = new JSONArray();
		JSONArray featuresAndConfigurationsForAnalytics = analyticsDataCollection.getJSONArray("featuresAndConfigurationsForAnalytics");
		for (int i = 0; i < featuresAndConfigurationsForAnalytics.size(); ++i)
		{
			String id = featuresAndConfigurationsForAnalytics.getString(i);
			String newId =  replaceId(id, id2Name, name2Id);
			if (newId != null)
				newArray.add(newId);
		}
		analyticsDataCollection.put("featuresAndConfigurationsForAnalytics", newArray);

		newAnalytics.put("analyticsDataCollection", analyticsDataCollection);
	//	Utilities.writeString(newAnalytics.write(true), "c:/client/josemina/newanalytics.json");
		setGlobalAnalytics(newAnalytics.toString());
	}

	JSONObject getGlobalAnalytics(String mode) throws Exception
	{
		String url = config.analyticsUrl +"/globalDataCollection/" + config.seasonId + getBranch() + "?mode=" + mode;
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		return parseError(res.message, "getAnalytics");
	}

	public void setGlobalAnalytics(String body) throws Exception
	{
		String url = config.analyticsUrl +"/globalDataCollection/" + config.seasonId  + getBranch();
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(url , body, config.sessionToken);
		if (res.code != 200)
			parseError(res.message, null);
	}

	Map<String,String> crossRef(JSONObject json, boolean id2Name) throws Exception
	{
		CrossRef xref = new CrossRef(id2Name);
		JSONObject root = json.getJSONObject("root");
		xref.run(root, null);
		return xref.map;
	}
	class CrossRef extends Visit
	{
		boolean id2Name;
		TreeMap<String,String> map = new TreeMap<String,String>();

		CrossRef(boolean id2Name) {
			this.id2Name = id2Name;
		}
		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			String id = getId(obj);
			String name = getName(obj);
			if (id2Name)
				map.put(id, name);
			else
				map.put(name, id);
			return null;
		}
	}
	String replaceId(String id, Map<String,String> id2Name, Map<String,String> name2Id)
	{
		String name = id2Name.get(id);
		if (name == null)
		{
			System.out.println("old id " + id + " not found in old features, skipping");
			return null;
		}

		String out = name2Id.get(name);
		if (out == null)
			System.out.println("name '" + name + "' of old id " + id + " not found in new features, skipping");		
		return out;
	}
	//----------------------------------------------------------------------------------------------------------
	// this method updates all input features by their name and ignores the tree hierarchy in the input JSON.
	// (tree structure is assumed to be unchanged and the old structure is maintained)
	void updateFeatureTree(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("json file was not provided");

		String jsonPath = composePath(instruction.parameters[0]);
		JSONObject input = Utilities.readJson(jsonPath);
		handleMissingUserGroups(input);

		JSONObject newTree;
		try {
			newTree = input.getJSONObject(Constants.JSON_FIELD_ROOT);
		}
		catch (Exception e) {
			newTree = input;
		}

		JSONObject oldTree = getFeatures().getJSONObject(Constants.JSON_FIELD_ROOT);
		Map<String,JSONObject> map = indexByName(newTree, false);

		MergeTrees mt = new MergeTrees(map);
		mt.run(oldTree, null);

		updateFeature(getId(oldTree), oldTree.toString());
	}

	class MergeTrees extends Visit
	{
		Map<String,JSONObject> name2Node;

		MergeTrees(Map<String,JSONObject> map) {
			name2Node = map;
		}
		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			BaseAirlockItem.Type type = getNodeType(obj);
			if (type != Type.FEATURE && type != Type.CONFIGURATION_RULE)
				return null;

			String name = getName(obj);
			JSONObject newNode = name2Node.get(name);

			if (newNode == null)
				System.out.println("node " + name + " is missing from input and will not be updated");
			else
			{
				System.out.println("updating node " + name);
				// copy everything except for removed fields from the new node to the old.
				// sub-features and sub-configurations are left untouched.
				removeFields(newNode);
				Utilities.mergeJson(obj, newNode);
			}
			return null;
		}
	}

	// this adds a new feature and its children
	void addOneFeature(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 2)
			throw new Exception(instruction.action.toString() + " requires 2 parameters ( namespace.name or id (for parent feature or configuration) ; json-filepath )");

		String name = instruction.parameters[0];
		String jsonPath = composePath(instruction.parameters[1]);

		JSONObject input = Utilities.readJson(jsonPath);
		handleMissingUserGroups(input);
		removeFields(input);

		JSONObject json = findName(getFeatures(), name);
		System.out.println("updating under " + getName(json));
		String parentID = getId(json);

		JSONArray arr = new JSONArray();
		arr.add(input);
		copyFeatures(arr, parentID);
	}

	void deleteFeature(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("featureId was not provided");

		String id = instruction.parameters[0];
		if (id.equals(LATEST))
		{
			id = config.featureId;
			if (id == null)
				throw new Exception("config.featureId has not been initialized");
			System.out.println("deleting latest feature " + id);
		}
		else // id may be a name, convert it
		{
			JSONObject json = findName(getFeatures(), id);
			id = getId(json);
			System.out.println("deleting feature " + id + " (" + getName(json) + ")");
		}

		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(config.url + "/products/seasons" + getBranch() + "/features/" + id, config.sessionToken);
		if (res.code != 200)
			parseError(res.message, "delete feature");
	}
	void deleteProduct(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 0)
			throw new Exception(instruction.action.toString() + " does not take parameters");

		// throws exception if productId does not exist
		JSONObject product = getProductData();

		// change PRODUCTION features to DEVELOMENT or the delete will fail
		removeProductionFlags(product);

 		try {
 			RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(config.url + "/products/" + config.productId, config.sessionToken);
 			// parseError(res.message, null);
 			if (res.code != 200)
 				throw new Exception("return code : " + res.code);
		} catch (Exception e) {
			throw new Exception("Cannot delete product " +  config.productId + "\n" + e.getMessage()) ;
		}

	}
	void deleteSeason(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing season ID");

		String id = instruction.parameters[0];
		if (id.equals(LATEST))
		{
			id = config.seasonId;
			if (id == null)
				throw new Exception("config.seasonId has not been initialized");
			System.out.println("deleting latest season " + id);
		}
		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(config.url + "seasons/" + id, config.sessionToken);
		if (res.code != 200)
			parseError(res.message, "delete season");
	}
	void deleteString(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing string key");

		String key = instruction.parameters[0];
		String id = getStringId(key);
		if (id == null)
			throw new Exception("String key not found: " + key);

		RestClientUtils.RestCallResults res = RestClientUtils.sendDelete(config.translationsUrl + "seasons/strings/" + id, config.sessionToken);
		if (res.code != 200)
			parseError(res.message, "delete string");
	}
	void doSleep(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing sleep seconds");

		int secs = -1;
		try { secs = Integer.parseInt(instruction.parameters[0]);}
		catch (Exception e) {}
		if (secs <= 0)
			throw new Exception("invalid sleep seconds");

		Thread.sleep(secs * 1000);
	}

	//--------------------------
	void importSeason(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 7)
			throw new Exception(instruction.action.toString() + " requires 7 parameters ( region; bucket; internalPrefix, runtimePrefix ; productId; seasonId;  imports-folder )");
		String region = instruction.parameters[0];
		String bucket = instruction.parameters[1];
		String internalPrefix = instruction.parameters[2];
		String runtimePrefix = instruction.parameters[3];
		String productId = instruction.parameters[4];
		String seasonId = instruction.parameters[5];
		String path = composePath(instruction.parameters[6]);

		File folder = new File(path);
		if (!folder.isDirectory())
			folder.mkdirs();

		String location = "/seasons/" + productId + "/" + seasonId;
		S3DataSerializer ds = S3DataSerializer.generateS3DataSerializer(region, bucket, internalPrefix, runtimePrefix);
		ds.copyFolder(location, folder.getAbsolutePath());
	}

	void cloneSeason(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 6)
			throw new Exception(instruction.action.toString() + " requires 6 parameters ( imports-folder ; oldProductId ; oldProductId/newProductId/'new' ; oldSeasonId ; newSeasonId/'new' ; report-file )");

		String path = composePath(instruction.parameters[0]);
		String productId = instruction.parameters[1];
		String newProductId = instruction.parameters[2]; // new product Id may be the same as the old one
		String oldSeasonId = instruction.parameters[3];
		String newSeaonId = instruction.parameters[4];
		String report = composePath(instruction.parameters[5]);

		if (newProductId.equals("new"))
			newProductId = UUID.randomUUID().toString();
		if (newSeaonId.equals("new"))
			newSeaonId = UUID.randomUUID().toString();

		String oldSeasonFolder = path + "/seasons/" + productId + "/" + oldSeasonId;
		File oldFolder = new File(oldSeasonFolder);
		if (!oldFolder.isDirectory())
			throw new Exception(oldSeasonFolder + " does not exist");

		String newSeasonFolder = path + "/seasons/" + newProductId + "/" + newSeaonId;
		File newFolder = new File(newSeasonFolder);
		if (newFolder.exists())
			throw new Exception(newSeasonFolder + " already exists");
		newFolder.mkdirs();

		String msg = "cloning from product " + productId + " to " + newProductId;
		String msg2 = "cloning season " + oldSeasonId + " to " + newSeaonId;
		PrintWriter pw = new PrintWriter(report);
		pw.println(msg); pw.println(msg2);
		System.out.println(msg); System.out.println(msg2);

		Map<String,String> idMap = new HashMap<String,String>();
		idMap.put(productId, newProductId);
		idMap.put(oldSeasonId, newSeaonId);
		cloneSeasonFolder(oldFolder, newFolder, idMap);

		pw.println("Id Map:");
		for (Map.Entry<String, String> ent : idMap.entrySet())
		{
			pw.println(ent.getKey() + " ---> " + ent.getValue());
		}
		pw.close();
	}

	void cloneSeasonFolder(File oldFolder, File newFolder, Map<String,String> idMap) throws Exception
	{
		for (File child : oldFolder.listFiles())
		{
			System.out.println(child.getAbsolutePath());
			String fileName = child.getName();
			if (child.isDirectory())
			{
				String newFileName = isUUID(fileName) ? getId(idMap, fileName, true) : fileName;
				File newOne = new File(newFolder, newFileName);
				newOne.mkdir();
				cloneSeasonFolder(child, newOne, idMap);
			}
			else
			{
				String str = Utilities.readString(child.getAbsolutePath());
				if (fileName.endsWith(".json"))
				{
					JSONObject json = new JSONObject(str);
					convertIds(idMap, json, true);
					str = json.write(true);
				}
				File newOne = new File(newFolder, fileName);
				Utilities.writeString(str, newOne.getAbsolutePath());
			}
		}
	}

	void convertIds(Map<String,String> idMap, Object in, boolean addMissing) throws JSONException
	{
		if (in instanceof JSONObject)
		{
			JSONObject json = (JSONObject) in;
			@SuppressWarnings("unchecked")
			Set<String> keys = new TreeSet<String>(json.keySet()); // independent copy
			for (String key : keys)
			{
				Object value = json.get(key);
				if (value instanceof String)
				{
					String str = (String) value;
//					boolean b1 = checkId(key);
//					boolean b2 = isUUID(str);
//					if (b1 != b2)
//						throw new JSONException("mismatched id: " + key);

					String newStr = changeId(idMap, str, addMissing);
					if (newStr != null)
						json.put(key, newStr);
				}
				else
					convertIds(idMap, value, addMissing);
			}
		}
		else if (in instanceof JSONArray)
		{
			JSONArray array = (JSONArray) in;
			for (int i = 0; i < array.size(); ++i)
			{
				Object value = array.get(i);
				if (value instanceof String)
				{
					String str = (String) value;
					String newStr = changeId(idMap, str, addMissing);
					if (newStr != null)
						array.set(i, newStr);
				}
				else
					convertIds(idMap, value, addMissing);
			}
		}
	}

	// this also changes mx.<uuid> (mutex name)
	String changeId(Map<String,String> idMap, String str, boolean addMissing)
	{
		final String mx = "mx.";
		String prefix = "";

		if (str.startsWith(mx))
		{
			prefix = mx;
			str = str.substring(mx.length());
		}

		if (isUUID(str) == false)
			return null; // no change

		str = getId(idMap, str, addMissing);
		return (str == null) ? null : prefix + str;
	}
	String getId(Map<String,String> idMap, String oldId, boolean addMissing)
	{
		String newId = idMap.get(oldId);
		if (newId == null)
		{
			if (!addMissing)
				return null;

			newId = UUID.randomUUID().toString();
			idMap.put(oldId, newId);
		}
		return newId;
	}
//	boolean checkId(String key)
//	{
//		// 'id' is only used in AirlockAnalytics.json
//		return key.equalsIgnoreCase("uniqueid") ||  key.equalsIgnoreCase("seasonid") || key.equalsIgnoreCase("productid") ||  key.equalsIgnoreCase("experimentid") || key.equalsIgnoreCase("id");
//	}

	

	void cloneExperiments(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 3)
			throw new Exception(instruction.action.toString() + " requires 3 parameters ( report-of-converted-uuids ; input-experiments-json ;  output-experiments-json)");

		String report = composePath(instruction.parameters[0]);
		String inFile = composePath(instruction.parameters[1]);
		String outFle = composePath(instruction.parameters[2]);

		String reportContent = Utilities.readString(report);
		JSONObject experiment = Utilities.readJson(inFile);
		Map<String,String> idMap = new HashMap<String,String>();

		String lines[] = reportContent.split("\n");
		for (int i = 3; i < lines.length; ++i) // skip 3 header lines
		{
			String line = lines[i];
			String items[] = line.split("\\s+");
			if (items.length != 3 || !items[1].equals("--->"))
				throw new Exception("invalid report line: " + line);
			idMap.put(items[0], items[2]);
		}
		if (idMap.isEmpty())
			throw new Exception("no mapping found in file: " + report);

		convertIds(idMap, experiment, false); // covert uuids found in the map, ignore the others
		String str = experiment.write(true);
		Utilities.writeString(str, outFle);
	}
	//--------------------------
	void suiteStart(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing suite name)");
		String path = composePath(instruction.parameters[0]);
		testSuite.startSuite(path);
	}
	void testStart(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing test name)");
		testSuite.startTest(instruction.parameters[0]);
	}
	void testEnd(Instruction instruction) throws Exception
	{
		String s3_url;
		try { s3_url = getQualifiedS3Path(); }
		catch (Exception e) { s3_url = "unknown"; }

		testSuite.addTestConfig(config, s3_url);
		testSuite.endTest();
	}

	void mergeSchema(Instruction instruction) throws Exception
	{
		if (instruction.parameters.length != 1)
			throw new Exception("missing output path)");
		String path = composePath(instruction.parameters[0]);

		JSONObject streams = getS3File(Constants.AIRLOCK_STREAMS_FILE_NAME);
		JSONObject schema = getS3File(Constants.AIRLOCK_INPUT_SCHEMA_FILE_NAME);
		boolean required = false;
		AddStreamsToSchema.merge(schema, streams, required);
		Utilities.writeString(schema.write(true), path);
	}
//--------------------------
	String getStringId(String key) throws Exception
	{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.translationsUrl + "seasons/" + config.seasonId + "/strings", config.sessionToken);
		JSONObject json = parseError(res.message, "get strings");
		JSONArray allStrings = json.getJSONArray("strings");
		for (int i = 0; i < allStrings.size(); ++i)
		{
			JSONObject single = allStrings.getJSONObject(i);
			if (key.equals(single.getString("key")))
				return getId(single);
		}
		return null;
	}
	void removeProductionFlags(JSONObject product)
	{
		try {
			JSONArray seasons = product.getJSONArray("seasons");
			if (seasons == null)
				return;

			for (int i = 0; i < seasons.size(); ++i)
			{
				JSONObject season = seasons.getJSONObject(i);
				String seasonID = getId(season);
				JSONArray features = getFeaturesBySeason(seasonID);
				replaceProductionFeatures(features);
			}
		}
		catch (Exception e) {
			System.out.println("warning: error when trying to change feature stages from production to development: " + e.getMessage());
			return;
		}
	}

	JSONArray getFeaturesBySeason(String seasonID) throws Exception
	{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "/products/seasons/" + seasonID + getBranch() + "/features", config.sessionToken);
	 	JSONObject jsonResponse = parseError(res.message, null);
	 	JSONObject season = jsonResponse.getJSONObject("root");
	 	return season.getJSONArray("features");	
	}
	void replaceProductionFeatures(JSONArray features) throws Exception
	{
		for (int j = 0; j < features.size(); ++j)
		{
			JSONObject json = features.getJSONObject(j);
			JSONObject feature = getFeature( getId(json) );

			JSONArray configurationRules = new JSONArray();
			if (feature.containsKey("configurationRules"))
				configurationRules = feature.getJSONArray("configurationRules");
			if (configurationRules.size() != 0)
				replaceProductionFeatures(configurationRules);				

			JSONArray subFeatures = new JSONArray();
			if (feature.containsKey("features"))
				subFeatures = feature.getJSONArray("features");				
			if (subFeatures.size() != 0)
				replaceProductionFeatures(subFeatures);

			// change stage to development
			if (feature.has("stage") && feature.getString("stage").equals("PRODUCTION"))
			{
				feature.put("stage", "DEVELOPMENT");
				String featureId = getId(feature);
				updateFeature(featureId, feature.toString());
			}
		}
	}	
	JSONObject getFeatures() throws Exception
	{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "/products/seasons/" + config.seasonId + getBranch() + "/features", config.sessionToken);
		return parseError(res.message, "features");
	}
	JSONObject getBranchFeatures(String branchName) throws Exception
	{
		String branchId = getBranchId(branchName);
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "/products/seasons/" + config.seasonId + "/branches/" + branchId + "/features", config.sessionToken);
		return parseError(res.message, "features");
	}
	String getBranchId(String branchName) throws Exception
	{
		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "/products/seasons/" + config.seasonId + "/branches?mode=BASIC", config.sessionToken);
		JSONObject obj = parseError(res.message, "branches");
		JSONArray array = obj.getJSONArray("branches");
		for (int i = 0; i < array.size(); ++i)
		{
			JSONObject branch = array.getJSONObject(i);
			String name = branch.getString("name");
			if (name.equals(branchName))
				return branch.getString("uniqueId");
		}
		throw new Exception("branch name " + branchName + " does not exist");
	}
	JSONObject getFeature(String featureID) throws Exception
	{
 		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "/products/seasons" + getBranch() + "/features/" + featureID, config.sessionToken);
 		return parseError(res.message, null);
	}
	String updateFeature(String featureID, String featureJson) throws Exception
	{
		RestClientUtils.RestCallResults res = RestClientUtils.sendPut(config.url + "/products/seasons" + getBranch() + "/features/" + featureID, featureJson, config.sessionToken);
		config.featureId = parseResultId(res.message, "feature base");
		return config.featureId;
	}

	JSONObject findName(JSONObject featureTree, String name) throws Exception
	{
		JSONObject features = featureTree.getJSONObject("root");
		Map<String,JSONObject> map = indexByName(features, isUUID(name));
		JSONObject item = map.get(name);

		if (item == null)
			throw new Exception("Feature or configuration not found: " + name);
		return item;
	}
	boolean isUUID(String id)
	{
		try {
			UUID.fromString(id);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	Map<String,JSONObject> indexByName(JSONObject json, boolean useId) throws Exception
	{
		Indexer indexer = new Indexer(useId);
		indexer.run(json, null);
		return indexer.map;
	}
	class Indexer extends Visit
	{
		boolean useId;
		TreeMap<String,JSONObject> map = new TreeMap<String,JSONObject>();

		Indexer(boolean useId) {
			this.useId = useId;
		}
		protected Object visit(JSONObject obj, Object state) throws Exception {
			String name = useId ? getId(obj) : getName(obj);
			map.put(name, obj);
			return null;
		}
	}

	void copyFeatures(JSONArray jsonArray, String parentId) throws Exception
	{
		if (jsonArray == null)
			return;

		for (int i = 0; i < jsonArray.length(); ++i)
		{
			String parent = parentId;
			JSONObject feature = jsonArray.getJSONObject(i);

			// get sub-features/configurations array
			JSONArray subFeatures = feature.optJSONArray("features");
			JSONArray configurationRules = feature.optJSONArray("configurationRules");

			// remove fields that are not allowed for feature creation
			removeFields(feature);

			// create the feature
			try {
				parent = createFeature(feature.toString(), parent);
				copyFeatures(configurationRules, parent);
			}
			catch (Exception e) {
				throw new Exception("Can't create feature " + getName(feature) + ". The reason: " + e.toString());
			}

			// create sub-features with this parent 
			copyFeatures(subFeatures, parent);
		} 
	}
	String createFeature(String featureJson, String parentID) throws Exception
	{
		JSONObject jsonF = new JSONObject(featureJson);
		String featureName = getName(jsonF);

		if (config.sleepMs > 0)
			Thread.sleep(config.sleepMs);

		RestClientUtils.RestCallResults res = RestClientUtils.sendPost(config.url+"/products/seasons/" + config.seasonId + getBranch() + "/features?parent=" + parentID, featureJson, config.sessionToken);
		config.featureId = parseResultId(res.message, featureName);
		return config.featureId;
	}

	 // remove lastModified, seasonId etc from input so they are recalculated
	void removeFields(JSONObject feature)
	{
		feature.remove("uniqueId");
		feature.remove("seasonId");
		feature.remove("rolloutPercentageBitmap");
		feature.remove("lastModified");
		feature.remove("creationDate");
		feature.remove("features");
		feature.remove("configurationRules");
	}
	String getBranch()
	{
		return (config.version.i < Version.v3_0.i) ? "" : "/branches/" + config.branch;
	}
	JSONObject parseError(String result, String itemName) throws Exception
	{
		if (result.isEmpty())
			return new JSONObject();

		JSONObject response;
		try {
			response = new JSONObject(result);
		}
		catch (Exception e) // not a JSON; serious error
		{
			String err = (itemName == null) ? "" : "error in " + itemName + ": ";
			throw new Exception(err + result);
		}

		if (response.containsKey("error"))
		{
			String err = (itemName == null) ? "" : "error in " + itemName + ": ";
			throw new Exception(err + response.getString("error"));
		}
		return response;
	}
	String parseResultId(String result, String itemName) throws Exception
	{
		JSONObject response = parseError(result, itemName);

		if (!response.containsKey(Constants.JSON_FIELD_UNIQUE_ID))
			throw new Exception("error in " + itemName + ": " + "ID is missing in result");

		return getId(response);
	}

	String filterUtilities(JSONObject utilities, String userAppVer) throws Exception
	{
		boolean production = config.stage.equals("PRODUCTION");
		StringBuilder sb = new StringBuilder();

		JSONArray utilsArr = utilities.getJSONArray("utilities");
		for (int i = 0; i < utilsArr.length(); ++i)
		{
			JSONObject utility = utilsArr.getJSONObject(i);

			String stage = utility.getString("stage");
			 // production mode does not look at development functions
			if (production && !stage.equals(config.stage))
				continue;

			if (config.version.i > Version.v1.i && config.version.i < Version.v2_5.i)
			{
				String minAppVersion = utility.optString("minAppVersion");
				if (stage == null || minAppVersion == null)
					continue;

				// ignore functions newer than the test's MinVer
				if (AirlockVersionComparator.compare(minAppVersion, userAppVer) > 0)
					continue;
			}

			String functions = utility.getString("utility");
			sb.append(functions);
			sb.append("\n");
		}
		return sb.toString();
	}

	String getUtilityModDate(JSONObject utilities, String id) throws Exception
	{
		JSONArray utilsArr = utilities.getJSONArray("utilities");
		for (int i = 0; i < utilsArr.length(); ++i)
		{
			JSONObject utility = utilsArr.getJSONObject(i);
			if ( id.equals(getId(utility)) )
				return utility.getString("lastModified");
		}
		throw new Exception("utilityId does not exist: " + id);
	}
	String composePath(String path)
	{
		if (new File(path).isAbsolute())
			return path;

		return new File(workingFolder, path).getAbsolutePath();
	}
	void fixPaths(CalclateParms parms)
	{
		if (parms.contextPath != null)
			parms.contextPath = composePath(parms.contextPath);
		if (parms.fallbackPath != null)
			parms.fallbackPath = composePath(parms.fallbackPath);
		if (parms.outputPath != null)
			parms.outputPath = composePath(parms.outputPath);
		// randParm is fixed elsewhere since it may be a legacy number
	}
	JSONObject getProductData() throws Exception
	{
		if (config.productId == null)
			throw new Exception("productId has not been defined");

 		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(config.url + "/products/" + config.productId, config.sessionToken);
 		JSONObject out = parseError(res.message, "product name");
 		return out;
	}
	JSONObject getUtilities() throws Exception
	{
		String url = config.url + "products/seasons/" + config.seasonId + "/utilities";
//		if (config.serializationNumber >= 3)
//			url += "?stage=" + config.stage;

		RestClientUtils.RestCallResults res = RestClientUtils.sendGet(url, config.sessionToken);
		return parseError(res.message, "utilities");
	}
	TreeSet<String> getSampleKeys(Object sample) throws Exception
	{
		TreeSet<String> out = new TreeSet<String>();
		Utilities.flatten(sample, null, out, null);
		return out;
	}
	TreeSet<String> getDiff(TreeSet<String> keys1, TreeSet<String> keys2)
	{
		TreeSet<String> out = new TreeSet<String>(keys1);
		out.removeAll(keys2);
		return out;
	}

	class ScanGroups extends Visit
	{
		Set<String> groups = new TreeSet<String>();
		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			JSONArray arr = obj.optJSONArray(Constants.JSON_FIELD_INTERNAL_USER_GROUPS);
			if (arr != null)
			{
			    for (int i = 0; i < arr.length(); ++i)
			    	groups.add(arr.getString(i));
			}
			return null;
		}
	}

	// map each child id to its parent id
	Map<String,String> scanParents(JSONObject obj, boolean byId) throws Exception
	{
		MapParents mp = new MapParents(byId);
		mp.run(obj, null);
		return mp.map;
	}
	class MapParents extends Visit
	{
		MapParents(boolean byId) { this.byId = byId; }
		boolean byId;
		Map<String,String> map = new TreeMap<String,String>();

		protected Object visit(JSONObject obj, Object state) throws Exception
		{
			String parent = (String) state; // get parent id from caller
			String childId = byId ? getId(obj) : getName(obj);
			map.put(childId, parent);
			return childId; // pass current id as a parent to the lower nodes
		}
	}

	static BaseAirlockItem.Type getNodeType(JSONObject obj)
	{
		String str = obj.optString(Constants.JSON_FEATURE_FIELD_TYPE, BaseAirlockItem.Type.FEATURE.toString());
		return BaseAirlockItem.Type.valueOf(str); // null on error
	}
	String getName(JSONObject obj) // using root, namespace.name or mx.GUID
	{
		switch (getNodeType(obj))
		{
		case ROOT: return "ROOT";
		case FEATURE: return obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE, "<unknown>") + "." + obj.optString(Constants.JSON_FIELD_NAME, "<unknown>");
		default: return "mx." + obj.optString(Constants.JSON_FIELD_UNIQUE_ID, "<unknown>");
		}
	}
	String getId(JSONObject obj) throws Exception
	{
		return obj.getString(Constants.JSON_FIELD_UNIQUE_ID);
	}
	String checkUUID(String id, String key) throws Exception
	{
		if (id == null)
			throw new Exception(key + " was not provided in configuration file");

		if (id.equals(NEW))
			return null; // will be filled in later

		UUID.fromString(id); // check for validity
		return id;
	}

	//------------------------------------------------------------------------------------------
}

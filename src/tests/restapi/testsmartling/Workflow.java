package tests.restapi.testsmartling;

import java.text.SimpleDateFormat;
//import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.admin.Utilities;

import tests.restapi.testsmartling.SmartlingClient.RestCallResults;
import tests.restapi.testsmartling.StringState.LifeCycle;
import tests.restapi.testsmartling.TranslationState.Translation;

public class Workflow
{
	TreeMap<String,StringState> pendingTranslation; // indexed by hash
	TreeMap<String,TranslationState> translated ; // indexed by hash
	TreeSet<String> locales;
	String localeParms;
	SmartlingClient client;
	boolean running;
	Thread thread;

	static final int SleepSeconds = 10;

	public Workflow(String propertyFile) throws Exception
	{
		client = new SmartlingClient(propertyFile);
		pendingTranslation = new TreeMap<String,StringState>();
		translated = new TreeMap<String,TranslationState>();
		getProjectLocales();

		Runnable r = new Runnable()
		{
			 public void run()
			 {
				 while (running)
				 {
					 try {
						Thread.sleep(SleepSeconds * 1000);
					}
					catch (InterruptedException e)
					{
						continue; // check if still running
					} 

					try {
						runTask();
					}
					catch (Exception e)
					{
						System.out.println("Exception");
						e.printStackTrace();
					}
				}
			}
		};

	     running = false;
	     thread = new Thread(r);
	}

	public void start() throws InterruptedException
	{
		running = true;
		thread.start();
	}
	public void stop() throws InterruptedException
	{
		running = false;
		thread.interrupt();
		thread.join();
	}

	void getProjectLocales() throws Exception
	{
		locales = new TreeSet<String>();
		StringBuilder sb = new StringBuilder();
		JSONObject json = client.getProjectLocales();
		JSONObject data = json.getJSONObject("data");
		JSONArray array = data.getJSONArray("targetLocales");
		for (int i = 0; i < array.length(); ++i)
		{
			JSONObject locale = array.getJSONObject(i);
			String str = locale.getString("localeId");
			locales.add(str);

			if (sb.length() > 0)
				sb.append("&");
			sb.append("targetLocaleId=");
			sb.append(str);
			localeParms = sb.toString();
		}
	}
	void createStrings(String namespace, String placeholderFormat, String customPlaceholder,
			ArrayList<StringState> allStrings) throws Exception
	{
		JSONObject json = new JSONObject();
		if (namespace != null)
			json.put("namespace", namespace);
		json.put("placeholderFormat", placeholderFormat);
		json.put("placeholderFormatCustom", customPlaceholder);

		JSONArray array = new JSONArray();
		
		for (StringState str : allStrings)
		{
			JSONObject obj = new JSONObject();
			obj.put("stringText", str.text);
			obj.put("format","plain_text");

			if (str.variant != null)
				obj.put("variant", str.variant);
			if (str.instruction != null)
				obj.put("instruction", str.instruction);

			array.add(obj);
		}
		json.put("strings", array);

		System.out.println("input: " + json.write(true));
		RestCallResults result = client.createStrings(json);
		if (result.code != 200 && result.code != 202)
			throw new Exception("code " + result.code + ": " + result.message);

		// we don't need to track pending string creations separately, since we have their hash codes.
		// they will be sent automatically to translations once injested.

		String status = (result.code == 202) ? " (waiting for addition) " : " (added) ";
		LifeCycle cycle = (result.code == 202) ? LifeCycle.WAIT_FOR_CREATION : LifeCycle.WAIT_FOR_TRANSLATION;
		// cycle = LifeCycle.WAIT_FOR_CREATION; // for testing

		JSONObject res = client.parseResult(result);
		System.out.println("\n\ncreateString result: " + res.write(true));

		res = res.getJSONObject("data");
		ArrayList<StringState> arr = StringState.arrayFromJSON(res, LifeCycle.WAIT_FOR_TRANSLATION);

		synchronized (pendingTranslation)
		{
			for (StringState item : arr)
			{
				item.cycle = cycle;
				item.translationState = new TranslationState();
				pendingTranslation.put(item.hash, item);
				System.out.println("adding source: " + item.hash + status + item.text);
			}
		}
	}

	@SuppressWarnings("unchecked")
	void runTask() throws Exception
	 {
		 TreeMap<String,StringState> clone1;
		 synchronized (pendingTranslation)
		 {
			 clone1 = (TreeMap<String, StringState>) pendingTranslation.clone();
		 }

		 if (clone1.isEmpty())
			 return;

		 TreeMap<String,StringState> clone2 = new TreeMap<String,StringState>();
		 ArrayList<StringState> errors = new  ArrayList<StringState>();
		 ArrayList<StringState> ready = new  ArrayList<StringState>();

		 try {
			 //-----
			 for (Map.Entry<String, StringState> ent : clone1.entrySet())
			 {
				//String hash = ent.getKey();
				StringState item = ent.getValue();

				if (item.cycle == LifeCycle.WAIT_FOR_CREATION)
					checkIfCreated(item);

				if (item.cycle == LifeCycle.WAIT_FOR_TRANSLATION || item.cycle == LifeCycle.PARTIALLY_TRANSLATED )
					checkIfTranslated(item);

				if (item.cycle == LifeCycle.ERROR)
				{
					errors.add(item);
				}
				else if (item.cycle == LifeCycle.TRANSLATED)
				{
					ready.add(item);
				}
				else if (item.cycle == LifeCycle.WAIT_FOR_CREATION || item.cycle == LifeCycle.WAIT_FOR_TRANSLATION || item.cycle == LifeCycle.PARTIALLY_TRANSLATED)
				{
					clone2.put(item.hash, item);
				}
			 }
			 //-----
			 synchronized (pendingTranslation)
			 {
				 pendingTranslation = clone2;
			 }

			 //-----
			 if (!errors.isEmpty())
			 {
				 // TODO: send again for creation?
			 }
			 //-----
			 if (!ready.isEmpty())
			 {
				 synchronized (translated)
				 {
					 for (StringState item : ready)
					 {
						 item.translationState.source = item.text;
						 item.translationState.sourceHash = item.hash;
						 translated.put(item.hash, item.translationState);
					 }
				 }
			 }
		 }
		 catch (Exception e)
		 {
			throw e; // TODO
		 }
	 }

	 void checkIfCreated(StringState item) throws Exception
	 {
		 System.out.println("check if created: " + item.hash + " " + item.text);
		 JSONObject json = client.checkCreateStringsStatus(item.createProcessId);
		 System.out.println("result: " + json.write(true));

		 JSONObject data = json.getJSONObject("data");
		 String state = data.getString("processState");

		 // documentation says "CLOSED" but we get "COMPLETED"
		 if (state.equals("CLOSED") || state.equals("COMPLETED"))
			 item.cycle = LifeCycle.WAIT_FOR_TRANSLATION;
		 else if (state.equals("FAILED"))
			item.cycle = LifeCycle.ERROR;

		 System.out.println("new lifecycle: " + item.cycle);
	 }
	 
	 // getting all available translations for a single hash
	 void checkIfTranslated(StringState item) throws Exception
	 {
		 System.out.println("check if translated: " + item.hash + " " + item.text);
		//JSONObject json = client.getOneStringTranslations(item.hash, localeParms); this doesn't work - need to loop on each one

		 for (String locale : locales)
		 {
			 System.out.println("locale: " + locale); 
			 if (item.translationState.translations.containsKey(locale))
			 {
				 System.out.println("ALREADY IN " + locale);
				 continue; // already translated (what if we want latest change?)
			 }

			 // what to do on REST error - set special state?
			 JSONObject json = client.getOneStringTranslations(item.hash, "targetLocaleId=" + locale);
			 System.out.println("result: " + json.write(true));

			 // TODO: on error set state to translation error?
			 JSONObject data = json.getJSONObject("data");
			 JSONArray array = data.getJSONArray("items");

			 if (array.isEmpty())
				 System.out.println("SKIPPED2 " + locale);

			 for (int i = 0; i < array.length(); ++i)
			 {
				 JSONObject result = array.getJSONObject(i);
				// String locale = result.getString("targetLocaleId"); should be same as above

				 JSONArray translations = result.optJSONArray("translations");
				 if (translations == null || translations.isEmpty())
				 {
					 System.out.println("SKIPPED3 " + locale);
					 continue;
				 }

				 // for now assume just one translation
				 JSONObject translation = translations.getJSONObject(0);

				 Translation t = new Translation();
				 t.translation = translation.getString("translation");
				 String date = translation.getString("modifiedDate");

				// t.modifiedDate = Date.from( Instant.parse(date) ); // Java 8, not supported everywhere
				//t.modifiedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(date); doesn't work

				 date = date.replace("Z", "-0000");
				 t.modifiedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(date);
				 t.localeId = locale;

				 item.translationState.translations.put(locale, t);
				 System.out.println("translation for " + locale + ": " + t.translation);
			 }
		 }

		 int translationCount = item.translationState.translations.size();
		 if (translationCount > 0)
		 {
			 item.cycle = (translationCount == locales.size()) ? LifeCycle.TRANSLATED : LifeCycle.PARTIALLY_TRANSLATED;
		 }

		 System.out.println("locales: " + locales.size() + ", translations: " + item.translationState.translations.size());
		 System.out.println("new lifecycle: " + item.cycle);
	 }

	 //----------------------------------------------------------------------------
	public static void main(String[] args)
	{
//		String namespace = "test_namespace";
		String placeholderFormat = "java";
		String customPlaceholder = "\\[\\[\\[(\\d+)\\]\\]\\]";

		String properties = "C:/Develop/Weather/SmartlingClient.properties";
		String strings = "C:/client/smartling/strings.json";

		try {
/*			
			SmartlingClient client = new SmartlingClient(properties);
			JSONObject json = new JSONObject();
			if (namespace != null)
				json.put("namespace", namespace);
			json.put("placeholderFormat", placeholderFormat);
			json.put("placeholderFormatCustom", customPlaceholder);

			JSONArray array1 = new JSONArray();
			JSONObject obj = new JSONObject();
			obj.put("stringText", "My Text");
			obj.put("format","plain_text");
			obj.put("variant", "Variant1");
			array1.add(obj);
			json.put("strings", array1);
			System.out.println("input: " + json.write(true));
			RestCallResults result = client.createStrings(json);
*/		

			Workflow w = new Workflow(properties);

			JSONObject input = Utilities.readJson(strings);
			 ArrayList<StringState> array = StringState.arrayFromJSON(input, LifeCycle.NEW);

			 w.createStrings(null /*namespace*/, placeholderFormat, customPlaceholder, array);
			 Thread.sleep(5 * 1000);
			 w.runTask();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

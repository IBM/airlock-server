package com.ibm.airlock.admin;

import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletContext;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.InputSampleGenerationMode;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.Constants.UtilityType;
import com.ibm.airlock.admin.translations.OriginalString;
import com.ibm.airlock.engine.Environment;
import com.ibm.airlock.engine.ScriptInvoker;
import com.ibm.airlock.engine.ScriptInvoker.InvokerException;
import com.ibm.airlock.engine.StreamsScriptInvoker;
import com.ibm.airlock.engine.VerifyRule;

public class ValidationCache
{
	static public class Info // output from cache
	{
		public String minimalContext, maximalContext, javascriptFunctions, translations;
		public ScriptInvoker minimalInvoker, maximalInvoker, streamsInvoker;
	}
	static class SeasonCache
	{
		String javascriptFunctions, translations;
		ScriptInvoker streamsInvoker;
		HashMap<String,String> minimalContext = new HashMap<String,String>(); // key is minAppVer
		HashMap<String,String> maximalContext = new HashMap<String,String>(); // key is minAppVer
		HashMap<String,ScriptInvoker> minimalInvoker = new HashMap<String,ScriptInvoker>();
		HashMap<String,ScriptInvoker> maximalInvoker = new HashMap<String,ScriptInvoker>();
		protected SeasonCache clone() {
			SeasonCache toRet = new SeasonCache();
			toRet.minimalContext = new HashMap<String,String>(this.minimalContext);
			toRet.maximalContext = new HashMap<String,String>(this.maximalContext);
			toRet.minimalInvoker = new HashMap<String,ScriptInvoker>();
			for (String key : this.minimalInvoker.keySet()) {
				ScriptInvoker val = this.minimalInvoker.get(key).clone();
				toRet.minimalInvoker.put(key, val);
			}
			toRet.maximalInvoker = new HashMap<String,ScriptInvoker>();
			for (String key : this.maximalInvoker.keySet()) {
				ScriptInvoker val = this.maximalInvoker.get(key).clone();
				toRet.maximalInvoker.put(key, val);
			}
			return toRet;
		}
	}

	// development/production caches
	HashMap<String,SeasonCache> devCache  = new HashMap<String,SeasonCache>(); // key is season
	HashMap<String,SeasonCache> prodCache = new HashMap<String,SeasonCache>(); // key is season
	
	HashMap<String,SeasonCache> devCacheAside  = new HashMap<String,SeasonCache>(); // key is season
	HashMap<String,SeasonCache> prodCacheAside = new HashMap<String,SeasonCache>(); // key is season
	
	// optional test data used while populating the cache
	String deleteString = null;
	List<OriginalString> addStrings = null;
	String newUtilityCode = null;
	String removedUtilityId = null;
	Stage newUtilStage = null;
	JSONObject newSchema = null;
	boolean streamsCache = false;

	public ValidationCache()
	{}
	public ValidationCache(String deleteString, List<OriginalString> addStrings)
	{
		this.deleteString = deleteString;
		this.addStrings = addStrings;
	}
	public ValidationCache(String newUtilityCode, String removedUtilityId, Stage newUtilStage)
	{
		this.newUtilityCode = newUtilityCode;
		this.removedUtilityId = removedUtilityId;
		this.newUtilStage = newUtilStage;
	}
	public ValidationCache clone() {
		ValidationCache toRet = new ValidationCache(this.newUtilityCode, this.removedUtilityId, this.newUtilStage);
		toRet.deleteString = this.deleteString;
		toRet.addStrings = this.addStrings;
		toRet.devCache = new HashMap<String,SeasonCache>();
		toRet.prodCache = new HashMap<String,SeasonCache>();
		for (String key : this.devCache.keySet()) {
			SeasonCache sCache = this.devCache.get(key).clone();
			toRet.devCache.put(key, sCache);
		}
		for (String key : this.prodCache.keySet()) {
			SeasonCache sCache = this.prodCache.get(key).clone();
			toRet.prodCache.put(key, sCache);
		}
		return toRet;
	}
	public void setSchema(JSONObject newSchema)
	{
		this.newSchema = newSchema;
	}
	public void setStreamsCache()
	{
		this.streamsCache = true;
	}

	public void startTransaction(Season season) {
		String seasonId = season.getUniqueId().toString();
		SeasonCache currentDevCache = this.devCache.get(seasonId);
		SeasonCache currentProdCache = this.prodCache.get(seasonId);
		//put aside the current caches
		this.devCacheAside.put(seasonId, currentDevCache);
		this.prodCacheAside.put(seasonId, currentProdCache);
		//now replace them with clones
		this.devCache.put(seasonId, currentDevCache.clone());
		this.prodCache.put(seasonId, currentProdCache.clone());
	}
	public void endTransaction(Season season) {
		String seasonId = season.getUniqueId().toString();
		SeasonCache asideDevCache = this.devCache.get(seasonId);
		SeasonCache asideProdCache = this.prodCache.get(seasonId);
		if (asideDevCache != null) {
			this.devCache.put(seasonId, asideDevCache);
			this.devCacheAside.remove(seasonId);
		}
		if (asideProdCache != null) {
			this.prodCache.put(seasonId, asideProdCache);
			this.prodCacheAside.remove(seasonId);
		}
	}
	HashMap<String,SeasonCache> getCache(Stage stage)
	{
		return (stage == Stage.PRODUCTION) ? prodCache : devCache;
	}
	SeasonCache getCacheInfo(Season season, Stage stage)
	{
		HashMap<String,SeasonCache> cache = getCache(stage);
		String seasonId = season.getUniqueId().toString();

		SeasonCache cacheInfo = cache.get(seasonId);
		if (cacheInfo == null)
		{
			cacheInfo = new SeasonCache();
			cache.put(seasonId, cacheInfo);
		}
		return cacheInfo;
	}

	public synchronized Info getInfo(ServletContext context, Season season, Stage stage, String minAppVer) throws JSONException, GenerationException
	{
		Info out = new Info();
		SeasonCache cacheInfo = getCacheInfo(season, stage);

		out.javascriptFunctions = cacheInfo.javascriptFunctions;
		if (out.javascriptFunctions == null)
		{
			UtilityType type = streamsCache ? UtilityType.STREAMS_UTILITY : UtilityType.MAIN_UTILITY;
			out.javascriptFunctions = cacheInfo.javascriptFunctions = // optional items can be null
				season.getUtilities().generateUtilityCodeSectionForStageAndType(stage, newUtilityCode, removedUtilityId, newUtilStage, type);
		}

		out.translations = cacheInfo.translations;
		if (out.translations == null)
		{
			if (streamsCache)
				out.translations = cacheInfo.translations = "{}";
			else
			{
				JSONObject enStrings = season.getOriginalStrings().toEnStringsJSON(stage).getJSONObject(Constants.JSON_FIELD_STRINGS);

				if (deleteString != null)
					enStrings.remove(deleteString);
	
				if (addStrings != null)
					for (OriginalString str : addStrings)
					{
						enStrings.put(str.getKey(), str.getValue());
					}

				out.translations = cacheInfo.translations =  enStrings.toString();
			}
		}

		if (streamsCache)
		{
			out.streamsInvoker = cacheInfo.streamsInvoker;
			if (out.streamsInvoker == null)
				out.streamsInvoker = cacheInfo.streamsInvoker = makeStreamsInvoker(out);
		}
		else
		{
			out.minimalContext = cacheInfo.minimalContext.get(minAppVer);
			if (out.minimalContext == null)
			{
				out.minimalContext = makeSample(context, season, stage, minAppVer, InputSampleGenerationMode.MINIMAL).toString();
				cacheInfo.minimalContext.put(minAppVer, out.minimalContext);
			}

			out.maximalContext = cacheInfo.maximalContext.get(minAppVer);
			if (out.maximalContext == null)
			{
				out.maximalContext = makeSample(context, season, stage, minAppVer, InputSampleGenerationMode.MAXIMAL).toString();
				cacheInfo.maximalContext.put(minAppVer, out.maximalContext);
			}
			
			out.minimalInvoker = cacheInfo.minimalInvoker.get(minAppVer);
			if (out.minimalInvoker == null)
			{
				out.minimalInvoker = makeInvoker(season, out, out.minimalContext);
				cacheInfo.minimalInvoker.put(minAppVer, out.minimalInvoker);
			}
			out.maximalInvoker = cacheInfo.maximalInvoker.get(minAppVer);
			if (out.maximalInvoker == null)
			{
				out.maximalInvoker = makeInvoker(season, out, out.maximalContext);
				cacheInfo.maximalInvoker.put(minAppVer, out.maximalInvoker);
			}
		}
		return out;
	}

	JSONObject makeSample(ServletContext context, Season season, Stage stage, String minAppVer, InputSampleGenerationMode mode) throws GenerationException, JSONException
	{
		if (newSchema == null)
			return season.getInputSchema().generateInputSample(stage, minAppVer, context, mode);
		else
			return InputSchema.generateInputSampleFromJSON(stage, minAppVer, context, newSchema, mode);
	}
	ScriptInvoker makeInvoker(Season season, Info info, String sample) throws JSONException, GenerationException
	{
		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion());
		try {
			return VerifyRule.makeInvoker(sample, info.javascriptFunctions, info.translations, true, env);
		} catch (InvokerException e) {
			throw new GenerationException(e.getMessage());
		}
	}
	ScriptInvoker makeStreamsInvoker(Info info) throws GenerationException
	{
		try {
			return new StreamsScriptInvoker(null, info.javascriptFunctions);
		} catch (InvokerException e) {
			throw new GenerationException(e.getMessage());
		}
	}

	public synchronized void clear()
	{
		devCache.clear();
		prodCache.clear();
	}
	public synchronized void clear(Season season)
	{
		clear(season, Stage.DEVELOPMENT);
		clear(season, Stage.PRODUCTION);
	}
	public synchronized void clear(Season season, Stage stage)
	{
		String seasonId = season.getUniqueId().toString();
		getCache(stage).remove(seasonId);
	}
	public synchronized void clearTranslations(Season season)
	{
		clearTranslations(season, Stage.DEVELOPMENT);
		clearTranslations(season, Stage.PRODUCTION);
	}
	public synchronized void clearTranslations(Season season, Stage stage)
	{
		SeasonCache cacheInfo = getCacheInfo(season, stage);
		cacheInfo.translations = null;
		// temporary - it may be better to override the translations inside the invokers
		cacheInfo.minimalInvoker.clear();
		cacheInfo.maximalInvoker.clear();
	}
	public synchronized void clearFunctions(Season season)
	{
		clearFunctions(season, Stage.DEVELOPMENT);
		clearFunctions(season, Stage.PRODUCTION);
	}
	public synchronized void clearFunctions(Season season, Stage stage)
	{
		SeasonCache cacheInfo = getCacheInfo(season, stage);
		cacheInfo.javascriptFunctions = null;
		// temporary - it may be better to override the functions inside the invokers
		cacheInfo.minimalInvoker.clear();
		cacheInfo.maximalInvoker.clear();
	}
	public synchronized void clearSamples(Season season)
	{
		clearSamples(season, Stage.DEVELOPMENT);
		clearSamples(season, Stage.PRODUCTION);
	}
	public synchronized void clearSamples(Season season, Stage stage)
	{
		SeasonCache cacheInfo = getCacheInfo(season, stage);
		cacheInfo.minimalContext.clear();
		cacheInfo.maximalContext.clear();
		cacheInfo.minimalInvoker.clear();
		cacheInfo.maximalInvoker.clear();
	}
}

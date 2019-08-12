package com.ibm.airlock.admin.translations;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.ibm.airlock.Strings;
import com.ibm.airlock.admin.serialize.Compression;
import com.ibm.airlock.admin.serialize.DataSerializer;
import com.ibm.airlock.admin.serialize.Encryption;
import com.ibm.airlock.engine.Environment;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.ibm.airlock.Constants;
import com.ibm.airlock.Constants.Stage;
import com.ibm.airlock.ProductServices;
import com.ibm.airlock.admin.Season;

public class TranslationUtilities {

	public static enum ReturnFields{
		SUPPORTED_LANGUAGES,
		TRANSLATIONS,
		ALL		
	}

	//return the existing locales according to the existing strings files
	public static List<String> getLocalesByStringsFiles(String product_id, String season_id, DataSerializer ds, Logger logger) throws IOException, JSONException {
		String separator = ds.getSeparator();
		HashMap<String, Integer> uniqueLocaleNamesMap = new HashMap<String, Integer>();
		//get all files from translations folder
		String translationsFolderPath = Constants.SEASONS_FOLDER_NAME+separator+product_id.toString()+
				separator+season_id+separator+Constants.TRANSLATIONS_FOLDER_NAME;

		Collection<String> locFileNames = ds.listFileNames (translationsFolderPath);
		for (String locFile:locFileNames) {
			if (locFile.equals(Constants.ORIGINAL_STRINGS_FILE_NAME))
				continue;

			//the file name format is: strings__<locale>.json
			int doubleUSpos = locFile.indexOf("__");				
			int dotPos = locFile.indexOf(".");

			if (doubleUSpos == -1 || dotPos == -1) {
				logger.warning("Illegal translations file name: " + locFile);
				continue;
			}
			
			String locale = locFile.substring(doubleUSpos+2, dotPos);

			if (locale.contains(Stage.DEVELOPMENT.toString())) {
				locale = locale.replace(Stage.DEVELOPMENT.toString(), "");
			}
			else if (locale.contains(Stage.PRODUCTION.toString())) {
				locale = locale.replace(Stage.PRODUCTION.toString(), "");
			}
			uniqueLocaleNamesMap.put(locale, 1); //this will remove duplications since the list contain a few file for each locale for example strings__enDEVELOPMENT.json strings__enPRODUCTION.json and maybe even strings__en.json
										
		}
		
		List<String> sortedKeys=new ArrayList<String>(uniqueLocaleNamesMap.keySet());
		Collections.sort(sortedKeys);

		return sortedKeys;

	}

	//fileStage can be null - this is for seasons from prev version in which there was no dev/prod separation. 
	public static JSONObject readLocaleStringsFile (String locale, Season season, ServletContext context, Logger logger, Stage fileStage) throws IOException, JSONException {

		String stageStr = "";
		if (fileStage != null)
			stageStr = fileStage.toString();

		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();

		String stringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
				separator + Constants.STRINGS_FILE_NAME_PREFIX + locale + stageStr + Constants.STRINGS_FILE_NAME_EXTENSION; 

		//boolean isProductRuntimeEncrypted = Utilities.isProductSupportRuntimeEncryption(context, season);
		boolean isSeasonRuntimeEncrypted = season.getRuntimeEncryption();
		
		Environment env = new Environment();
		env.setServerVersion(season.getServerVersion());
		try {
			if (isSeasonRuntimeEncrypted && ProductServices.isEncryptionSupported(env)) {
				byte[] byteArr = ds.readDataToByteArray(stringsFilePath);
				String keyStr = season.getEncryptionKey();				
				byte[] key = Encryption.fromString(keyStr);
				
				Encryption e = new Encryption(key);					
				byte[] decrypted = e.decrypt(byteArr);
				String data = Compression.decompress(decrypted);
				return new JSONObject(data);
			}
			else {
				return ds.readDataToJSON(stringsFilePath);
			}
		} catch (IOException e) {
			String errMsg = String.format(Strings.failedReadingFile,stringsFilePath) + e.getMessage();
			logger.severe(errMsg);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(errMsg);
		} catch (JSONException e) {
			String errMsg = stringsFilePath + " file is not in a legal JSON format: " + e.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(errMsg);
		} catch (GeneralSecurityException gse){
			String errMsg = stringsFilePath + " file is not in a legal encryption format: " + gse.getMessage();
			logger.severe(errMsg);			
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			throw new IOException(errMsg);
		}
		


	}

	public static JSONObject readOriginalStringsFile (Season season, ServletContext context, Logger logger) throws IOException, JSONException {

		DataSerializer ds = (DataSerializer)context.getAttribute(Constants.DATA_SERIALIZER_PARAM_NAME);
		String separator = ds.getSeparator();

		String stringsFilePath = Constants.SEASONS_FOLDER_NAME+separator+season.getProductId().toString()+
				separator+season.getUniqueId().toString()+separator+Constants.TRANSLATIONS_FOLDER_NAME + 
				separator + Constants.ORIGINAL_STRINGS_FILE_NAME;

		try {
			return ds.readDataToJSON(stringsFilePath);
		} catch (IOException ioe) {
			//failed reading 3 times to s3. Set server state to ERROR.
			//All subsequent requests will be denied by stateVerificationFilter till fixed and restarted
			context.setAttribute(Constants.SERVICE_STATE_PARAM_NAME, Constants.ServiceState.S3_IO_ERROR);
			String error = String.format(Strings.failedReadingFile,stringsFilePath) + ioe.getMessage();
			logger.severe(error);
			logger.severe(Strings.changeAirlockSerevrStateTo + "S3_IO_ERROR.");
			throw new IOException(error);	
		}

	}
}

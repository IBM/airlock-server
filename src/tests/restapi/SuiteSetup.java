package tests.restapi;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;



public class SuiteSetup {

	private String filePath;
	private String m_url;
	private String sessionToken = "";
	private String productsToDelete;
	
	@BeforeSuite
	@Parameters({"fsPathPrefix", "url", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "seasonVersion"})
	public void init(@Optional String fsPathPrefix, @Optional String url, String configPath, @Optional String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, @Optional String seasonVersion) throws IOException{

		filePath = configPath;
		productsToDelete = productsToDeleteFile;
		m_url = url;
		AirlockUtils utils = new AirlockUtils(m_url, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		try {
			//utils.createUserGroups();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// file that contains all products created by tests. In SuiteCleanup products are deleted according to this list
		File file = new File(productsToDelete); 
		if (file.exists()) {
			file.delete();
		}

		file.createNewFile();
		
		//to differentiate between pre-version 3.0 and versions 3.x seasons, since features actions paths are different
		System.setProperty("seasonVersion", seasonVersion);
		if (fsPathPrefix != null) {
			System.setProperty("fsPathPrefix", fsPathPrefix);
		}
		
	}

	
	
}

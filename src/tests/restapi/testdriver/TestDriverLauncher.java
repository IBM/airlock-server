package tests.restapi.testdriver;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import org.apache.wink.json4j.JSONException;
import org.testng.Assert;
import org.testng.ITestContext;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FolderUtils;
import tests.restapi.testdriver.TestDriver.Result;

public class TestDriverLauncher {

	protected String filePathNegative;
	protected String filePathPositive;
	protected String sessionToken;

	@BeforeClass
	@Parameters({"url", "positiveScenarios", "negativeScenarios", "sessionToken"})
	public void init(String url, String positiveScenarios, String negativeScenarios, @Optional String sToken, ITestContext context) throws IOException, JSONException{
		filePathNegative = negativeScenarios;
		filePathPositive = positiveScenarios;
		if (sToken != null)
			sessionToken = sToken;


	}
	

	
	@Test(dataProvider = "positiveScenarios", description = "Positive scenarios")
	public void positiveScenarios(String path){
		//System.out.println(path);
		TestDriver t = new TestDriver();
		Result res = t.runTest(path);
		//Assert.assertTrue(res.toString().equals("OK"), "The positive test " + path + " didn't pass. Failed " + res.badAction + " with error " + res.error);
		Assert.assertTrue(res.toString().equals("OK"), "The positive test " + path + " didn't pass. ");
	}
	
	@Test(dataProvider = "negativeScenarios", description = "Negative scenarios")
	public void negativeScenarios(String path){
		//System.out.println(path);
		TestDriver t = new TestDriver();
		Result res = t.runTest(path);
		System.out.println(res.errors);
		Assert.assertTrue(res.errors.get(0).toString().contains("error") || res.errors.get(0).toString().contains("Failed to") || res.errors.get(0).toString().contains("Unable to"), "The negative test " + path + " didn't pass");
		
	}

	
	@DataProvider (name="positiveScenarios")
	private Iterator<Object[]> positiveScenarios(){

		List<Object[]> dataToBeReturned = new ArrayList<Object[]>();
		ArrayList<File> files = FolderUtils.allFilesFromFolder(filePathPositive);
		
		
		if (files != null && files.size() > 0){
			for (File file : files){
				if (file.getName().contains(".ini")){
					dataToBeReturned.add(new Object[] {file.getPath() });
				}
			}
		}
		return dataToBeReturned.iterator(); 

	}
	
	@DataProvider (name="negativeScenarios")
	private Iterator<Object[]> negativeScenarios(){

		List<Object[]> dataToBeReturned = new ArrayList<Object[]>();
		ArrayList<File> files = FolderUtils.allFilesFromFolder(filePathNegative);
		
		if (files != null && files.size() > 0){
			for (File file : files){
				if (file.getName().contains(".ini")){
					dataToBeReturned.add(new Object[] {file.getPath() });
				}
			}
		}
		return dataToBeReturned.iterator(); 

	}
	
	@DataProvider (name="testScenarios")
	private Iterator<Object[]> testScenarios(ITestContext context){
		System.out.println("param = " + context.getAttribute("filePathPositive"));
		

		List<Object[]> dataToBeReturned = new ArrayList<Object[]>();
		ArrayList<File> files = FolderUtils.allFilesFromFolder(filePathPositive);
		
		
		if (files != null && files.size() > 0){
			for (File file : files){
				if (file.getName().contains(".ini")){
					dataToBeReturned.add(new Object[] {file.getPath() });
				}
			}
		}
		return dataToBeReturned.iterator(); 

	}
	
	

	/*
	 * 
	 * 	
	@DataProvider (name="fileReader")
	private Iterator<Object[]> fileReader(Method m){
		List<Object[]> dataToBeReturned = new ArrayList<Object[]>();
		for (String filePath : ((Test) m.getAnnotation(Test.class)).groups()) {
	        // add each to the list
			
			ArrayList<File> files = FolderUtils.allFilesFromFolder(filePath);
			
			
			for (File file : files){
				if (file.getName().contains(".ini")){
					dataToBeReturned.add(new Object[] {file.getPath() });
				}
			}
	    }
		

		return dataToBeReturned.iterator(); 

	}
	 */
	
	

}

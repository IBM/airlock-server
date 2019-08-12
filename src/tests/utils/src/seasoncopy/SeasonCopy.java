package tests.utils.src.seasoncopy;

import java.io.IOException;

import java.util.Properties;

public class SeasonCopy {

	private static Properties props = new Properties();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length == 0){
			System.out.println("You must specify a full path to the cofiguration file.");
			return;
		}
		String configPath = args[0];
		props = new Properties();
		try {
			props = SeasonCopyProperties.getPropValues(configPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ActionExecution act = new ActionExecution(props);
		act.doExecute();
		
		System.out.println("DONE!");
	}
	

}

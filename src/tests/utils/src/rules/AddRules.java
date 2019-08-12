package tests.utils.src.rules;

import java.io.IOException;
import java.util.Properties;


public class AddRules {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length == 0){
			System.out.println("You must specify a full path to the cofiguration file.");
			return;
		}
		String configPath = args[0];
		AddRulesProperties properties = new AddRulesProperties();
		Properties props = new Properties();
		try {
			props = properties.getPropValues(configPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		AddRulesExecution act = new AddRulesExecution(props);
		act.doExecute();

	}
}

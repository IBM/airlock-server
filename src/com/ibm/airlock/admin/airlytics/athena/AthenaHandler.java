package com.ibm.airlock.admin.airlytics.athena;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.AthenaException;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.paginators.GetQueryResultsIterable;

import java.util.ArrayList;
import java.util.List;

import com.ibm.airlock.Constants.DATA_TYPE;

public class AthenaHandler {

	public static final int CLIENT_EXECUTION_TIMEOUT = 100000;
	public static final long SLEEP_AMOUNT_IN_MS = 1000;
	
	private Region region;
	private String outputBucket;
	private String catalog;
	
	public AthenaHandler(String region, String outputBucket, String catalog) {
		if (region.equalsIgnoreCase("eu_west_1")) {
			this.region = Region.EU_WEST_1;
		}
		else if (region.equalsIgnoreCase("us_east_1")) {
			this.region = Region.US_EAST_1;
		}
		else {
			throw new RuntimeException("unsupported athena region: " + region + ". Currently only eu_west_1 and us_east_1 are supported");
		}
		this.outputBucket = "s3://"+outputBucket;
		this.catalog = catalog;
	}

	private static String convertDataTypeToAthenaType(DATA_TYPE columnType, boolean isArray) {
		if (isArray) {
			return "string";
		}
		
		switch (columnType) {
			case STRING :
				return "string";
			case LONG : 
				return "bigint";
			case INTEGER:
				return "int";
			case BOOLEAN:
				return "boolean";
			case DOUBLE:
				return "double";
			case TIMESTAMP:
				return "bigint";
			case JSON:
				return "string";
		}
		return null;
	} 
	
	public void addColumn(String athenaDB, String athenaTable, String columnName, DATA_TYPE type, boolean isArray) throws InterruptedException {
		String cmd = "alter table " + athenaTable + " add columns ("+ columnName + " "  + convertDataTypeToAthenaType(type, isArray) + ");";
		AthenaClient athenaClient = AthenaClient.builder()
                .region(region)
                .build();
		
		String queryExecutionId = submitAthenaQuery(athenaClient, cmd, athenaDB);
		waitForQueryToComplete(athenaClient, queryExecutionId);
        athenaClient.close();
	}
	
	
	public void deleteColumn(String athenaDB, String athenaTable, String columnName) throws InterruptedException {
		//get current columns list
		ArrayList<ColumnData> originalColumns = getColumnData(athenaDB, athenaTable );
		
		//remove the requested column from list
		StringBuilder cmdSB = new StringBuilder("ALTER TABLE " + athenaTable + " REPLACE COLUMNS (");
		for (ColumnData cd : originalColumns) {
			if (!columnName.equals(cd.columnName) && !cd.isPartitionKey) {
				cmdSB.append(cd.columnName + " " + cd.columnType);
				cmdSB.append(",");
			}	
		}
		
		cmdSB.deleteCharAt(cmdSB.length()-1);
		cmdSB.append(")");
		
		runReplaceColumn(cmdSB.toString(), athenaDB);
	}
	
	public void updateColumnName(String athenaDB, String athenaTable, String prevAthenaColumn, String newAthenaColumn) throws InterruptedException {
		//get current columns list
		ArrayList<ColumnData> originalColumns = getColumnData(athenaDB, athenaTable );
		
		//rename the requested column
		StringBuilder cmdSB = new StringBuilder("ALTER TABLE " + athenaTable + " REPLACE COLUMNS (");
		for (ColumnData cd : originalColumns) {
			if (!cd.isPartitionKey) {
				if (prevAthenaColumn.equals(cd.columnName)) { 
					cmdSB.append(newAthenaColumn + " " + cd.columnType);
				}
				else {
					cmdSB.append(cd.columnName + " " + cd.columnType);
				}
				cmdSB.append(",");
			}	
		}
		
		cmdSB.deleteCharAt(cmdSB.length()-1);
		cmdSB.append(")");
		
		runReplaceColumn(cmdSB.toString(), athenaDB);
	}
	
	private void runReplaceColumn(String cmd, String athenaDB) throws InterruptedException {
		cmd = cmd.replace("varchar", "string");
		cmd = cmd.replace("integer", "int");
		
		AthenaClient athenaClient = AthenaClient.builder()
                .region(region)
                .build();
		
		String queryExecutionId = submitAthenaQuery(athenaClient, cmd, athenaDB);
		waitForQueryToComplete(athenaClient, queryExecutionId);
        athenaClient.close();
	}

	// Submits a sample query to Amazon Athena and returns the execution ID of the query
	private String submitAthenaQuery(AthenaClient athenaClient, String cmd, String athenaDB) throws AthenaException{
		try {
			// The QueryExecutionContext allows us to set the database
			QueryExecutionContext queryExecutionContext = null;
			if (athenaDB!=null) {
				queryExecutionContext = QueryExecutionContext.builder()
						.database(athenaDB).build();
			}
			else {
				queryExecutionContext = QueryExecutionContext.builder().catalog(catalog).build();
			}
			
			// The result configuration specifies where the results of the query should go
			ResultConfiguration resultConfiguration = ResultConfiguration.builder()
					.outputLocation(outputBucket)
					.build();

			StartQueryExecutionRequest startQueryExecutionRequest = StartQueryExecutionRequest.builder()
					.queryString(cmd)
					.queryExecutionContext(queryExecutionContext)
					.resultConfiguration(resultConfiguration)
					.build();

			StartQueryExecutionResponse startQueryExecutionResponse = athenaClient.startQueryExecution(startQueryExecutionRequest);
			return startQueryExecutionResponse.queryExecutionId();

		} catch (AthenaException e) {
			e.printStackTrace();
			throw e;
		}
	}

	// Wait for an Amazon Athena query to complete, fail or to be cancelled
	private void waitForQueryToComplete(AthenaClient athenaClient, String queryExecutionId) throws InterruptedException {
		GetQueryExecutionRequest getQueryExecutionRequest = GetQueryExecutionRequest.builder()
				.queryExecutionId(queryExecutionId).build();

		GetQueryExecutionResponse getQueryExecutionResponse;
		boolean isQueryStillRunning = true;
		while (isQueryStillRunning) {
			getQueryExecutionResponse = athenaClient.getQueryExecution(getQueryExecutionRequest);
			String queryState = getQueryExecutionResponse.queryExecution().status().state().toString();
			if (queryState.equals(QueryExecutionState.FAILED.toString())) {
				throw new RuntimeException("The Amazon Athena query failed to run with error message: " + getQueryExecutionResponse
						.queryExecution().status().stateChangeReason());
			} else if (queryState.equals(QueryExecutionState.CANCELLED.toString())) {
				throw new RuntimeException("The Amazon Athena query was cancelled.");
			} else if (queryState.equals(QueryExecutionState.SUCCEEDED.toString())) {
				isQueryStillRunning = false;
			} else {
				// Sleep an amount of time before retrying again
				Thread.sleep(SLEEP_AMOUNT_IN_MS);
			}
			//System.out.println("The current status is: " + queryState);
		}
	}
	
	public static class ColumnData {
		public String columnName;
		public String columnType;
		public Boolean isPartitionKey;
	}
	
	public ArrayList<ColumnData> getColumnData(String athenaDB, String athenaTable) throws InterruptedException {
		ArrayList<ColumnData> res = new ArrayList<>();
		AthenaClient athenaClient = AthenaClient.builder()
				.region(region)
	            .build();
		
		String command = "SELECT column_name, data_type, extra_info FROM information_schema.columns WHERE table_schema = '" + athenaDB + "' and table_name = '"+ athenaTable + "'";
		String queryExecutionId = submitAthenaQuery(athenaClient, command, athenaDB);
		waitForQueryToComplete(athenaClient, queryExecutionId);
	    
		GetQueryResultsRequest getQueryResultsRequest = GetQueryResultsRequest.builder()
				.queryExecutionId(queryExecutionId)
				.build();
	
		GetQueryResultsIterable getQueryResultsResults = athenaClient.getQueryResultsPaginator(getQueryResultsRequest);
	
		for (GetQueryResultsResponse result : getQueryResultsResults) {
			List<Row> results = result.resultSet().rows();
			for (Row row : results) {
				List<Datum> allData = row.data();
				int i=0;
				ColumnData cd = null;
				for (Datum data : allData) {
					String val = data.varCharValue();
					if (i==0 && val.equals("column_name")) {
						break;
					}
					if (i==0) {
						cd = new ColumnData();
						cd.columnName = val;
					}
					else if (i==1){
						cd.columnType = val;
					}
					else if (i==2){
						cd.isPartitionKey = val!=null&&val.equalsIgnoreCase("partition key");
					}
					i++;
					//System.out.println(data.varCharValue());
				}
				if (cd!=null) {
					res.add(cd);
				}
			}
		}
		
	    athenaClient.close();
	    return res;
	}
	public ArrayList<String> getAthenaTableColumns(String athenaTable, String athenaDB) throws InterruptedException {
		String cmd = "SHOW COLUMNS IN " + athenaTable +";";
		return getResultsAsStringsList(cmd, athenaDB);	
	}
	
	public ArrayList<String> getAthenaTablesInDatabase(String athenaDB) throws InterruptedException {
		String cmd = "SHOW TABLES IN " + athenaDB +";";
		return getResultsAsStringsList(cmd, null);	
	}
	
	public ArrayList<String> getAthenaDatabases() throws InterruptedException {
		String cmd = "SHOW DATABASES;";
		return getResultsAsStringsList(cmd, null);	
	}
	
	private ArrayList<String> getResultsAsStringsList (String command, String athenaDB) throws InterruptedException {
		ArrayList<String> res = new ArrayList<>();
		AthenaClient athenaClient = AthenaClient.builder()
				.region(region)
                .build();
		
		String queryExecutionId = submitAthenaQuery(athenaClient, command, athenaDB);
		waitForQueryToComplete(athenaClient, queryExecutionId);
        
		GetQueryResultsRequest getQueryResultsRequest = GetQueryResultsRequest.builder()
				.queryExecutionId(queryExecutionId)
				.build();

		GetQueryResultsIterable getQueryResultsResults = athenaClient.getQueryResultsPaginator(getQueryResultsRequest);

		for (GetQueryResultsResponse result : getQueryResultsResults) {
			List<Row> results = result.resultSet().rows();
			for (Row row : results) {
				List<Datum> allData = row.data();
				for (Datum data : allData) {
					res.add(data.varCharValue().trim());
				}
			}
		}
		
        athenaClient.close();
        return res;
	}
}




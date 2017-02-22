package com.csmckelvey.reticle;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.json.JSONException;
import org.json.JSONObject;

import com.csmckelvey.reticle.util.ReticleLogger;
import com.csmckelvey.reticle.util.ReticleUtils;

public class ReticleServer {

	private static final boolean DEPLOYED = true;
	private static final String LOG_PREFIX = "[Reticle_Server] ";
	
	private Properties props = null;
	private String clientBTAddress = null;
	private String serverBTAddress = null;
	private static ReticleLogger logger = null;
	
	private static UUID myListeningUUID;
	private static String myListeningURL;
	private static String myListeningUUIDString = null;
	
	private static String remoteResponseURL;
	private static String remoteResponseUUIDString = null;
	
	private static RemoteDevice remoteDevice = null;
	private static StreamConnectionNotifier connectionNotifier;

	static {
		logger = ReticleLogger.getLogger();
	}
	
	public static void main(String[] args) {
		ReticleServer server = new ReticleServer();
		server.loadProperties();
		
		logger.log("").log(LOG_PREFIX + "Reticle Server Startup!");		
		
		myListeningUUID = new UUID(myListeningUUIDString.replaceAll("-", ""), false);
		myListeningURL = "btspp://localhost:" + myListeningUUID  + ";name=ReticleServer;authenticate=false;encrypt=false;";
		
		try {
			connectionNotifier = (StreamConnectionNotifier) Connector.open(myListeningURL);
			logger.log(LOG_PREFIX + "Starting to listen @ " + myListeningURL);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.log(LOG_PREFIX + "Ready for business!").log("");
		
		for (;;) { 
			server.acceptReticleConnection(); 
		}
	}
	
	private void acceptReticleConnection() {
		try {
			LocalDevice.getLocalDevice().setDiscoverable(DiscoveryAgent.GIAC);

			StreamConnection connection = connectionNotifier.acceptAndOpen();
			
			logger.log(LOG_PREFIX + "Connection Accepted and Opened!");
	        remoteDevice = RemoteDevice.getRemoteDevice(connection);
	        logger.log(LOG_PREFIX + "Connected to [" + remoteDevice.getFriendlyName(true) + " @ " + remoteDevice.getBluetoothAddress() + "]");
	        
	        logger.log(LOG_PREFIX + "Reading request ...");
	        InputStream in = connection.openInputStream();
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
	        String request = bufferedReader.readLine();
	        
	        in.close();
	        bufferedReader.close();
	        connection.close();
	        
	        logger.log(LOG_PREFIX + "Recieved: " + request);
	        if (request != null) {
	        	new Thread(new ReticleProcess(request)).start();	        	
	        }
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadProperties() {
		props = new Properties();
		InputStream input = null;

		try {
			
			//	If the server is deployed as a jar we want to use the properties file that is right next to the jar
			//	If the server is running in eclipse we want to use the properties file in the workspace
			if (DEPLOYED) { 
				input = new FileInputStream("./config.properties"); 
			}
			else { 
				input = ReticleServer.class.getResourceAsStream("/config/config.properties"); 
			}
			
			props.load(input);
			
			myListeningUUIDString = props.getProperty("ListeningUUID");
			remoteResponseUUIDString = props.getProperty("RespondUUID");
			clientBTAddress = props.getProperty("clientBluetoothAddress");
			serverBTAddress = props.getProperty("serverBluetoothAddress");
			
			if ("true".equals(props.getProperty("debug"))) { 
				ReticleLogger.setDebug(true); 
			}
			if ("true".equals(props.getProperty("outputToFile"))) { 
				ReticleLogger.setOutputToFile(true); 
			}
			ReticleLogger.setOutputFileName(props.getProperty("outputFileName"));
			ReticleLogger.setExceptionOutputFileName(props.getProperty("exceptionOutputFileName"));
			
			//logger.log("").log(LOG_PREFIX + "Reticle Server Startup!");
			logger.log(LOG_PREFIX + "Current Properties");
			logger.log(LOG_PREFIX + "IP Address          | " + props.getProperty("ipaddress"));
			logger.log(LOG_PREFIX + "Debug Output        | " + props.getProperty("debug"));
			logger.log(LOG_PREFIX + "Platform Code       | " + props.getProperty("platform"));
			logger.log(LOG_PREFIX + "Platform Name       | " + props.getProperty("platformName"));
			logger.log(LOG_PREFIX + "Listening UUID      | " + myListeningUUIDString);
			logger.log(LOG_PREFIX + "Responding UUID     | " + remoteResponseUUIDString);
			logger.log(LOG_PREFIX + "Output To File      | " + props.getProperty("outputToFile"));
			logger.log(LOG_PREFIX + "Output File Name    | " + props.getProperty("outputFileName"));
			logger.log(LOG_PREFIX + "Exception File Name | " + props.getProperty("exceptionOutputFileName"));
			logger.log(LOG_PREFIX + "Client BT Address   | " + clientBTAddress);
			logger.log(LOG_PREFIX + "Server BT Address   | " + serverBTAddress);
		} catch (IOException e) {
			logger.logException(e);
		} finally {
			if (input != null) {
				try { 
					input.close(); 
				} 
				catch (IOException e) { 
					logger.logException(e); 
				}
			}
		}
		
		logger.log(LOG_PREFIX + "Loading Properties Complete!", 0, 0, true);
	}
	
	private class ReticleProcess implements Runnable {
		
		private String request = null;
		private static final String LOG_PREFIX = "[Reticle_Process] ";
		
		public ReticleProcess(String request) {
			this.request = request;
		}
		
		@Override
		public void run() {
			
			logger.log(LOG_PREFIX + "Reticle Process Starting ...");
			JSONObject requestObject;
			try {
				requestObject = new JSONObject(request);
				String requestCommand = requestObject.getString("commandName");
				
				JSONObject responseObject = new JSONObject();
				responseObject.put("responseFor", requestObject.getInt("id"));
				
				switch(requestCommand) {
					case "bluetoothTest":
						responseObject.put("name", "bluetoothTest_response");
						break;
					case "isWifiConnected":
						responseObject.put("name", "isWifiConnected_response");
						break;
					case "getAvailableWifi":
						responseObject.put("name", "getAvailableWifi_response");
						break;
					case "getWifiConfig":
						responseObject.put("name", "getWifiConfig_response");
						break;
					case "simpleNetworkMap":
						responseObject.put("name", "simpleNetworkMap_response");
						break;
				}
				
				sendResponse(responseObject, requestObject.getInt("id"));
			} catch (JSONException e) {
				logger.logException(e);
			} catch (Exception e) {
				logger.logException(e);
			}
			
			logger.log(LOG_PREFIX + "Reticle Process Completed!");
		}
		
		private void sendResponse(JSONObject responseObject, int responseForId) {
			ReticleUtils utils = new ReticleUtils();
			utils.searchClientForBluetoothServices(remoteResponseUUIDString, remoteDevice);
			
			long counter = 0;
			remoteResponseURL = null;
			while (remoteResponseURL == null && counter++ < 10_000_000) {
				remoteResponseURL = utils.getURLForService(remoteResponseUUIDString.replaceAll("-", ""));
			}
			
			logger.log(LOG_PREFIX + "Response URL: " + remoteResponseURL);
			
			if (remoteResponseURL != null) {
				try {
					StreamConnection streamConnection = (StreamConnection) Connector.open(remoteResponseURL);
					OutputStream out = streamConnection.openOutputStream();
					out.write("ReticleServer AWK\n".getBytes());
					
					String output = "";
					String requestCommand = responseObject.getString("name");
					switch(requestCommand) {
						case "bluetoothTest_response":
							output = "Success";
							break;
						case "isWifiConnected_response":
							output = "";
							break;
						case "getAvailableWifi_response":
							output = "";
							break;
						case "getWifiConfig_response":
							output = utils.executeCommand("ifconfig -a");
							break;
						case "simpleNetworkMap_response":
							output = "";
							break;
					}
					
					responseObject.put("responseText", output);
					
					String[] parsedOutput = output.split("~~~~");
					for (String line : parsedOutput) {
						logger.log("[Command_Output] " + line);					
					}
					
					out.write((responseObject.toString() + "\n").getBytes());
					out.flush();
					out.close();
					
					logger.log(LOG_PREFIX + "Response for id = " + responseForId + " Sent!");					
				} catch(IOException e) {
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			else {
				logger.log(LOG_PREFIX + "Unable to locate phone's bluetooth service");
			}
		}
		
		private void getWifiConfig() {
			
		}
	}

}

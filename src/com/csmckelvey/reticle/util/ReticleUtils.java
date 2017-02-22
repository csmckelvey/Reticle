package com.csmckelvey.reticle.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class ReticleUtils {
	protected boolean clientFound;
	protected String clientToSearchFor = "";
	protected RemoteDevice clientDevice = null;
	
	private static ReticleLogger logger;
	private static final String LOG_PREFIX = "[Reticle_Utils] "; //logger.log(LOG_PREFIX + "");
	
	protected List<RemoteDevice> currentScannedDevices = new ArrayList<>();
	protected List<RemoteDevice> allBluetoothClientsFound = new ArrayList<>();
	//	UUID -> URL
	protected Map<String, String> bluetoothServiceURLsFound = new HashMap<>();
	
	protected final Object deviceScanCompletedEvent = new Object();
	protected final Object serviceScanCompletedEvent = new Object();
	protected final Object deviceSearchCompletedEvent = new Object();
	
	static {
		logger = ReticleLogger.getLogger();
	}
	
	public void scanAllBluetoothDevices() {
		synchronized(deviceScanCompletedEvent) {
	        try {
	        	currentScannedDevices.clear();
	            LocalDevice local = LocalDevice.getLocalDevice();
	            local.setDiscoverable(DiscoveryAgent.GIAC);
	            DiscoveryAgent discoveryAgent = local.getDiscoveryAgent();
	            boolean startedInquiry = discoveryAgent.startInquiry(DiscoveryAgent.GIAC, new ScanDiscoveryListener());
	            if (startedInquiry) {
	            	deviceScanCompletedEvent.wait();
	            }
	        } catch (BluetoothStateException e) {
	        	logger.log(LOG_PREFIX + "BluetoothStateException exception: " + e);
	        } catch (InterruptedException e) {
	        	logger.log(LOG_PREFIX + "InterruptedException exception: " + e);
	        	Thread.currentThread().interrupt();
	        }
		}
		
		logger.log(LOG_PREFIX + "Found " + currentScannedDevices.size() + " device(s) during this scan");
		logger.log(LOG_PREFIX + allBluetoothClientsFound.size() + " total device(s) discovered");
		
		String tmpName;
		for (RemoteDevice device : allBluetoothClientsFound) {
			try { 
				tmpName = device.getFriendlyName(true); 
			} catch (IOException e) { 
				tmpName = "Unknown Name";
				logger.logException(e);
			}
			
			logger.log(LOG_PREFIX + tmpName + " @ " + device.getBluetoothAddress(), 1, 0);
		}
	}
	
	public boolean findBluetoothDevice(String deviceAddress) {
		clientFound = false;
		clientToSearchFor = deviceAddress;
		
		for (RemoteDevice device : allBluetoothClientsFound) {
			if (device.getBluetoothAddress().equals(clientToSearchFor)) {
				clientFound = true;
				clientToSearchFor = "";
				clientDevice = device;
				return clientFound;
			}
		}
		
		synchronized(deviceSearchCompletedEvent) {
	        try {
	            LocalDevice local = LocalDevice.getLocalDevice();
	            local.setDiscoverable(DiscoveryAgent.GIAC);
	            DiscoveryAgent discoveryAgent = local.getDiscoveryAgent();
	            boolean startedInquiry = discoveryAgent.startInquiry(DiscoveryAgent.GIAC, new SearchDiscoveryListener());
	            if (startedInquiry) {
	                deviceSearchCompletedEvent.wait();
	            }
	        } catch (BluetoothStateException e) {
	        	logger.log(LOG_PREFIX + "BluetoothStateException exception: " + e);
	        } catch (InterruptedException e) {
	        	logger.log(LOG_PREFIX + "InterruptedException exception: " + e);
	        	Thread.currentThread().interrupt();
	        }
	        
	        logger.log(LOG_PREFIX + "Bluetooth Device Search Completed");
	        
	        clientToSearchFor = "";
	        return clientFound;
	    }
	}

	public String getURLForService(String uuidToFind) {
		String result = null;
		for (Entry<String, String> e : bluetoothServiceURLsFound.entrySet()) {
			if (e.getKey().trim().equals(uuidToFind.trim())) {
				result = e.getValue();
			}
		}
		return result;
	}
	
	public void searchClientForBluetoothServices(String uuid, RemoteDevice remoteDevice) {
		synchronized(serviceScanCompletedEvent){
			try {
				logger.log(LOG_PREFIX + "Beginning Service Scan");
				UUID[] serviceUUID = new UUID[] {new UUID(uuid.replaceAll("-", ""), false)};
				DiscoveryAgent agent = LocalDevice.getLocalDevice().getDiscoveryAgent();
				agent.searchServices(null, serviceUUID, remoteDevice, new ServiceDiscoveryListener());
			} catch (BluetoothStateException e) {
				logger.logException(e);
			}
		}
	}
	
	public Map<String, String> getBluetoothServicesFound() {
		return bluetoothServiceURLsFound;
	}

	public List<RemoteDevice> getAllBluetoothClientsFound() {
		return allBluetoothClientsFound;
	}

	public String executeCommand(String command) {
		Process process;
		StringBuffer output = new StringBuffer();

		try {
			logger.log(LOG_PREFIX + "Executing ["+command+"] ...");
			process = Runtime.getRuntime().exec(command);
			process.waitFor();
			BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line = null;			
			while ((line = in.readLine()) != null) {
				output.append(line + "~~~~");
			}
		} catch (Exception e) {
			logger.logException(e);
			output.append("Error while executing native command");
		}

		return output.toString();
	}
	
	class ScanDiscoveryListener implements DiscoveryListener {
		
		@Override
	    public void deviceDiscovered(RemoteDevice device, DeviceClass cod) {
	    	String address = device.getBluetoothAddress();
	    	logger.log(LOG_PREFIX + "Device found @ " + address);
	    	currentScannedDevices.add(device);
	    	
			if (!allBluetoothClientsFound.contains(address)) {
				allBluetoothClientsFound.add(device);
			}
	    }
	
		@Override
	    public void inquiryCompleted(int discType) {
	    	logger.log(LOG_PREFIX + "Device Scan Complete");
	        synchronized(deviceScanCompletedEvent) {
	            deviceScanCompletedEvent.notifyAll();
	        }
	    }
	
		@Override
	    public void serviceSearchCompleted(int transID, int respCode) {}
		
		@Override
	    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {}    
	}
	
	class SearchDiscoveryListener implements DiscoveryListener {
		
		@Override
	    public void deviceDiscovered(RemoteDevice device, DeviceClass cod) {
	    	String address = device.getBluetoothAddress();
	        
        	if (address.equals(clientToSearchFor)) {
				clientFound = true;
				clientDevice = device;
			}
			
			if (!allBluetoothClientsFound.contains(address)) {
				allBluetoothClientsFound.add(device);
			}
	    }
	
		@Override
	    public void inquiryCompleted(int discType) {
	    	logger.log(LOG_PREFIX + "Device Search Complete");
	    	
	        synchronized(deviceSearchCompletedEvent) {
	            deviceSearchCompletedEvent.notifyAll();
	        }
	    }
	
		@Override
	    public void serviceSearchCompleted(int transID, int respCode) {}
	
		@Override
	    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {}    
	}
	
	class ServiceDiscoveryListener implements DiscoveryListener {

		@Override
		public void inquiryCompleted(int arg0) {}
		
		@Override
		public void deviceDiscovered(RemoteDevice arg0, DeviceClass arg1) {}

		@Override
		public void serviceSearchCompleted(int arg0, int arg1) {
			logger.log(LOG_PREFIX + "Service Scan Complete");
			synchronized (serviceScanCompletedEvent) {
				serviceScanCompletedEvent.notifyAll();
			}
		}

		@Override
		public void servicesDiscovered(int arg0, ServiceRecord[] services) {
			for (int i = 0; i < services.length; i++) {
				String url = services[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
				if (url == null) { 
					continue; 
				}
				logger.log(LOG_PREFIX + "Discovered Service @ " + url);
				
				ServiceRecord currentService = services[i];
				int[] attributes = currentService.getAttributeIDs();
				DataElement dataElement = currentService.getAttributeValue(attributes[1]);
				String dataElementString = ((Object) dataElement).toString();
				
				int startIndex = dataElementString.indexOf("UUID") + 5;
				int endIndex = startIndex + 33;
				String discoveredUUID = dataElementString.substring(startIndex, endIndex);
				
				bluetoothServiceURLsFound.put(discoveredUUID, url);
			}
		}
	}
}


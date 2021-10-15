package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

// New Imports
import java.util.concurrent.ConcurrentHashMap;
import net.floodlightcontroller.packet.MACAddress;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
//


/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	
	// ***NEW*** ConcurrentHashMap for storing packet entries
	private ConcurrentHashMap<String, SwitchEntry> switchTable;
	

	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
		// ***NEW*** part of init
		switchTable = new ConcurrentHashMap<String, SwitchEntry>();
		TableChecker checkerThread = new TableChecker(switchTable);
		checkerThread.start();
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
                etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* TODO: Handle packets                                             */
		
		/********************************************************************/
		// Check table entries to see if src exists in table
		MACAddress sourceMac = etherPacket.getSourceMAC();
		//System.out.printf("Checking Table for source MAC: %s\n", sourceMac);
		SwitchEntry retrievedEntry = switchTable.get(sourceMac.toString());
		System.out.println("UPDATING SWITCH TABLE");
		if (retrievedEntry != null) {
			System.out.println("Refreshing table entry.");
			System.out.printf("Found entry key: %s\n", sourceMac.toString());
			// If the entry exists, reset the time
			long curTime = System.currentTimeMillis();
			System.out.printf("Setting time to: %d\n", curTime);
			retrievedEntry.setTimeStamp(curTime);
			System.out.printf("Setting Iface to inIface: %s\n", inIface.getName());
			retrievedEntry.setIfNum(inIface);
		} else {
			System.out.println("Adding new table entry.");
			long curTime = System.currentTimeMillis();
			SwitchEntry newEntry = new SwitchEntry(inIface, curTime);
			System.out.printf("New Entry key: %s\n", sourceMac.toString());
			System.out.printf("New Entry time to: %d\n", curTime);
			System.out.printf("New Entry Iface to inIface: %s\n", inIface.getName());
			switchTable.put(sourceMac.toString(), newEntry);
		}
		
		// Check if table contains the current dest MAC
		System.out.println("USING SWITCH TABLE TO SEND PACKET");
		MACAddress destMac = etherPacket.getDestinationMAC();
		System.out.printf("destMac: %s\n", destMac.toString());
		retrievedEntry = switchTable.get(destMac.toString());
		if (retrievedEntry != null) {
			// Dest MAC exists in table
			System.out.printf("Entry Found. Sending on %s\n", retrievedEntry.getIfNum().getName());
			sendPacket(etherPacket, retrievedEntry.getIfNum());
		}
		else {
			System.out.println("Entry Not Found. Broadcasting.");
			// Dest MAC does not exist in table -> broadcast
			HashMap<String, Iface> interfaces = (HashMap)getInterfaces();
			int numFail = 0;
			for(Map.Entry<String, Iface> if_entry : interfaces.entrySet()) {
				if(!if_entry.getValue().equals(inIface)) {
					// Do broadcast to this interface (DEST)
					System.out.printf("Broadcasting on: %s\n", if_entry.getValue().getName());
					if(!sendPacket(etherPacket, if_entry.getValue())){
						numFail++;
					}
				}
			}
		}
		
	}
	
	
	class SwitchEntry {
		private Iface ifNum;
		private long timeStamp;
		
		SwitchEntry(Iface ifNum, long timeStamp) {
			System.out.println("NEW ENTRY:");
			System.out.printf("ifNum: %h\n", ifNum);
			System.out.printf("timeStamp: %d\n", timeStamp);
			this.ifNum = ifNum;
			this.timeStamp = timeStamp;
		}
		
		Iface getIfNum() {
			return ifNum;
		}
		
		void setIfNum(Iface ifNum) {
			this.ifNum = ifNum;
		}
		
		long getTimeStamp() {
			return timeStamp;
		}
		
		void setTimeStamp(long timeStamp) {
			this.timeStamp = timeStamp;
		}
	}

	class TableChecker extends Thread {
		
		private final ConcurrentHashMap<String, SwitchEntry> switchTable;
		
		public TableChecker(ConcurrentHashMap<String, SwitchEntry> switchTable) {
			this.switchTable = switchTable;
		}
		
		public void run() {
			System.out.println("STARTING THREAD");
			while (true) {
				for (Map.Entry<String, SwitchEntry> entry : switchTable.entrySet()) {
					String key = entry.getKey();
					SwitchEntry value = entry.getValue();
					if((System.currentTimeMillis() - value.getTimeStamp()) > 15000 ){
						System.out.println("REMOVING ENTRY");
						System.out.printf("Removed macAddr: %s\n", key);
						System.out.printf("Removed value: %h\n", value);
						System.out.printf("Removed ifNum: %s\n", value.getIfNum().getName());
						switchTable.remove(key, value);
					}
					
				}
			}
		}
		
	}
	
}



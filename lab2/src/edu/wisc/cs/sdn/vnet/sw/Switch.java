package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

// New Imports
import java.util.concurrent.ConcurrentHashMap;
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
	private ConcurrentHashMap<byte[], SwitchEntry> switchTable;
	

	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
		// ***NEW*** part of init
		switchTable = new ConcurrentHashMap<byte[], SwitchEntry>();
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
		byte[] sourceMac = etherPacket.getSourceMACAddress();
		SwitchEntry retrievedEntry = switchTable.get(sourceMac);
		if (retrievedEntry != null) {
			// If the entry exists, reset the time
			long curTime = System.currentTimeMillis();
			System.out.printf("Setting time to: %d\n", curTime);
			retrievedEntry.setTimeStamp(curTime);
			System.out.printf("Setting Iface to inIface: %h\n", inIface);
			retrievedEntry.setIfNum(inIface);
		} else {
			SwitchEntry newEntry = new SwitchEntry(inIface, System.currentTimeMillis());
			switchTable.put(sourceMac, newEntry);
		}
		
		// Check if table contains the current dest MAC
		byte[] destMac = etherPacket.getDestinationMACAddress();
		retrievedEntry = switchTable.get(destMac);
		if (retrievedEntry != null) {
			// Dest MAC exists in table
			System.out.printf("Entry Found. Sending on %h\n", retrievedEntry.getIfNum());
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
					System.out.printf("Broadcasting on: %h\n", if_entry.getValue());
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
			System.out.println("NEW ENTRY");
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
		
		private final ConcurrentHashMap<byte[], SwitchEntry> switchTable;
		
		public TableChecker(ConcurrentHashMap<byte[], SwitchEntry> switchTable) {
			this.switchTable = switchTable;
		}
		
		public void run() {
			System.out.println("STARTING THREAD");
			while (true) {
				for (Map.Entry<byte[], SwitchEntry> entry : switchTable.entrySet()) {
					byte[] key = entry.getKey();
					SwitchEntry value = entry.getValue();
					if((System.currentTimeMillis() - value.getTimeStamp()) > 15000 ) {
						switchTable.remove(key, value);
					}
					
				}
			}
		}
		
	}
	
}



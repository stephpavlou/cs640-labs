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
		// Here we initialize the forward table
		switchTable = new ConcurrentHashMap<String, SwitchEntry>();
		
		// Here we initialize and start the thread that checks the forward table
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
		
		// Check table entries to see if src MAC exists in table
		MACAddress sourceMac = etherPacket.getSourceMAC();
		SwitchEntry retrievedEntry = switchTable.get(sourceMac.toString());
		if (retrievedEntry != null) {

			// If the entry exists, reset the time, and update the Iface in case of a change
			long curTime = System.currentTimeMillis();
			retrievedEntry.setTimeStamp(curTime);
			retrievedEntry.setIfNum(inIface);
		} else {
			
			// If the entry does not exist, make a new entry
			long curTime = System.currentTimeMillis();
			SwitchEntry newEntry = new SwitchEntry(inIface, curTime);
			switchTable.put(sourceMac.toString(), newEntry);
		}
		
		// Check if Forwarding table contains the current dest MAC
		MACAddress destMac = etherPacket.getDestinationMAC();
		retrievedEntry = switchTable.get(destMac.toString());
		if (retrievedEntry != null) {
			
			// Dest MAC exists in forwarding table, send the packet on interface in table entry
			sendPacket(etherPacket, retrievedEntry.getIfNum());
		}
		else {
			
			// Dest MAC does not exist in table -> broadcast on all interfaces except the interface
			// that the packet was received on
			HashMap<String, Iface> interfaces = (HashMap)getInterfaces();
			int numFail = 0;
			
			// This loop iterates over all interfaces of this switch
			for(Map.Entry<String, Iface> if_entry : interfaces.entrySet()) {
				if(!if_entry.getValue().equals(inIface)) {
					
					// Do broadcast to this interface (DEST)
					if(!sendPacket(etherPacket, if_entry.getValue())){
						numFail++;
					}
				}
			}
		}
		
	}
	
	/**
	*  class: SwitchEntry
	* SwitchEntry is an object used to help store information in the forwarding table.
	* The interface number and the timestamp are stored in the contents of this object.
	* SwitchEntries are intended to be used as values in a hashtable, with the key being
	* the MAC address corresponding to the interface and timestamp. This class also includes
	* some helper setter and getter functions to access provate fields.
	*
	**/
	
	
	class SwitchEntry {
		private Iface ifNum;
		private long timeStamp;
		
		
		/**
		* This is the constructor for the class, it assigns the arguments to the private fields.
		*
		*
		**/
		SwitchEntry(Iface ifNum, long timeStamp) {
			this.ifNum = ifNum;
			this.timeStamp = timeStamp;
		}
		
		/**
		* The following functions are standard setter and getter functions
		* meant to assist in using the private fields of the class.
		*
		**/
		
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

	/**
	*	class TableChecker:
	* 	This class is meant to be run as a helper thread that will look throught
	*	the forward table, and remove any entries that are older than 15 seconds.
	*
	**/

	class TableChecker extends Thread {
		
		// This is used for the thread to haver access to the forward table
		private final ConcurrentHashMap<String, SwitchEntry> switchTable;
		
		
		// This is the constructor for the class, it allows the forward table to be passed in
		public TableChecker(ConcurrentHashMap<String, SwitchEntry> switchTable) {
			this.switchTable = switchTable;
		}
		
		
		// This is the code run by the thread, which continuously searches the table for 
		// entries older than 15 seconds. If the thread finds such an entry, it removes the entry
		// from the list.
		public void run() {
			while (true) {
				for (Map.Entry<String, SwitchEntry> entry : switchTable.entrySet()) {
					String key = entry.getKey();
					SwitchEntry value = entry.getValue();
					if((System.currentTimeMillis() - value.getTimeStamp()) > 15000 ){
						switchTable.remove(key, value);
					}
					
				}
			}
		}
		
	}
	
}



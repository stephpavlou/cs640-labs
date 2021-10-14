package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

// New Imports
import java.util.concurrent.ConcurrentHashMap;
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
		byte[] sourceMac = getSourceMAC(etherPacket);
		SwitchEntry retrievedEntry = switchTable.get(sourceMac);
		if (retrievedEntry != null) {
			// If the entry exists, reset the time
			retrievedEntry.setTimeStamp(System.currentTimeMillis());
			retrievedEntry.setIfNum(inIface);
		} else {
			SwitchEntry newEntry = new SwitchEntry(inIface, System.currentTimeMillis());
			switchTable.put(sourceMac, newEntry);
		}
		
		// Check if table contains the current dest MAC
		byte[] destMac = getDestinationMAC(etherPacket);
		retrievedEntry = switchTable.get(destMac);
		if (retrievedEntry != null) {
			// Dest MAC exists in table
			sendPacket(etherPacket, retrievedEntry.getIfNum());
		}
		else {
			// Dest MAC does not exist in table -> broadcast
			HashMap<String, Iface> interfaces = getInterfaces();
			int numFail = 0;
			for(Map.Entry<String, IFace> if_entry : iterfaces.entrySet()) {
				if(if_entry.getValue().compareTo(inIface) < 0) {
					// Do broadcast to this interface (DEST)
					if(!sendPacket(etherPacket, if_entry.getValue())){
						numFail++;
					}
				}
			}
		}
		
	}
}


private class SwitchEntry {
	private Iface ifNum;
	private long timeStamp;
	
	SwitchEntry(int ifnum, int timeStamp) {
		this.ifNum = ifNum;
		this.timeStamp = timeStamp;
	}
	
	Iface getIfNum() {
		return this.ifNum
	}
	
	void setIfNum(int inIfNum) {
		this.ifNum = inIfNum;
	}
	
	int getTimeStamp() {
		return this.ttl;
	}
	
	void setTimeStamp(int inTtl) {
		this.ttl = inTtl;
	}
}
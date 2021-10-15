package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

// New Imports
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
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
		new Thread(new TableChecker(switchTable));
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
			SwitchEntry newEntry = new SwitchEntry(sourceMac, inIface, System.currentTimeMillis());
			switchTable.put(sourceMac, newEntry);
		}
		
		// Check if table contains the current dest MAC
		byte[] destMac = etherPacket.getDestinationMACAddress();
		retrievedEntry = switchTable.get(destMac);
		if (retrievedEntry != null) {
			// Dest MAC exists in table
			sendPacket(etherPacket, retrievedEntry.getIfNum());
		}
		else {
			// Dest MAC does not exist in table -> broadcast
			//HashMap<String, Iface> interfaces = getInterfaces();
			//int numFail = 0;
			//for(Map.Entry<String, IFace> if_entry : iterfaces.entrySet()) {
			//	if(if_entry.getValue().compareTo(inIface) < 0) {
			//		// Do broadcast to this interface (DEST)
			//		if(!sendPacket(etherPacket, if_entry.getValue())){
			//			numFail++;
			//		}
			//	}
			//}
			int numFail = 0;
			System.out.printf("switchTable.isEmpty(): %b\n", switchTable.isEmpty());
			if(!switchTable.isEmpty()) {
				for (Map.Entry<byte[], SwitchEntry> entry : switchTable.entrySet()) {
					System.out.printf("entry: %h\n",entry);
					byte[] key = entry.getKey();
					SwitchEntry value = entry.getValue();
					System.out.printf("key: %h\n",key);
					System.out.printf("value: %h\n", value);
					Iface ifNum = value.getIfNum();
					System.out.printf("ifNum: %h\n", ifNum);
					long entryTime
					if(!ifNum.equals(inIface)) {
						if(!sendPacket(etherPacket, value.getIfNum())) {
							numFail++;
						}
					}
				
				}
			}
		}
		
	}
	
	
	class SwitchEntry {
		private byte[] macAddr;
		private Iface ifNum;
		private long timeStamp;
		
		SwitchEntry(byte[] macAddr, Iface ifNum, long timeStamp) {
			System.out.println("NEW ENTRY");
			System.out.printf("macAddr: %h\n", macAddr);
			System.out.printf("ifnum: %h\n", ifNum);
			System.out.printf("timeStamp: %h\n", timeStamp);
			this.macAddr = macAddr;
			this.ifNum = ifNum;
			this.timeStamp = timeStamp;
		}
		
		byte[] getMacAddr () {
			return macAddr;
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



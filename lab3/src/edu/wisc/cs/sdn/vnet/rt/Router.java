package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.MACAddress;
import net.floodlightcontroller.packet.RIPv2;
import net.floodlightcontroller.packet.RIPv2Entry;
import net.floodlightcontroller.packet.UDP;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/** queue for packets */
	private ConcurrentHashMap<Integer, Queue> packetQueues;
	
	// Plasce to hold RIP requester and checker thread
	private RIPResponder ripManager;
	
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
		
		// This sets up a way to store queues for packets while sending ARP requests
		this.packetQueues = new ConcurrentHashMap<Integer, Queue>();
		
	}
	
	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }
	
	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}
	
	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
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
		
		switch(etherPacket.getEtherType())
		{
		case Ethernet.TYPE_IPv4:
			IPv4 ip = (IPv4)etherPacket.getPayload();
			
			// TODO: Determine how to receive Solicited responses (that have a direct IP address)
			System.out.printf("ip.getDestinationAddress(): %d\n", ip.getDestinationAddress());
			System.out.printf("IPv4.toIPv4Address(\"224.0.0.9\"): %d\n", IPv4.toIPv4Address("224.0.0.9"));
			System.out.printf("ip.getProtocol(): %d\n", ip.getProtocol());
			System.out.printf("IPv4.PROTOCOL_UDP: %d\n", IPv4.PROTOCOL_UDP);
			if((ip.getProtocol() == IPv4.PROTOCOL_UDP)){
				UDP udp = (UDP)ip.getPayload();
				System.out.printf("udp.getDestinationPort(): %d\n",udp.getDestinationPort());
				System.out.printf("(short)520: %d\n", (short)520);
				if(udp.getDestinationPort() == (short)520) {
					System.out.println("Received Packet is a RIP packet");
					handleRipPacket(etherPacket, inIface);
					break;
				}
			}
			this.handleIpPacket(etherPacket, inIface);
			break;
		case Ethernet.TYPE_ARP:
			this.handleArpPacket(etherPacket, inIface);
			break;
		// Ignore all other packet types, for now
		}
		
		/********************************************************************/
	}
	
	private void handleIpPacket(Ethernet etherPacket, Iface inIface)
	{
		// Make sure it's an IP packet
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4) { 
			return; 	
		} 
		
		// Get IP header
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
        System.out.println("Handle IP packet");

        // Verify checksum
        short origCksum = ipPacket.getChecksum();
        ipPacket.resetChecksum();
        byte[] serialized = ipPacket.serialize();
        ipPacket.deserialize(serialized, 0, serialized.length);
        short calcCksum = ipPacket.getChecksum();
        if (origCksum != calcCksum)
        { return; }
        
        // Check TTL
        ipPacket.setTtl((byte)(ipPacket.getTtl()-1));
        if (0 == ipPacket.getTtl()) { 
			ICMPmake(etherPacket, inIface, (byte)11, (byte)0);
			return; 
		}
        
        // Reset checksum now that TTL is decremented
        ipPacket.resetChecksum();
        
        // Check if packet is destined for one of router's interfaces
        for (Iface iface : this.interfaces.values())
        {
        	if (ipPacket.getDestinationAddress() == iface.getIpAddress()) { 
				
				if ((ipPacket.getProtocol() == IPv4.PROTOCOL_TCP) || (ipPacket.getProtocol() == IPv4.PROTOCOL_UDP)) {
					ICMPmake(etherPacket, inIface, (byte)3, (byte)3);
				} else if(ipPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
					if (((ICMP)ipPacket.getPayload()).getIcmpType() == (byte)8) {
						ICMPmake(etherPacket, inIface, (byte)0, (byte)0);
					}
				}
				return; 
			}
        }
		
        // Do route lookup and forward
        this.forwardIpPacket(etherPacket, inIface);
	}

    private void forwardIpPacket(Ethernet etherPacket, Iface inIface)
    {
        // Make sure it's an IP packet
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
		{ return; }
        System.out.println("Forward IP packet");
		
		// Get IP header
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
        int dstAddr = ipPacket.getDestinationAddress();

        // Find matching route table entry 
		System.out.println("Forward IP packet lookup");
        RouteEntry bestMatch = this.routeTable.lookup(dstAddr);

        // If no entry matched, do nothing
        if (null == bestMatch) { 
			ICMPmake(etherPacket, inIface, (byte)3, (byte)0);
			return; 
		}

        // Make sure we don't sent a packet back out the interface it came in
        Iface outIface = bestMatch.getInterface();
        if (outIface == inIface)
        { return; }

        // Set source MAC address in Ethernet header
        etherPacket.setSourceMACAddress(outIface.getMacAddress().toBytes());

        // If no gateway, then nextHop is IP destination
        int nextHop = bestMatch.getGatewayAddress();
        if (0 == nextHop)
        { nextHop = dstAddr; }

        // Set destination MAC address in Ethernet header
		System.out.printf("dstAddr: %d\n", dstAddr);
		System.out.printf("nextHop: %d\n", nextHop);
		checkARPCache(etherPacket, inIface, nextHop, outIface);
    }
	
	private void SendARPRequest(Ethernet etherPacket, Iface inIface, int nextHop) {
		
		// Broadcast ARP request out of all interfaces (including interface original packet
		// was received on, for case of empty ARPCache on startup)
		HashMap<String, Iface> interfaces = (HashMap)getInterfaces();
		for(Map.Entry<String, Iface> if_entry : interfaces.entrySet()) {
				//if(!if_entry.getValue().equals(inIface)) {
					
					Ethernet ether = new Ethernet();
					ARP arp = new ARP();
					ether.setPayload(arp);
					
					// Ethernet stuff
					// Set Ethernet Type
					ether.setEtherType(Ethernet.TYPE_ARP);
					
					// Set Source MAC
					ether.setSourceMACAddress(if_entry.getValue().getMacAddress().toBytes());
					
					// Set Destination MAC
					ether.setDestinationMACAddress("FF:FF:FF:FF:FF:FF");
					
					// ARP stuff
					// Set Hardware Type
					arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
					
					// Set Protocol Type
					arp.setProtocolType(ARP.PROTO_TYPE_IP);
					
					// Set Hardware address length
					arp.setHardwareAddressLength((byte)Ethernet.DATALAYER_ADDRESS_LENGTH);
					
					// Set Protocol addr length
					arp.setProtocolAddressLength((byte)4);
					
					// Set Opcode
					arp.setOpCode(ARP.OP_REQUEST);
					
					// Set sender Hardware address
					arp.setSenderHardwareAddress(if_entry.getValue().getMacAddress().toBytes());
					
					// Set Sender Protocol Address
					arp.setSenderProtocolAddress(if_entry.getValue().getIpAddress());
					
					// Set Target Hardware Address
					byte[] empty = new byte[arp.getHardwareAddressLength()];
					for (int i = 0; i < arp.getHardwareAddressLength(); i++) {
						empty[i] = 0;
					}			
					arp.setTargetHardwareAddress(empty);
					
					// Set Target protocol Address
					arp.setTargetProtocolAddress(nextHop);

					// Do broadcast to this interface (DEST)
					System.out.println("SENDING ARP REQUEST");
					System.out.println(ether.toString());
					System.out.printf("Iface: %s", if_entry.getValue().toString());
					this.sendPacket(ether, if_entry.getValue());
					
				//}
			}
		
	}
	
	
	private void RIPmake(Ethernet etherPacket, Iface outIface, boolean isResponse, boolean isSolicited) {
		Ethernet ether = new Ethernet();
		IPv4 ip = new IPv4();
		UDP udp = new UDP();
		RIPv2 rip = new RIPv2();
		
		ether.setPayload(ip);
		ip.setPayload(udp);
		udp.setPayload(rip);
		
		// Ethernet Setup
		// Set destination MAC
		if((isResponse) && (isSolicited)) {
			ether.setDestinationMACAddress(etherPacket.getSourceMACAddress());
		} else {
			ether.setDestinationMACAddress("FF:FF:FF:FF:FF:FF");
		}
		
		// Set source MAC
		ether.setSourceMACAddress(outIface.getMacAddress().toBytes());
		
		// Set Ether Type
		ether.setEtherType(Ethernet.TYPE_IPv4);
		
	
		// IP setup
		// Set TTl
		ip.setTtl((byte)15);
		
		// Set Protocol
		ip.setProtocol(IPv4.PROTOCOL_UDP);
		
		// Set Source Address
		ip.setSourceAddress(outIface.getIpAddress());
		
		// Set Destination Address
		if ((isResponse) && (isSolicited)) {
			ip.setDestinationAddress(((IPv4)etherPacket.getPayload()).getSourceAddress());
		} else {
			ip.setDestinationAddress("224.0.0.9");
		}
		
		// UDP setup
		// Set up Source Port
		udp.setSourcePort((short)520);
		
		// Set up Destination Port
		udp.setDestinationPort((short)520);
		
		
		// RIP setup
		// Set Command
		if(isResponse) {
			// Response
			rip.setCommand(RIPv2.COMMAND_RESPONSE);
			
			// add on a RIPv2 entry for each table entry
			for (RouteEntry entry: routeTable.getEntries()) {
				RIPv2Entry ripEntry = new RIPv2Entry(entry.getDestinationAddress(), entry.getMaskAddress(), entry.getMetric());
				rip.addEntry(ripEntry);
			}
			
		} else {
			rip.setCommand(RIPv2.COMMAND_REQUEST);
		}
		
		this.sendPacket(ether, outIface);
	}
	
	
	private void ICMPmake(Ethernet etherPacket, Iface inIface, byte icmpType, byte icmpCode) {
		Ethernet ether = new Ethernet();
		IPv4 ip = new IPv4();
		ICMP icmp = new ICMP();
		Data data = new Data();
		ether.setPayload(ip);
		ip.setPayload(icmp);
		icmp.setPayload(data);
		
		// Ethernet Header Stuff
		// Setting Ethernet type
		ether.setEtherType(Ethernet.TYPE_IPv4);
		
		// Setting Source MAC address
		RouteEntry sourceEntry = routeTable.lookup(((IPv4)etherPacket.getPayload()).getSourceAddress());
		if(sourceEntry == null) {
			return;
		}
		Iface sourceIface = sourceEntry.getInterface();
		ether.setSourceMACAddress(sourceIface.getMacAddress().toBytes());
		//ether.setSourceMACAddress(inIface.getMacAddress.toBytes());
		
		// Setting Destination MAC address
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
		RouteEntry bestMatch = routeTable.lookup(ipPacket.getSourceAddress());
		int nextHop = bestMatch.getGatewayAddress();
        if (0 == nextHop)
        { nextHop = ipPacket.getSourceAddress(); }
		
		// IP Header Stuff
		// Set TTL
		ip.setTtl((byte)64);
		
		// Set ProtocolException
		ip.setProtocol(IPv4.PROTOCOL_ICMP);
		
		// Set Source IP
		if(icmpType == 0) {
			ip.setSourceAddress(ipPacket.getDestinationAddress());
		} else {
			ip.setSourceAddress(sourceIface.getIpAddress());
		}
		
		// Set Dest IP address
		ip.setDestinationAddress(ipPacket.getSourceAddress());
		
		// ICMP stuff
		// Set ICMP type
		icmp.setIcmpType(icmpType);
		
		// Set ICMP code
		icmp.setIcmpCode(icmpCode);
		
		// Setting Data for ICMP payload
		if(icmpType == 0) {
			data.setData(((ICMP)ipPacket.getPayload()).getPayload().serialize());
		} else {
			int origHeaderLen = (ipPacket.getHeaderLength()) * 4;
			short origTotalLen = ipPacket.getTotalLength();
			byte[] newData = new byte[4 + origHeaderLen + 8];
			byte[] oldIpHeader = ipPacket.serialize();
			for(int i = 4; i < 4 + origHeaderLen + 8; i++) {
				newData[i] = oldIpHeader[i - 4];
			}
			data.setData(newData);
		}
		System.out.println("MAKING ICMP MESSAGE");
		checkARPCache(ether, inIface, nextHop, bestMatch.getInterface());
	}
	
	private void checkARPCache(Ethernet etherPacket, Iface inIface, int nextHop, Iface outIface) {
		ArpEntry arpEntry = this.arpCache.lookup(nextHop);
        if (null == arpEntry) { 
			if(!packetQueues.containsKey(nextHop)) {
				System.out.println("Making New Queue");
				ARPRequester newRequester = new ARPRequester(packetQueues, etherPacket, inIface, nextHop);				
				Queue newQueue = new Queue(newRequester);
				newQueue.queuedPackets.put(etherPacket.getSourceMAC().toString(), new QueuePacket(etherPacket, inIface));
				packetQueues.put(nextHop, newQueue);
				newRequester.start();
				
			} else {
				System.out.println("Adding packet to Queue");
				Queue curQueue = packetQueues.get(nextHop);
				curQueue.queuedPackets.put(etherPacket.getSourceMAC().toString(), new QueuePacket(etherPacket, inIface));
			}
		} else {
			etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());
			System.out.println("FORWARDING IP PACKET:");
			System.out.println(etherPacket.toString());
			this.sendPacket(etherPacket, outIface);
		}
	}
	
	private void handleRipPacket(Ethernet etherPacket, Iface inIface) {
		System.out.println("Handle RIP packet");
		// Breakdown Breakdown
		IPv4 ip = (IPv4)etherPacket.getPayload();
		UDP udp = (UDP)ip.getPayload();
		RIPv2 rip = (RIPv2)udp.getPayload();
		if (rip.getCommand() == RIPv2.COMMAND_REQUEST) {
			// This is a request, send RIP response
			RIPmake(etherPacket, inIface, true, true);
		} else if(rip.getCommand() == RIPv2.COMMAND_RESPONSE) {
			// This is a response, update table
			List<RIPv2Entry> ripEntries = rip.getEntries();
			for(RIPv2Entry ripEntry : ripEntries) {
				
				RouteEntry bestEntry = routeTable.find(ripEntry.getAddress(), ripEntry.getSubnetMask());
				
				if(bestEntry == null) {
					
					// This entry Doesn't exist, add to route table
					System.out.println("Handling RIP Response, Adding new Entry");
					routeTable.insert(ripEntry.getAddress(), ip.getSourceAddress(), ripEntry.getSubnetMask(), inIface, ripEntry.getMetric() + 1, false);
				} else {
					// Entry is in table, check if needs to be updated
					System.out.println("Handling RIP Packet, entry match Found:");
					System.out.println(ripEntry.toString());
					System.out.println(bestEntry.toString());
					if ((bestEntry.getMetric() > (ripEntry.getMetric() + 1)) || (bestEntry.getGatewayAddress() == ip.getSourceAddress())) {
						System.out.println("Handling RIP Response, Updating Entry");
						// Found a better path, update entry
						bestEntry.setGatewayAddress(ip.getSourceAddress());
						bestEntry.setMetric(ripEntry.getMetric() + 1);
						bestEntry.setInterface(inIface);
						bestEntry.setUpdateTime();
					}
				}
			}
		}
		
		
	}
	
	public void loadAllIfaces() {
		HashMap<String, Iface> interfaces = (HashMap)getInterfaces();
		System.out.println("Adding interfaces to RouteTable Entries!");
		for(Map.Entry<String, Iface> if_entry : interfaces.entrySet()) {
			Iface curIface = if_entry.getValue();
			System.out.printf("DestAddresss: %d\n", (curIface.getIpAddress() & curIface.getSubnetMask()));
			System.out.printf("Gateway: %d\n", 0);
			System.out.printf("Subnet Mask: %d\n", curIface.getSubnetMask());
			System.out.printf("Interface: %s\n", if_entry.getValue().toString());
			System.out.printf("Metric: %d\n", 0);
			System.out.printf("directlyConnected: %b\n", true);
			routeTable.insert((curIface.getIpAddress() & curIface.getSubnetMask()), 0, curIface.getSubnetMask(), if_entry.getValue(), 0, true);
		}
		ripManager = new RIPResponder(routeTable);
		ripManager.start();
	}
	
	private void handleArpPacket(Ethernet etherPacket, Iface inIface) {
		System.out.println("Handling ARP Packet!");
		// Make sure it's an ARP packet
		System.out.printf("EtherType: %d", etherPacket.getEtherType());
		System.out.printf("ArpType: %d", Ethernet.TYPE_ARP);
		if (etherPacket.getEtherType() != Ethernet.TYPE_ARP) { 
			return; 	
		} 
		
		ARP arpPacket = (ARP)etherPacket.getPayload();
		int targetIp = ByteBuffer.wrap(arpPacket.getTargetProtocolAddress()).getInt();
		if ((arpPacket.getOpCode() == ARP.OP_REQUEST) && (targetIp == inIface.getIpAddress())) {
			
			// System.out.println("MAKING ARP REPLY!");
			// Make ARP reply packet
			Ethernet ether = new Ethernet();
			ARP arp = new ARP();
			ether.setPayload(arp);
			
			// Ethernet stuff
			// Set Ethernet Type
			ether.setEtherType(Ethernet.TYPE_ARP);
			
			// Set Source MAC
			ether.setSourceMACAddress(inIface.getMacAddress().toBytes());
			
			// Set Destination MAC
			ether.setDestinationMACAddress(etherPacket.getSourceMACAddress());
			
			// ARP stuff
			// Set Hardware Type
			arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
			
			// Set Protocol Type
			arp.setProtocolType(ARP.PROTO_TYPE_IP);
			
			// Set Hardware address length
			arp.setHardwareAddressLength((byte)Ethernet.DATALAYER_ADDRESS_LENGTH);
			
			// Set Protocol addr length
			arp.setProtocolAddressLength((byte)4);
			
			// Set Opcode
			arp.setOpCode(ARP.OP_REPLY);
			
			// Set sender Hardware address
			arp.setSenderHardwareAddress(inIface.getMacAddress().toBytes());
			
			// Set Sender Protocol Address
			arp.setSenderProtocolAddress(inIface.getIpAddress());
			
			// Set Target Hardware Address
			arp.setTargetHardwareAddress(arpPacket.getSenderHardwareAddress());
			
			// Set Target protocol Address
			arp.setTargetProtocolAddress(arpPacket.getSenderProtocolAddress());
			
			
			// System.out.println("ORIGINAL PACKET");
			// System.out.println(etherPacket.toString());
			// System.out.println("NEW PACKET");
			// System.out.println(ether.toString());
			
			this.sendPacket(ether, inIface);
		}
		System.out.printf("OpCode: %d",arpPacket.getOpCode());
		System.out.printf("OP_REPLY: %d",ARP.OP_REPLY);
		System.out.printf("targetIP: %d", targetIp);
		System.out.printf("InterfaceIp: %d", inIface.getIpAddress());
		
		if ((arpPacket.getOpCode() == ARP.OP_REPLY) && (targetIp == inIface.getIpAddress())) {
			System.out.println("ARP REPLY RECEIVED");
			int ipAddr = ByteBuffer.wrap(arpPacket.getSenderProtocolAddress()).getInt();
			MACAddress macAddr = new MACAddress(arpPacket.getSenderHardwareAddress());
			arpCache.insert(macAddr, ipAddr);
			
			// Clear queue
			Queue curQueue = packetQueues.get(ipAddr);
			if(curQueue == null) {
				System.out.println("curQueue is null");
				return;
			}
			
			// Signal thread that the IP address was found
			curQueue.requester.found();
			ConcurrentHashMap<String, QueuePacket> queuedPackets = curQueue.queuedPackets;
			for (Map.Entry<String, QueuePacket> entry: queuedPackets.entrySet()) {
				QueuePacket value = entry.getValue();
				value.ether.setDestinationMACAddress(macAddr.toBytes());
				System.out.println("SENDING OUT PACKET FROM QUEUE");
				System.out.printf(value.ether.toString());
				System.out.printf("Iface: %s\n", inIface.toString());
				if(!sendPacket(value.ether, inIface)) {
					System.out.println("ERROR SENDING PACKET");
				}
				queuedPackets.remove(entry.getKey());
			}
			packetQueues.remove(ipAddr);
			System.out.println("ARP REPLY COMPLETED");
		}
		
	}
	
	
	private class QueuePacket {
		Ethernet ether;
		Iface inIface;
		
		public QueuePacket (Ethernet ether, Iface inIface) {
			this.ether = ether;
			this.inIface = inIface;
		}
	}
	private class Queue {
		ARPRequester requester;
		// Use this instead of a linked list as this is meant to be used
		// for concurrency
		ConcurrentHashMap<String, QueuePacket> queuedPackets;
		
		public Queue(ARPRequester newRequester) {
			this.requester = newRequester;
			this.queuedPackets = new ConcurrentHashMap<String, QueuePacket>();
		}
	}
	
	
	class RIPResponder extends Thread {
		private RouteTable routeTable;
		public RIPResponder(RouteTable routeTable) {
			this.routeTable = routeTable;
		}
		
		public void run() {
			long prevCheckTime = System.currentTimeMillis();
			
			// Send RIP request on startup
			HashMap<String, Iface> interfaces = (HashMap)getInterfaces();
			for(Map.Entry<String, Iface> if_entry : interfaces.entrySet()) {
					RIPmake(null, if_entry.getValue(), false, false);
			}
			
			while (true) {
				
				if((System.currentTimeMillis() - prevCheckTime) >= 10000) {
					for(Map.Entry<String, Iface> if_entry : interfaces.entrySet()) {
						if(if_entry == null) {
							System.out.println("IN RIPRESPONDER: if_enty == null");
						} else if(if_entry.getValue() == null) {
							System.out.println("IN RIPRESPONDER: if_enty.getValue() == null");
						}
						RIPmake(null, if_entry.getValue(), true, false);
					}
					prevCheckTime = System.currentTimeMillis();
				}
				
				
				for(RouteEntry entry: routeTable.getEntries()) {
					if(!entry.isDirectlyConnected()) {
						if((System.currentTimeMillis() - entry.getUpdateTime()) >= 30000){
							routeTable.remove(entry.getDestinationAddress(), entry.getMaskAddress());
						}
					}
				}
								
				
				try {
					this.sleep((long)1000);
				} catch (Exception ex) {
				}
			}
		}	
	}
	
	
	class ARPRequester extends Thread {
		private volatile boolean found;
		private boolean timeout;
		private ConcurrentHashMap<Integer, Queue> packetQueues;
		private int numPackets;
		private Ethernet etherPacket;
		private Iface inIface;
		private int nextHop;
		
		public ARPRequester(ConcurrentHashMap<Integer, Queue> packetQueues, 
			Ethernet etherPacket, Iface inIface, int nextHop) {
				
			this.found = false;
			this.timeout = false;
			this.packetQueues = packetQueues;
			this.numPackets = 0;
			this.etherPacket = etherPacket;
			this.inIface = inIface;
			this.nextHop = nextHop;
		}
		
		public void found() {
			this.found = true;
		}
		
		public void run() {
			while ((!found) && (!timeout)) {
				SendARPRequest(etherPacket, inIface, nextHop);
				numPackets++;
				try {
					this.sleep((long)1000);
				} catch (Exception ex) {
				}
				if(numPackets == 3) {
					// Remove Queue
					timeout = true;
				}
			}
			if (!found) {
				// Remove queued packets
				Queue curQueue = packetQueues.get(nextHop);
				ConcurrentHashMap<String, QueuePacket> queuedPackets = curQueue.queuedPackets;
				for (Map.Entry<String, QueuePacket> entry: queuedPackets.entrySet()) {
					ICMPmake(entry.getValue().ether, entry.getValue().inIface, (byte)3, (byte)1);
					queuedPackets.remove(entry.getKey());
				}
				packetQueues.remove(nextHop);
			}	
		}
		
		
	}
}

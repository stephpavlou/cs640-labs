package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.ARP;
import java.nio.ByteBuffer;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
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
        ArpEntry arpEntry = this.arpCache.lookup(nextHop);
        if (null == arpEntry) { 
			ICMPmake(etherPacket, inIface, (byte)3, (byte)1);
			return; 
		}
        etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());
        
        this.sendPacket(etherPacket, outIface);
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
		Iface sourceIface = sourceEntry.getInterface();
		ether.setSourceMACAddress(sourceIface.getMacAddress().toBytes());
		//ether.setSourceMACAddress(inIface.getMacAddress.toBytes());
		
		// Setting Destination MAC address
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
		RouteEntry bestMatch = this.routeTable.lookup(ipPacket.getSourceAddress());
		int nextHop = bestMatch.getGatewayAddress();
        if (0 == nextHop)
        { nextHop = ipPacket.getSourceAddress(); }
		ArpEntry arpEntry = this.arpCache.lookup(nextHop);
        if (null == arpEntry)
        { return; }
		ether.setDestinationMACAddress(arpEntry.getMac().toBytes());
		
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
		
		// System.out.println("ORIGINAL PACKET");
		// System.out.println(etherPacket.toString());
		// System.out.println("NEW PACKET");
		// System.out.println(ether.toString());
		this.sendPacket(ether, bestMatch.getInterface());
	}
	
	private void handleArpPacket(Ethernet etherPacket, Iface inIface) {
		
		// Make sure it's an ARP packet
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
		
		
		
		
		
	}
}

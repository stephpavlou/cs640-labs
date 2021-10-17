package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
// NEW IMPORTS
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.MACAddress;
import java.util.Map;

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
		
		
		/********************************************************************/
		//check if etherPacket contains an IPv4
		if(etherPacket.getEtherType() != etherPacket.TYPE_IPv4) {
			System.out.println("Packet is not IPv4. Dropping packet.");
			return;
		}

		//verify checksum and TTL of IPv4 packet
		IPv4 payloadin = (IPv4)etherPacket.getPayload();
		short checksum = payloadin.getChecksum();
		short computed_checksum;
		byte ttl;

		
		payloadin.setChecksum((short)0);
		payloadin.serialize();
		computed_checksum = payloadin.getChecksum();

		

		if(computed_checksum != checksum) {
			System.out.println("Packet has bad checksum. Dropping packet.");
			return;
		}
		
		ttl = payloadin.getTtl();
		ttl--;
		
		if (ttl == 0) {
			System.out.println("Packet has a decremented ttl of 0. Dropping packet.");
			return;
		}
		
		payloadin.setTtl(ttl);
		
		payloadin.setChecksum((short)0);
		payloadin.serialize();
		
		int payloadDest = payloadin.getDestinationAddress();
		
		for(Map.Entry<String, Iface> curMapEntry : interfaces.entrySet()) {
			Iface curIface = curMapEntry.getValue();
			int ifaceIp = curIface.getIpAddress();
			if (ifaceIp == payloadDest) {
				System.out.println("Packet dest IP matches an Interface IP. Dropping Packet.");
				return;
			}
		}
		
		
		// FORWARDING PACKETS
		
		RouteEntry matchRouteEntry = routeTable.lookup(payloadDest);
		if (matchRouteEntry == null) {
			System.out.println("No route entry matching destination. Dropping Packet.");
			return;
		}
		System.out.println("Found Entry!");
		System.out.printf("matchRouteEntry.destinationAddress: %d\n", matchRouteEntry.getDestinationAddress());
		System.out.printf("matchRouteEntry.gatewayAddress: %d\n", matchRouteEntry.getGatewayAddress());
		System.out.printf("matchRouteEntry.maskAddress: %d\n", matchRouteEntry.getMaskAddress());
		
		ArpEntry matchArpEntry = arpCache.lookup(payloadDest);
		if (matchArpEntry == null) {
			System.out.println("matchArpEntry not found!");
		} else {
			System.out.println("matchArpEntry is not null.");
		}
		
		MACAddress ArpEntryMac = matchArpEntry.getMac();
		
		if (ArpEntryMac == null) {
			System.out.println("ArpEntryMac is null!");
		} else {
			System.out.println("ArpEntryMac is not null.");
		}
		
		etherPacket.setDestinationMACAddress(ArpEntryMac.toString());
		Iface routeEntryIface = matchRouteEntry.getInterface();
		
		if(routeEntryIface == null) {
			System.out.println("routeEntryIFace is null!");
		} else {
			System.out.println("routeEntryIface is not null.");
		}
		
		etherPacket.setSourceMACAddress(routeEntryIface.getMacAddress().toString());
		System.out.println("Reached sendPacket!");
		sendPacket(etherPacket, routeEntryIface);
	}
}

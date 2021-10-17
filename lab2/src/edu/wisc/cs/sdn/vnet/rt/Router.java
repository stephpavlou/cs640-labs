package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;

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
		if(etherPacket.getEtherType() != TYPE_IPv4) {
			return;
		}

		//verify checksum and TTL of IPv4 packet
		IPv4 payloadin = (IPv4)etherPacket.getPayload();
		short checksum = payloadin.getChecksum();
		short ttl, computed_checksum;

		payloadin.setChecksum(0);
		payloadin.serialize();
		computed_checksum = payloadin.getChecksum();

		ttl = payloadin.getTtl();
		ttl--;

		if(computed_checksum != checksum || ttl == 0) {
			return;
		}
		
		int payloadDest = payloadin.getDestinationAddress();
		
		for (Iface curIface: interfaces) {
			int ifaceIp = curIface.getIpAddress();
			if (ifaceIp == payloadDest) {
				return;
			}
		}
		
		
		// FORWARDING PACKETS
		
		RouteEntry matchRouteEntry = routeTable.lookup(payloadDest);
		if (matchRouteEntry == null) {
			return;
		}
		
		ArpEntry matchArpEntry = arpCache.lookup(payloadDest);
		MACAddress ArpEntryMac = matchArpEntry.getMac();
		etherPacket.setDestinationMACAddress(ArpEntryMac.toString());
		Iface routeEntryIface = matchRouteEntry.getInterface();
		etherPacket.setSourceMACAddress(routeEntryIface.getMacAddress().toString());
		sendPacket(etherPacket, routeEntryIface);
	}
}

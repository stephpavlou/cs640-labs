package edu.wisc.cs.sdn.vnet.rt;

import net.floodlightcontroller.packet.IPv4;
import edu.wisc.cs.sdn.vnet.Iface;

/**
 * An entry in a route table.
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class RouteEntry 
{
	/** Destination IP address */
	private int destinationAddress;
	
	/** Gateway IP address */
	private int gatewayAddress;
	
	/** Subnet mask */
	private int maskAddress;
	
	/** Router interface out which packets should be sent to reach
	 * the destination or gateway */
	private Iface iface;
	
	// NEW FIELDS
	
	private int metric;
	private long updateTime;
	private boolean directlyConnected;
	// NEW FIELDS END
	/**
	 * Create a new route table entry.
	 * @param destinationAddress destination IP address
	 * @param gatewayAddress gateway IP address
	 * @param maskAddress subnet mask
	 * @param iface the router interface out which packets should 
	 *        be sent to reach the destination or gateway
	 */
	 // Added metric and isDirectlyConnected
	public RouteEntry(int destinationAddress, int gatewayAddress, 
			int maskAddress, Iface iface, int metric, boolean directlyConnected)
	{
		this.destinationAddress = destinationAddress;
		this.gatewayAddress = gatewayAddress;
		this.maskAddress = maskAddress;
		this.iface = iface;
		
		// New
		this.metric = metric;
		this.updateTime = System.currentTimeMillis();
		this.directlyConnected = directlyConnected;
	}
	
	// New functions
	public boolean isDirectlyConnected() {
		return this.directlyConnected;
	}
	
	public void setMetric(int metric) {
		this.metric = metric;
	}
	
	public int getMetric() {
		return this.metric;
	}
	
	public void setUpdateTime() {
		this.updateTime = System.currentTimeMillis();
	}
	
	public long getUpdateTime() {
		return this.updateTime;
	}
	
	// New functions end
	
	
	
	/**
	 * @return destination IP address
	 */
	public int getDestinationAddress()
	{ return this.destinationAddress; }
	
	/**
	 * @return gateway IP address
	 */
	public int getGatewayAddress()
	{ return this.gatewayAddress; }

    public void setGatewayAddress(int gatewayAddress)
    { this.gatewayAddress = gatewayAddress; }
	
	/**
	 * @return subnet mask 
	 */
	public int getMaskAddress()
	{ return this.maskAddress; }
	
	/**
	 * @return the router interface out which packets should be sent to 
	 *         reach the destination or gateway
	 */
	public Iface getInterface()
	{ return this.iface; }

    public void setInterface(Iface iface)
    { this.iface = iface; }
	
	public String toString()
	{
		return String.format("%s \t%s \t%s \t%s \t%d \t%d \t%b",
				IPv4.fromIPv4Address(this.destinationAddress),
				IPv4.fromIPv4Address(this.gatewayAddress),
				IPv4.fromIPv4Address(this.maskAddress),
				this.iface.getName(),
				this.metric,
				this.updateTime,
				this.directlyConnected);
	}
}

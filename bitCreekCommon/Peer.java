package bitCreekCommon;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Un peer e' identificato in modo univoco nella rete da una coppia indirizzo numero di porta.
 * L'indirizzo e la porta sono quelle della socket sulla quale e' in attesa il server p2p del peer.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 */
public class Peer implements Serializable {

	private static final long serialVersionUID = -5220272936791381821L;

	/** indirizzo del peer */
	private final InetAddress address;

	/** porta del server p2p del peer */
	private final int port;

	/**
	 * Crea un nuovo peer.
	 * 
	 * @param address
	 *            l'indirizzo del peer
	 * @param port
	 *            la porta sulla quale
	 */
	public Peer(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	/**
	 * Restituisce l'indirizzo del peer.
	 * 
	 * @return l'indirizzo del peer.
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * Restituisce la porta del peer.
	 * 
	 * @return la porta del peer.
	 */
	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return "[" + address.getCanonicalHostName() + "::" + port + "]";
	}

	@Override
	public boolean equals(Object o) {
		Peer other = (Peer) o;
		return other.address.equals(this.address) && (other.port == this.port);
	}
}

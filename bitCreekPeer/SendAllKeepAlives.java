package bitCreekPeer;

import ioInterface.OutInterface;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Invia ogni {@link #KEEP_ALIVE_DELAY_MILLIS} millisecondi tutti i messaggi di keep alive di un
 * peer.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
class SendAllKeepAlives extends Thread {

	/** intervallo di tempo tra due sessioni di invio dei messaggi */
	private static final long KEEP_ALIVE_DELAY_MILLIS = 1000;

	private DatagramSocket socket;

	/** memorizza le associazioni nome file - messaggio keep alive */
	private final Map<String, DatagramPacket> keepAlivePacketsMap;

	/** interaccia di output */
	private final OutInterface log;

	/** indirizzo uguale per tutti i tracker UDP */
	private InetAddress trackerUDPAddress;

	/**
	 * 
	 * @param log
	 *            l'interfaccia di output
	 * @param trackerUDPAddress
	 *            l'indirizzo uguale per tutti tracker UDP
	 */
	SendAllKeepAlives(OutInterface log, String trackerUDPAddress) {
		this.log = log;
		keepAlivePacketsMap = new HashMap<String, DatagramPacket>();
		try {
			this.trackerUDPAddress = InetAddress.getByName(trackerUDPAddress);
			socket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Aggiunge un messaggio di keep alive per comunicare al tracker che il peer intende rimanere a
	 * far parte dello swarm del file <param>fileName</param>.
	 * 
	 * @param trackerUDPPort
	 *            la porta del tracker UDP.
	 * @param fileName
	 *            il nome del file
	 * @param peerPort
	 *            la porta del peer
	 */
	void addKeepAliveMessage(int trackerUDPPort, String fileName, int peerPort) {
		synchronized (keepAlivePacketsMap) {
			if (keepAlivePacketsMap.get(fileName) == null) {
				byte[] keepAliveMessage = ("KEEPALIVE\n" + peerPort + "\n" + fileName + "\n")
						.getBytes();
				DatagramPacket keepAlivePacket = new DatagramPacket(keepAliveMessage,
						keepAliveMessage.length, trackerUDPAddress, trackerUDPPort);
				keepAlivePacketsMap.put(fileName, keepAlivePacket);
			} else
				throw new IllegalArgumentException(" readding a keep alive ");
		}
	}

	@Override
	public void run() {
		try {
			while (!this.isInterrupted()) {
				synchronized (keepAlivePacketsMap) {
					for (DatagramPacket keepAlivePacket : keepAlivePacketsMap.values())
						socket.send(keepAlivePacket);
				}
				Thread.sleep(KEEP_ALIVE_DELAY_MILLIS);
			}
		} catch (InterruptedException e) {
			log.println("send keep alive thread terminated");
		} catch (IOException e) {
			log.println("send keep alive thread terminated");
		} finally {
			socket.close();
		}
	}

	/**
	 * Rimuove un messaggio di keep alive se presente. Al ritorno dal metodo non verra piu' inviato
	 * il messaggio di keep alive per far si che il peer rimanga a far parte dello swarm relativo al
	 * file <param>fileName</param>.
	 * 
	 * @param fileName
	 *            il nome del file per lo swarm del quale non si vogliono piu' inviare messaggi di
	 *            keep alive
	 */
	void remove(String fileName) {
		synchronized (keepAlivePacketsMap) {
			keepAlivePacketsMap.remove(fileName);
		}
	}

	ArrayList<String> getAllNames() {
		ArrayList<String> allNames = new ArrayList<String>();
		for (String name : keepAlivePacketsMap.keySet())
			allNames.add(name);
		return allNames;
	}
}

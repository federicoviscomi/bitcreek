package bitCreekServer;

import ioInterface.OutInterface;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.StringTokenizer;

/**
 * Il tracker UDP attende i messaggi di keep alive da parte di peer relativamente a un certo insieme
 * di swarm. Il formato dei messaggi di keep alive e':
 * 
 * <pre>
 * KEEPALIVE\n
 * portNumber\n
 * fileName\n
 * </pre>
 * 
 * il peer che ha inviato il messaggio e' identificato dall'indirizzo ottenibile dalla socket e dal
 * numero di porta specificato nel messaggio. Tale numero e' il numero di porta sulla quale e' in
 * ascolto il server p2p del peer che ha inviato il keep alive. Quando un peer invia un messaggio di
 * keep alive indica al tracker che intende continuare a far parte dello swarm del file di nome
 * fileName.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 */
class TrackerUDP extends Thread {

	/** lunghezza massima dei datagrammi in arrivo */
	private static final int MAX_DATAGRAM_LENGTH = 1 << 10;

	/**  */
	private DatagramSocket socket;

	/** l'interfaccia di output */
	private final OutInterface out;

	/** gestore di un insieme di swarm */
	private final SwarmsSet swarmManager;

	/**
	 * Crea un nuovo tracker UDP.
	 * 
	 * @param port
	 *            la porta sulla quale il tracker si mette in ascolto
	 * @param out
	 *            l'interfaccia di output
	 * @param swarmSet
	 *            l'insieme di swarm di cui si occupa questo tracker
	 */
	TrackerUDP(int port, OutInterface out, SwarmsSet swarmSet) {
		this.out = out;
		this.swarmManager = swarmSet;
		try {
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Termina il tracker
	 */
	void closeTracker() {
		socket.close();
	}

	@Override
	public void run() {
		try {
			byte[] buffer = new byte[TrackerUDP.MAX_DATAGRAM_LENGTH];
			DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
			while (true) {
				socket.receive(incomingPacket);
				String received = new String(incomingPacket.getData(), 0, incomingPacket
						.getLength());
				StringTokenizer tokenizer = new StringTokenizer(received);
				if (tokenizer.nextToken().equals("KEEPALIVE")) {
					int peerPort = Integer.parseInt(tokenizer.nextToken());
					String fileName = tokenizer.nextToken("").trim();
					Flag flag;
					synchronized (swarmManager) {
						if ((flag = swarmManager.getFlag(fileName, incomingPacket.getAddress(),
								peerPort)) != null) {
							flag.flag = Flag.ALIVE;
						} else {
							out.printerr("trackerUDP there is no swarm associated with file "
									+ fileName + " or peer " + incomingPacket.getAddress() + "::"
									+ peerPort + " is not in the swarm of that file\n");
							System.exit(-1);
						}
					}
				} else {
					out.printerr("tracker UDP unrecognized command ");
					System.exit(-1);
				}
			}
		} catch (SocketException e) {
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

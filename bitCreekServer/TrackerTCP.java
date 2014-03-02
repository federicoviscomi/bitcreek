package bitCreekServer;

import ioInterface.OutInterface;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import bitCreekCommon.Peer;

/**
 * Il tracker TCP gestisce un certo insieme di swarm e gestisce le richieste da parte dei peer. Ci
 * sono due tipi di messaggio che possono arrivare da un peerr:
 * 
 * JOINSWARM fileName port e in tal caso comunicano al tracker che intendono fare parte dello swarm
 * associato al file di nome fileName. Il tracker risponde con ALLOWED o DENIED
 * 
 * 
 * QUERY fileName e in tal caso comunicano al tracker che intendono ricevere la lista dei peer che
 * fanno parte dello swarm. Il tracker risponde con tale lista che puo' eventualmente essere vuota
 * 
 * Il tracker e' formato da un solo thread che implementa un server sequenziale che serve una
 * richiesta per volta prima di ascoltare le altre richieste.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
class TrackerTCP extends Thread {

	/** il socket ssl del tracker TCP */
	private SSLServerSocket server;

	/** l'interfaccia di output */
	final OutInterface io;

	/** l'insieme di swarm gestiti */
	final SwarmsSet swarmManager;

	private ThreadPoolExecutor pool;

	private class TrackerTCPServeASingleRequest implements Runnable {
		private final Socket socket;

		public TrackerTCPServeASingleRequest(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				String action = (String) in.readObject();
				if (action.equals("JOINSWARM")) {
					String fileName = (String) in.readObject();
					int port = ((Integer) in.readObject()).intValue();
					Peer toAdd = new Peer(socket.getInetAddress(), port);
					if (swarmManager.addToExistingSwarm(fileName, toAdd)) {
						out.writeObject("ALLOWED");
						io.println(" peer " + toAdd + " joined swarm associated with file "
								+ fileName);
					} else {
						out.writeObject("DENIED");
					}
				} else if (action.equals("QUERY")) {
					String fileName = (String) in.readObject();
					io.println(" query request from peer with address " + socket.getInetAddress()
							+ " for swarm associated with file " + fileName);
					ArrayList<Peer> peerList = swarmManager.getPeersInSwarm(fileName);
					out.writeObject(peerList);
				} else {
					io.println(" trackerTCP unrecognized message " + action);
				}
				in.close();
				out.close();
				socket.close();
			} catch (IOException e) {
			} catch (ClassNotFoundException e) {
			}
		}
	}

	/**
	 * Crea un nuovo tracker TCP
	 * 
	 * @param port
	 * @param io
	 * @param swarmManager
	 */
	TrackerTCP(int port, OutInterface io, SwarmsSet swarmManager) {
		this.swarmManager = swarmManager;
		this.io = io;
		try {
			SSLContext context = SSLContext.getInstance("SSL");
			/* the reference implementation only supports X.509 keys */
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			/* sun's default kind of key store */
			KeyStore ks = KeyStore.getInstance("JKS");

			char[] password = "millemila".toCharArray();

			ks.load(new FileInputStream("mykey"), password);
			kmf.init(ks, password);
			context.init(kmf.getKeyManagers(), null, null);
			SSLServerSocketFactory factory = context.getServerSocketFactory();
			server = (SSLServerSocket) factory.createServerSocket(port);
			String[] supported = server.getSupportedCipherSuites();
			String[] anonCipherSuitesSupported = new String[supported.length];
			int numAnonCipherSuitesSupported = 0;
			for (int i = 0; i < supported.length; i++) {
				if (supported[i].indexOf("_anon_") > 0) {
					anonCipherSuitesSupported[numAnonCipherSuitesSupported++] = supported[i];
				}
			}
			String[] oldEnabled = server.getEnabledCipherSuites();
			String[] newEnabled = new String[oldEnabled.length + numAnonCipherSuitesSupported];
			System.arraycopy(oldEnabled, 0, newEnabled, 0, oldEnabled.length);
			System.arraycopy(anonCipherSuitesSupported, 0, newEnabled, oldEnabled.length,
					numAnonCipherSuitesSupported);
			server.setEnabledCipherSuites(newEnabled);
			pool = new ThreadPoolExecutor(10, 100, 10, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>());
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				Socket socket = server.accept();
				pool.submit(new TrackerTCPServeASingleRequest(socket));
			}
		} catch (SocketException e) {
			io.println("tracker TCP closed ");
			return;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Termina il tracker.
	 */
	public void closeTracker() {
		try {
			server.close();
			pool.shutdownNow();
		} catch (IOException e) {
		}
	}
}

package bitCreekPeer;

import ioInterface.OutInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import bitCreekCommon.Peer;
import bitCreekCommon.TorrentFile;

/**
 * Il client del peer. Si occupa di scaricare uno e un solo file.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
class Peer2PeerClient extends Thread {

	private static class Count {
		/** il numero massimo di connessioni in download che un peer puo' stabilire con altri peer */
		private static final int MAX_DOWNLOAD_CONNECTION = 100;

		private static int connectionCount = 0;

		synchronized static boolean otherConnection() {
			return connectionCount < MAX_DOWNLOAD_CONNECTION;
		}

		synchronized static void add(int i) {
			connectionCount += i;
		}
	}

	/** l'interfaccia di output */
	private final OutInterface log;

	/** il gestore dei file */
	private final FileManager fileManager;

	/** il descrittore del file da scaricare */
	private TorrentFile torrent;

	/** elenco di peer non fidati */
	private final ArrayList<Peer> bannedList;

	/** l'indirizzo del server */
	private String serverAddress;

	/**
	 * Crea un nuovo client del peer che cerca di scaricare il file descritto da <param>torrent</param>
	 * 
	 * @param torrent
	 *            il descrittore del file da scaricare.
	 * @param log
	 *            l'interfaccia di output
	 * @param fileManager
	 *            il gestore dei file
	 * @param bannedList
	 *            la lista dei peer non fidati
	 * @param serverAddress
	 *            l'indirizzo del server
	 * @throws TooMuchConnectionException
	 */
	Peer2PeerClient(TorrentFile torrent, OutInterface log, FileManager fileManager,
			ArrayList<Peer> bannedList, String serverAddress) throws TooMuchConnectionException {
		if (!Count.otherConnection())
			throw new TooMuchConnectionException();
		this.torrent = torrent;
		this.log = log;
		this.fileManager = fileManager;
		this.bannedList = bannedList;
		this.serverAddress = serverAddress;
	}

	@Override
	public void run() {
		try {
			PiecesChooser partChooser = new PiecesChooser(torrent.fileName, fileManager);
			ArrayList<Peer> triedPeerList = new ArrayList<Peer>();
			while (!fileManager.hasACompleteCopy(torrent) && !this.isInterrupted()) {
				/* P contatta il Tracker TCP per ottenere la lista dei peer. */
				SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				SSLSocket socket = (SSLSocket) socketFactory.createSocket(serverAddress,
						torrent.tPort);

				socket.setEnabledCipherSuites(socket.getEnabledCipherSuites());

				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

				/* P invia una query al tracker */
				out.writeObject("QUERY");
				out.writeObject(torrent.fileName);

				/*
				 * il tracker risponde con una lista che contiene gli indirizzi che fanno parte
				 * dello swarm del file
				 */
				ArrayList<Peer> peerList = (ArrayList<Peer>) in.readObject();

				Iterator<Peer> iterator = peerList.iterator();
				while (iterator.hasNext()) {
					Peer next = iterator.next();
					if (triedPeerList.contains(next)
							|| InetAddress.getLocalHost().equals(next.getAddress())
							|| bannedList.contains(next))
						iterator.remove();
				}
				ThreadPoolExecutor pool = new ThreadPoolExecutor(100, 100, 1000,
						TimeUnit.MILLISECONDS,
						(BlockingQueue) new LinkedBlockingQueue<PiecesDownloader>());
				for (Peer bannedPeer : bannedList)
					for (int i = 0; i < peerList.size(); i++)
						if (peerList.get(i).equals(bannedPeer))
							peerList.remove(i);

				if (peerList.isEmpty()) {
					log.println(" file " + torrent.fileName
							+ " not retrieved completely but peer list is empty");
					try {
						socket.close();
						out.close();
						in.close();
					} catch (Exception e) {
					}
					return;
				}
				for (Peer p : peerList)
					System.out.println(this.getClass().getCanonicalName() + " "
							+ p.toString());

				int newEstablishedConnection = 0;
				for (Peer p2pServer : peerList) {
					if (!Count.otherConnection())
						break;
					try {
						if (this.isInterrupted())
							break;
						pool.submit(new PiecesDownloader(p2pServer, torrent, bannedList,
								fileManager, partChooser, log));
						Count.add(1);
						newEstablishedConnection++;
					} catch (UnableToConnectToPeerException e) {
					}
				}

				pool.shutdown();
				try {
					while (!pool.awaitTermination(20, TimeUnit.MINUTES))
						;
				} catch (InterruptedException e) {
				}
				pool.shutdownNow();
				Count.add(-newEstablishedConnection);
				try {
					socket.close();
					out.close();
					in.close();
				} catch (Exception e) {
				}
			}
			if (fileManager.hasACompleteCopy(torrent))
				fileManager.composeFile(torrent);
			log.println("\n file " + torrent.fileName + " downloaded ");
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

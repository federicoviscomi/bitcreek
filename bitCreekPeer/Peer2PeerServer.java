package bitCreekPeer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bitCreekCommon.TorrentFile;

/**
 * Il server del peer bitCreek. Ogni peer si mette in ascolto di connessioni da parte di altri peer.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
class Peer2PeerServer extends Thread {

	/** numero massimo di connessioni in upload che puo' gestire il peer */
	private static final int MAX_UPLOAD_CONNECTION = 100;

	/** la porta sulla quale e' in ascolto il server del peer */
	private int p2pServerPort = 5700;

	/** il gestore dei file */
	final FileManager fileManager;

	/** il socket */
	ServerSocket server;

	/** il pool che esegue i thread che servono le richieste dei peer */
	private ThreadPoolExecutor pool;

	/**
	 * Crea un nuovo server peer to peer
	 * 
	 * @param fileManager
	 *            il gestore dei file
	 */
	Peer2PeerServer(FileManager fileManager) {
		this.fileManager = fileManager;
		pool = new ThreadPoolExecutor(10, MAX_UPLOAD_CONNECTION, 10, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
		BindException bindException = null;
		do {
			try {
				bindException = null;
				server = new ServerSocket(p2pServerPort);
			} catch (BindException e) {
				bindException = e;
				p2pServerPort++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		} while (bindException != null && this.p2pServerPort < 1 >> 16);
	}

	private class ServeASingleUploadRequest implements Runnable {
		private final Socket connection;

		public ServeASingleUploadRequest(Socket connection) {
			this.connection = connection;
		}

		public void run() {
			synchronized (server) {
				try {
					connection.setSendBufferSize(TorrentFile.PIECE_LENGTH);
					ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
					ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
					/** il peerServer ottiene una query che contiene il nome di un file F */
					String fileName = (String) in.readObject();

					/**
					 * il peerServer invia al peerClient la lista dei pezzi disponibili per quel
					 * file
					 */
					out.writeObject(fileManager.getPieceOffests(fileName));

					/** riceve la lista dei pezzi che il peerClient desidera ricevere */
					ArrayList<Long> choosed = (ArrayList<Long>) in.readObject();

					/** il peerServer invia i pezzi scelti */
					for (long offset : choosed) {
						out.writeObject(fileManager.getPiece(fileName, offset));
					}
					out.close();
					in.close();
					connection.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				Socket connection = server.accept();
				pool.submit(new ServeASingleUploadRequest(connection));
			}
		} catch (SocketException e) {
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Termina questo server.
	 */
	void stopServer() {
		synchronized (server) {
			try {
				this.server.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Restituisce la porta sulla quale e' in ascolto questo peer.
	 * 
	 * @return la porta sulla quale e' in ascolto questo peer.
	 */
	int getPort() {
		return this.p2pServerPort;
	}
}

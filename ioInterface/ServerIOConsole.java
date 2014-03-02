package ioInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.StringTokenizer;

import bitCreekCommon.Peer;
import bitCreekCommon.TorrentFile;
import bitCreekServer.BitCreekServer;

/**
 * Un interprete minimale di comandi da console per conoscere lo stato interno del server.
 * 
 * @author feffo
 * 
 */
public class ServerIOConsole {

	private UserServerInterface userInterface;

	final BitCreekServer server;

	class UserServerInterface extends Thread {
		@Override
		public void run() {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String read = null;
			while (true) {
				do {
					try {
						System.out.print("$ ");
						read = in.readLine();
					} catch (IOException e) {
						e.printStackTrace();
						server.shutDownServer();
						System.exit(-1);
					}
				} while (read == null || read.trim().equals(""));
				read = read.trim();
				if (read.startsWith("lp")) {
					/* elenca gli indirizzi di ogni peer che fa parte di qualche swarm */
					System.out.println(" peer list follows: ");
					for (Peer peer : server.getAllPeers())
						System.out.println(" " + peer);
					System.out.println(" peer list ends. ");
				} else if (read.startsWith("lt")) {
					/* elenca tutti i torrent publicati per i quali c'e' ancora uno swarm attivo */
					System.out.println(" torrent list follows: ");
					for (TorrentFile torrent : server.getAllTorrent()) {
						System.out.println("[file name=" + torrent.fileName + ", file length="
								+ torrent.fileLength + ", tcp port=" + torrent.tPort
								+ ", udp port=" + torrent.tPort + ", pieces number="
								+ torrent.getPiecesNumber() + "]");
					}
					System.out.println(" torrent list ends. ");
				} else if (read.startsWith("address")) {
					System.out.println(" server address is " + server.getAddress());
				} else if (read.startsWith("exit")) {
					System.out.println(" bye ");
					server.shutDownServer();
					System.exit(-1);
				} else {
					System.err
							.println("\n unrecognized command "
									+ read
									+ "\n use:"
									+ "\n lp - elenca gli indirizzi di ogni peer che fa parte di qualche swarm "
									+ "\n lt - elenca tutti i torrent publicati per i quali c'e' ancora uno swarm attivo"
									+ "\n address - visualizza l'indirizzo del server"
									+ "\n exit - termina l'esecuzione del server\n");
				}
			}

		}
	}

	public ServerIOConsole(BitCreekServer server) {
		if (server == null)
			throw new IllegalArgumentException();
		this.server = server;
		userInterface = new UserServerInterface();
		userInterface.start();
	}

}

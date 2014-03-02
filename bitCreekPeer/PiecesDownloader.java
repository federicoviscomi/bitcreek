package bitCreekPeer;

import ioInterface.OutInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import bitCreekCommon.Peer;
import bitCreekCommon.TorrentFile;

/**
 * Scarica un certo insieme di pezzi da uno e un solo peer.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
class PiecesDownloader implements Runnable {

	/** il server p2p del peer da cui scaricare */
	private final Peer peerServer;

	private final Socket socket;

	/** il descrittore del file da scaricare */
	private final TorrentFile torrent;

	/** lista di peer non fidati */
	private ArrayList<Peer> bannedList;

	/** gestore dei file del peer */
	private FileManager fileManager;

	/** oggetto che implementa la politica di scelta dei pezzi da scaricare */
	private final PiecesChooser piecesChooser;

	/** l'interfaccia di output */
	private final OutInterface io;

	PiecesDownloader(Peer peerServer, TorrentFile torrent, ArrayList<Peer> bannedList,
			FileManager owned, PiecesChooser chunckChooser, OutInterface log)
			throws UnableToConnectToPeerException {
		this.peerServer = peerServer;
		this.torrent = torrent;
		this.bannedList = bannedList;
		this.fileManager = owned;
		this.piecesChooser = chunckChooser;
		this.io = log;
		try {
			socket = new Socket(peerServer.getAddress(), peerServer.getPort());
		} catch (IOException e) {
			log.println(" unable to connect to peer " + peerServer.toString());
			throw new UnableToConnectToPeerException();
		}
	}

	public void run() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

			/*
			 * invia al peer che fa da server una query che contiene il nome del file di cui il peer
			 * vuole scaricare dei pezzi
			 */
			out.writeObject(torrent.fileName);

			/* la risposta del server al peer e' una lista eventualmente vuota di offsets */
			ArrayList<Long> availableList = (ArrayList<Long>) in.readObject();
			piecesChooser.addAvailablePieces(peerServer, availableList);

			ArrayList<Long> choosed = piecesChooser.choosePieces(peerServer);

			/* il peer risponde al server con una lista eventualmente vuota di pezzi richiesti */
			out.writeObject(choosed);

			/*
			 * il peer attende i pezzi in ordine di offsets crescente e per ogni pezzo ricevuto
			 * controlla l'sha1. se l'sha1 e' corretto allora memorizza il pezzo nel file system
			 * altrimenti aggiunge il peer nella banned list
			 */
			for (long choosedOffset : choosed) {
				byte[] piece = (byte[]) in.readObject();
				if (torrent.hashsMatch(choosedOffset, piece)) {
					fileManager.addAndStorePiece(torrent.fileName, choosedOffset, piece, torrent
							.getLengthOfPiece(choosedOffset));
					io.println("a piece has been downloaded. file name=" + torrent.fileName
							+ ", piece offset=" + choosedOffset + " piece length ="
							+ torrent.getLengthOfPiece(choosedOffset) + " from peer="
							+ peerServer.toString());
				} else {
					bannedList.add(peerServer);
					io.println(" downloaded piece " + choosedOffset
							+ " has a wrong sha1. adding peer " + peerServer.toString()
							+ " to list of banned peers");
					break;
				}
			}
			out.close();
			in.close();
			socket.close();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

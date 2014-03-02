package bitCreekPeer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import bitCreekCommon.Peer;

/**
 * Tiene traccia dei pezzi posseduti dagli altri peer per uno e un solo file e implementa una
 * politica di scelta di quali pezzi scaricare. Il peer non possiede nessuno di tali pezzi.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 */
class PiecesChooser {

	/** memorizza i pezzi del file posseduti da alcuni degli altri peer nella rete */
	private Map<Peer, ArrayList<Long>> peerOffsetMap;

	/** pezzi che questo peer sta correntemente scaricando da qualche altro peer */
	private ArrayList<Long> currentDownloading;

	/** il nome del file */
	private String fileName;

	private final FileManager fileManager;

	/**

	 * 
	 * @param fileName
	 */
	PiecesChooser(String fileName, FileManager fileManager) {
		this.fileName = fileName;
		this.fileManager = fileManager;
		peerOffsetMap = new HashMap<Peer, ArrayList<Long>>();
		this.currentDownloading = new ArrayList<Long>();
	}

	/**
	 * Sceglie quali pezzi il peer deve richiedere al peer <param>fromPeer</param>.
	 * 
	 * @param fromPeer
	 *            il peer al quale richiedere dei pezzi del file
	 * @return la lista di pezzi da richiedere al peer <param>fromPeer</param>
	 */
	synchronized ArrayList<Long> choosePieces(Peer fromPeer) {
		ArrayList<Long> available;
		if ((available = peerOffsetMap.get(fromPeer)) == null)
			throw new IllegalArgumentException(" peer not found ");
		
		ArrayList<Long> ownedList;
		if ((ownedList = fileManager.getPieceOffests(fileName)) != null) {
			for (Long ownedOff : ownedList) {
				available.remove(ownedOff);
			}
		}

		for (Long pieceInDownload : currentDownloading)
			available.remove(pieceInDownload);
		currentDownloading.addAll(available);
		return available;
	}

	/**
	 * Comunica che il peer <param>address</param> in ascolto sulla porta <param>port</param>
	 * possiede le parti <param>availableList</param>
	 * 
	 * @param peer
	 *            il peer che possiede le parti di file
	 * @param availableList
	 *            la lista di pezzi resi disponibili dal peer
	 */
	synchronized void addAvailablePieces(Peer peer, ArrayList<Long> availableList) {
		ArrayList<Long> current;
		if ((current = peerOffsetMap.get(peer)) == null) {
			peerOffsetMap.put(peer, availableList);
		} else {
			current.addAll(availableList);
		}
	}

}

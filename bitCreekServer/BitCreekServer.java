package bitCreekServer;

import ioInterface.ConsoleOut;
import ioInterface.ServerIOConsole;

import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import bitCreekCommon.Peer;
import bitCreekCommon.ServerToPeerRemoteInterface;
import bitCreekCommon.TorrentFile;

/**
 * Il server bitCreek.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
public class BitCreekServer implements ServerToPeerRemoteInterface {

	public static void main(String args[]) {
		new BitCreekServer();
	}

	private Registry registry;

	/** memorizza i descrittori dei file publicati */
	private Map<String, TorrentFile> fileTorrentMap;

	/** gestisce gli swarm */
	private UnitManager unitManager;

	private ConsoleOut out;

	/**
	 * Crea un nuovo server remoto.
	 */
	public BitCreekServer() {
		try {
			out = new ConsoleOut();
			registry = LocateRegistry.getRegistry();
			registry.bind(ServerToPeerRemoteInterface.serverName, UnicastRemoteObject
					.exportObject(this));
			fileTorrentMap = new HashMap<String, TorrentFile>();
			unitManager = new UnitManager(out, this);
			new ServerIOConsole(this);
		} catch (RemoteException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (AlreadyBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Restituisce l'indirizzo del server.
	 * 
	 * @return l'indirizzo del server.
	 */
	public String getAddress() {
		try {
			return InetAddress.getLocalHost().getCanonicalHostName() + ":[bounded name="
					+ ServerToPeerRemoteInterface.serverName + "]";
		} catch (Exception e) {
			return e.getLocalizedMessage();
		}
	}

	public ArrayList<Peer> getAllPeers() {
		return unitManager.getAllPeers();
	}

	/**
	 * Restituisce tutti i descrittori presenti nella rete o meglio tutti i descrittori per i quali
	 * esiste almeno un peer nello swarm associato.
	 * 
	 * @return tutti i descrittori presenti nella rete.
	 */
	synchronized public Collection<TorrentFile> getAllTorrent() {
		return fileTorrentMap.values();
	}

	/**
	 * Restituisce i nomi di tutti i file publicati nella rete.
	 * 
	 * @return i nomi di tutti i file publicati nella rete.
	 */
	synchronized public ArrayList<String> getAllTorrentFileName() {
		ArrayList<String> allTorrentsName = new ArrayList<String>();
		for (TorrentFile torrent : fileTorrentMap.values())
			allTorrentsName.add(torrent.fileName);
		return allTorrentsName;
	}

	synchronized public TorrentFile lookup(String fileName) {
		return fileTorrentMap.get(fileName);
	}

	/**
	 * Indica che lo swarm relativo al file di nome <param>fileName</param> e' vuoto e quindi
	 * bisogna rimuovere il descrittore del file ed eventualmente i tracker relativi.
	 * 
	 * @param fileName
	 *            il nome del file il cui swarm e' vuoto
	 */
	synchronized void notifyEmptySwarm(String fileName) {
		out.println(" swarm associated with file " + fileName + " is empty");
		unitManager.notifyEmptySwarm(fileName);
		fileTorrentMap.remove(fileName);
	}

	synchronized public int publish(TorrentFile torrent, Peer seeder) {
		TorrentFile oldTorrent;
		int ports;
		if ((oldTorrent = fileTorrentMap.get(torrent.fileName)) == null) {
			out.println(" peer " + seeder + " published a new file " + torrent);
			unitManager.createNewSwarm(torrent.fileName, seeder);
			ports = unitManager.getTrackersPort(torrent.fileName);
			torrent.tPort = ports;
			fileTorrentMap.put(torrent.fileName, torrent);
		} else {
			out.println(" peer " + seeder + " publish an already published file " + torrent);
			ports = oldTorrent.tPort;
			unitManager.addToSwarm(torrent.fileName, seeder);
		}
		return ports;
	}

	/**
	 * Termina il server.
	 */
	public void shutDownServer() {
		try {
			registry.unbind(ServerToPeerRemoteInterface.serverName);
		} catch (Exception e) {
		}
		unitManager.shutDownAllTrackers();
	}
}

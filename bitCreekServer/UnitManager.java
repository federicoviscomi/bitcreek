package bitCreekServer;

import ioInterface.OutInterface;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import bitCreekCommon.Peer;

/**
 * Gestisce gli swarm.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
class UnitManager {
	/** numero massimo di swarm gestibili da una coppia di tracker */
	private static final int MAX_SWARM_ALLOWED = 10;

	/**
	 * Gestisce un insieme di tracker e relativi swarm
	 */
	private static class Unit {

		/** insieme di swarm */
		final SwarmsSet swarmManager;

		/** il tracker TCP */
		TrackerTCP tTCP;

		/** il tracker UDP */
		TrackerUDP tUDP;

		/** il thread che si occupa dei flag */
		ResetAndCheckFlagsThread rstCkThread;

		/** la porta sulla quale sono in ascolto i tracker */
		int tPort;

		public Unit(int port, OutInterface io, BitCreekServer server) {
			swarmManager = new SwarmsSet(server, io);
			tTCP = new TrackerTCP(port, io, swarmManager);
			tUDP = new TrackerUDP(port, io, swarmManager);
			rstCkThread = new ResetAndCheckFlagsThread(swarmManager);
			tPort = port;
			tTCP.start();
			tUDP.start();
			rstCkThread.start();
		}

	}

	/** il primo numero di porta usato per i tracker */
	private static final int MIN_PORT_NUMBER = 6000;

	/** il successivo numero di porta per i tracker */
	private static int nextPort = MIN_PORT_NUMBER;

	/**  */
	private final Map<String, Unit> unitMap;

	/** l'interfaccia di out */
	private final OutInterface io;

	/** riferimento al server */
	private final BitCreekServer server;

	public UnitManager(OutInterface io, BitCreekServer server) {
		this.io = io;
		this.server = server;
		unitMap = new HashMap<String, Unit>();
	}

	/**
	 * Aggiunge allo swarm del file <param>fileName</param> il peer <param>seeder</param>
	 * 
	 * @param fileName
	 *            il nome del file dello swarm nel quale aggiungere il peer <param>seeder</param>
	 * 
	 * @param seeder
	 *            il peer da aggiungere nello swarm
	 */
	public void addToSwarm(String fileName, Peer seeder) {
		Unit unit;
		if ((unit = unitMap.get(fileName)) == null) {
			throw new IllegalArgumentException(" there is no trackers associated with file "
					+ fileName);
		}
		unit.swarmManager.addToExistingSwarm(fileName, seeder);
	}

	/**
	 * Crea un nuovo swarm relativo al file <param>fileName</param> che contiene il peer
	 * <param>seeder</param>
	 * 
	 * @param fileName
	 *            il nome del file relativo allo swarm da creare
	 * @param seeder
	 *            il peer che fara' parte dello swarm da creare
	 */
	public void createNewSwarm(String fileName, Peer seeder) {
		if (unitMap.containsKey(fileName))
			throw new IllegalArgumentException(
					" attemp to create a swarm for a file that already has a swarm associated ");
		ArrayList<Peer> peerList = new ArrayList<Peer>();
		peerList.add(seeder);
		if (!unitMap.isEmpty()) {
			for (Map.Entry<String, Unit> entry : unitMap.entrySet()) {
				if (entry.getValue().swarmManager.swarmsCount() < MAX_SWARM_ALLOWED) {
					entry.getValue().swarmManager.createNewSwarm(fileName, peerList);
					unitMap.put(fileName, entry.getValue());
					return;
				}
			}
		}
		int newPort = (nextPort++ % ((1 << 16) - MIN_PORT_NUMBER)) + MIN_PORT_NUMBER;
		Unit unit = new Unit(newPort, io, server);
		unit.swarmManager.createNewSwarm(fileName, peerList);
		unitMap.put(fileName, unit);
	}

	/**
	 * Restituisce una lista di tutti i peer che fanno parte di almeno uno swarm
	 * 
	 * @return una lista di tutti i peer che fanno parte di almeno uno swarm
	 */
	public ArrayList<Peer> getAllPeers() {
		ArrayList<Peer> allPeers = new ArrayList<Peer>();
		for (Unit unit : unitMap.values()) {
			for (Peer peer : unit.swarmManager.getAllPeers())
				if (!allPeers.contains(peer))
					allPeers.add(peer);
		}
		return allPeers;
	}

	/**
	 * Restituisce la porta sulla quale sono in ascolto i tracker che gestiscono lo swarm associato
	 * al file di nome <param>fileName</param>
	 * 
	 * @param fileName
	 * @return la porta sulla quale sono in ascolto i tracker che gestiscono lo swarm associato al
	 *         file di nome <param>fileName</param>
	 */
	public int getTrackersPort(String fileName) {
		Unit unit;
		if ((unit = unitMap.get(fileName)) == null) {
			throw new IllegalArgumentException(" there is no trackers associated with file "
					+ fileName);
		}
		return unit.tPort;
	}

	/**
	 * Segnala lo svuotamento dello swarm relativo al file <param>fileName</param>
	 * 
	 * @param fileName
	 *            il nome del file in cui swarm e' diventato vuoto
	 */
	void notifyEmptySwarm(String fileName) {
		Unit unit;
		if ((unit = unitMap.get(fileName)) == null) {
			throw new IllegalArgumentException(" there is no trackers associated with file "
					+ fileName);
		}
		unit.rstCkThread.interrupt();
		unit.tTCP.interrupt();
		unit.tTCP.closeTracker();
		unit.tUDP.interrupt();
		unit.tUDP.closeTracker();
		unitMap.remove(fileName);
	}

	/**
	 * Interrompe tutti i tracker.
	 */
	public void shutDownAllTrackers() {
		for (Unit unit : unitMap.values()) {
			unit.rstCkThread.interrupt();
			try {
				unit.rstCkThread.join();
			} catch (InterruptedException e) {
			}
			unit.tTCP.interrupt();
			unit.tTCP.closeTracker();
			try {
				unit.tTCP.join();
			} catch (InterruptedException e) {
			}
			unit.tUDP.interrupt();
			unit.tUDP.closeTracker();
			try {
				unit.tUDP.join();
			} catch (InterruptedException e) {
			}
		}
	}
}

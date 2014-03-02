package bitCreekServer;

import ioInterface.OutInterface;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.Map.Entry;

import bitCreekCommon.Peer;

/**
 * Memorizza un insieme di swarm. Uno swarm e' un insieme di peer che possiedono parti di un dato
 * file o intendono scaricarne.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 */
class SwarmsSet implements Iterable<Flag> {

	class Pair<T1, T2> {

		Pair(T1 first, T2 second) {
			this.first = first;
			this.second = second;
		}

		T1 first;

		T2 second;
	}

	/**
	 * Iteratore sui flag di tutti gli swarm. Il metodo {@link #remove()} rimuove dallo swarm il
	 * peer e notifica i tracker nel caso in cui lo swarm diventa vuoto.
	 */
	class FlagsIterator implements Iterator<Flag> {

		/** itera sugli swarm */
		private Iterator<Entry<String, ArrayList<Pair<Peer, Flag>>>> iteratorOverSwarms;

		/** itera sul contenuto di un singolo swarm */
		private Iterator<Pair<Peer, Flag>> singleSwarmIterator;

		/** memorizza lo swarm coinvolto nell'iterazione corrente */
		private Entry<String, ArrayList<Pair<Peer, Flag>>> currentSwarm;

		/** il peer il cui flag e' stato restituito dall'ultima chiamata a next() */
		private Pair<Peer, Flag> currentPeer;

		FlagsIterator() {
			iteratorOverSwarms = swarms.entrySet().iterator();
			currentSwarm = iteratorOverSwarms.next();
			singleSwarmIterator = currentSwarm.getValue().iterator();
		}

		public boolean hasNext() {
			if (singleSwarmIterator.hasNext())
				return true;
			if (!iteratorOverSwarms.hasNext())
				return false;
			currentSwarm = iteratorOverSwarms.next();
			singleSwarmIterator = currentSwarm.getValue().iterator();
			return singleSwarmIterator.hasNext();
		}

		public Flag next() {
			if (!this.hasNext())
				throw new NoSuchElementException();
			currentPeer = singleSwarmIterator.next();
			return currentPeer.second;
		}

		public void remove() {
			out.println("removing peer " + currentPeer.first + " from swarm associated with file "
					+ currentSwarm.getKey());
			singleSwarmIterator.remove();
			if (currentSwarm.getValue().isEmpty()) {
				iteratorOverSwarms.remove();
				server.notifyEmptySwarm(currentSwarm.getKey());
			}
		}

	}

	/** memorizza gli swarm */
	final Map<String, ArrayList<Pair<Peer, Flag>>> swarms;

	/** un riferimento al server */
	final BitCreekServer server;

	/** l'interfaccia di output */
	final OutInterface out;

	SwarmsSet(BitCreekServer server, OutInterface out) {
		this.server = server;
		this.out = out;
		swarms = new TreeMap<String, ArrayList<Pair<Peer, Flag>>>();
	}

	/**
	 * Aggiunge allo swarm del file <param>fileName</param> il peer <param>peer</param> in ascolto
	 * sulla porta <param>port</param>.
	 * 
	 * @param fileName
	 *            il nome del file dello swarm nel quale aggiungere il peer
	 * @param peer
	 *            il peer da aggiungere nello swarm
	 * @return <code>true</code> se esiste uno swarm per il file <param>fileName</param>;
	 *         <code>false</code> altrimenti
	 */
	synchronized boolean addToExistingSwarm(String fileName, Peer peer) {
		ArrayList<Pair<Peer, Flag>> swarm;
		if ((swarm = swarms.get(fileName)) == null)
			return false;
		swarm.add(new Pair<Peer, Flag>(peer, new Flag()));
		return true;

	}

	/**
	 * Crea un nuovo swarm relativo al file di nome <param>fileName</param> che contiene i peer
	 * nella lista <param>peerList</param>
	 * 
	 * @param fileName
	 *            il nome del file relativo allo swarm da creare
	 * 
	 * @param peerList
	 *            la lista di peer da inserire nello swarm
	 */
	synchronized void createNewSwarm(String fileName, ArrayList<Peer> peerList) {
		if (fileName == null || peerList == null || peerList.isEmpty())
			throw new IllegalArgumentException(" null/empty arg/s ");
		if (swarms.get(fileName) != null) {
			throw new IllegalArgumentException("a swarm for file " + fileName + " already exists");
		}
		ArrayList<Pair<Peer, Flag>> swarm = new ArrayList<Pair<Peer, Flag>>();
		for (Peer peer : peerList) {
			swarm.add(new Pair<Peer, Flag>(peer, new Flag()));
		}
		swarms.put(fileName, swarm);
	}

	/**
	 * Restituisce il flag associato al peer <param>peer port</param> nello swarm del file
	 * <param>fileName</param>.
	 * 
	 * @param fileName
	 *            il nome del file
	 * @param address
	 * @param port
	 * @return il flag associato al peer <param>peer port</param> nello swarm del file
	 *         <param>fileName</param> se e' presente; <code>null</code> altrimenti
	 */
	synchronized Flag getFlag(String fileName, InetAddress address, int port) {
		ArrayList<Pair<Peer, Flag>> swarm;
		if ((swarm = swarms.get(fileName)) == null) {
			return null;
		}
		Peer peer = new Peer(address, port);
		for (Pair<Peer, Flag> triple : swarm) {
			if (peer.equals(triple.first)) {
				return triple.second;
			}
		}
		return null;
	}

	/**
	 * Restituisce tutti i peer che fanno parte dello swarm del file <param>fileName</param>
	 * 
	 * @param fileName
	 *            il nome del file
	 * @return tutti i peer che fanno parte dello swarm del file <param>fileName</param>
	 */
	synchronized ArrayList<Peer> getPeersInSwarm(String fileName) {
		ArrayList<Pair<Peer, Flag>> swarm;
		if ((swarm = swarms.get(fileName)) == null) {
			return null;
		}
		ArrayList<Peer> list = new ArrayList<Peer>();
		for (Pair<Peer, Flag> triple : swarm) {
			list.add(triple.first);
		}
		return list;
	}

	synchronized public Iterator<Flag> iterator() {
		return new FlagsIterator();
	}

	/**
	 * Restituisce il numero di swarm gestiti da <code>this</code>
	 * 
	 * @return il numero di swarm gestiti da <code>this</code>
	 */
	synchronized int swarmsCount() {
		return this.swarms.size();
	}

	/**
	 * Restituisce una lista di tutti i peer che fanno parte di almeno uno swarm
	 * 
	 * @return una lista di tutti i peer che fanno parte di almeno uno swarm
	 */
	public ArrayList<Peer> getAllPeers() {
		ArrayList<Peer> allPeers = new ArrayList<Peer>();
		for (ArrayList<Pair<Peer, Flag>> swarm : swarms.values()) {
			for (Pair<Peer, Flag> pair : swarm)
				if (!allPeers.contains(pair.first))
					allPeers.add(pair.first);
		}
		return allPeers;
	}

}

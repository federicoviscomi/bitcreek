package bitCreekPeer;

import ioInterface.ClientIOConsole;
import ioInterface.ConsoleOut;
import ioInterface.OutInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import bitCreekCommon.Peer;
import bitCreekCommon.ServerToPeerRemoteInterface;
import bitCreekCommon.TorrentFile;

/**
 * Il peer bitCreek.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 */
public class BitCreekPeer {

	/** server remoto */
	private ServerToPeerRemoteInterface serverRemote;

	/** thread che invia i messaggi di keep-alive */
	private SendAllKeepAlives keepAlives;

	/** gestisce i file e le parti di file possedute dal peer */
	private FileManager fileManager;

	/** serve le richieste di altri peer */
	private Peer2PeerServer p2pServer;

	/** interfaccia di output e di log */
	private OutInterface log;

	/** memorizza le associazioni torrent - client che scarica il file associato */
	private HashMap<TorrentFile, Peer2PeerClient> retrieversMap;

	/** lista si peer non fidati */
	private ArrayList<Peer> bannedList;

	/** indrizzo di questo peer */
	private InetAddress peerAddress;

	/** l'interprete dei comandi */
	private CommandInterpreter commandInterpreter;

	/** indirizzo del server bitCreek */
	private String serverAddress;

	public static void main(String args[]) {
		if (args.length < 2) {			
			System.err.println("USAGE: bitCreekPeer sourceFileName serverAddress");
			System.exit(-1);
		}
		new BitCreekPeer(args[0], args[1]);
	}

	private BitCreekPeer(String serverAddress) {
		try {
			if (serverAddress == null)
				throw new IllegalArgumentException(" null server address ");
			this.serverAddress = serverAddress;
			new ClientIOConsole(this);
			log = new ConsoleOut();
			fileManager = new FileManager();
			bannedList = new ArrayList<Peer>();
			retrieversMap = new HashMap<TorrentFile, Peer2PeerClient>();

			Registry remoteServerRegistry = LocateRegistry.getRegistry(serverAddress);
			serverRemote = (ServerToPeerRemoteInterface) remoteServerRegistry
					.lookup(ServerToPeerRemoteInterface.serverName);

			keepAlives = new SendAllKeepAlives(log, serverAddress);
			keepAlives.start();

			p2pServer = new Peer2PeerServer(fileManager);
			p2pServer.start();

			peerAddress = InetAddress.getLocalHost();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Crea un nuovo peer.
	 * 
	 * @param source
	 *            il file da cui leggere le azioni da eseguire
	 * @param serverAddress
	 *            l'indirizzo del server bitCreek
	 */
	public BitCreekPeer(String source, String serverAddress) {
		this(serverAddress);
		if (source == null)
			throw new IllegalArgumentException("null source file for peer action");
		try {
			commandInterpreter = new CommandInterpreter(new BufferedReader(new FileReader(source)),
					log, this);
			commandInterpreter.execute();
		} catch (FileNotFoundException e) {
			log.printerr("file " + System.getProperty("user.dir") + File.pathSeparator + source
					+ " not found\n");
			System.exit(-1);
		}
	}

	/**
	 * Se l'esecuzione va a buon fine questo peer si unisce allo swarm relativo al torrent
	 * <param>torrent</param> altrimenti viene sollevata l'eccezione
	 * <code>UnableToJoinSwarmException</code>
	 * 
	 * @param torrent
	 *            il descrittore del file
	 * 
	 * @throws UnableToJoinSwarmException
	 */
	private void join(TorrentFile torrent) throws UnableToJoinSwarmException {
		if (torrent == null)
			throw new IllegalArgumentException(" null argument ");
		try {
			/* P contatta il Tracker TCP #nonfun(mediante SSL). */
			SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket socket = (SSLSocket) socketFactory.createSocket(serverAddress, torrent.tPort);

			/*
			 * puo' darsi che un peer debba connettersi con lo stesso tracker per torrent diversi
			 * perche' uno stesso tracker puo' gestire piu' torrent
			 */
			socket.setEnableSessionCreation(true);

			socket.setEnabledCipherSuites(socket.getEnabledCipherSuites());

			/* invia al trackerTCP JOINSWARM fileName */
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

			out.writeObject("JOINSWARM");
			out.writeObject(torrent.fileName);
			out.writeObject(new Integer(p2pServer.getPort()));

			/* attende la risposta del tracker */
			String trackerAnswer = (String) in.readObject();
			if (trackerAnswer.equals("ALLOWED")) {
				log.println(" peer " + InetAddress.getLocalHost().getCanonicalHostName()
						+ " joined swarm for file " + torrent.fileName + ". trackerTCP is "
						+ socket.getInetAddress().getCanonicalHostName() + " on port "
						+ torrent.tPort);
			} else {
				log.println(" peer " + InetAddress.getLocalHost().getCanonicalHostName()
						+ " UNABLE TO JOIN swarm for file " + torrent.fileName + ". trackerTCP is "
						+ socket.getInetAddress().getCanonicalHostName() + " on port "
						+ torrent.tPort);
				throw new UnableToJoinSwarmException();
			}

			in.close();
			out.close();
			socket.close();

			/*
			 * comincia ad inviare i messaggi di keep-alive solo se tutte le precedenti operazioni
			 * sono andate a buon fine
			 */
			keepAlives.addKeepAliveMessage(torrent.tPort, torrent.fileName, p2pServer.getPort());
		} catch (IOException e) {
			throw new UnableToJoinSwarmException();
		} catch (ClassNotFoundException e) {
			throw new UnableToJoinSwarmException();
		}
	}

	/**
	 * Abbandona la content distribution network. In particolare:
	 * <p> -- interrompe l'interprete dei comandi
	 * <p> -- interrompe l'invio di tutti i messaggi di keep-alive
	 * <p> -- interrompe il server p2p
	 * 
	 * @see CommandInterpreter
	 */
	 void leave() {
		try {
			commandInterpreter.interrupt();
			commandInterpreter.join();
		} catch (InterruptedException e) {
		}
		try {
			keepAlives.interrupt();
			keepAlives.join();
		} catch (InterruptedException e) {
		}
		try {
			p2pServer.interrupt();
			p2pServer.stopServer();
			p2pServer.join();
		} catch (InterruptedException e) {
		}
		for (Peer2PeerClient p2p : retrieversMap.values()) {
			try {
				p2p.interrupt();
				p2p.join();
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Abbandona lo swarm per il file associato al torrent <param>torrent</param>. In particolare:
	 * <p> -- interrompe l'invio del messaggio di keep-alive
	 * <p> -- interrompe un eventuale client p2p intento a scaricare il file
	 * 
	 * @param torrent
	 *            il torrent associato al file da abbandonare
	 */
	void leaveSwarm(TorrentFile torrent) {
		keepAlives.remove(torrent.fileName);
		Peer2PeerClient retriever;
		if ((retriever = retrieversMap.get(torrent)) != null) {
			retriever.interrupt();
			try {
				retriever.join();
			} catch (InterruptedException e) {
			}
			retrieversMap.remove(torrent);
		}
	}

	/**
	 * 
	 * cerca il torrent associato al file di nome <param>fileName</param>
	 * 
	 * @param fileName
	 *            il nome del file da cercare nella CDN
	 * 
	 * @return se nella rete CDN nessun peer ha publicato un file con nome <code>fileName</code>
	 *         oppure se si verificano errori di rete allora restituisce <code>null</code>;
	 *         altrimenti restituisce il torrent associato
	 */
	TorrentFile lookup(String fileName) {
		if (fileName == null)
			throw new IllegalArgumentException(" null argument ");
		TorrentFile torrent = null;
		try {
			torrent = serverRemote.lookup(fileName);
		} catch (RemoteException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return torrent;
	}

	/**
	 * publica il file di nome <param>fileName</param>
	 * 
	 * @param fileName
	 *            il file da publicare
	 * 
	 * @throws UnableToPublishException
	 *             se non e' stato possibile publicare il file
	 */
	void publish(String fileName) throws UnableToPublishException {
		try {
			if (fileName == null)
				throw new IllegalArgumentException(" null argument ");
			if (!fileManager.hasACompleteCopy(fileName))
				throw new UnableToPublishException(
						"attemp to publish a file the peer is not a seeder of");
			TorrentFile torrent = new TorrentFile(fileName, fileManager
					.getAbsolutePathOfCompleteFile(fileName));
			torrent.tPort = serverRemote.publish(torrent,
					new Peer(peerAddress, p2pServer.getPort()));
			keepAlives.addKeepAliveMessage(torrent.tPort, torrent.fileName, p2pServer.getPort());
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new UnableToPublishException();
		}
	}

	/**
	 * avvia un client peer to peer che tenta di scaricare il file associato al descrittore
	 * <param>torrent</param>
	 * 
	 * @param torrent
	 *            il torrent associato al file da scaricare
	 */
	void retrieve(TorrentFile torrent) {
		if (torrent == null)
			throw new IllegalArgumentException(" null torrent ");
		try {
			Peer2PeerClient retreiver = new Peer2PeerClient(torrent, log, fileManager, bannedList,
					serverAddress);
			this.join(torrent);
			retrieversMap.put(torrent, retreiver);
			retreiver.start();
		} catch (UnableToJoinSwarmException e) {
			log.println("unable to retrieve file " + torrent.fileName + ": unable to join swarm");
		} catch (TooMuchConnectionException e) {
			log.println("unable to retrieve file " + torrent.fileName
					+ ": too much opened connection");
		}
	}

	/**
	 * Restituisce l'elenco del file e delle parti di file possedute.
	 * 
	 * @return l'elenco del file e delle parti di file possedute.
	 */
	public Set<Entry<String, ArrayList<Long>>> getOwned() {
		return fileManager.getOwned();
	}

	/**
	 * Restituisce tutti gli swarm di cui fa parte questo peer.
	 * 
	 * @return tutti gli swarm di cui fa parte questo peer.
	 */
	public ArrayList<String> getSwarm() {
		return keepAlives.getAllNames();
	}

	/**
	 * Restituisce <code>true</code> se il peer ha una copia completa del file di nome
	 * <param>fileName</param>; altrimenti restituisce <code>false</code>.
	 * 
	 * @param fileName
	 *            il nome del file
	 * @return <code>true</code> se il peer ha una copia completa del file di nome <param>fileName</param>;
	 *         altrimenti restituisce <code>false</code>.
	 */
	public boolean hasACompleteCopy(String fileName) {
		return fileManager.hasACompleteCopy(fileName);
	}

}

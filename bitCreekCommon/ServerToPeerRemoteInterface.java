package bitCreekCommon;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * L'interfaccia remota che i peer usano per comunicare col server centrale.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 */
public interface ServerToPeerRemoteInterface extends Remote {

	/** il nome del server nel registro */
	public static final String serverName = "bitcreekserver";

	/** l'indirizzo di default del server */
	public static final String defaultServerAddress = "fujihm.cli.di.unipi.it";

	/**
	 * Cerca nel server centrale un descrittore di file con nome uguale carattere per carattere a
	 * <param>fileName</param>.
	 * 
	 * @param fileName
	 *            il nome del file di cui cercare il descrittore
	 * @return se esiste un descrittore per il file <param>fileName</param> allora lo restituisce
	 *         altrimenti restituisce <code>null</code>
	 * 
	 * @throws RemoteException
	 */
	TorrentFile lookup(String fileName) throws RemoteException;

	/**
	 * Se nel server centrale e' attivo uno swarm relativo a un file con lo stesso nome allora
	 * aggiunge il peer di indirizzo <param>seeder</param> nello swarm e restituisce la porta sulla
	 * quale sono in ascolto i tracker. Altrimenti crea un nuovo swarm sceglie due tracker
	 * eventualmente creandone di nuovi e restituisce la porta sulla quale sono in ascolto.
	 * <p>
	 * Per publicare un file il peer deve possederne una copia intera in altre parole deve essere un
	 * seeder.
	 * 
	 * @param torrent
	 *            il descrittore del file da publicare
	 * @param seeder
	 *            il peer che publica il file.
	 * 
	 * 
	 * @return la porta sulla quale sono in ascolto i tracker relativi allo swarm del file publicato
	 * 
	 * @throws RemoteException
	 */
	int publish(TorrentFile torrent, Peer seeder) throws RemoteException;

}

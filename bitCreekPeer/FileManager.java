package bitCreekPeer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import bitCreekCommon.TorrentFile;

/**
 * 
 * Gestisce i file posseduti dal peer.
 * 
 * Ogni peer ha due cartelle:
 * <ul>
 * <li>{@link #partFileDirName}: contiene le parti di file scaricate</li>
 * <li>{@link #completeFileDirName}: contiene i file completi.</li>
 * </ul>
 * 
 * Le parti di file vengono memorizzate secondo il formato
 * <code>partFileDirName + File.separatorChar + fileName + ".part." + offset</code> in altre
 * parole il nome di una parte relativo alla directory delle parti e' dato concatenando il nome del
 * file con la stringa ".part." seguita dall'offset della parte.
 * 
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
class FileManager {

	/** memorizza le associazioni tra nomi e parti del file possedute */
	private Map<String, ArrayList<Long>> ownedPartMap;

	/**
	 * il nome relativo alla current working directory del processo della cartella che contiene le
	 * parti di file
	 */
	private String partFileDirName = "part.dir";

	/**
	 * il nome relativo alla current working directory del processo della cartella che contiene i
	 * file completi
	 */
	private String completeFileDirName = "complete.dir";

	/**
	 * Crea un nuovo gestore dei file
	 */
	FileManager() {
		try {
			ownedPartMap = new HashMap<String, ArrayList<Long>>();
			File partFileDir = new File(partFileDirName);
			File completeFileDir = new File(completeFileDirName);

			if (!partFileDir.exists()) {
				partFileDir.mkdir();
			}
			if (!completeFileDir.exists()) {
				completeFileDir.mkdir();
			}
			if (!partFileDir.isDirectory()) {
				throw new Error(
						"there already exist a file with the same name of the part file directory but this file is not a directory");
			}
			if (!completeFileDir.isDirectory()) {
				throw new Error(
						"there already exist a file with the same name of the complete file directory but this file is not a directory");
			}
			for (String dirEntry : partFileDir.list()) {
				int index;
				if ((index = dirEntry.indexOf(".part.")) >= 0) {
					long offset = Long.parseLong(dirEntry.substring(index));
					String fileName = dirEntry.substring(0, index);
					this.addPiece(fileName, offset);
				} else {
					throw new Error("in part file directory:\"" + partFileDir.getAbsolutePath()
							+ "\" + file name not valid:\"" + dirEntry + "\"");
				}
			}
			for (String dirEntry : completeFileDir.list()) {
				if (dirEntry.indexOf(".part.") >= 0) {
					throw new Error("in complete file directory:\""
							+ completeFileDir.getAbsolutePath() + "\" + file name not valid:\""
							+ dirEntry + "\"");
				}
				this.addWholeFile(dirEntry);
			}
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Aggiunge all'insieme dei pezzi di file posseduti il pezzo <param>offset</param> del file
	 * <param>fileName</param>.
	 * 
	 * @param fileName
	 *            il nome del file
	 * @param offset
	 *            l'offset che individua il pezzo del file da aggiungere
	 */
	synchronized void addPiece(String fileName, long offset) {
		ArrayList<Long> partList;
		if ((partList = ownedPartMap.get(fileName)) == null) {
			partList = new ArrayList<Long>();
			partList.add(new Long(offset));
			ownedPartMap.put(fileName, partList);
		} else {
			partList.add(new Long(offset));
		}
	}

	/**
	 * Aggiunge un file all'insieme dei file posseduti.
	 * 
	 * @param fileName
	 *            il file da aggiungere.
	 */
	synchronized private void addWholeFile(String fileName) {
		if (ownedPartMap.get(fileName) == null) {
			ArrayList<Long> allPieces = new ArrayList<Long>();
			File file = new File(this.getAbsolutePathOfCompleteFile(fileName));
			for (long offset = 0; offset < file.length(); offset += TorrentFile.PIECE_LENGTH)
				allPieces.add(new Long(offset));
			ownedPartMap.put(fileName, allPieces);
		} else {
			throw new IllegalArgumentException(" file already present " + fileName);
		}
	}

	/**
	 * Restituisce <code>null</code> se il peer non possiede nessuna parte del file
	 * <param>fileName</param> altrimenti restituisce tutte le parti possedute. Se il peer possiede
	 * una copia completa del file restituisce semplicemente tutte le parti del file. La lista delle
	 * parti possedute e' una lista di offset.
	 * 
	 * @param fileName
	 *            il file di cui si richiedono le parti possedute.
	 * 
	 * @return <code>null</code> se il peer non possiede nessuna parte del file <param>fileName</param>
	 *         altrimenti restituisce tutte le parti possedute.
	 */
	synchronized ArrayList<Long> getPieceOffests(String fileName) {
		return ownedPartMap.get(fileName);
	}

	/**
	 * Restituisce il pezzo di indice <param>offset</param> del file di nome <param>fileName</param>.
	 * Se il peer non possiede tale pezzo di tale file allora solleva eccezzione.
	 * 
	 * @param fileName
	 *            il file di cui si vuole avere un pezzo
	 * @param offset
	 *            l'indice del pezzo all'interno del file
	 * @return il pezzo di indice <param>offset</param> del file di nome <param>fileName</param>
	 *         se il peer lo possiede altrimenti solleva eccezzione.
	 * 
	 * @throws IllegalArgumentException
	 *             se il peer non possiede il pezzo di indice <param>offset</param> del file di
	 *             nome <param>fileName</param>
	 */
	synchronized byte[] getPiece(String fileName, long offset) throws IllegalArgumentException {
		try {
			File f = new File(this.getAbsolutePathOfCompleteFile(fileName));
			byte[] buffer = new byte[TorrentFile.PIECE_LENGTH];
			if (f.exists() && f.length() > offset) {
				FileInputStream in = new FileInputStream(f);
				in.skip(offset);
				in.read(buffer);
				return buffer;
			}
			f = new File(this.getAbsolutePathOfPartFile(fileName, offset));
			if (f.exists()) {
				FileInputStream in = new FileInputStream(f);
				in.read(buffer);
				return buffer;
			}
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		throw new IllegalArgumentException(" part " + offset + " of file " + fileName
				+ " not found ");
	}

	/**
	 * Restituisce <code>true</code> se il peer possiede una copia completa del file
	 * <param>fileName</param>; altrimenti <code>false</code>.
	 * 
	 * @param fileName
	 *            il nome del file
	 * @return <code>true</code> se il peer possiede una copia completa del file <param>fileName</param>;
	 *         altrimenti <code>false</code>.
	 */
	synchronized boolean hasACompleteCopy(String fileName) {
		ArrayList<Long> piecesList;
		if ((piecesList = ownedPartMap.get(fileName)) == null)
			return false;
		long length = new File(this.getAbsolutePathOfCompleteFile(fileName)).length();
		int piecesNumber = (int) (length / TorrentFile.PIECE_LENGTH);
		if (length % TorrentFile.PIECE_LENGTH != 0)
			piecesNumber++;
		return piecesList.size() == piecesNumber;
	}

	/**
	 * Aggiunge il pezzo <param>offset</param> del file di nome <param>fileName</param>
	 * nell'insieme dei file posseduti dal peer e lo memorizza nella directory delle parti di file.
	 * 
	 * @param fileName
	 *            il nome del file
	 * @param offset
	 *            l'identificatore del pezzo
	 * @param piece
	 *            il pezzo da memorizzare
	 * @param length
	 *            la lunghezza del pezzo
	 */
	synchronized void addAndStorePiece(String fileName, long offset, byte[] piece, int length) {
		try {
			this.addPiece(fileName, offset);
			FileOutputStream out = new FileOutputStream(this.getAbsolutePathOfPartFile(fileName,
					offset));
			out.write(piece, 0, length);
			out.close();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * 
	 * Restituisce il nome del file <param>fileName</param> relativo alla directory delle parti di
	 * file
	 * 
	 * @param fileName
	 * 
	 * @return il nome del file <param>fileName</param> relativo alla directory delle parti di file
	 */
	synchronized public String getAbsolutePathOfCompleteFile(String fileName) {
		return this.completeFileDirName + File.separatorChar + fileName;
	}

	/**
	 * Crea il file completo a partire dalle parti di file scaricate.
	 * 
	 * @param torrent
	 *            il descrittore del file da creare
	 * 
	 * @throws IllegalArgumentException
	 *             se il peer non possiede tutte le parti del file
	 */
	synchronized void composeFile(TorrentFile torrent) throws IllegalArgumentException {
		try {
			long offset;
			FileOutputStream out = new FileOutputStream(this
					.getAbsolutePathOfCompleteFile(torrent.fileName));
			byte[] buffer = new byte[TorrentFile.PIECE_LENGTH];
			for (offset = 0; offset < torrent.fileLength; offset += TorrentFile.PIECE_LENGTH) {
				File partFile = new File(this.getAbsolutePathOfPartFile(torrent.fileName, offset));
				FileInputStream in = new FileInputStream(partFile);
				int length = in.read(buffer);
				out.write(buffer, 0, length);
				in.close();
				partFile.delete();
			}
			out.close();
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(" il peer non possiede una copia completa del file ");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * 
	 * Restituisce il nome del file che memorizza il pezzo <param>offset</param> del file
	 * <param>fileName</param> relativo alla direcotory delle parti di file.
	 * 
	 * @param fileName
	 *            il nome del file
	 * @param offset
	 *            l'offset del pezzo
	 * @return il nome del file che memorizza il pezzo <param>offset</param> del file
	 *         <param>fileName</param> relativo alla direcotory delle parti di file.
	 */
	synchronized private String getAbsolutePathOfPartFile(String fileName, long offset) {
		return this.partFileDirName + File.separatorChar + fileName + ".part." + offset;
	}

	/**
	 * Restituisce l'insieme dei file posseduti con l'indice dei relativi pezzi.
	 * 
	 * @return l'insieme dei file posseduti con l'indice dei relativi pezzi.
	 */
	synchronized Set<Entry<String, ArrayList<Long>>> getOwned() {
		return ownedPartMap.entrySet();
	}

	/**
	 * Restituisce <code>true</code> se il peer possiede una copia completa del file il cui
	 * descrittore e' <param>torrent</param>; altrimenti restituisce <code>false</code>
	 * 
	 * @param torrent
	 *            il descrittore del file di cui si vuol sapere se il peer possiede una copia
	 *            completa o meno
	 * @return <code>true</code> se il peer possiede una copia completa del file il cui
	 *         descrittore e' <param>torrent</param>; altrimenti restituisce <code>false</code>
	 */
	synchronized boolean hasACompleteCopy(TorrentFile torrent) {
		ArrayList<Long> piecesList;
		if ((piecesList = ownedPartMap.get(torrent.fileName)) == null) {
			return false;
		}
		return (piecesList.size() == torrent.getPiecesNumber());

	}

}

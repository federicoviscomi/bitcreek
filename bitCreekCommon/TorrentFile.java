package bitCreekCommon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

/**
 * Il descrittore di un file. Contiene le seguenti informazioni:
 * <ul>
 * <li>il nome del file</li>
 * <li>la lunghezza totale del file in byte</li>
 * <li>la sequenza di hash dei pezzi del file calcolati usando l'algoritmo
 * <code>java.security.MessageDigest.getInstance("SHA1")</code></li>
 * <li>la porta sulla quale sono in ascolto sia il trackerTCP che il trackerUDP</li>
 * </ul>
 * 
 * Ogni descrittore e' associato ad uno e un solo file. Il file viene diviso in pezzi ciascuno di
 * lunghezza 4K byte tranne l'ultimo che ha lunghezza l byte con 0 &lt l &le 4K e pari al resto
 * della divisione euclidea della lunghezza totale del file per 4K.
 * <p>
 * Ogni pezzo di file viene identificato in modo univoco da un offset che e' la posizione del primo
 * byte del pezzo all'interno del file o in altre parole e' il numero di byte che precedono il primo
 * byte del pezzo nel file.
 * <p>
 * Si assume che due descrittori si riferiscano allo stesso file se e solo se i nomi dei file
 * conicidono carattere per carattere.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
public final class TorrentFile implements Serializable {

	private static final long serialVersionUID = 5321775414524932152L;

	/** dimensione dei pezzi del file */
	public transient static final int PIECE_LENGTH = 1 << 12;

	/** il nome del file */
	public final String fileName;

	/** la lunghezza del file */
	public long fileLength;

	/** la sequenza di hash dei pezzi del file */
	private byte[][] sha1s;

	/** la porta dei trackers */
	public int tPort = -1;

	/**
	 * Crea il descrittore del file di nome <param>fileName</param>.
	 * 
	 * @param fileName
	 *            il nome del file di cui creare il descrittore.
	 */
	public TorrentFile(String fileName, String absoluteFileName) {
		if (fileName == null)
			throw new IllegalArgumentException(" null argument ");
		this.fileName = fileName;
		try {
			File file = new File(absoluteFileName);
			FileInputStream in = new FileInputStream(file);
			fileLength = file.length();
			int size = ((int) (fileLength / PIECE_LENGTH) + (fileLength % PIECE_LENGTH == 0 ? 0 : 1));
			sha1s = new byte[size][];
			byte[] buffer = new byte[TorrentFile.PIECE_LENGTH];
			for (int i = 0; i < size; i++) {
				MessageDigest sha1 = MessageDigest.getInstance("SHA1");
				DigestOutputStream dout = new DigestOutputStream(new ByteArrayOutputStream(), sha1);
				int readByte = in.read(buffer);
				dout.write(buffer, 0, readByte);
				dout.flush();
				sha1s[i] = dout.getMessageDigest().digest();
				dout.close();
			}
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Un istanza di questa classe e un altro oggetto sono uguali se e solo se hanno lo stesso tipo
	 * e il nome del file e' uguale carattere per carattere.
	 * 
	 * @return <code>true</code> se e solo se <param>other</param> e' un istanza di questa classe
	 *         e <code>other.fileName</code> e' uguale carattere per carattere a
	 *         <code>other.fileName</code>; altrimenti restituisce <code>false</code>.
	 */
	@Override
	synchronized public boolean equals(Object other) {
		if (other == null)
			throw new IllegalArgumentException(" null argument ");
		return ((TorrentFile) other).fileName.equals(this.fileName);
	}
		
	/**
	 * restituisce la lunghezza in byte del pezzo di offset <param>offset</param> del file
	 * descritto da questo descrittore. Tutti i pezzi in cui viene diviso un file hanno dimensione
	 * 4K eccetto l'ultimo pezzo che avra' dimensione compresa tra 0 e 4K e pari al resto della
	 * divisione intera tra la lunghezza totale del file e 4K.
	 * 
	 * @param offset
	 *            la posizione del primo byte del pezzo all'interno del file misurata in byte. In
	 *            altre parole questo parametro e' uguale al numero totale di byte che precedono il
	 *            primo byte del pezzo in questione all'interno del file.
	 * 
	 * @return la lunghezza in byte del pezzo di offset <param>offset</param> del file descritto da
	 *         questo descrittore.
	 */
	synchronized public int getLengthOfPiece(long offset) {
		if (offset < this.fileLength - PIECE_LENGTH)
			return PIECE_LENGTH;
		return (int) (this.fileLength - offset);
	}

	/**
	 * restituisce il numero di pezzi in cui e' scomposto il file descritto da questo descrittore.
	 * 
	 * @return il numero di pezzi in cui e' scomposto il file descritto da questo descrittore.
	 */
	synchronized public int getPiecesNumber() {
		return sha1s.length;
	}

	/**
	 * controlla se l'hash calcolato dal peer che ha publicato il file e quello calcolato sul pezzo
	 * scaricato sono uguali. Il confronto avviene byte per byte.
	 * 
	 * @param offset
	 *            l'offset del pezzo scaricato
	 * @param piece
	 *            il pezzo scaricato
	 * @return <code>true</code> se l'hash del pezzo scaricato e' uguale a quello che trova nel
	 *         descrittore publicato sul server; altrimenti <code>false</code>.
	 */
	synchronized public boolean hashsMatch(long offset, byte[] piece) {
		if (offset % TorrentFile.PIECE_LENGTH != 0)
			throw new IllegalArgumentException("offset is not multiple of piece length");
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			DigestOutputStream dout = new DigestOutputStream(new ByteArrayOutputStream(), sha1);
			dout.write(piece, 0, getLengthOfPiece(offset));
			dout.flush();
			byte[] digest = dout.getMessageDigest().digest();
			dout.close();
			byte[] torrentDigest = sha1s[(int) (offset / TorrentFile.PIECE_LENGTH)];
			if (digest.length != torrentDigest.length)
				return false;
			for (int i = 0; i < digest.length; i++)
				if (torrentDigest[i] != digest[i])
					return false;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return true;
	}

	@Override
	synchronized public String toString() {
		return TorrentFile.class.getCanonicalName() + "[file name=" + fileName + ", file length="
				+ fileLength + ", tcp port=" + tPort + ", udp port=" + tPort + "]";
	}

}

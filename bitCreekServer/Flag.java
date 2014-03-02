package bitCreekServer;

/**
 * Per ogni swarm e per ogni peer nello swarm esiste un oggeto di questa classe. Ogni volta che il
 * trackerUDP riceve un messaggio di keep alive relativo a certi peer e file aggiorna il valore del
 * flag a {@link #ALIVE}. Un altra parte del server si occupa invece di riportare tutti flag a
 * {@link #DEAD} periodicamente e di eliminare dagli swarm i peer che non hanno inviato il keep
 * alive entro un certo intervallo di tempo.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
final class Flag {
	/**
	 * indica che non e' ancora stato ricevuto il messaggio di keep alive dall'inizio dell'ultimo
	 * slot di tempo
	 */
	static final byte DEAD = (byte) 0;

	/** indica che il messaggio di keep alive e' arrivato nell'ultimo slot di tempo */
	static final byte ALIVE = (byte) 1;

	/** il flag */
	byte flag = ALIVE;

	@Override
	public String toString() {
		return (flag == ALIVE ? "ALIVE" : "DEAD");
	}
}

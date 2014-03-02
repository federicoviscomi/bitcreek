package bitCreekServer;

import java.util.Iterator;

/**
 * 
 * Ogni peer partecipa a zero o piu' swarm e per ogni swarm di cui fa parte invia un messaggio di
 * keep-alive al trackerUDP relativo. Se il messaggio di keep-alive non arriva entro
 * <code>TIMEOUT</code> millisecondi si considera il peer come non facente piu' parte dello swarm.
 * Per ogni thread e per ogni swarm di cui il peer fa parte c'e' un flag. Tale flag viene impostato
 * a <code>Flag.ALIVE</code> ogni volta che il trackerUDP di competenza riceve il messaggio di
 * keep-alive appropriato.
 * 
 * Questo thread controlla tutti i flag ogni <code>TIMEOUT</code> millisecondi:
 * <p>
 * se il flag vale <code>Flag.ALIVE</code> lo imposta a <code>Flag.DEAD</code>
 * <p>
 * se il flag vale <code>Flag.DEAD</code> allora segnala al tracker che il peer non fa piu' parte
 * dello swarm
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
final class ResetAndCheckFlagsThread extends Thread {

	/**
	 * durata di uno slot di tempo o meglio intervallo di tempo misurato in millisecondi che ha un
	 * peer per mandare il messaggio keep alive prima di essere eliminato dallo swarm
	 */
	private final int TIMEOUT = 15000;

	/** gestisce gli swarm */
	private final SwarmsSet swarmManager;

	ResetAndCheckFlagsThread(SwarmsSet swarmManager) {
		this.swarmManager = swarmManager;
	}

	@Override
	public void run() {
		try {
			while (true) {
				Thread.sleep(TIMEOUT);
				synchronized (swarmManager) {
					Iterator<Flag> iterator = swarmManager.iterator();
					while (iterator.hasNext()) {
						Flag nextFlag = iterator.next();
						if (nextFlag.flag == Flag.ALIVE) {
							nextFlag.flag = Flag.DEAD;
						} else {
							iterator.remove();
						}
					}
				}
			}
		} catch (InterruptedException e) {
		}
	}
}

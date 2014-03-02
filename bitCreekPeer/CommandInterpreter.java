package bitCreekPeer;

import ioInterface.OutInterface;

import java.io.BufferedReader;
import java.util.StringTokenizer;

import bitCreekCommon.TorrentFile;

/**
 * Interprete dei comandi. Legge i comandi da un <code>BufferedReader</code> uno per linea ed fa
 * si che il peer esegua le azioni indicate.
 * <p>
 * Il linguaggio interpretato e' dato dalla seguente grammatica in BNF:
 * <p>
 * &#60;COMMANDFILE&#62; ::= &#60;COMMAND&#62;\n &#60;COMMANDFILE&#62; | &#60;COMMAND&#62;\n
 * <p>
 * &#60;COMMAND&#62; ::= publish fileName | lookup fileName | download fileName | leaveSwarm
 * fileName | wait millis | leaveNetwork
 * <p>
 * col seguente significato:
 * <p>
 * <table border="1">
 * <tr>
 * <td>SINTASSI</td>
 * <td>SEMANTICA</td>
 * </tr>
 * <tr>
 * <td>publish fileName</td>
 * <td> publica nella rete il file di nome fileName </td>
 * </tr>
 * <tr>
 * <td>lookup fileName</td>
 * <td> cerca nella rete il file di nome fileName </td>
 * </tr>
 * <tr>
 * <td>download fileName</td>
 * <td> cerca di scaricare il file di nome fileName </td>
 * </tr>
 * <tr>
 * <td>leaveSwarm fileName</td>
 * <td> abbandona lo swarm del file di nome fileName </td>
 * </tr>
 * <tr>
 * <td>wait millis</td>
 * <td> l'interprete si sospende per millis millisecondi </td>
 * </tr>
 * <tr>
 * <td>leaveNetwork</td>
 * <td> abbandona la rete e termina </td>
 * </tr>
 * </table>
 * 
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
class CommandInterpreter extends Thread {

	/** fornisce il codice da interpretare */
	private final BufferedReader in;

	/** interaccia di output */
	private final OutInterface log;

	/** il peer su cui eseguire i comandi */
	private final BitCreekPeer peer;

	/**
	 * Crea un interprete dei comandi.
	 * 
	 * @param in
	 *            l'oggetto da cui vengono letti i comandi da eseguire
	 * @param log
	 *            l'interfaccia su cui mostrare l'output
	 * @param peer
	 *            il peer su cui eseguire i comandi
	 */
	public CommandInterpreter(BufferedReader in, OutInterface log, BitCreekPeer peer) {
		this.in = in;
		this.log = log;
		this.peer = peer;
	}

	/** Avvia l'interprete. */
	void execute() {
		try {
			String nextLine = null;
			while ((nextLine = in.readLine()) != null && !nextLine.trim().equals("")
					&& !this.isInterrupted()) {
				nextLine = nextLine.trim();
				StringTokenizer token = new StringTokenizer(nextLine);
				String command = token.nextToken();
				String argument = null;

				if (!command.startsWith("leaveNetwork")) {
					argument = nextLine.substring(command.length()).trim();
					log.print(" < " + command + "(" + argument + ")");
				} else {
					log.print(" < " + command + "()");
				}
				if (command.startsWith("publish")) {
					try {
						peer.publish(argument);
						log.println(" > OK");
					} catch (UnableToPublishException e) {
						e.printStackTrace();
						log.println(" > Unable to publish " + e.getMessage());
					}
				} else if (command.startsWith("lookup")) {
					if (peer.lookup(argument) == null) {
						log.println(" > file " + argument + " NOT found");
					} else {
						log.println(" > file " + argument + " found");
					}
				} else if (command.startsWith("download")) {
					TorrentFile torrent;
					if ((torrent = peer.lookup(argument)) == null) {
						log.println(" > file " + argument + " NOT found");
					} else {
						log.println(" > file " + argument + " found");
						peer.retrieve(torrent);
					}
				} else if (command.startsWith("leaveSwarm")) {
					TorrentFile torrent;
					if ((torrent = peer.lookup(argument)) == null) {
						log.println(" > file " + argument + " NOT found");
					} else {
						log.println(" > file " + argument + " found");
						peer.leaveSwarm(torrent);
					}
				} else if (command.startsWith("wait")) {
					if (argument.startsWith("forever")) {
						this.start();
						log.println(" > waiting forever ");
						while (true) {
							try {
								Thread.sleep(100000);
							} catch (InterruptedException e) {
								return;
							}
						}
					}
					log.println(" > OK");
					Thread.sleep(Long.parseLong(argument));
				} else if (command.startsWith("leaveNetwork")) {
					peer.leave();
					return;
				} else {
					throw new IllegalArgumentException(" command file not valid at line \n"
							+ nextLine);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			peer.leave();
		} finally {
			System.exit(-1);
		}
	}
}

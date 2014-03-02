package ioInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Interfaccia di output che scrive sullo stram di output standard del processo e su un file di nome
 * "log"
 * 
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
public class ConsoleOut implements OutInterface {

	PrintWriter log;

	class CloseInterface extends Thread {
		@Override
		public void run() {
			log.close();
		}
	}

	public ConsoleOut() {
		try {
			log = new PrintWriter(new FileOutputStream(new File("log")));
			Runtime.getRuntime().addShutdownHook(new CloseInterface());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public synchronized void println(String string) {
		print(string + "\n", System.out);
	}

	public synchronized void print(String string) {
		print(string, System.out);
	}

	public synchronized void printerr(String string) {
		print(string, System.err);
	}

	private void print(String string, PrintStream stream) {
		stream.print(string);
		stream.flush();
		log.write(string);
	}
}

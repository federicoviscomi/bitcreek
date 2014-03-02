package ioInterface;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map.Entry;

import bitCreekPeer.BitCreekPeer;

/**
 * Un interprete minimale di comandi per conoscere lo stato del peer.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
public class ClientIOConsole {

	final BitCreekPeer peer;

	class Interpreter extends Thread {
		@Override
		public void run() {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				String read = null;
				while (true) {
					do {
						System.out.print("$ ");
						read = in.readLine();
					} while (read == null || read.trim().equals(""));
					read = read.trim();
					if (read.startsWith("lo")) {
						/* elenca i file posseduti con le relative parti */
						System.out.println(" owned file list follows");
						for (Entry<String, ArrayList<Long>> owned : peer.getOwned()) {
							System.out.print("\t file=\"" + owned.getKey() + "\"");
							if (peer.hasACompleteCopy(owned.getKey()))
								System.out.print(" has a complete copy\n");
							else {
								System.out.print("\n  offset list follows \n");
								for (Long part : owned.getValue()) {
									System.out.print(" " + part);
								}
								System.out.println("\n  offset list ends");
							}
						}
						System.out.println(" owned file list ends");
					} else if (read.startsWith("ls")) {
						/* elenca gli swarm di cui fa parte il peer */
						System.out.println(" swarm list follows");
						for (String fileName : peer.getSwarm()) {
							System.out.print(" " + fileName);
						}
						System.out.println("\n swarm list ends");
					} else if (read.startsWith("exit")) {
						System.out.println(" bye ");
						System.exit(-1);
					} else {
						System.err.println(" unrecognized command " + read + "\n USAGE lo ls\n");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

	}

	public ClientIOConsole(BitCreekPeer peer) {
		this.peer = peer;
		new Interpreter().start();
	}
}

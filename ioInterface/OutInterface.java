package ioInterface;

/**
 * Interfaccia di output usata da client e server.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 *
 */
public interface OutInterface {
	
	/**
	 * Mostra all'utente la stringa <param>string</param>. 
	 * 
	 * @param string la stringa da mostrare.
	 */
	void println(String string);

	/**
	 * Mostra all'utente la stringa <param>string</param>. 
	 * 
	 * @param string la stringa da mostrare.
	 */
	void print(String string);

	/**
	 * Mostra all'utente la stringa d'errore <param>string</param> 
	 * 
	 * @param string la stringa da mostrare.
	 */
	void printerr(String string);

}

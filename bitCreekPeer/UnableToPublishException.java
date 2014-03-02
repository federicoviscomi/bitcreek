package bitCreekPeer;

/**
 * Sollevata dal peer quando non e' in grado di publicare un file.
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 *
 */
public class UnableToPublishException extends Exception {
	private static final long serialVersionUID = -8856888571211713870L;
	public UnableToPublishException(String string) {
		super(string);
	}
	public UnableToPublishException() {
	}
}

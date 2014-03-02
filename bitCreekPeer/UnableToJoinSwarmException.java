package bitCreekPeer;

/**
 * Sollevata dal peer quando non riesce a entrare a far parte di uno swarm. 
 * 
 * @author Federico Viscomi 412006 viscomi@cli.di.unipi.it
 * 
 */
public class UnableToJoinSwarmException extends Exception {
	private static final long serialVersionUID = -772038659768378479L;

	public UnableToJoinSwarmException(String string) {
		super(string);
	}

	public UnableToJoinSwarmException() {
	}
}

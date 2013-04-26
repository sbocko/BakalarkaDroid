package eu.jergus.crypto;

/**
 * A simple interface used when a method that takes one
 * {@link RemoteParticipant} as its argument needs to be specified.
 */
public interface ParticipantAction {

	public void run(RemoteParticipant p);

}
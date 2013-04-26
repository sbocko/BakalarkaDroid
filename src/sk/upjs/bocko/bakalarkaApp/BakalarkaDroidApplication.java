package sk.upjs.bocko.bakalarkaApp;

import java.util.List;

import sk.upjs.bocko.protocol.GameInvitation;
import sk.upjs.bocko.protocol.MessageSender;
import sk.upjs.bocko.protocol.RSAcipher;
import sk.upjs.bocko.protocol.RSAkeys;
import eu.jergus.cryperm.Cryperm;
import eu.jergus.crypto.util.ConnectionManager;
import android.app.Application;

public class BakalarkaDroidApplication extends Application {
	protected final int permutationLength = 12;
	protected int elementsUncovered = 0;
	protected int playerCount;
	protected ConnectionManager cm;
	protected int currentUserId = 0;
	protected List<Integer> activeIds;
	public final int TAG_TRUE = 255;
	public final int TAG_FALSE = 254;
	public final int TAG_EXIT = 253;
	public final String TAG_END = "endInv";
	protected MessageSender messageSender;
	public final int PORT_NO = 4201;
	protected RSAcipher rsaCipher = new RSAcipher();
	private GameInvitation gInv;
	protected int localId;
	protected int requiredPlayerCount = 2;
	protected int keySize = 64;
	protected int proofIterations = 10;
	protected Cryperm cryperm;
	protected String[] addresses;
	protected RSAkeys rsaKeys;
	protected int nextUserId;
	protected int playerTrust;
	protected int kocka1;
	protected int kocka2;
	protected int throwValue;

	public GameInvitation getGameInvitation() {
		if (gInv == null) {
			gInv = new GameInvitation(rsaCipher);
		}
		return gInv;
	}

}

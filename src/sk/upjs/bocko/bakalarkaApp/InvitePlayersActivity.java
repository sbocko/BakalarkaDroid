package sk.upjs.bocko.bakalarkaApp;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import sk.upjs.bocko.protocol.AEScipher;
import sk.upjs.bocko.protocol.GameInvitation;
import sk.upjs.bocko.protocol.MessageSender;
import sk.upjs.bocko.protocol.PlayerIpKeyPair;
import sk.upjs.bocko.protocol.RSAkeys;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import eu.jergus.cryperm.Cryperm;
import eu.jergus.crypto.util.ConnectionManager;

public class InvitePlayersActivity extends Activity {
	private BakalarkaDroidApplication app;
	private List<String> hosts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		hosts = new ArrayList<String>();
		app = (BakalarkaDroidApplication) getApplication();

		setContentView(R.layout.create_server);

		ListView invHosts = (ListView) findViewById(R.id.invHostsLV);
		if (invHosts == null) {
			Log.d(InvitePlayersActivity.class.getSimpleName(),
					"invHosts is null!");
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, hosts);
		invHosts.setAdapter(adapter);

	}

	private class InvitationSender extends
			AsyncTask<List<String>, Void, Boolean> {
		private ProgressDialog dialog;

		public InvitationSender() {
			this.dialog = new ProgressDialog(InvitePlayersActivity.this);
			dialog.setCancelable(false);
		}

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage(getString(R.string.sending_invitation));
			this.dialog.show();
		}

		@Override
		protected Boolean doInBackground(List<String>... params) {
			GameInvitation gInv = app.getGameInvitation();
			PlayerIpKeyPair keyPair = null;
			for (int i = 0; i < params[0].size(); i++) {
				try {
					keyPair = gInv.inviteToGame(params[0].get(i));
				} catch (SocketException e) {
					Log.e(InvitePlayersActivity.class.getSimpleName(),
							"Connection error!");
					e.printStackTrace();
					return false;
				}
				Log.d(InvitePlayersActivity.class.getSimpleName(),
						"Received key pair: " + keyPair);
			}

			List<PlayerIpKeyPair> players = gInv.getPlayers();

			// change 10.0.2.15 to localhost address
			for (int i = 0; i < players.size(); i++) {
				if (players.get(i).getIpAddress().equals("10.0.2.15:")) {
					players.get(i).setIpAddress("localhost:");
					break;
				}
			}

			// send addresses
			gInv.sendPlayerAddresses();

			// initialize protocol run

			app.addresses = new String[players.size()];
			for (int i = 0; i < players.size(); i++) {
				app.addresses[i] = players.get(i).getIpAddress()
						+ (app.PORT_NO + i);
			}

			System.out.println("Adresy: " + Arrays.toString(app.addresses));

			app.rsaKeys = new RSAkeys(app.rsaCipher, gInv.getPublicKeys());
			app.playerCount = app.addresses.length;
			app.activeIds = new LinkedList<Integer>();
			for (int i = 0; i < app.playerCount; i++) {
				app.activeIds.add(i);
			}
			app.requiredPlayerCount = 2;

			Log.d(InvitationSender.class.getSimpleName(),
					"Pozvanka akceptovana...poslane kluce aj adresy: "
							+ Arrays.toString(app.addresses));
			try {
				app.cm = new ConnectionManager(app.localId, app.addresses, 2);
			} catch (IOException e) {
				Log.e(InvitationActivity.class.getSimpleName(),
						"Error creating connection between players!");
				return false;
			}
			app.messageSender = new MessageSender(app.cm, app.rsaKeys,
					app.playerCount, app.localId);
			AEScipher aesCipher = app.messageSender.getAesCipher();

			app.cryperm = new Cryperm(app.localId,
					app.cm.getPartialInputStreams(1),
					app.cm.getPartialOutputStreams(1), app.requiredPlayerCount,
					app.permutationLength, app.keySize, app.proofIterations);

			// % playerCount ...initialization
			app.nextUserId = (app.currentUserId + 1) % app.activeIds.size();
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			if (result == false) {
				// invitation failed
				Toast.makeText(InvitePlayersActivity.this,
						getString(R.string.invitation_failed),
						Toast.LENGTH_LONG).show();
			} else {
				Intent intent = new Intent(InvitePlayersActivity.this,
						GameThrowActivity.class);
				startActivity(intent);
			}
		}
	}

	public void addBtnClicked(View view) {
		ListView invHosts = (ListView) findViewById(R.id.invHostsLV);
		EditText ipAddressET = (EditText) findViewById(R.id.ipAddressET);
		String ipAddress = ipAddressET.getText().toString().trim();
		hosts.add(ipAddress);

		invHosts.invalidateViews();
	}

	public void sendBtnClicked(View view) {
		InvitationSender invSender = new InvitationSender();
		invSender.execute(hosts);
	}

	public void clearBtnClicked(View view) {
		ListView invHosts = (ListView) findViewById(R.id.invHostsLV);
		hosts.clear();
		invHosts.invalidateViews();
	}

}

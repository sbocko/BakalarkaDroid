package sk.upjs.bocko.bakalarkaApp;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import sk.upjs.bocko.protocol.AEScipher;
import sk.upjs.bocko.protocol.GameInvitation;
import sk.upjs.bocko.protocol.MessageSender;
import sk.upjs.bocko.protocol.PlayerIpKeyPair;
import sk.upjs.bocko.protocol.RSAkeys;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import eu.jergus.cryperm.Cryperm;
import eu.jergus.crypto.exception.ProtocolException;
import eu.jergus.crypto.util.ConnectionManager;

public class InvitationAcceptActivity extends Activity {
	private BakalarkaDroidApplication app;
	private final String TAG_INV_IP = "INV_IP";
	private String serverIp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (BakalarkaDroidApplication) getApplication();
		setContentView(R.layout.invitation_accept);

		Intent intent = getIntent();
		serverIp = intent.getStringExtra(TAG_INV_IP);
		TextView invNameTV = (TextView) findViewById(R.id.invitationNameTV);
		invNameTV.setText(serverIp);
		Log.d(InvitationAcceptActivity.class.getSimpleName(),
				"Invitation from " + serverIp + " received.");
	}

	public void acceptBtnClicked(View view) {
		Log.d(InvitationAcceptActivity.class.getSimpleName(),
				"Accept button clicked");
		// accept invitation with ACCEPT TAG and receive ip addresses
		AcceptInvitation acceptInv = new AcceptInvitation();
		acceptInv.execute(serverIp);

	}

	public void declineBtnClicked(View view) {
		Log.d(InvitationAcceptActivity.class.getSimpleName(),
				"Decline button clicked");
		// return to previous activity
		super.onBackPressed();
	}

	private class AcceptInvitation extends AsyncTask<String, Void, Boolean> {
		private ProgressDialog dialog;

		public AcceptInvitation() {
			this.dialog = new ProgressDialog(InvitationAcceptActivity.this);
			dialog.setCancelable(false);
		}

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage(getString(R.string.accepting_inv));
			this.dialog.show();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			GameInvitation gInv = app.getGameInvitation();
			Log.d(AcceptInvitation.class.getSimpleName(),
					"Accepting invitation from " + params[0]);
			// accept invitation
			int id = gInv.acceptInvitation(params[0]);

			if (id == -1) {
				Log.d(InvitationActivity.class.getSimpleName(),
						"Server not responding!");
				return false;
			} else {
				Log.d(InvitationActivity.class.getSimpleName(),
						"Received local id: " + id);
				app.localId = id;
				// receive other players addresses
				try {
					gInv.receivePlayerAddresses();
				} catch (SocketTimeoutException e) {
					Log.d(InvitationActivity.class.getSimpleName(),
							"Server not responding!");
					return false;
				}
				// initialize protocol run
				List<PlayerIpKeyPair> players = gInv.getPlayers();
				app.addresses = new String[players.size()];
				for (int i = 0; i < players.size(); i++) {
					app.addresses[i] = players.get(i).getIpAddress()
							+ (app.PORT_NO + i);
				}
				if (app.addresses[0].equals("127.0.0.1:" + app.PORT_NO)) {
					app.addresses[0] = "10.0.2.2:" + app.PORT_NO;
				}
				Log.d(AcceptInvitation.class.getSimpleName(),
						"Adresy po preklade: " + Arrays.toString(app.addresses));

				app.rsaKeys = new RSAkeys(app.rsaCipher, gInv.getPublicKeys());
				app.playerCount = app.addresses.length;
				app.activeIds = new LinkedList<Integer>();
				for (int i = 0; i < app.playerCount; i++) {
					app.activeIds.add(i);
				}
				app.requiredPlayerCount = 2;

				try {
					app.cm = new ConnectionManager(app.localId, app.addresses,
							2);
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
						app.cm.getPartialOutputStreams(1),
						app.requiredPlayerCount, app.permutationLength,
						app.keySize, app.proofIterations);
				// % playerCount ...initialization
				app.nextUserId = (app.currentUserId + 1) % app.activeIds.size();
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			if (result) {
				// protocol initialization successful
				// TODO game
				Toast.makeText(InvitationAcceptActivity.this,
						"Invitation was succesfully accepted",
						Toast.LENGTH_LONG).show();
				Intent intent = new Intent(InvitationAcceptActivity.this,
						GameInactiveActivity.class);
				startActivity(intent);
			} else {
				// protocol initialization failed
				// show alert dialog and return to initial activity
				AlertDialog alertDialog = new AlertDialog.Builder(
						InvitationAcceptActivity.this).create();
				alertDialog.setTitle(getString(R.string.error_accepting_inv));
				alertDialog
						.setMessage(getString(R.string.inv_acc_error_message));
				alertDialog.setButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								Intent intent = new Intent(
										InvitationAcceptActivity.this,
										InvitationActivity.class);
								InvitationAcceptActivity.this
										.startActivity(intent);
								InvitationAcceptActivity.this.finish();
							}
						});
				alertDialog.show();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}

}

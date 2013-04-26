package sk.upjs.bocko.bakalarkaApp;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

import sk.upjs.bocko.protocol.GameInvitation;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class InvitationActivity extends Activity {
	BakalarkaDroidApplication app;

	private static final String TAG_INV_IP = "INV_IP";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (BakalarkaDroidApplication) getApplication();
		Log.d(InvitationActivity.class.getSimpleName(),"Local ip: " + getLocalIpAddress());
		
		setContentView(R.layout.choose_server);
	}

	private class WaitForInvitation extends AsyncTask<Void, Void, String> {
		private ProgressDialog dialog;

		public WaitForInvitation() {
			this.dialog = new ProgressDialog(InvitationActivity.this);
			dialog.setCancelable(false);
		}

		@Override
		protected void onPreExecute() {
			this.dialog
					.setMessage(getString(R.string.looking_for_available_servers));
			this.dialog.show();
		}

		@Override
		protected String doInBackground(Void... params) {
			GameInvitation gInv = app.getGameInvitation();
			String invitation = null;
			try {
				invitation = gInv.getInvitation();
			} catch (SocketTimeoutException e) {
				Log.d(InvitationActivity.class.getSimpleName(),
						"No invitation received. Socket timeout exception.");
			}
			Log.d(InvitationActivity.class.getSimpleName(),
					"Received Invitation: " + invitation);
			return invitation;
		}

		@Override
		protected void onPostExecute(String result) {
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			if (result == null) {
				// no invitation received
				Toast.makeText(InvitationActivity.this,
						getString(R.string.no_invitation_received),
						Toast.LENGTH_LONG).show();
			} else {
				// start new activity for invitation
				Intent intent = new Intent(InvitationActivity.this,
						InvitationAcceptActivity.class);
				intent.putExtra(TAG_INV_IP, result);
				InvitationActivity.this.startActivity(intent);
			}
		}
	}

	public void createServerClicked(View view) {
//		Toast.makeText(this, "Create server clicked.", Toast.LENGTH_LONG)
//				.show();
		Intent intent = new Intent(InvitationActivity.this,
				InvitePlayersActivity.class);
		InvitationActivity.this.startActivity(intent);
	}

	public void joinServerClicked(View view) {
		WaitForInvitation invTask = new WaitForInvitation();
		invTask.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(InvitationActivity.class.getSimpleName(), ex.toString());
		}
		return null;
	}

}

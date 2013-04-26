package sk.upjs.bocko.bakalarkaApp;

import java.io.IOException;

import sk.upjs.bocko.protocol.AEScipher;
import sk.upjs.bocko.protocol.MessageSender;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import eu.jergus.cryperm.Cryperm;
import eu.jergus.crypto.exception.ProtocolException;
import eu.jergus.crypto.util.ConnectionManager;

public class GameInactiveActivity extends Activity {
	BakalarkaDroidApplication app;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(GameInactiveActivity.class.getSimpleName(), "Activity created.");
		app = (BakalarkaDroidApplication) getApplication();

		setContentView(R.layout.game_inactive);
		TextView turnInactiveTV = (TextView) findViewById(R.id.turnInactiveInfoTV);
		turnInactiveTV.setText("Toss player number "
				+ app.activeIds.get(app.currentUserId));

		TextView stateInactiveTV = (TextView) findViewById(R.id.stateInactiveTV);
		stateInactiveTV.setText("Please wait...");

		nextRound();
	}

	private void nextRound() {
		NewThrowGenerator newThrowGenerator = new NewThrowGenerator();
		newThrowGenerator.execute();
	}

	private class NewThrowGenerator extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Log.d(NewThrowGenerator.class.getSimpleName(), "Player "
						+ app.activeIds.get(app.currentUserId)
						+ " is throwing cubes...");
				// odkryjeme 2 hody pre dalsieho hraca
				if (app.elementsUncovered >= app.permutationLength / 2) {
					app.cryperm.kill();

					app.cm = new ConnectionManager(app.localId, app.addresses,
							2);

					AEScipher aesCipher = app.messageSender.getAesCipher();
					app.messageSender = new MessageSender(app.cm, aesCipher,
							app.rsaKeys, app.playerCount, app.localId);
					app.cryperm = new Cryperm(app.localId,
							app.cm.getPartialInputStreams(1),
							app.cm.getPartialOutputStreams(1),
							app.requiredPlayerCount, app.permutationLength,
							app.keySize, app.proofIterations);
					Log.d(NewThrowGenerator.class.getSimpleName(),
							"Generating new permutation.");
					app.elementsUncovered = 0;
				}

				app.kocka1 = (app.cryperm.uncover(app.activeIds
						.get(app.currentUserId)) % 6) + 1;
				app.kocka2 = (app.cryperm.uncover(app.activeIds
						.get(app.currentUserId)) % 6) + 1;
				app.elementsUncovered += 2;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ProtocolException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			// pockam na prijatu spravu o hode
			ReceiveThrowValueTask receiveTask = new ReceiveThrowValueTask();
			receiveTask.execute();

		}

	}

	private class ReceiveThrowValueTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			int thrown = Integer.parseInt(app.messageSender
					.receiveSignedMessage(app.activeIds.get(app.currentUserId),
							true));

			return thrown;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			app.throwValue = result;
			// prijal som hodnotu hodu a musim poslat odpoved
			if (app.activeIds.get(app.nextUserId) == app.localId) {
				Intent intent = new Intent(GameInactiveActivity.this,
						GameTrustActivity.class);
				startActivity(intent);

				// inactive spravia update a cakaju na spravu o dovere
			} else {

				TextView throwInactiveTV = (TextView) findViewById(R.id.throwInactiveTV);
				throwInactiveTV.setText("" + result);

				TextView stateInactiveTV = (TextView) findViewById(R.id.stateInactiveTV);
				stateInactiveTV.setText("Player "
						+ app.activeIds.get(app.nextUserId) + " is on turn");

				// wait for trust msg
				ReceiveTrustValueTask receiveTask = new ReceiveTrustValueTask();
				receiveTask.execute();
			}

		}

	}

	private class ReceiveTrustValueTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			int thrown = Integer.parseInt(app.messageSender
					.receiveSignedMessage(app.activeIds.get(app.nextUserId),
							true));

			return thrown;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);

			if (result == app.TAG_TRUE) {
				Toast.makeText(
						GameInactiveActivity.this,
						"Player " + app.activeIds.get(app.nextUserId)
								+ " trusts to player number"
								+ app.activeIds.get(app.currentUserId),
						Toast.LENGTH_LONG).show();
				// nove kolo
				app.currentUserId = app.nextUserId;
				app.nextUserId = (app.currentUserId + 1) % app.activeIds.size();
				nextRound();
			} else {
				// koniec hry
				Toast.makeText(
						GameInactiveActivity.this,
						"Player " + app.activeIds.get(app.nextUserId)
								+ " doesn't trust player number"
								+ app.activeIds.get(app.currentUserId),
						Toast.LENGTH_LONG).show();

			}
		}
	}

	private class ReceiveTruthTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				int result = Integer.parseInt(app.messageSender
						.receiveSignedMessage(app.currentUserId, true));

				app.cryperm.uncover(app.activeIds.get(app.nextUserId),
						app.elementsUncovered - 2);
				app.cryperm.uncover(app.activeIds.get(app.nextUserId),
						app.elementsUncovered - 1);

				return result;
			} catch (ProtocolException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			Intent intent = new Intent(GameInactiveActivity.this,
					EndGameActivity.class);
			if (result.intValue() == app.TAG_FALSE) {
				intent.putExtra(
						EndGameActivity.END_MESSAGE,
						"Player " + app.activeIds.get(app.currentUserId)
								+ " lied. Won player number "
								+ app.activeIds.get(app.nextUserId));
			} else {
				intent.putExtra(EndGameActivity.END_MESSAGE,
						"Player " + app.activeIds.get(app.currentUserId)
								+ " told Truth. Won player number "
								+ app.activeIds.get(app.currentUserId));
			}
			startActivity(intent);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}

}

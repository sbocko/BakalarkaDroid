package sk.upjs.bocko.bakalarkaApp;

import java.io.IOException;

import sk.upjs.bocko.protocol.AEScipher;
import sk.upjs.bocko.protocol.MessageSender;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import eu.jergus.cryperm.Cryperm;
import eu.jergus.crypto.exception.ProtocolException;
import eu.jergus.crypto.util.ConnectionManager;

public class GameThrowActivity extends Activity {
	private BakalarkaDroidApplication app;
	private int valueToSend;
	private int oldThrowValue;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(GameThrowActivity.class.getSimpleName(), "Activity created.");
		app = (BakalarkaDroidApplication) getApplication();
		oldThrowValue = app.throwValue;
		
		Log.d(GameThrowActivity.class.getSimpleName(), "currentUser: "
				+ app.activeIds.get(app.currentUserId) + ", nextUser: "
				+ app.activeIds.get(app.nextUserId));

		setContentView(R.layout.game_throw);

		TextView turnThrowInfoTV = (TextView) findViewById(R.id.turnThrowInfoTV);
		turnThrowInfoTV.setText("Throwing cubes...");
		Spinner spinner = (Spinner) findViewById(R.id.throwValueSpinner);
		SpinnerAdapter adap = new ArrayAdapter<String>(GameThrowActivity.this,
				R.layout.my_spinner_style, getResources().getStringArray(
						R.array.throw_values));
		spinner.setAdapter(adap);

		nextRound();

	}

	private void nextRound() {
		NewThrowGenerator newThrowGenerator = new NewThrowGenerator();
		newThrowGenerator.execute();
	}

	public void sendValueBtnClicked(View view) {
		Spinner spinner = (Spinner) findViewById(R.id.throwValueSpinner);
		valueToSend = Integer.parseInt((String) spinner.getSelectedItem());

		if (valueToSend <= oldThrowValue) {
			Toast.makeText(this, "You have to send value greater than " + oldThrowValue,Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "Selected: -" + valueToSend + "-",
					Toast.LENGTH_LONG).show();

			// poslem spravu o hode
			SendValueTask sendTask = new SendValueTask();
			sendTask.execute(valueToSend);
		}
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

				if (app.kocka1 > app.kocka2) {
					app.throwValue = 10 * app.kocka1 + app.kocka2;
				} else {
					app.throwValue = 10 * app.kocka2 + app.kocka1;
				}
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
			TextView turnThrowInfoTV = (TextView) findViewById(R.id.turnThrowInfoTV);
			turnThrowInfoTV.setText(getString(R.string.youThrew));
			TextView throwTV = (TextView) findViewById(R.id.throwTV);
			throwTV.setText("" + app.throwValue);
		}
	}

	private class UncoverTruthTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (app.throwValue == valueToSend) {
					app.messageSender.sendSignedMessageToAll("" + app.TAG_TRUE);
				} else {
					app.messageSender
							.sendSignedMessageToAll("" + app.TAG_FALSE);
				}

				app.cryperm.uncover(app.activeIds.get(app.nextUserId),
						app.elementsUncovered - 2);
				app.cryperm.uncover(app.activeIds.get(app.nextUserId),
						app.elementsUncovered - 1);

			} catch (ProtocolException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			Intent intent = new Intent(GameThrowActivity.this,
					EndGameActivity.class);
			if (app.throwValue == valueToSend) {
				intent.putExtra(EndGameActivity.END_MESSAGE,
						getString(R.string.winner));
			} else {
				intent.putExtra(EndGameActivity.END_MESSAGE,
						getString(R.string.looser));
			}
			startActivity(intent);
		}
	}

	private class SendValueTask extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... params) {

			app.messageSender.sendSignedMessageToAll("" + params[0]);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			// pockam na spravu o dovere
			Toast.makeText(GameThrowActivity.this, "value sent",
					Toast.LENGTH_SHORT).show();
			Button throwSendBtn = (Button) findViewById(R.id.throwSendBtn);
			throwSendBtn.setClickable(false);

			ReceiveTrustValueTask receiveTrustTask = new ReceiveTrustValueTask();
			receiveTrustTask.execute();

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
						GameThrowActivity.this,
						"Player " + app.activeIds.get(app.nextUserId)
								+ " trusts you", Toast.LENGTH_LONG).show();
				// nove kolo
				app.currentUserId = app.nextUserId;
				app.nextUserId = (app.currentUserId + 1) % app.activeIds.size();
				Intent intent = new Intent(GameThrowActivity.this,
						GameInactiveActivity.class);
				startActivity(intent);
			} else {
				// koniec hry
				Toast.makeText(
						GameThrowActivity.this,
						"Player " + app.activeIds.get(app.nextUserId)
								+ " doesn't trust you", Toast.LENGTH_LONG)
						.show();

				// odkry vysledok
				new UncoverTruthTask().execute();

			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}
}

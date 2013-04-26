package sk.upjs.bocko.bakalarkaApp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import eu.jergus.crypto.exception.ProtocolException;

public class GameTrustActivity extends Activity {
	private BakalarkaDroidApplication app;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(GameTrustActivity.class.getSimpleName(), "Activity created.");
		app = (BakalarkaDroidApplication) getApplication();

		setContentView(R.layout.game_turn);
		TextView turnInfoTV = (TextView) findViewById(R.id.turnInfoTV);
		turnInfoTV.setText("Player " + app.activeIds.get(app.currentUserId)
				+ " threw");
		TextView throwTurnTV = (TextView) findViewById(R.id.throwTurnTV);
		throwTurnTV.setText("" + app.throwValue);
	}

	public void trustBtnClicked(View view) {
		SendValueTask sendTask = new SendValueTask();
		sendTask.execute(app.TAG_TRUE);
		app.playerTrust = app.TAG_TRUE;
		newRound();
	}

	public void dontTrustBtnClicked(View view) {
		SendValueTask sendTask = new SendValueTask();
		sendTask.execute(app.TAG_FALSE);
		app.playerTrust = app.TAG_FALSE;
		// koniec hry
		new ReceiveTruthTask().execute();
	}

	private class ReceiveTruthTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				Integer.parseInt(app.messageSender
						.receiveSignedMessage(app.currentUserId, true));

				app.kocka1 = (app.cryperm.uncover(
						app.activeIds.get(app.nextUserId),
						app.elementsUncovered - 2) % 6) + 1;
				app.kocka2 = (app.cryperm.uncover(
						app.activeIds.get(app.nextUserId),
						app.elementsUncovered - 1) % 6) + 1;

				int realThrow1 = app.kocka1 * 10 + app.kocka2;
				int realThrow2 = app.kocka2 * 10 + app.kocka1;
				Log.d(ReceiveTruthTask.class.getSimpleName(),
						"possible real throws: " + realThrow1 + " and "
								+ realThrow2);
				
				int result = app.TAG_FALSE;
				if(app.throwValue == realThrow1 || app.throwValue == realThrow2){
					result = app.TAG_TRUE;
				}
				Log.d(ReceiveTruthTask.class.getSimpleName(),
						"result: " + result + " and actthrow"
								+ app.throwValue);
				return result;

			} catch (ProtocolException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			Intent intent = new Intent(GameTrustActivity.this,
					EndGameActivity.class);
			if (result.intValue() == app.TAG_FALSE) {
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
			if (params[0] == app.TAG_TRUE) {
				app.messageSender.sendSignedMessageToAll(Integer
						.toString(app.TAG_TRUE));
			} else {
				app.messageSender.sendSignedMessageToAll(Integer
						.toString(app.TAG_FALSE));
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}

	}

	private void newRound() {
		app.currentUserId = app.nextUserId;
		// % playerCount
		app.nextUserId = (app.currentUserId + 1) % app.activeIds.size();

		Intent intent = new Intent(GameTrustActivity.this,
				GameThrowActivity.class);
		startActivity(intent);
	}

	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}

}

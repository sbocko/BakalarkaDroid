package sk.upjs.bocko.bakalarkaApp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class EndGameActivity extends Activity {
	public static final String END_MESSAGE = "END_MSG";
	private BakalarkaDroidApplication app;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (BakalarkaDroidApplication) this.getApplication();
		
		setContentView(R.layout.game_finished);
		TextView winStateTV = (TextView) findViewById(R.id.winStateTV);
		String text = getIntent().getStringExtra(END_MESSAGE);
		winStateTV.setText(text);
	}

	public void newGameBtnClicked(View view) {
//		Intent intent = new Intent(EndGameActivity.this,
//				InvitationActivity.class);
		app.throwValue = 0;
		app.currentUserId = app.nextUserId;
		app.nextUserId = (app.currentUserId + 1) % app.activeIds.size();
		Intent intent;
		if(app.activeIds.get(app.currentUserId) == app.localId){
			intent = new Intent(EndGameActivity.this, GameThrowActivity.class);
		}else{
			intent = new Intent(EndGameActivity.this, GameInactiveActivity.class);
		}
		startActivity(intent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}
}

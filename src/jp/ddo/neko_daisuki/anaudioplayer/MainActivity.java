package jp.ddo.neko_daisuki.anaudioplayer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ViewFlipper;

public class MainActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);

		ViewFlipper flipper = (ViewFlipper)this.findViewById(R.id.flipper);
		int[] next_buttons = { R.id.next0, R.id.next1, R.id.next2 };
		this.setClickListener(next_buttons, new NextFlipper(flipper));
		int[] previous_buttons = { R.id.prev0, R.id.prev1, R.id.prev2 };
		this.setClickListener(previous_buttons, new PreviousFlipper(flipper));
    }

	private void setClickListener(int[] widgets, View.OnClickListener listener) {
		for (int id: widgets) {
			Button button = (Button)this.findViewById(id);
			button.setOnClickListener(listener);
		}
	}

	private abstract class Flipper implements View.OnClickListener {

		public Flipper(ViewFlipper flipper) {
			this.flipper = flipper;
		}

		protected ViewFlipper flipper;
	}

	private class NextFlipper extends Flipper {

		public NextFlipper(ViewFlipper flipper) {
			super(flipper);
		}

		@Override
		public void onClick(View view) {
			this.flipper.showNext();
		}
	}

	private class PreviousFlipper extends Flipper {

		public PreviousFlipper(ViewFlipper flipper) {
			super(flipper);
		}

		@Override
		public void onClick(View view) {
			this.flipper.showPrevious();
		}
	}
}

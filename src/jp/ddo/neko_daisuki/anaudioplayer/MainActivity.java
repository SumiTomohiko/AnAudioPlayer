package jp.ddo.neko_daisuki.anaudioplayer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ViewFlipper;

import jp.ddo.neko_daisuki.android.widget.UzumakiSlider;

public class MainActivity extends Activity
{
	private static final String log_tag = "An Audio Player";

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);

		this.initializeFlipButtonListener();
		this.initializeDirList();
		this.initializeAnimation();
		this.initializePlayButton();

		this.task = new PlayerTask(this);
    }

	private void initializePlayButton() {
		this.play_button = (Button)this.findViewById(R.id.play);
		this.pause_listener = new PauseButtonListener(this);
		this.play_button.setOnClickListener(this.pause_listener);
		this.play_listener = new PlayButtonListener(this);
	}

	private class ActivityListener {

		public ActivityListener(MainActivity activity) {
			this.activity = activity;
		}

		protected MainActivity activity;
	}

	private class PauseButtonListener extends ActivityListener implements View.OnClickListener {

		public PauseButtonListener(MainActivity activity) {
			super(activity);
		}

		@Override
		public void onClick(View view) {
			this.activity.pause();
		}
	}

	private class PlayButtonListener extends ActivityListener implements View.OnClickListener {

		public PlayButtonListener(MainActivity activity) {
			super(activity);
		}

		@Override
		public void onClick(View view) {
			this.activity.play();
		}
	}

	private static final long ANIMATION_DURATION = 250;
	//private static final int INTERPOLATOR = android.R.anim.decelerate_interpolator;
	private static final int INTERPOLATOR = android.R.anim.linear_interpolator;

	private Animation loadAnimation(int id, Interpolator interp) {
		Animation anim = AnimationUtils.loadAnimation(this, id);
		anim.setDuration(ANIMATION_DURATION);
		anim.setInterpolator(interp);
		return anim;
	}

	private void initializeAnimation() {
		Interpolator interp = AnimationUtils.loadInterpolator(this, INTERPOLATOR);
		this.leftInAnimation = this.loadAnimation(R.anim.anim_left_in, interp);
		this.leftOutAnimation = this.loadAnimation(R.anim.anim_left_out, interp);
		this.rightInAnimation = this.loadAnimation(R.anim.anim_right_in, interp);
		this.rightOutAnimation = this.loadAnimation(R.anim.anim_right_out, interp);
	}

	private static final String MEDIA_PATH = "/sdcard/u1";

	private void initializeDirList() {
		ListView view = (ListView)this.findViewById(R.id.dir_list);
		int layout = android.R.layout.simple_list_item_1;
		this.dirList = this.listMp3Dir(new File(MEDIA_PATH));
		view.setAdapter(new ArrayAdapter<String>(this, layout, this.dirList));
		view.setOnItemClickListener(new DirectoryListListener(this));
	}

	private File[] listFiles(File dir, FilenameFilter filter) {
		File[] files;
		try {
			files = dir.listFiles(filter);
		}
		catch (SecurityException _) {
			files = null;
		}
		return files != null ? files : (new File[0]);
	}

	private List<String> listMp3Dir(File dir) {
		List<String> list = new ArrayList<String>();

		for (File d: this.listFiles(dir, new DirectoryFilter())) {
			list.addAll(this.listMp3Dir(d));
		}
		if (0 < this.listFiles(dir, new Mp3Filter()).length) {
			try {
				list.add(dir.getCanonicalPath());
			}
			catch (IOException _) {
			}
		}

		return list;
	}

	private class Mp3Filter implements FilenameFilter {

		public boolean accept(File dir, String name) {
			return name.endsWith(".mp3");
		}
	}

	private class DirectoryFilter implements FilenameFilter {

		public boolean accept(File dir, String name) {
			String path;
			try {
				path = dir.getCanonicalPath() + File.separator + name;
			}
			catch (IOException _) {
				return false;
			}
			return (new File(path)).isDirectory();
		}
	}

	private void initializeFlipButtonListener() {
		this.flipper = (ViewFlipper)this.findViewById(R.id.flipper);
		int[] next_buttons = { R.id.next0, R.id.next1, R.id.next2 };
		this.setClickListener(next_buttons, new NextButtonListener(this));
		int[] previous_buttons = { R.id.prev0, R.id.prev1, R.id.prev2 };
		this.setClickListener(previous_buttons, new PreviousButtonListener(this));
	}

	private void selectDir(int position) {
		int layout = android.R.layout.simple_list_item_1;
		String dir = this.dirList.get(position);
		this.files = (new File(dir)).list(new Mp3Filter());

		ListView view = (ListView)this.findViewById(R.id.file_list);
		view.setAdapter(new ArrayAdapter<String>(this, layout, this.files));
		view.setOnItemClickListener(new FileListListener(this));

		this.showNext();
	}

	private void pause() {
		this.timer.cancel();
		this.play_button.setOnClickListener(this.play_listener);
	}

	private class PlayerTask extends TimerTask {

		public PlayerTask(MainActivity activity) {
			this.handler = new Handler();
			this.proc = new Proc(activity);
		}

		public class Proc implements Runnable {

			public Proc(MainActivity activity) {
				this.activity = activity;
			}

			public void run() {
				this.activity.updateSlider();
			}

			private MainActivity activity;
		}

		@Override
		public void run() {
			this.handler.post(this.proc);
		}

		private Handler handler;
		private Runnable proc;
	}

	private void updateSlider() {
		UzumakiSlider slider = (UzumakiSlider)this.findViewById(R.id.slider);
		slider.setProgress(slider.getProgress() + 1);
	}

	private void play() {
		this.timer = new Timer(true);
		this.timer.scheduleAtFixedRate(this.task, 0, 100);

		this.play_button.setOnClickListener(this.pause_listener);
	}

	private void selectFile(int position) {
		this.filePosition = position;
		this.showNext();
		this.play();
	}

	private void showPrevious() {
		this.flipper.setInAnimation(this.leftInAnimation);
		this.flipper.setOutAnimation(this.rightOutAnimation);
		this.flipper.showPrevious();
	}

	private void showNext() {
		this.flipper.setInAnimation(this.rightInAnimation);
		this.flipper.setOutAnimation(this.leftOutAnimation);
		this.flipper.showNext();
	}

	private abstract class ListListener implements AdapterView.OnItemClickListener {

		public ListListener(MainActivity activity) {
			this.activity = activity;
		}

		protected MainActivity activity;
	}

	private class DirectoryListListener extends ListListener {

		public DirectoryListListener(MainActivity activity) {
			super(activity);
		}

		public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
			this.activity.selectDir(position);
		}
	}

	private class FileListListener extends ListListener {

		public FileListListener(MainActivity activity) {
			super(activity);
		}

		public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
			this.activity.selectFile(position);
		}
	}

	private void setClickListener(int[] widgets, View.OnClickListener listener) {
		for (int id: widgets) {
			Button button = (Button)this.findViewById(id);
			button.setOnClickListener(listener);
		}
	}

	private abstract class FlipButtonListener implements View.OnClickListener {

		public FlipButtonListener(MainActivity activity) {
			this.activity = activity;
		}

		protected MainActivity activity;
	}

	private class NextButtonListener extends FlipButtonListener {

		public NextButtonListener(MainActivity activity) {
			super(activity);
		}

		@Override
		public void onClick(View view) {
			this.activity.showNext();
		}
	}

	private class PreviousButtonListener extends FlipButtonListener {

		public PreviousButtonListener(MainActivity activity) {
			super(activity);
		}

		@Override
		public void onClick(View view) {
			this.activity.showPrevious();
		}
	}

	private List<String> dirList = null;
	private String[] files = new String[0];
	private ViewFlipper flipper;
	private Animation leftInAnimation;
	private Animation leftOutAnimation;
	private Animation rightInAnimation;
	private Animation rightOutAnimation;
	private int filePosition;

	private Button play_button;
	private View.OnClickListener pause_listener;
	private View.OnClickListener play_listener;
	private Timer timer;
	private PlayerTask task;
}

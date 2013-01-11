package jp.ddo.neko_daisuki.anaudioplayer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import jp.ddo.neko_daisuki.android.widget.RotatingUzumakiSlider;
import jp.ddo.neko_daisuki.android.widget.UzumakiHead;
/*
 * UzumakiImageHead is referred only from main.xml. So if without the following import statment,
 * UzumakiImageHead.java will be out of compile targets.
 */
import jp.ddo.neko_daisuki.android.widget.UzumakiImageHead;
import jp.ddo.neko_daisuki.android.widget.UzumakiSlider;

public class MainActivity extends Activity
{
    private class RotatingListener {

        protected MainActivity activity;

        public RotatingListener(MainActivity activity) {
            this.activity = activity;
        }
    }

    private class OnStartRotatingListener extends RotatingListener implements RotatingUzumakiSlider.OnStartRotatingListener {

        public OnStartRotatingListener(MainActivity activity) {
            super(activity);
        }

        public void onStartRotating(RotatingUzumakiSlider slider) {
            this.activity.pause();
        }
    }

    private class OnStopRotatingListener extends RotatingListener implements RotatingUzumakiSlider.OnStopRotatingListener {

        public OnStopRotatingListener(MainActivity activity) {
            super(activity);
        }

        public void onStopRotating(RotatingUzumakiSlider slider) {
            this.activity.onStopHeadMoving();
        }
    }

    private interface Player {

        public void setup(String path) throws IOException;
        public int getCurrentPosition();
        public void play();
        public void pause();
        public void release();
        public void seekTo(int msec);
    }

    private interface ProcAfterSeeking {

        public void run();
    }

    private class PlayAfterSeeking implements ProcAfterSeeking {

        private MainActivity activity;

        public PlayAfterSeeking(MainActivity activity) {
            this.activity = activity;
        }

        public void run() {
            this.activity.play();
        }
    }

    private class StayAfterSeeking implements ProcAfterSeeking {

        public void run() {
        }
    }

    private class SliderLogger implements UzumakiSlider.Logger {

        private MainActivity activity;

        public SliderLogger(MainActivity activity) {
            this.activity = activity;
        }

        public void log(String msg) {
            this.activity.log(msg);
        }
    }

    private class FakePlayer implements Player {

        public void setup(String path) throws IOException {
        }

        public int getCurrentPosition() {
            return 0;
        }

        public void play() {
        }

        public void pause() {
        }

        public void seekTo(int msec) {
        }

        public void release() {
        }
    }

    private class TruePlayer implements Player {

        private MediaPlayer mp;

        public TruePlayer() {
            this.mp = new MediaPlayer();
        }

        public void setup(String path) throws IOException {
            this.mp.reset();
            this.mp.setDataSource(path);
            this.mp.prepare();
        }

        public int getCurrentPosition() {
            return this.mp.getCurrentPosition();
        }

        public void play() {
            this.mp.start();
        }

        public void pause() {
            this.mp.pause();
        }
        public void seekTo(int msec) {
            this.mp.seekTo(msec);
        }

        public void release() {
            this.mp.release();
        }
    }

    private class OnStartHeadMovingListener implements UzumakiSlider.OnStartHeadMovingListener {

        private MainActivity activity;

        public OnStartHeadMovingListener(MainActivity activity) {
            this.activity = activity;
        }

        public void onStartHeadMoving(UzumakiSlider slider, UzumakiHead head) {
            this.activity.pause();
        }
    }

    private class OnStopHeadMovingListener implements UzumakiSlider.OnStopHeadMovingListener {

        private MainActivity activity;

        public OnStopHeadMovingListener(MainActivity activity) {
            this.activity = activity;
        }

        public void onStopHeadMoving(UzumakiSlider slider, UzumakiHead head) {
            this.activity.onStopHeadMoving();
        }
    }

    private abstract class MenuDispatcher {

        protected MainActivity activity;

        public MenuDispatcher(MainActivity activity) {
            this.activity = activity;
        }

        public boolean dispatch() {
            this.callback();
            return true;
        }

        protected abstract void callback();
    }

    private class AboutDispatcher extends MenuDispatcher {

        public AboutDispatcher(MainActivity activity) {
            super(activity);
        }

        protected void callback() {
            this.activity.showAbout();
        }
    }

    private static final String LOG_TAG = "An Audio Player";

    private ViewFlipper flipper;

    private ListView dirList;
    private View nextButton0;

    private View prevButton1;
    private ListView fileList;
    private View nextButton1;

    private View prevButton2;
    private Button playButton;
    private RotatingUzumakiSlider slider;
    private UzumakiHead head;
    private TextView title;
    private TextView currentTime;
    private TextView totalTime;

    private List<String> dirs = null;
    private String selectedDir = null;
    private String[] files = new String[0];
    private int filePosition;
    private Player player = new FakePlayer();

    private Animation leftInAnimation;
    private Animation leftOutAnimation;
    private Animation rightInAnimation;
    private Animation rightOutAnimation;

    private View.OnClickListener pauseListener;
    private View.OnClickListener playListener;
    private TimerInterface timer;
    private FakeTimer fakeTimer;
    private ProcAfterSeeking procAfterSeeking;

    private Map<Integer, MenuDispatcher> menuDispatchers = new HashMap<Integer, MenuDispatcher>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);

        this.findViews();
        this.initializeFlipButtonListener();
        this.initializeDirList();
        this.initializeAnimation();
        this.initializePlayButton();
        this.initializeTimer();
        this.initializeSlider();
        this.initializeMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MenuDispatcher dispatcher = this.menuDispatchers.get(item.getItemId());
        return dispatcher != null ? dispatcher.dispatch() : super.onOptionsItemSelected(item);
    }

    private void showAbout() {
        Intent i = new Intent(this, AboutActivity.class);
        this.startActivity(i);
    }

    private void initializeMenu() {
        this.menuDispatchers.put(R.id.about, new AboutDispatcher(this));
    }

    private void initializeSlider() {
        this.slider.attachHead(this.head);
        this.slider.addOnStartHeadMovingListener(new OnStartHeadMovingListener(this));
        this.slider.addOnStopHeadMovingListener(new OnStopHeadMovingListener(this));
        this.slider.addOnStartRotatingListener(new OnStartRotatingListener(this));
        this.slider.addOnStopRotatingListener(new OnStopRotatingListener(this));
        this.slider.setLogger(new SliderLogger(this));
    }

    private void initializeTimer() {
        this.timer = this.fakeTimer = new FakeTimer();
    }

    private void findViews() {
        this.flipper = (ViewFlipper)this.findViewById(R.id.flipper);

        this.dirList = (ListView)this.findViewById(R.id.dir_list);
        this.nextButton0 = (View)this.findViewById(R.id.next0);

        this.prevButton1 = (View)this.findViewById(R.id.prev1);
        this.fileList = (ListView)this.findViewById(R.id.file_list);
        this.nextButton1 = (View)this.findViewById(R.id.next1);

        this.prevButton2 = (View)this.findViewById(R.id.prev2);
        this.playButton = (Button)this.findViewById(R.id.play);
        this.slider = (RotatingUzumakiSlider)this.findViewById(R.id.slider);
        this.head = (UzumakiHead)this.findViewById(R.id.head);

        this.title = (TextView)this.findViewById(R.id.title);
        this.currentTime = (TextView)this.findViewById(R.id.current_time);
        this.totalTime = (TextView)this.findViewById(R.id.total_time);
    }

    private void initializePlayButton() {
        this.pauseListener = new PauseButtonListener(this);
        this.playButton.setOnClickListener(this.pauseListener);
        this.playListener = new PlayButtonListener(this);
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
            this.activity.procAfterSeeking = new StayAfterSeeking();
            this.activity.pause();
        }
    }

    private class PlayButtonListener extends ActivityListener implements View.OnClickListener {

        public PlayButtonListener(MainActivity activity) {
            super(activity);
        }

        @Override
        public void onClick(View view) {
            this.activity.procAfterSeeking = new PlayAfterSeeking(this.activity);
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
        this.dirs = this.listMp3Dir(new File(MEDIA_PATH));
        this.dirList.setAdapter(new ArrayAdapter<String>(this, R.layout.dir_row, R.id.path, this.dirs));
        this.dirList.setOnItemClickListener(new DirectoryListListener(this));
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
        View[] next_buttons = { this.nextButton0, this.nextButton1 };
        this.setClickListener(next_buttons, new NextButtonListener(this));
        View[] previous_buttons = { this.prevButton1, this.prevButton2 };
        this.setClickListener(previous_buttons, new PreviousButtonListener(this));
    }

    private void selectDir(int position) {
        this.selectedDir = this.dirs.get(position);
        this.files = (new File(this.selectedDir)).list(new Mp3Filter());

        this.fileList.setAdapter(new ArrayAdapter<String>(this, R.layout.file_row, R.id.name, this.files));
        this.fileList.setOnItemClickListener(new FileListListener(this));

        this.nextButton0.setEnabled(true);
        this.showNext();
    }

    private void pause() {
        this.timer.cancel();
        this.timer = this.fakeTimer;

        this.player.pause();

        this.playButton.setOnClickListener(this.playListener);
        this.playButton.setText(">");
    }

    private class PlayerTask extends TimerTask {

        public PlayerTask(MainActivity activity) {
            this.handler = new Handler();
            this.proc = new Proc(activity);
        }

        private class Proc implements Runnable {

            public Proc(MainActivity activity) {
                this.activity = activity;
            }

            public void run() {
                this.activity.updateCurrentTime();
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

    private void updateCurrentTime() {
        int position = this.player.getCurrentPosition();
        this.slider.setProgress(position);
        //this.showTime(this.currentTime, position);
    }

    private void play() {
        this.timer.cancel();
        this.timer = new TrueTimer();
        // Each Timer requests new TimerTask object (Timers cannot share one task).
        this.timer.scheduleAtFixedRate(new PlayerTask(this), 0, 10);

        this.player.play();

        this.playButton.setOnClickListener(this.pauseListener);
        this.playButton.setText("II");
    }

    private void onStopHeadMoving() {
        this.seekTo(this.slider.getProgress());
        this.procAfterSeeking.run();
    }

    private String getSelectedFile() {
        return this.files[this.filePosition];
    }

    private String getSelectedPath() {
        return this.selectedDir + File.separator + this.getSelectedFile();
    }

    private int getDuration(String path) {
        // MediaMetadataRetriever is not reusable.
        MediaMetadataRetriever meta = new MediaMetadataRetriever();
        meta.setDataSource(path);
        int key = MediaMetadataRetriever.METADATA_KEY_DURATION;
        String datum;
        try {
            datum = meta.extractMetadata(key);
        }
        finally {
            meta.release();
        }
        return Integer.parseInt(datum);
    }

    private void showTime(TextView view, int time_msec) {
        int time_sec = time_msec / 1000;
        int min = (time_sec / 60) % 100;
        int sec = time_sec % 60;
        view.setText(String.format("%02d:%02d", min, sec));
    }

    private void selectFile(int position) {
        this.player.release();

        this.player = new TruePlayer();
        this.filePosition = position;
        String path = this.getSelectedPath();
        try {
            this.player.setup(path);
        }
        catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
            return;
        }

        int duration = this.getDuration(path);
        this.slider.setMax(duration);
        this.slider.setProgress(0);
        this.procAfterSeeking = new PlayAfterSeeking(this);
        this.title.setText(this.getSelectedFile());
        this.showTime(this.currentTime, 0);
        this.showTime(this.totalTime, duration);

        this.nextButton1.setEnabled(true);
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

    private void setClickListener(View[] buttons, View.OnClickListener listener) {
        for (View button: buttons) {
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

    private interface TimerInterface {

        public void scheduleAtFixedRate(TimerTask task, long deley, long period);
        public void cancel();
    }

    private class TrueTimer implements TimerInterface {

        public TrueTimer() {
            this.timer = new Timer(true);
        }

        public void scheduleAtFixedRate(TimerTask task, long deley, long period) {
            this.timer.scheduleAtFixedRate(task, deley, period);
        }

        public void cancel() {
            this.timer.cancel();
        }

        private Timer timer;
    }

    private class FakeTimer implements TimerInterface {

        public void scheduleAtFixedRate(TimerTask task, long deley, long period) {
        }

        public void cancel() {
        }
    }

    private void seekTo(int msec) {
        this.player.seekTo(msec);
    }

    private void log(String msg) {
        this.title.setText(msg);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4

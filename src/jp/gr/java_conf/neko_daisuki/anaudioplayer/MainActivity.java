package jp.gr.java_conf.neko_daisuki.anaudioplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import jp.gr.java_conf.neko_daisuki.android.util.ActivityUtil;
import jp.gr.java_conf.neko_daisuki.android.widget.RotatingUzumakiSlider;
import jp.gr.java_conf.neko_daisuki.android.widget.UzumakiHead;
import jp.gr.java_conf.neko_daisuki.android.widget.UzumakiSlider;

public class MainActivity extends Activity {

    private static class PreferencesUtil {

        public static String[] getStringArray(SharedPreferences prefs,
                                              String key) {
            String s = prefs.getString(key, null);
            return s != null ? s.split("\n") : new String[0];
        }

        public static void putStringArray(Editor editor, String key,
                                          String[] values) {
            int len = values.length;
            editor.putString(key, 0 < len ? buildArray(values) : null);
        }

        private static String buildArray(String[] sa) {
            StringBuilder buffer = new StringBuilder(sa[0]);
            for (int i = 1; i < sa.length; i++) {
                buffer.append("\n");
                buffer.append(sa[i]);
            }
            return buffer.toString();
        }
    }

    private static class FileSystem {

        public String directory;

        /**
         * An array of files' name. This must be non-null.
         */
        public String[] files = new String[0];

        public void copyFrom(FileSystem src) {
            directory = src.directory;
            files = src.files;
        }

        public String toString() {
            String fmt = "directory=%s, files=[%s]";
            return String.format(fmt, directory, join(files));
        }

        private String join(String[] sa) {
            int length = sa.length;
            String init = 0 < length ? sa[0] : "";
            StringBuilder buffer = new StringBuilder(init);
            for (int i = 1; i < length; i++) {
                buffer.append(", ");
                buffer.append(sa[i]);
            }
            return buffer.toString();
        }
    }

    private abstract class Adapter extends ArrayAdapter<String> {

        protected LayoutInflater mInflater;

        public Adapter(String[] objects) {
            super(MainActivity.this, 0, objects);

            String service = Context.LAYOUT_INFLATER_SERVICE;
            mInflater = (LayoutInflater)getSystemService(service);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return convertView == null
                ? getView(position, makeConvertView(parent), parent)
                : makeView(position, convertView);
        }

        protected abstract View makeConvertView(ViewGroup parent);
        protected abstract View makeView(int position, View convertView);
    }

    private class FileAdapter extends Adapter {

        private class Row {

            public ImageView playingIcon;
            public TextView name;
        }

        public FileAdapter(String[] objects) {
            super(objects);
        }

        @Override
        protected View makeView(int position, View convertView) {
            String file = getPlayingFile();
            boolean isPlaying = isPlayingDirectoryShown() && mShownFiles.files[position].equals(file);
            int src = isPlaying ? R.drawable.ic_playing : R.drawable.ic_blank;
            Row row = (Row)convertView.getTag();
            row.playingIcon.setImageResource(src);
            row.name.setText(getItem(position));

            return convertView;
        }

        @Override
        protected View makeConvertView(ViewGroup parent) {
            View view = mInflater.inflate(R.layout.file_row, parent, false);
            Row row = new Row();
            row.playingIcon = (ImageView)view.findViewById(R.id.playing_icon);
            row.name = (TextView)view.findViewById(R.id.name);
            view.setTag(row);
            return view;
        }

        private boolean isPlayingDirectoryShown() {
            String shown = mShownFiles.directory;
            String playing = mPlayingFiles.directory;
            return (shown != null) && shown.equals(playing);
        }
    }

    private class DirectoryAdapter extends Adapter {

        private class Row {

            public ImageView playingIcon;
            public TextView path;
        }

        public DirectoryAdapter(String[] objects) {
            super(objects);
        }

        @Override
        protected View makeView(int position, View convertView) {
            setPlayingIcon(mDirectories[position], convertView);
            return convertView;
        }

        @Override
        protected View makeConvertView(ViewGroup parent) {
            View view = mInflater.inflate(R.layout.dir_row, parent, false);
            Row row = new Row();
            row.playingIcon = (ImageView)view.findViewById(R.id.playing_icon);
            row.path = (TextView)view.findViewById(R.id.path);
            view.setTag(row);
            return view;
        }

        private void setPlayingIcon(String path, View view) {
            String directory = getPlayingDirectory();
            boolean isPlaying = path.equals(directory);
            int src = isPlaying ? R.drawable.ic_playing : R.drawable.ic_blank;
            Row row = (Row)view.getTag();
            row.playingIcon.setImageResource(src);
            row.path.setText(path);
        }
    }

    private interface MessengerWrapper {

        public void send(Message msg) throws RemoteException;
    }

    private static class TrueMessenger implements MessengerWrapper {

        private Messenger mMessenger;

        public TrueMessenger(Messenger messenger) {
            mMessenger = messenger;
        }

        public void send(Message msg) throws RemoteException {
            mMessenger.send(msg);
        }
    }

    private static class FakeMessenger implements MessengerWrapper {

        public void send(Message _) throws RemoteException {
        }
    }

    private class Connection implements ServiceConnection {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Messenger messenger = new Messenger(service);
            mOutgoingMessenger = new TrueMessenger(messenger);

            sendMessage(AudioService.MSG_WHAT_STATUS);
            sendMessage(AudioService.MSG_WHAT_LIST);
            sendMessage(AudioService.MSG_WHAT_AUTO_REPEAT);

            Log.i(LOG_TAG, "MainActivity connected to AudioService.");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(LOG_TAG, "MainActivity disconnected from AudioService.");
        }
    }

    private class OnStartRotatingListener implements RotatingUzumakiSlider.OnStartRotatingListener {

        public void onStartRotating(RotatingUzumakiSlider slider) {
            onStartSliding();
        }
    }

    private class OnStopRotatingListener implements RotatingUzumakiSlider.OnStopRotatingListener {

        public void onStopRotating(RotatingUzumakiSlider slider) {
            mProcAfterSeeking.run();
        }
    }

    private interface ProcBeforeSeeking extends Runnable {
    }

    private class NopBeforeSeeking implements ProcBeforeSeeking {

        @Override
        public void run() {
        }
    }

    private class PauseBeforeSeeking implements ProcBeforeSeeking {

        @Override
        public void run() {
            pause();
        }
    }

    private abstract class ProcAfterSeeking {

        public void run() {
            work();
            mInSeeking = false;
        }

        protected abstract void work();
    }

    private class PlayAfterSeeking extends ProcAfterSeeking {

        protected void work() {
            sendPlay();
        }
    }

    private class StayAfterSeeking extends ProcAfterSeeking {

        protected void work() {
        }
    }

    private class SliderLogger implements UzumakiSlider.Logger {

        public void log(String msg) {
            log(msg);
        }
    }

    private class OnStartHeadMovingListener implements UzumakiSlider.OnStartHeadMovingListener {

        public void onStartHeadMoving(UzumakiSlider slider, UzumakiHead head) {
            onStartSliding();
        }
    }

    private class OnStopHeadMovingListener implements UzumakiSlider.OnStopHeadMovingListener {

        public void onStopHeadMoving(UzumakiSlider slider, UzumakiHead head) {
            mProcAfterSeeking.run();
        }
    }

    private abstract class MenuDispatcher {

        public boolean dispatch() {
            callback();
            return true;
        }

        protected abstract void callback();
    }

    private class AboutDispatcher extends MenuDispatcher {

        protected void callback() {
            showAbout();
        }
    }

    private static class IncomingHandler extends Handler {

        private abstract class MessageHandler {

            public abstract void handle(Message msg);
        }

        private class PausedHandler extends MessageHandler {

            @Override
            public void handle(Message msg) {
                Object o = msg.obj;
                AudioService.PausedArgument a = (AudioService.PausedArgument)o;
                mActivity.onPaused(a);
            }
        }

        private class PlayingHandler extends MessageHandler {

            public void handle(Message msg) {
                mActivity.onPlaying((AudioService.PlayingArgument)msg.obj);
            }
        }

        private class ListHandler extends MessageHandler {

            public void handle(Message msg) {
                AudioService.ListArgument a;
                a = (AudioService.ListArgument)msg.obj;
                FileSystem fs = mActivity.mPlayingFiles;
                fs.directory = a.directory;
                String[] files = a.files;
                fs.files = files != null ? files : new String[0];
            }
        }

        private class AutoRepeatHandler extends MessageHandler {

            @Override
            public void handle(Message msg) {
                AudioService.AutoRepeatArgument a;
                a = (AudioService.AutoRepeatArgument)msg.obj;
                int resId = a.value
                        ? R.drawable.ic_auto_repeat_enabled
                        : R.drawable.ic_auto_repeat_disabled;
                mActivity.mAutoRepeatButton.setImageResource(resId);
            }
        }

        private MainActivity mActivity;
        private SparseArray<MessageHandler> mHandlers;

        public IncomingHandler(MainActivity activity) {
            mActivity = activity;
            mHandlers = new SparseArray<MessageHandler>();
            mHandlers.put(AudioService.MSG_PLAYING, new PlayingHandler());
            mHandlers.put(AudioService.MSG_PAUSED, new PausedHandler());
            mHandlers.put(AudioService.MSG_LIST, new ListHandler());
            mHandlers.put(AudioService.MSG_AUTO_REPEAT,
                          new AutoRepeatHandler());
        }

        @Override
        public void handleMessage(Message msg) {
            String s = AudioService.Utils.getMessageString(msg);
            Log.i(LOG_TAG, String.format("recv: %s", s));

            mHandlers.get(msg.what).handle(msg);
        }
    }

    private abstract class ContentTask extends AsyncTask<Void, Void, List<String>> {

        protected List<String> mEmptyList;

        public ContentTask() {
            mEmptyList = new ArrayList<String>();
        }

        protected List<String> queryExistingMp3() {
            return selectMp3(queryAudio());
        }

        protected List<String> makeList(String s) {
            List<String> l = new ArrayList<String>();
            l.add(s);
            return l;
        }

        private boolean isMp3(String path) {
            File file = new File(path);
            /*
             * The second expression is for rejecting non-mp3 files. This way is
             * not strict, because there might be a non-mp3 file with ".mp3"
             * extension.
             */
            return file.exists() && path.endsWith(".mp3");
        }

        private List<String> selectMp3(List<String> files) {
            List<String> l = new ArrayList<String>();

            for (String file: files) {
                l.addAll(isMp3(file) ? makeList(file) : mEmptyList);
            }

            return l;
        }

        private List<String> fetchRecord(Cursor c) {
            try {
                List<String> l = new ArrayList<String>();

                int index = c.getColumnIndex(MediaStore.MediaColumns.DATA);
                while (c.moveToNext()) {
                    l.add(c.getString(index));
                }

                return l;
            }
            finally {
                c.close();
            }
        }

        private List<String> queryAudio() {
            String trackColumn = MediaStore.Audio.AudioColumns.TRACK;
            String pathColumn = MediaStore.MediaColumns.DATA;
            String order = String.format("%s, %s", trackColumn, pathColumn);
            Cursor c = getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[] { pathColumn },
                    null,   // selection
                    null,   // selection arguments
                    order); // order
            return c != null ? fetchRecord(c) : new ArrayList<String>();
        }
    }

    private class FileListingTask extends ContentTask {

        private String mPath;

        public FileListingTask(String path) {
            mPath = path;
        }

        @Override
        protected void onPostExecute(List<String> files) {
            showFiles(files);
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            Log.i(LOG_TAG, "FileListingTask started.");

            List<String> files = selectFiles(queryExistingMp3());

            Log.i(LOG_TAG, "FileListingTask ended.");
            return files;
        }

        private void addFile(List<String> l, String path) {
            File file = new File(path);
            if (!file.getParent().equals(mPath)) {
                return;
            }
            l.add(file.getName());
        }

        private List<String> selectFiles(List<String> files) {
            List<String> l = new ArrayList<String>();

            for (String file: files) {
                addFile(l, file);
            }

            return l;
        }
    }

    private class DirectoryListingTask extends ContentTask {

        @Override
        protected void onPostExecute(List<String> directories) {
            showDirectories(directories.toArray(new String[0]));
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            Log.i(LOG_TAG, "DirectoryListingTask started.");

            List<String> audio = queryExistingMp3();
            List<String> directories = listDirectories(audio);
            Collections.sort(directories);

            Log.i(LOG_TAG, "DirectoryListingTask ended.");
            return directories;
        }

        private List<String> listDirectories(List<String> audio) {
            Set<String> set = new HashSet<String>();
            for (String path: audio) {
                File file = new File(path);
                set.add(file.getParent());
            }

            List<String> directories = new ArrayList<String>();
            for (String path: set) {
                directories.add(path);
            }

            return directories;
        }
    }

    private class TrueSliderListener implements UzumakiSlider.OnSliderChangeListener {

        public void onProgressChanged(UzumakiSlider _) {
            showCurrentTime();
        }
    }

    private static class FakeSliderListener implements UzumakiSlider.OnSliderChangeListener {

        public void onProgressChanged(UzumakiSlider _) {
        }
    }

    private class PauseButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            pause();
        }
    }

    private class PlayButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            play();
        }
    }

    private class AutoRepeatButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            sendMessage(AudioService.MSG_TOGGLE_AUTO_REPEAT);
        }
    }

    private abstract class ListListener implements AdapterView.OnItemClickListener {
    }

    private class DirectoryListListener extends ListListener {

        public void onItemClick(AdapterView<?> adapter, View view, int position,
                                long id) {
            selectDirectory(mDirectories[position]);
        }
    }

    private class FileListListener extends ListListener {

        public void onItemClick(AdapterView<?> adapter, View view, int position,
                                long id) {
            selectFile(position);
        }
    }

    private class PlayerTask extends TimerTask {

        private class Proc implements Runnable {

            private long mTimeAtStart;
            private int mOffsetAtStart;

            public Proc(long timeAtStart, int offsetAtStart) {
                mTimeAtStart = timeAtStart;
                mOffsetAtStart = offsetAtStart;
            }

            public void run() {
                long now = new Date().getTime();
                updateCurrentTime((int)(now - mTimeAtStart + mOffsetAtStart));
            }
        }

        public PlayerTask(long timeAtStart, int offsetAtStart) {
            mHandler = new Handler();
            mProc = new Proc(timeAtStart, offsetAtStart);
        }

        private Handler mHandler;
        private Runnable mProc;

        @Override
        public void run() {
            mHandler.post(mProc);
        }
    }

    private abstract class FlipButtonListener implements View.OnClickListener {
    }

    private class NextButtonListener extends FlipButtonListener {

        @Override
        public void onClick(View view) {
            showNext();
        }
    }

    private class PreviousButtonListener extends FlipButtonListener {

        @Override
        public void onClick(View view) {
            showPrevious();
        }
    }

    private interface TimerInterface {

        public void scheduleAtFixedRate(TimerTask task, long deley,
                                        long period);
        public void cancel();
    }

    private class TrueTimer implements TimerInterface {

        public TrueTimer() {
            mTimer = new Timer(true);
        }

        public void scheduleAtFixedRate(TimerTask task, long deley,
                                        long period) {
            mTimer.scheduleAtFixedRate(task, deley, period);
        }

        public void cancel() {
            mTimer.cancel();
        }

        private Timer mTimer;
    }

    private class FakeTimer implements TimerInterface {

        public void scheduleAtFixedRate(TimerTask task, long deley,
                                        long period) {
        }

        public void cancel() {
        }
    }

    private enum Key {
        PAGE_INDEX,
        NEXT_BUTTON0_ENABLED,
        NEXT_BUTTON1_ENABLED,
        DIRECTORY_LABEL,
        DURATION,
        PROGRESS,
        CURRENT_TIME,

        DIRECTORIES,
        SHOWN_DIRECTORY,
        SHOWN_FILES,
        PLAYING_DIRECTORY,
        PLAYING_FILES,
        PLAYING_FILE_POSITION,

        PLAYER_STATE;

        public String getKey() {
            return name();
        }
    }

    public static final String LOG_TAG = "activity";

    private static final long ANIMATION_DURATION = 250;
    private static final int INTERPOLATOR = android.R.anim.linear_interpolator;

    // Widgets
    private ViewFlipper mFlipper;
    private ListView mDirList;
    private ImageButton mNextButton0;

    private View mPrevButton1;
    private TextView mDirLabel;
    private ListView mFileList;
    private ImageView mAutoRepeatButton;
    private ImageButton mNextButton1;

    private View mPrevButton2;
    private ImageButton mPlayButton;
    private RotatingUzumakiSlider mSlider;
    private TextView mTitle;
    private TextView mCurrentTime;
    private TextView mTotalTime;

    // Objects supporting any widgets. They are stateless.
    private Animation mLeftInAnimation;
    private Animation mLeftOutAnimation;
    private Animation mRightInAnimation;
    private Animation mRightOutAnimation;
    private View.OnClickListener mPauseListener;
    private View.OnClickListener mPlayListener;
    private SparseArray<MenuDispatcher> mMenuDispatchers = new SparseArray<MenuDispatcher>();

    /**
     * Timer. This is stateful, but it is configured automatically.
     */
    private TimerInterface mTimer;

    // Stateful internal data
    private String[] mDirectories;
    private FileSystem mShownFiles = new FileSystem();
    private FileSystem mPlayingFiles = new FileSystem();
    private int mPlayingFilePosition;

    private boolean mInPlaying;
    private boolean mInSeeking = false;
    private ProcBeforeSeeking mProcBeforeSeeking;
    private ProcAfterSeeking mProcAfterSeeking;

    private ServiceConnection mConnection;
    private MessengerWrapper mOutgoingMessenger = new FakeMessenger();
    private Messenger mIncomingMessenger;
    private IncomingHandler mIncomingHandler;

    // Stateless internal data (reusable)
    private TimerInterface mFakeTimer;
    private UzumakiSlider.OnSliderChangeListener mTrueSliderListener;
    private UzumakiSlider.OnSliderChangeListener mFakeSliderListener;

    @Override
    public void onStart() {
        super.onStart();
        new DirectoryListingTask().execute();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mConnection;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViews();
        initializeFlipButtonListener();
        initializeDirList();
        initializeFileList();
        initializeAnimation();
        initializePlayButton();
        initializeTimer();
        initializeSlider();
        initializeMenu();

        mIncomingHandler = new IncomingHandler(this);
        mIncomingMessenger = new Messenger(mIncomingHandler);

        Log.i(LOG_TAG, "MainActivity was created.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MenuDispatcher dispatcher = mMenuDispatchers.get(item.getItemId());
        return dispatcher != null ? dispatcher.dispatch() : super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "MainActivity was destroyed.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeState();

        mConnection = new Connection();
        Intent intent = new Intent(this, AudioService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Log.i(LOG_TAG, "MainActivity was resumed.");
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(mConnection);
        stopTimer();
        saveState();

        Log.i(LOG_TAG, "MainActivity was paused.");
    }

    private void showAbout() {
        Intent i = new Intent(this, AboutActivity.class);
        startActivity(i);
    }

    private void initializeMenu() {
        mMenuDispatchers.put(R.id.about, new AboutDispatcher());
    }

    private void initializeSlider() {
        mSlider.addOnStartHeadMovingListener(new OnStartHeadMovingListener());
        mSlider.addOnStopHeadMovingListener(new OnStopHeadMovingListener());
        mSlider.addOnStartRotatingListener(new OnStartRotatingListener());
        mSlider.addOnStopRotatingListener(new OnStopRotatingListener());
        mSlider.setLogger(new SliderLogger());

        mTrueSliderListener = new TrueSliderListener();
        mFakeSliderListener = new FakeSliderListener();
    }

    private void initializeTimer() {
        mTimer = mFakeTimer = new FakeTimer();
    }

    private void findViews() {
        mFlipper = (ViewFlipper)findViewById(R.id.flipper);

        mDirList = (ListView)findViewById(R.id.dir_list);
        mNextButton0 = (ImageButton)findViewById(R.id.next0);

        mPrevButton1 = (View)findViewById(R.id.prev1);
        mDirLabel = (TextView)findViewById(R.id.dir_label);
        mFileList = (ListView)findViewById(R.id.file_list);
        mAutoRepeatButton = (ImageView)findViewById(R.id.auto_repeat_button);
        mNextButton1 = (ImageButton)findViewById(R.id.next1);

        mPrevButton2 = (View)findViewById(R.id.prev2);
        mPlayButton = (ImageButton)findViewById(R.id.play);
        mSlider = (RotatingUzumakiSlider)findViewById(R.id.slider);

        mTitle = (TextView)findViewById(R.id.title);
        mCurrentTime = (TextView)findViewById(R.id.current_time);
        mTotalTime = (TextView)findViewById(R.id.total_time);
    }

    private void initializePlayButton() {
        mPauseListener = new PauseButtonListener();
        mPlayButton.setOnClickListener(mPauseListener);
        mPlayListener = new PlayButtonListener();
    }

    private Animation loadAnimation(int id, Interpolator interp) {
        Animation anim = AnimationUtils.loadAnimation(this, id);
        anim.setDuration(ANIMATION_DURATION);
        anim.setInterpolator(interp);
        return anim;
    }

    private void initializeAnimation() {
        Interpolator interp = AnimationUtils.loadInterpolator(this, INTERPOLATOR);
        mLeftInAnimation = loadAnimation(R.anim.anim_left_in, interp);
        mLeftOutAnimation = loadAnimation(R.anim.anim_left_out, interp);
        mRightInAnimation = loadAnimation(R.anim.anim_right_in, interp);
        mRightOutAnimation = loadAnimation(R.anim.anim_right_out, interp);
    }

    private void initializeDirList() {
        mDirList.setOnItemClickListener(new DirectoryListListener());
    }

    private void showDirectories(String[] dirs) {
        mDirectories = dirs;
        mDirList.setAdapter(new DirectoryAdapter(dirs));
    }

    private void initializeFlipButtonListener() {
        ImageButton[] nextButtons = { mNextButton0, mNextButton1 };
        setClickListener(nextButtons, new NextButtonListener());

        View[] previousButtons = { mPrevButton1, mPrevButton2 };
        setClickListener(previousButtons, new PreviousButtonListener());
    }

    private String getPlayingDirectory() {
        return mPlayingFiles.directory;
    }

    private void selectDirectory(String directory) {
        mShownFiles.directory = directory;

        new FileListingTask(directory).execute();

        mDirLabel.setText(directory);
        enableButton(mNextButton0, true);
        showNext();
    }

    private void showFiles(List<String> files) {
        showFiles(files.toArray(new String[0]));
    }

    private void showFiles(String[] files) {
        mShownFiles.files = files;
        mFileList.setAdapter(new FileAdapter(files));
    }

    private void stopTimer() {
        mTimer.cancel();
        mTimer = mFakeTimer;
    }

    private void setSliderChangeListener(UzumakiSlider.OnSliderChangeListener l) {
        mSlider.clearOnSliderChangeListeners();
        mSlider.addOnSliderChangeListener(l);
    }

    private void enableSliderChangeListener() {
        setSliderChangeListener(mTrueSliderListener);
    }

    private void disableSliderChangeListener() {
        setSliderChangeListener(mFakeSliderListener);
    }

    private void showCurrentTime() {
        showTime(mCurrentTime, mSlider.getProgress());
    }

    private void updateCurrentTime(int position) {
        mSlider.setProgress(position);
        showCurrentTime();
    }

    private void startTimer(long timeAtStart, int offsetAtStart) {
        mTimer = new TrueTimer();
        int duration = getDuration(getPlayingPath());   // [msec]
        int angle = Math.abs(mSlider.getSweepAngle());
        int period = (int)((float)duration / (float)angle) / 2;
        /*
         * Each Timer requests new TimerTask object (Timers cannot share one
         * task).
         */
        TimerTask task = new PlayerTask(timeAtStart, offsetAtStart);
        mTimer.scheduleAtFixedRate(task, 0, Math.max(period, 10));
    }

    private void changePlayButtonToPauseButton() {
        mPlayButton.setOnClickListener(mPauseListener);
        mPlayButton.setImageResource(R.drawable.ic_pause);
    }

    private void changePauseButtonToPlayButton() {
        mPlayButton.setOnClickListener(mPlayListener);
        mPlayButton.setImageResource(R.drawable.ic_play);
    }

    private void onPaused(AudioService.PausedArgument a) {
        mPlayingFilePosition = a.filePosition;
        mProcBeforeSeeking = new NopBeforeSeeking();
        mProcAfterSeeking = mInPlaying && mInSeeking
                ? new PlayAfterSeeking()
                : new StayAfterSeeking();
        mInPlaying = false;

        stopTimer();

        showPlayingFile();
        mSlider.setProgress(a.currentOffset);
        changePauseButtonToPlayButton();
        enableSliderChangeListener();
    }

    private void onPlaying(AudioService.PlayingArgument a) {
        mPlayingFilePosition = a.filePosition;
        mProcBeforeSeeking = new PauseBeforeSeeking();
        mProcAfterSeeking = new PlayAfterSeeking();
        mInPlaying = true;

        stopTimer();
        startTimer(a.timeAtStart, a.offsetAtStart);

        showPlayingFile();
        changePlayButtonToPauseButton();
        disableSliderChangeListener();
    }

    private void play() {
        sendPlay();
    }

    private void sendInit() {
        AudioService.InitArgument a = new AudioService.InitArgument();
        a.directory = mPlayingFiles.directory;
        a.files = mPlayingFiles.files;
        sendMessage(AudioService.MSG_INIT, a);
    }

    private void initializeList() {
        sendInit();
    }

    private void sendPlay() {
        AudioService.PlayArgument a = new AudioService.PlayArgument();
        a.filePosition = mPlayingFilePosition;
        a.offset = mSlider.getProgress();
        sendMessage(AudioService.MSG_PLAY, a);
    }

    private String getPlayingFile() {
        String[] files = mPlayingFiles.files;
        int pos = mPlayingFilePosition;
        // Returning "" must be harmless.
        return pos < files.length ? files[pos] : "";
    }

    private String getPlayingPath() {
        String dir = getPlayingDirectory();
        return dir + File.separator + getPlayingFile();
    }

    private int getDuration(String path) {
        String col = MediaStore.Audio.AudioColumns.DURATION;
        Cursor c = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { col },
                String.format("%s=?", MediaStore.Audio.Media.DATA),
                new String[] { path },
                null);  // order
        try {
            c.moveToNext();
            int index = c.getColumnIndex(col);
            try {
                return c.getInt(index);
            }
            catch (CursorIndexOutOfBoundsException e) {
                return 1;       // one is harmless, zero causes zero division.
            }
        }
        finally {
            c.close();
        }
    }

    private void showTime(TextView view, int time_msec) {
        int time_sec = time_msec / 1000;
        int min = (time_sec / 60) % 100;
        int sec = time_sec % 60;
        view.setText(String.format("%02d:%02d", min, sec));
    }

    private void enableButton(ImageButton button, boolean enabled) {
        /*
         * Why did not I use style?
         * ========================
         *
         * I tried to give the following settings only in *.xml, but it did not
         * work. I do not know why still. So I give the settings at here.
         */
        button.setClickable(enabled);
        int resourceId = enabled ? R.drawable.nav_right : R.drawable.ic_blank;
        button.setImageResource(resourceId);
    }

    private void pause() {
        sendMessage(AudioService.MSG_PAUSE);
    }

    private void selectFile(int position) {
        pause();

        mPlayingFiles.copyFrom(mShownFiles);
        mPlayingFilePosition = position;
        initializeList();

        enableButton(mNextButton1, true);
        showNext();
        mSlider.setProgress(0);
        mDirList.invalidateViews();

        play();
    }

    /**
     * Updates views which relate only with a playing file. Current time does
     * not relate with a playing file only, but also time, so it is out of
     * targets of this method.
     */
    private void showPlayingFile() {
        mFileList.invalidateViews();

        String path = getPlayingPath();
        int duration = getDuration(path);
        mSlider.setMax(duration);
        mTitle.setText(getPlayingFile());
        showTime(mTotalTime, duration);
    }

    private void showPrevious() {
        mFlipper.setInAnimation(mLeftInAnimation);
        mFlipper.setOutAnimation(mRightOutAnimation);
        mFlipper.showPrevious();
    }

    private void showNext() {
        mFlipper.setInAnimation(mRightInAnimation);
        mFlipper.setOutAnimation(mLeftOutAnimation);
        mFlipper.showNext();
    }

    private void setClickListener(View[] buttons,
                                  View.OnClickListener listener) {
        for (View button: buttons) {
            button.setOnClickListener(listener);
        }
    }
    private SharedPreferences getPrivatePreferences() {
        return getPreferences(Context.MODE_PRIVATE);
    }

    private void saveState() {
        Editor editor = getPrivatePreferences().edit();

        // widgets' states
        saveInt(editor, Key.PAGE_INDEX, mFlipper.getDisplayedChild());
        saveButton(editor, Key.NEXT_BUTTON0_ENABLED, mNextButton0);
        saveButton(editor, Key.NEXT_BUTTON1_ENABLED, mNextButton1);
        saveTextView(editor, Key.DIRECTORY_LABEL, mDirLabel);
        saveInt(editor, Key.DURATION, mSlider.getMax());
        saveInt(editor, Key.PROGRESS, mSlider.getProgress());
        saveTextView(editor, Key.CURRENT_TIME, mCurrentTime);

        // internal data
        saveStringArray(editor, Key.DIRECTORIES, mDirectories);
        saveFileSystem(editor, mShownFiles, Key.SHOWN_DIRECTORY,
                       Key.SHOWN_FILES);
        saveFileSystem(editor, mPlayingFiles, Key.PLAYING_DIRECTORY,
                       Key.PLAYING_FILES);
        saveInt(editor, Key.PLAYING_FILE_POSITION, mPlayingFilePosition);

        editor.commit();
    }

    private void resumeState() {
        SharedPreferences prefs = getPrivatePreferences();

        // Widgets
        int childIndex = prefs.getInt(Key.PAGE_INDEX.getKey(), 0);
        mFlipper.setDisplayedChild(childIndex);
        restoreButton(prefs, Key.NEXT_BUTTON0_ENABLED, mNextButton0);
        restoreButton(prefs, Key.NEXT_BUTTON1_ENABLED, mNextButton1);
        restoreTextView(prefs, Key.DIRECTORY_LABEL, mDirLabel);
        restoreSlider(prefs);
        restoreTextView(prefs, Key.CURRENT_TIME, mCurrentTime);

        // Internal data
        mDirectories = PreferencesUtil.getStringArray(prefs,
                                                      Key.DIRECTORIES.name());
        restoreFileSystem(prefs, mShownFiles, Key.SHOWN_DIRECTORY,
                          Key.SHOWN_FILES);
        restoreFileSystem(prefs, mPlayingFiles, Key.PLAYING_DIRECTORY,
                          Key.PLAYING_FILES);
        mPlayingFilePosition = prefs.getInt(Key.PLAYING_FILE_POSITION.name(),
                                            0);

        // Restores UI.
        showFiles(mShownFiles.files);
    }

    private void saveInt(Editor editor, Key key, int n) {
        editor.putInt(key.name(), n);
    }

    private void restoreSlider(SharedPreferences prefs) {
        /*
         * The default value is dummy. Its role is only avoiding the exception
         * of divide by zero.
         */
        mSlider.setMax(prefs.getInt(Key.DURATION.name(), 1));

        mSlider.setProgress(prefs.getInt(Key.PROGRESS.name(), 0));
    }

    private void saveTextView(Editor editor, Key key, TextView view) {
        editor.putString(key.name(), view.getText().toString());
    }

    private void restoreTextView(SharedPreferences prefs, Key key,
                                 TextView view) {
        view.setText(prefs.getString(key.name(), null));
    }

    private void saveString(Editor editor, Key key, String value) {
        editor.putString(key.name(), value);
    }

    private void saveStringArray(Editor editor, Key key, String[] values) {
        PreferencesUtil.putStringArray(editor, key.name(), values);
    }

    private void saveFileSystem(Editor editor, FileSystem fs, Key directoryKey,
                                Key filesKey) {
        saveString(editor, directoryKey, fs.directory);
        saveStringArray(editor, filesKey, fs.files);
    }

    private void restoreFileSystem(SharedPreferences prefs, FileSystem fs,
                                   Key directoryKey, Key filesKey) {
        fs.directory = prefs.getString(directoryKey.name(), null);
        fs.files = PreferencesUtil.getStringArray(prefs, filesKey.name());
    }

    private void restoreButton(SharedPreferences prefs, Key key,
                               ImageButton button) {
        enableButton(button, prefs.getBoolean(key.getKey(), false));
    }

    private void saveButton(Editor editor, Key key, ImageButton button) {
        editor.putBoolean(key.getKey(), button.isClickable());
    }

    private void sendMessage(int what, AudioService.Argument a) {
        Message msg = Message.obtain(null, what, a);
        String s = AudioService.Utils.getMessageString(msg);
        Log.i(LOG_TAG, String.format("send: %s", s));

        msg.replyTo = mIncomingMessenger;
        try {
            mOutgoingMessenger.send(msg);
        }
        catch (RemoteException e) {
            ActivityUtil.showException(this, "Cannot send a message", e);
        }
    }

    private void sendMessage(int what) {
        sendMessage(what, null);
    }

    private void onStartSliding() {
        mProcBeforeSeeking.run();
        mInSeeking = true;
    }

    private void initializeFileList() {
        mFileList.setOnItemClickListener(new FileListListener());
        mAutoRepeatButton.setOnClickListener(new AutoRepeatButtonListener());
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4

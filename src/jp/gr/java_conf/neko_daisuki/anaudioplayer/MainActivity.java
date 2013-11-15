package jp.gr.java_conf.neko_daisuki.anaudioplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

import jp.gr.java_conf.neko_daisuki.android.widget.RotatingUzumakiSlider;
import jp.gr.java_conf.neko_daisuki.android.widget.UzumakiHead;
import jp.gr.java_conf.neko_daisuki.android.widget.UzumakiSlider;

public class MainActivity extends Activity {

    private enum PlayerState {
        PLAYING,
        PAUSED
    };

    private static class PreferencesUtil {

        private static final String KEY_PLAYER_STATE = "PlayerState";

        public static PlayerState getPlayerState(SharedPreferences prefs) {
            String value = prefs.getString(KEY_PLAYER_STATE, "");
            return value.equals(PlayerState.PLAYING.name())
                ? PlayerState.PLAYING
                : PlayerState.PAUSED;
        }

        public static void putPlayerState(Editor editor, PlayerState state) {
            editor.putString(KEY_PLAYER_STATE, state.name());
        }

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
    }

    private static class ActivityHolder {

        protected MainActivity mActivity;

        public ActivityHolder(MainActivity activity) {
            mActivity = activity;
        }
    }

    private abstract static class Adapter extends ArrayAdapter<String> {

        protected LayoutInflater mInflater;
        protected MainActivity mActivity;

        public Adapter(MainActivity activity, String[] objects) {
            super(activity, 0, objects);
            initialize(activity);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return convertView == null
                ? getView(position, makeConvertView(parent), parent)
                : makeView(position, convertView);
        }

        protected abstract View makeConvertView(ViewGroup parent);
        protected abstract View makeView(int position, View convertView);

        private void initialize(MainActivity activity) {
            mActivity = activity;
            String service = Context.LAYOUT_INFLATER_SERVICE;
            mInflater = (LayoutInflater)activity.getSystemService(service);
        }
    }

    private static class FileAdapter extends Adapter {

        private static class Row {

            public ImageView playingIcon;
            public TextView name;
        }

        public FileAdapter(MainActivity activity, String[] objects) {
            super(activity, objects);
        }

        @Override
        protected View makeView(int position, View convertView) {
            String file = mActivity.getPlayingFile();
            boolean isPlaying = isPlayingDirectoryShown() && mActivity.mShownFiles.files[position].equals(file);
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
            MainActivity activity = mActivity;
            String shown = activity.mShownFiles.directory;
            String playing = activity.mPlayingFiles.directory;
            return (shown != null) && shown.equals(playing);
        }
    }

    private static class DirectoryAdapter extends Adapter {

        private static class Row {

            public ImageView playingIcon;
            public TextView path;
        }

        public DirectoryAdapter(MainActivity activity, String[] objects) {
            super(activity, objects);
        }

        @Override
        protected View makeView(int position, View convertView) {
            String path = mActivity.mDirectories[position];
            setPlayingIcon(path, convertView);

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
            String directory = mActivity.getPlayingDirectory();
            boolean isPlaying = path.equals(directory);
            int src = isPlaying ? R.drawable.ic_playing : R.drawable.ic_blank;
            Row row = (Row)view.getTag();
            row.playingIcon.setImageResource(src);
            row.path.setText(path);
        }
    }

    private abstract static class ProcedureOnConnected extends ActivityHolder implements Runnable {

        public ProcedureOnConnected(MainActivity activity) {
            super(activity);
        }

        public abstract void run();
    }

    private static class PlayProcedureOnConnected extends ProcedureOnConnected {

        public PlayProcedureOnConnected(MainActivity activity) {
            super(activity);
        }

        public void run() {
            mActivity.sendInit();
            mActivity.sendPlay();
            mActivity.onConnectedWithService();
        }
    }

    private static class ResumeProcedureOnConnected extends ProcedureOnConnected {

        public ResumeProcedureOnConnected(MainActivity activity) {
            super(activity);
        }

        public void run() {
            mActivity.sendWhatFile();
            mActivity.onConnectedWithService();
        }
    }

    private interface ServiceUnbinder {

        public void unbind();
    }

    private class TrueServiceUnbinder extends ActivityHolder implements ServiceUnbinder {

        public TrueServiceUnbinder(MainActivity activity) {
            super(activity);
        }

        public void unbind() {
            mActivity.unbindService(mActivity.mConnection);
        }
    }

    private class FakeServiceUnbinder implements ServiceUnbinder {

        public void unbind() {
        }
    }

    private interface ServiceStarter {

        public void start();
    }

    private class TrueServiceStarter extends ActivityHolder implements ServiceStarter {

        public TrueServiceStarter(MainActivity activity) {
            super(activity);
        }

        public void start() {
            Intent intent = new Intent(mActivity, AudioService.class);
            mActivity.startService(intent);
        }
    }

    private class FakeServiceStarter implements ServiceStarter {

        public void start() {
        }
    }

    private interface ServiceStopper {

        public void stop();
    }

    private class TrueServiceStopper extends ActivityHolder implements ServiceStopper {

        public TrueServiceStopper(MainActivity activity) {
            super(activity);
        }

        public void stop() {
            mActivity.unbindAudioService();
            Intent intent = new Intent(mActivity, AudioService.class);
            mActivity.stopService(intent);
        }
    }

    private class FakeServiceStopper implements ServiceStopper {

        public void stop() {
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

    private class Connection extends ActivityHolder implements ServiceConnection {

        private Runnable mProcedureOnConnected;

        public Connection(MainActivity activity,
                          Runnable procedureOnConnected) {
            super(activity);
            mProcedureOnConnected = procedureOnConnected;
        }

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Messenger messenger = new Messenger(service);
            mActivity.mOutgoingMessenger = new TrueMessenger(messenger);

            mProcedureOnConnected.run();

            Log.i(LOG_TAG, "MainActivity connected to AudioService.");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(LOG_TAG, "MainActivity disconnected from AudioService.");
        }
    }

    private class OnStartRotatingListener extends ActivityHolder implements RotatingUzumakiSlider.OnStartRotatingListener {

        public OnStartRotatingListener(MainActivity activity) {
            super(activity);
        }

        public void onStartRotating(RotatingUzumakiSlider slider) {
            mActivity.onStartSliding();
        }
    }

    private class OnStopRotatingListener extends ActivityHolder implements RotatingUzumakiSlider.OnStopRotatingListener {

        public OnStopRotatingListener(MainActivity activity) {
            super(activity);
        }

        public void onStopRotating(RotatingUzumakiSlider slider) {
            mActivity.mProcAfterSeeking.run();
        }
    }

    private class PlayAfterSeeking extends ActivityHolder implements Runnable {

        public PlayAfterSeeking(MainActivity activity) {
            super(activity);
        }

        public void run() {
            mActivity.startTimer();
            mActivity.sendPlay();
        }
    }

    private class StayAfterSeeking implements Runnable {

        public void run() {
        }
    }

    private class SliderLogger implements UzumakiSlider.Logger {

        private MainActivity mActivity;

        public SliderLogger(MainActivity activity) {
            mActivity = activity;
        }

        public void log(String msg) {
            mActivity.log(msg);
        }
    }

    private class OnStartHeadMovingListener implements UzumakiSlider.OnStartHeadMovingListener {

        private MainActivity mActivity;

        public OnStartHeadMovingListener(MainActivity activity) {
            mActivity = activity;
        }

        public void onStartHeadMoving(UzumakiSlider slider, UzumakiHead head) {
            mActivity.onStartSliding();
        }
    }

    private class OnStopHeadMovingListener extends ActivityHolder implements UzumakiSlider.OnStopHeadMovingListener {

        public OnStopHeadMovingListener(MainActivity activity) {
            super(activity);
        }

        public void onStopHeadMoving(UzumakiSlider slider, UzumakiHead head) {
            mActivity.mProcAfterSeeking.run();
        }
    }

    private abstract class MenuDispatcher {

        protected MainActivity mActivity;

        public MenuDispatcher(MainActivity activity) {
            mActivity = activity;
        }

        public boolean dispatch() {
            callback();
            return true;
        }

        protected abstract void callback();
    }

    private class AboutDispatcher extends MenuDispatcher {

        public AboutDispatcher(MainActivity activity) {
            super(activity);
        }

        protected void callback() {
            mActivity.showAbout();
        }
    }

    private static class IncomingHandler extends Handler {

        private abstract class MessageHandler extends ActivityHolder {

            public MessageHandler(MainActivity activity) {
                super(activity);
            }

            public abstract void handle(Message msg);
        }

        private class CompletionHandler extends MessageHandler {

            public CompletionHandler(MainActivity activity) {
                super(activity);
            }

            public void handle(Message msg) {
                completeSlider();
                mActivity.pause();
            }

            /**
             * Moves the slider head to the last.
             *
             * During AAP is on background, UI is not updated. UI is updated
             * when AAP comes back to foreground. If music is on air,
             * MSG_WHAT_TIME message updates the slider. Similaly,
             * MSG_COMPLETION must update UI.
             */
            private void completeSlider() {
                RotatingUzumakiSlider slider = mActivity.mSlider;
                slider.setProgress(slider.getMax());
            }
        }

        private class WhatTimeHandler extends MessageHandler {

            public WhatTimeHandler(MainActivity activity) {
                super(activity);
            }

            public void handle(Message msg) {
                mActivity.updateCurrentTime(msg.arg1);
            }
        }

        private class NotPlayingHandler extends MessageHandler {

            public NotPlayingHandler(MainActivity activity) {
                super(activity);
            }

            public void handle(Message msg) {
                mActivity.pause();
            }
        }

        private class NopHandler extends MessageHandler {

            public NopHandler() {
                super(null);
            }

            public void handle(Message _) {
            }
        }

        private class PlayingHandler extends MessageHandler {

            public PlayingHandler(MainActivity activity) {
                super(activity);
            }

            public void handle(Message msg) {
                AudioService.PlayingArgument a = (AudioService.PlayingArgument)msg.obj;
                mActivity.mPlayingFilePosition = a.position;
                mActivity.showPlayingFile();

                mActivity.mIncomingHandler.enableResponse();
            }
        }

        private SparseArray<MessageHandler> mHandlers;
        private MessageHandler mWhatTimeHandler;
        private MessageHandler mCompletionHandler;
        private MessageHandler mNopHandler;

        public IncomingHandler(MainActivity activity) {
            mHandlers = new SparseArray<MessageHandler>();
            mHandlers.put(AudioService.MSG_PLAYING,
                          new PlayingHandler(activity));
            mHandlers.put(AudioService.MSG_NOT_PLAYING,
                          new NotPlayingHandler(activity));

            mWhatTimeHandler = new WhatTimeHandler(activity);
            mCompletionHandler = new CompletionHandler(activity);
            mNopHandler = new NopHandler();

            ignoreResponseUntilPlaying();
        }

        @Override
        public void handleMessage(Message msg) {
            mHandlers.get(msg.what).handle(msg);
        }

        /**
         * Orders to ignore MSG_WHAT_TIME/MSG_COMPLETION messages until a next
         * MSG_PLAYING.
         *
         * When a user selects a new music in playing another one, some
         * MSG_WHAT_TIME messages may be included in the message queue. These
         * messages will change time before starting to play the new music. So,
         * MSG_WHAT_TIME must be ignored until a next MSG_PLAY response.
         *
         * Same case is on MSG_COMPLETION. If a music finished, AudioService
         * send back MSG_COMPLETION for each MSG_WHAT_TIME message in the queue.
         * These responses start playing a next music (finally, no music but
         * last one will be skipped). This is a reason why I must drop
         * MSG_COMPLETION responses once I accepted first one.
         *
         * NOTE: At first, I tried to use a new messenger and a handler, but
         * they did not work expectedly. I guess that all messengers must share
         * one singleton message queue.
         */
        public void ignoreResponseUntilPlaying() {
            setWhatTimeHandler(mNopHandler);
            setCompletionHandler(mNopHandler);
        }

        private void enableResponse() {
            setWhatTimeHandler(mWhatTimeHandler);
            setCompletionHandler(mCompletionHandler);
        }

        private void setWhatTimeHandler(MessageHandler handler) {
            mHandlers.put(AudioService.MSG_WHAT_TIME, handler);
        }

        private void setCompletionHandler(MessageHandler handler) {
            mHandlers.put(AudioService.MSG_COMPLETION, handler);
        }
    }

    private static abstract class ContentTask extends AsyncTask<Void, Void, List<String>> {

        protected MainActivity mActivity;
        protected List<String> mEmptyList;

        public ContentTask(MainActivity activity) {
            mActivity = activity;
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
            Cursor c = mActivity.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[] { pathColumn },
                    null,   // selection
                    null,   // selection arguments
                    order); // order
            return c != null ? fetchRecord(c) : new ArrayList<String>();
        }
    }

    private static class FileListingTask extends ContentTask {

        private String mPath;

        public FileListingTask(MainActivity activity, String path) {
            super(activity);
            mPath = path;
        }

        @Override
        protected void onPostExecute(List<String> files) {
            mActivity.showFiles(files);
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

    private static class DirectoryListingTask extends ContentTask {

        public DirectoryListingTask(MainActivity activity) {
            super(activity);
        }

        @Override
        protected void onPostExecute(List<String> directories) {
            mActivity.showDirectories(directories.toArray(new String[0]));
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

    private static class TrueSliderListener extends ActivityHolder implements UzumakiSlider.OnSliderChangeListener {

        public TrueSliderListener(MainActivity activity) {
            super(activity);
        }

        public void onProgressChanged(UzumakiSlider _) {
            mActivity.showCurrentTime();
        }
    }

    private static class FakeSliderListener implements UzumakiSlider.OnSliderChangeListener {

        public void onProgressChanged(UzumakiSlider _) {
        }
    }

    public static final String LOG_TAG = "anaudioplayer";

    // Widgets
    private ViewFlipper mFlipper;
    private ListView mDirList;
    private ImageButton mNextButton0;

    private View mPrevButton1;
    private TextView mDirLabel;
    private ListView mFileList;
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
    private PlayerState mPlayerState;
    private String[] mDirectories;
    private FileSystem mShownFiles = new FileSystem();
    private FileSystem mPlayingFiles = new FileSystem();
    private int mPlayingFilePosition;

    private Runnable mProcAfterSeeking;

    private ServiceStarter mServiceStarter;
    private ServiceStopper mServiceStopper;
    private ServiceUnbinder mServiceUnbinder;
    private ServiceConnection mConnection;
    private MessengerWrapper mOutgoingMessenger;
    private Messenger mIncomingMessenger;
    private IncomingHandler mIncomingHandler;

    // Stateless internal data (reusable)
    private TimerInterface mFakeTimer;
    private UzumakiSlider.OnSliderChangeListener mTrueSliderListener;
    private UzumakiSlider.OnSliderChangeListener mFakeSliderListener;
    private MessengerWrapper mFakeOutgoingMessenger;

    @Override
    public void onStart() {
        super.onStart();

        new DirectoryListingTask(this).execute();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mConnection;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPlayerState = PlayerState.PAUSED;
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
        mFakeOutgoingMessenger = new FakeMessenger();

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

    private void showAbout() {
        Intent i = new Intent(this, AboutActivity.class);
        startActivity(i);
    }

    private void initializeMenu() {
        mMenuDispatchers.put(R.id.about, new AboutDispatcher(this));
    }

    private void initializeSlider() {
        mSlider.addOnStartHeadMovingListener(new OnStartHeadMovingListener(this));
        mSlider.addOnStopHeadMovingListener(new OnStopHeadMovingListener(this));
        mSlider.addOnStartRotatingListener(new OnStartRotatingListener(this));
        mSlider.addOnStopRotatingListener(new OnStopRotatingListener(this));
        mSlider.setLogger(new SliderLogger(this));

        mTrueSliderListener = new TrueSliderListener(this);
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
        mNextButton1 = (ImageButton)findViewById(R.id.next1);

        mPrevButton2 = (View)findViewById(R.id.prev2);
        mPlayButton = (ImageButton)findViewById(R.id.play);
        mSlider = (RotatingUzumakiSlider)findViewById(R.id.slider);

        mTitle = (TextView)findViewById(R.id.title);
        mCurrentTime = (TextView)findViewById(R.id.current_time);
        mTotalTime = (TextView)findViewById(R.id.total_time);
    }

    private void initializePlayButton() {
        mPauseListener = new PauseButtonListener(this);
        mPlayButton.setOnClickListener(mPauseListener);
        mPlayListener = new PlayButtonListener(this);
    }

    private class PauseButtonListener extends ActivityHolder implements View.OnClickListener {

        public PauseButtonListener(MainActivity activity) {
            super(activity);
        }

        @Override
        public void onClick(View view) {
            mActivity.pause();
        }
    }

    private class PlayButtonListener extends ActivityHolder implements View.OnClickListener {

        public PlayButtonListener(MainActivity activity) {
            super(activity);
        }

        @Override
        public void onClick(View view) {
            mActivity.play();
        }
    }

    private static final long ANIMATION_DURATION = 250;
    private static final int INTERPOLATOR = android.R.anim.linear_interpolator;

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
        mDirList.setOnItemClickListener(new DirectoryListListener(this));
    }

    private void showDirectories(String[] dirs) {
        mDirectories = dirs;
        mDirList.setAdapter(new DirectoryAdapter(this, dirs));
    }

    private void initializeFlipButtonListener() {
        ImageButton[] nextButtons = { mNextButton0, mNextButton1 };
        setClickListener(nextButtons, new NextButtonListener(this));

        View[] previousButtons = { mPrevButton1, mPrevButton2 };
        setClickListener(previousButtons, new PreviousButtonListener(this));
    }

    private String getPlayingDirectory() {
        return mPlayingFiles.directory;
    }

    private void selectDirectory(String directory) {
        mShownFiles.directory = directory;

        new FileListingTask(this, directory).execute();

        mDirLabel.setText(directory);
        enableButton(mNextButton0, true);
        showNext();
    }

    private void showFiles(List<String> files) {
        showFiles(files.toArray(new String[0]));
    }

    private void showFiles(String[] files) {
        mShownFiles.files = files;
        mFileList.setAdapter(new FileAdapter(this, files));
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

    private void pause() {
        mProcAfterSeeking = new StayAfterSeeking();
        stopTimer();
        stopAudioService();
        changePauseButtonToPlayButton();
        enableSliderChangeListener();
        mOutgoingMessenger = mFakeOutgoingMessenger;
        mPlayerState = PlayerState.PAUSED;
    }

    private class PlayerTask extends TimerTask {

        private class Proc extends ActivityHolder implements Runnable {

            public Proc(MainActivity activity) {
                super(activity);
            }

            public void run() {
                mActivity.sendMessage(AudioService.MSG_WHAT_TIME);
            }
        }

        public PlayerTask(MainActivity activity) {
            mHandler = new Handler();
            mProc = new Proc(activity);
        }

        private Handler mHandler;
        private Runnable mProc;

        @Override
        public void run() {
            mHandler.post(mProc);
        }
    }

    private void showCurrentTime() {
        showTime(mCurrentTime, mSlider.getProgress());
    }

    private void updateCurrentTime(int position) {
        mSlider.setProgress(position);
        showCurrentTime();
    }

    private void startTimer() {
        mTimer = new TrueTimer();
        /*
         * Each Timer requests new TimerTask object (Timers cannot share one
         * task).
         */
        mTimer.scheduleAtFixedRate(new PlayerTask(this), 0, 10);
    }

    private void changePlayButtonToPauseButton() {
        mPlayButton.setOnClickListener(mPauseListener);
        mPlayButton.setImageResource(R.drawable.ic_pause);
    }

    private void changePauseButtonToPlayButton() {
        mPlayButton.setOnClickListener(mPlayListener);
        mPlayButton.setImageResource(R.drawable.ic_play);
    }

    private void onConnectedWithService() {
        startTimer();
        mProcAfterSeeking = new PlayAfterSeeking(this);
        changePlayButtonToPauseButton();
        disableSliderChangeListener();
    }

    private void play() {
        /*
         * Stops the current timer. New timer will start by
         * PlayProcedureOnConnected later.
         */
        stopTimer();

        startAudioService();
        bindAudioService(new PlayProcedureOnConnected(this));

        mPlayerState = PlayerState.PLAYING;
    }

    private void sendWhatFile() {
        sendMessage(AudioService.MSG_WHAT_FILE);
    }

    private void sendInit() {
        AudioService.InitArgument a = new AudioService.InitArgument();
        a.directory = mPlayingFiles.directory;
        a.files = mPlayingFiles.files;
        a.position = mPlayingFilePosition;
        sendMessage(AudioService.MSG_INIT, a);
    }

    private void sendPlay() {
        AudioService.PlayArgument a = new AudioService.PlayArgument();
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
            // FIXME: This code crashes when no record is found.
            c.moveToNext();
            return c.getInt(c.getColumnIndex(col));
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

    private void selectFile(int position) {
        pause();
        mIncomingHandler.ignoreResponseUntilPlaying();

        mPlayingFiles.copyFrom(mShownFiles);
        mPlayingFilePosition = position;

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

    private abstract class ListListener implements AdapterView.OnItemClickListener {

        public ListListener(MainActivity activity) {
            mActivity = activity;
        }

        protected MainActivity mActivity;
    }

    private class DirectoryListListener extends ListListener {

        public DirectoryListListener(MainActivity activity) {
            super(activity);
        }

        public void onItemClick(AdapterView<?> adapter, View view, int position,
                                long id) {
            mActivity.selectDirectory(mActivity.mDirectories[position]);
        }
    }

    private class FileListListener extends ListListener {

        public FileListListener(MainActivity activity) {
            super(activity);
        }

        public void onItemClick(AdapterView<?> adapter, View view, int position,
                                long id) {
            mActivity.selectFile(position);
        }
    }

    private void setClickListener(View[] buttons,
                                  View.OnClickListener listener) {
        for (View button: buttons) {
            button.setOnClickListener(listener);
        }
    }

    private abstract class FlipButtonListener implements View.OnClickListener {

        public FlipButtonListener(MainActivity activity) {
            mActivity = activity;
        }

        protected MainActivity mActivity;
    }

    private class NextButtonListener extends FlipButtonListener {

        public NextButtonListener(MainActivity activity) {
            super(activity);
        }

        @Override
        public void onClick(View view) {
            mActivity.showNext();
        }
    }

    private class PreviousButtonListener extends FlipButtonListener {

        public PreviousButtonListener(MainActivity activity) {
            super(activity);
        }

        @Override
        public void onClick(View view) {
            mActivity.showPrevious();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "MainActivity was destroyed.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        resumeState();

        if (mPlayerState == PlayerState.PLAYING) {
            bindAudioService(new ResumeProcedureOnConnected(this));
            mServiceStarter = new FakeServiceStarter();
            mServiceStopper = new TrueServiceStopper(this);
        }
        else {
            mServiceStarter = new TrueServiceStarter(this);
            mServiceStopper = new FakeServiceStopper();
            mServiceUnbinder = new FakeServiceUnbinder();
            mConnection = null;
            /*
             * To initialize other members (timer, the play button, outgoing
             * messenger, etc), pause() is called.
             */
            pause();
        }

        Log.i(LOG_TAG, "MainActivity was resumed.");
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopTimer();
        unbindAudioService();
        saveState();

        Log.i(LOG_TAG, "MainActivity was paused.");
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
        saveTextView(editor, Key.TITLE, mTitle);
        saveTextView(editor, Key.CURRENT_TIME, mCurrentTime);
        saveTextView(editor, Key.TOTAL_TIME, mTotalTime);

        // internal data
        PreferencesUtil.putPlayerState(editor, mPlayerState);
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
        restoreTextView(prefs, Key.TITLE, mTitle);
        restoreTextView(prefs, Key.CURRENT_TIME, mCurrentTime);
        restoreTextView(prefs, Key.TOTAL_TIME, mTotalTime);

        // Internal data
        mPlayerState = PreferencesUtil.getPlayerState(prefs);
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

    private enum Key {
        PAGE_INDEX,
        NEXT_BUTTON0_ENABLED,
        NEXT_BUTTON1_ENABLED,
        DIRECTORY_LABEL,
        DURATION,
        PROGRESS,
        TITLE,
        CURRENT_TIME,
        TOTAL_TIME,

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

    private void sendMessage(int what, Object o) {
        Message msg = Message.obtain(null, what, o);
        msg.replyTo = mIncomingMessenger;
        try {
            mOutgoingMessenger.send(msg);
        }
        catch (RemoteException e) {
            // TODO: MainActivity must show error to users.
            e.printStackTrace();
        }
    }

    private void sendMessage(int what) {
        sendMessage(what, null);
    }

    private void log(String msg) {
        mTitle.setText(msg);
    }

    private void startAudioService() {
        mServiceStarter.start();
        mServiceStarter = new FakeServiceStarter();
        mServiceStopper = new TrueServiceStopper(this);
    }

    private void stopAudioService() {
        mServiceStopper.stop();
        mServiceStarter = new TrueServiceStarter(this);
        mServiceStopper = new FakeServiceStopper();
    }

    private void unbindAudioService() {
        mServiceUnbinder.unbind();
        mServiceUnbinder = new FakeServiceUnbinder();
    }

    private Intent makeAudioServiceIntent() {
        return new Intent(this, AudioService.class);
    }

    private void bindAudioService(Runnable procedureOnConnected) {
        Intent intent = makeAudioServiceIntent();
        mConnection = new Connection(this, procedureOnConnected);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mServiceUnbinder = new TrueServiceUnbinder(this);
    }

    private void onStartSliding() {
        stopTimer();
        enableSliderChangeListener();
        sendMessage(AudioService.MSG_PAUSE);
    }

    private void initializeFileList() {
        mFileList.setOnItemClickListener(new FileListListener(this));
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4

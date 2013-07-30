package jp.gr.java_conf.neko_daisuki.anaudioplayer;

import java.io.File;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

public class AudioService extends Service {

    public static class PlayArgument {

        public int offset;
    }

    public static class InitArgument {

        public String directory;
        public String[] files;
        public int position;
    }

    public static class PlayingArgument {

        public int position;
    }

    private interface Player {

        public void play(String path, int offset) throws IOException;
        public void pause();
        public int getCurrentPosition();
        public void release();
        public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener);
    }

    private static class TruePlayer implements Player {

        private MediaPlayer mp = new MediaPlayer();

        public void play(String path, int offset) throws IOException {
            this.mp.reset();
            this.mp.setDataSource(path);
            this.mp.prepare();
            this.mp.seekTo(offset);
            this.mp.start();
        }

        public void pause() {
            this.mp.pause();
        }

        public int getCurrentPosition() {
            return this.mp.getCurrentPosition();
        }

        public void release() {
            this.mp.release();
        }

        public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
            this.mp.setOnCompletionListener(listener);
        }
    }

    private static class FakePlayer implements Player {

        private int position;

        public FakePlayer(int position) {
            this.position = position;
        }

        public void play(String path, int offset) throws IOException {
        }

        public void pause() {
        }

        public int getCurrentPosition() {
            return this.position;
        }

        public void release() {
        }

        public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        }
    }

    private static class CompletionListener implements MediaPlayer.OnCompletionListener {

        private AudioService service;

        public CompletionListener(AudioService service) {
            this.service = service;
        }

        @Override
        public void onCompletion(MediaPlayer _) {
            this.service.completionProc.run();
        }
    }

    private static class IncomingHandler extends Handler {

        private abstract static class MessageHandler {

            protected AudioService service;

            public MessageHandler(AudioService service) {
                this.service = service;
            }

            public abstract void handle(Message msg);

            protected void reply(Message msg, Message res) {
                try {
                    msg.replyTo.send(res);
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            protected void sendPlaying(Message msg) {
                PlayingArgument a = new PlayingArgument();
                a.position = this.service.position;
                this.reply(msg, Message.obtain(null, MSG_PLAYING, a));
            }
        }

        private static class PauseHandler extends MessageHandler {

            public PauseHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                this.service.player.pause();
            }
        }

        private static class WhatTimeCompletionHandler extends MessageHandler {

            public WhatTimeCompletionHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                Message reply = Message.obtain(null, MSG_COMPLETION);
                try {
                    msg.replyTo.send(reply);
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        private static class WhatTimeHandler extends MessageHandler {

            public WhatTimeHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                int what = MSG_WHAT_TIME;
                int pos = this.service.player.getCurrentPosition();
                Message reply = Message.obtain(null, what, pos, 0, msg.obj);
                this.reply(msg,  reply);
            }
        }

        private static class PlayHandler extends MessageHandler {

            public PlayHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                PlayArgument a = (PlayArgument)msg.obj;
                this.service.play(a.offset);
            }
        }

        private static class WhatTimePlayingHandler extends MessageHandler {

            public WhatTimePlayingHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                this.sendPlaying(msg);
                this.service.handler.sendWhatTime();
            }
        }

        private static class WhatFileHandler extends MessageHandler {

            public WhatFileHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                this.sendPlaying(msg);
            }
        }

        private static class WhatTimeNotPlayingHandler extends MessageHandler {

            public WhatTimeNotPlayingHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                this.reply(msg, Message.obtain(null, MSG_NOT_PLAYING));
            }
        }

        private static class InitHandler extends MessageHandler {

            public InitHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                InitArgument a = (InitArgument)msg.obj;
                this.service.directory = a.directory;
                this.service.files = a.files;
                this.service.position = a.position;
            }
        }

        private AudioService service;
        private SparseArray<MessageHandler> handlers;
        private MessageHandler whatTimeHandler;
        private MessageHandler whatTimePlayingHandler;

        public IncomingHandler(AudioService service) {
            super();
            this.initializeHandlers(service);
        }

        @Override
        public void handleMessage(Message msg) {
            this.handlers.get(msg.what).handle(msg);
        }

        public void complete() {
            MessageHandler h = new WhatTimeCompletionHandler(this.service);
            this.handlers.put(MSG_WHAT_TIME, h);
        }

        public void sendPlaying() {
            this.handlers.put(MSG_WHAT_TIME, this.whatTimePlayingHandler);
        }

        public void sendWhatTime() {
            this.handlers.put(MSG_WHAT_TIME, this.whatTimeHandler);
        }

        private void initializeHandlers(AudioService service) {
            this.service = service;
            this.handlers = new SparseArray<MessageHandler>();
            this.handlers.put(MSG_PLAY,  new PlayHandler(service));
            this.handlers.put(MSG_INIT, new InitHandler(service));
            this.handlers.put(MSG_PAUSE, new PauseHandler(service));
            this.handlers.put(MSG_WHAT_FILE, new WhatFileHandler(service));
            this.handlers.put(
                    MSG_WHAT_TIME,
                    new WhatTimeNotPlayingHandler(service));
            this.whatTimeHandler = new WhatTimeHandler(service);
            this.whatTimePlayingHandler = new WhatTimePlayingHandler(service);
        }
    }

    private abstract static class CompletionProcedure {

        protected AudioService service;

        public CompletionProcedure(AudioService service) {
            this.service = service;
        }

        public abstract void run();
    }

    private class StopProcedure extends CompletionProcedure {

        public StopProcedure(AudioService service) {
            super(service);
        }

        @Override
        public void run() {
            this.service.handler.complete();
        }
    }

    private class PlayNextProcedure extends CompletionProcedure {

        public PlayNextProcedure(AudioService service) {
            super(service);
        }

        @Override
        public void run() {
            this.service.position += 1;
            this.service.play(0);
        }
    }

    /*
     * Protocol for the service
     * ========================
     *
     * +-------------+---------------+-----------------------------------------+
     * |Request      |Response       |Description                              |
     * +=============+===============+=========================================+
     * |MSG_INIT     |(nothing)      |Initializes the service with a file list.|
     * |             |               |The service set current audio as a first |
     * |             |               |one in the list.                         |
     * +-------------+---------------+-----------------------------------------+
     * |MSG_PLAY     |(nothing)      |Plays the current audio from given       |
     * |             |               |offset.                                  |
     * +-------------+---------------+-----------------------------------------+
     * |MSG_PAUSE    |(nothing)      |                                         |
     * +-------------+---------------+-----------------------------------------+
     * |MSG_WHAT_TIME|MSG_WHAT_TIME  |Tells current offset.                    |
     * +             +---------------+-----------------------------------------+
     * |             |MSG_PLAYING    |Tells new file started.                  |
     * +             +---------------+-----------------------------------------+
     * |             |MSG_COMPLETION |Tells that the list ended.               |
     * +             +---------------+-----------------------------------------+
     * |             |MSG_NOT_PLAYING|Tells that no music is on air.           |
     * +-------------+---------------+-----------------------------------------+
     * |MSG_WHAT_FILE|MSG_PLAYING    |Tells what file the service playing.     |
     * +-------------+---------------+-----------------------------------------+
     *
     * About MSG_NOT_PLAYING
     * ---------------------
     *
     *  Sometimes Android kills the process which is playing music. Android re-
     *  creates the service, but the service gets initialized (The service is
     *  playing nothing). So, when a user restart the application, because the
     *  application is resumed to be playing the killed music, then it sends
     *  MSG_WHAT_TIME. The service must tell that no music is playing to stop
     *  the timer, etc.
     */
    public static final int MSG_PLAY = 0x00;
    public static final int MSG_INIT = 0x01;
    public static final int MSG_PLAYING = 0x02;
    public static final int MSG_PAUSE = 0x04;
    public static final int MSG_WHAT_TIME = 0x08;
    public static final int MSG_WHAT_FILE = 0x10;
    public static final int MSG_COMPLETION = 0x20;
    public static final int MSG_NOT_PLAYING = 0x40;

    private static final String LOG_TAG = MainActivity.LOG_TAG;

    private String directory;
    private String[] files;
    private int position;

    private IncomingHandler handler;
    private Messenger messenger;
    private Player player;
    private CompletionProcedure completionProc;
    private CompletionProcedure stopProc;
    private CompletionProcedure playNextProc;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "One client was bound with AudioService.");
        return this.messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG, "The client was unbound of AudioService.");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.handler = new IncomingHandler(this);
        this.messenger = new Messenger(this.handler);
        this.player = new TruePlayer();
        this.player.setOnCompletionListener(new CompletionListener(this));
        this.stopProc = new StopProcedure(this);
        this.playNextProc = new PlayNextProcedure(this);

        Log.i(LOG_TAG, "AudioService was created.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Player player = this.player;
        player.pause();
        /*
         * MSG_WHAT_TIME message comes even after onDestroy(). So I placed
         * FakePlayer to handle MSG_WHAT_TIME.
         */
        this.player = new FakePlayer(player.getCurrentPosition());
        player.release();

        Log.i(LOG_TAG, "AudioService was destroyed.");
    }

    private void updateCompletionProcedure() {
        boolean isLast = this.position == this.files.length - 1;
        this.completionProc = isLast ? this.stopProc : this.playNextProc;
    }

    private void play(int offset) {
        String file = this.files[this.position];
        String path = this.directory + File.separator + file;
        try {
            this.player.play(path, offset);
        }
        catch (IOException e) {
            e.printStackTrace();
            // TODO: The handler must return an error to a client.
            return;
        }
        this.updateCompletionProcedure();
        this.handler.sendPlaying();

        Log.i(LOG_TAG, String.format("Play: %s from %d", path, offset));
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4

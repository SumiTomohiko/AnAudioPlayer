package jp.gr.java_conf.neko_daisuki.anaudioplayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class AudioService extends Service {

    private static class PlayArgument {

        public String path;
        public int offset;
    }

    private interface Player {

        public void play(String path, int offset) throws IOException;
        public void pause();
        public int getCurrentPosition();
        public void release();
    }

    private class TruePlayer implements Player {

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
    }

    private class FakePlayer implements Player {

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
    }

    private class IncomingHandler extends Handler {

        private abstract class MessageHandler {

            protected AudioService service;

            public MessageHandler(AudioService service) {
                this.service = service;
            }

            public abstract void handle(Message msg);
        }

        private class PauseHandler extends MessageHandler {

            public PauseHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                this.service.player.pause();
            }
        }

        private class WhatTimeHandler extends MessageHandler {

            public WhatTimeHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                int what = MSG_WHAT_TIME;
                int pos = this.service.player.getCurrentPosition();
                Message reply = Message.obtain(null, what, pos, 0, null);
                try {
                    msg.replyTo.send(reply);
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        private class PlayHandler extends MessageHandler {

            public PlayHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                PlayArgument a = (PlayArgument)msg.obj;

                try {
                    this.service.player.play(a.path, a.offset);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    // TODO: The handler must return an error to a client.
                    return;
                }

                Log.i(LOG_TAG, String.format("Play: %s from %d", a.path, a.offset));
            }
        }

        private Map<Integer, MessageHandler> handlers;

        public IncomingHandler(AudioService service) {
            super();
            this.initializeHandlers(service);
        }

        @Override
        public void handleMessage(Message msg) {
            this.handlers.get(msg.what).handle(msg);
        }

        private void initializeHandlers(AudioService service) {
            this.handlers = new HashMap<Integer, MessageHandler>();
            this.handlers.put(MSG_PLAY, new PlayHandler(service));
            this.handlers.put(MSG_PAUSE, new PauseHandler(service));
            this.handlers.put(MSG_WHAT_TIME, new WhatTimeHandler(service));
        }
    }

    public static final int MSG_PLAY = 3;
    public static final int MSG_PAUSE = 4;
    public static final int MSG_WHAT_TIME = 6;

    private static final String LOG_TAG = MainActivity.LOG_TAG;

    private Messenger messenger;
    private Player player;

    public static Object makePlayArgument(String path, int offset) {
        PlayArgument a = new PlayArgument();
        a.path = path;
        a.offset = offset;
        return a;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "Bound.");
        return this.messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.messenger = new Messenger(new IncomingHandler(this));
        this.player = new TruePlayer();
        Log.i(LOG_TAG, "AudioService was created.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        /*
         * MSG_WHAT_TIME message comes even after onDestroy().
         * So I placed FakePlayer to handle MSG_WHAT_TIME.
         */
        Player player = this.player;
        player.pause();
        this.player = new FakePlayer(player.getCurrentPosition());
        player.release();

        Log.i(LOG_TAG, "AudioService was destroyed.");
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4

package jp.ddo.neko_daisuki.anaudioplayer;

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

    private class IncomingHandler extends Handler {

        private abstract class MessageHandler {

            protected AudioService service;

            public MessageHandler(AudioService service) {
                this.service = service;
            }

            public abstract void handle(Message msg);
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

                MediaPlayer player = this.service.player;
                player.reset();
                try {
                    player.setDataSource(a.path);
                    player.prepare();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    // TODO: The handler must return an error to a client.
                    return;
                }
                player.seekTo(a.offset);
                player.start();

                Log.i(LOG_TAG, String.format("Play: %s", a.path));
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
            this.handlers.put(MSG_WHAT_TIME, new WhatTimeHandler(service));
        }
    }

    public static final int MSG_PLAY = 3;
    //public static final int MSG_PAUSE = 4;
    public static final int MSG_WHAT_TIME = 6;

    private static final String LOG_TAG = MainActivity.LOG_TAG;

    private Messenger messenger;
    private MediaPlayer player;

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
        this.player = new MediaPlayer();
        Log.i(LOG_TAG, "AudioService was created.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.player.release();
        Log.i(LOG_TAG, "AudioService was destroyed.");
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4

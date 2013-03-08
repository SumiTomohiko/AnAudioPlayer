package jp.gr.java_conf.neko_daisuki.anaudioplayer;

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

    private static class PlayArgument {

        public String path;
        public int offset;
    }

    private interface Player {

        public void play(String path, int offset) throws IOException;
        public void pause();
        public int getCurrentPosition();
        public void release();
        public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener);
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

        public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
            this.mp.setOnCompletionListener(listener);
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
            this.service.handler.complete();
        }
    }

    private static class IncomingHandler extends Handler {

        private abstract class MessageHandler {

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
        }

        private class PauseHandler extends MessageHandler {

            public PauseHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                this.service.player.pause();
            }
        }

        private class WhatTimeCompletionHandler extends MessageHandler {

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

        private class WhatTimeHandler extends MessageHandler {

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

        private class PlayHandler extends MessageHandler {

            public PlayHandler(AudioService service) {
                super(service);
            }

            public void handle(Message msg) {
                /*
                 * I hoped to echo back msg simply, but Android rejected it with
                 * android.util.AndroidRuntimeException of "This message is
                 * already in use".
                 *
                 * My Android is Acer A500 (Android 3.2).
                 */
                Message reply = Message.obtain(null, MSG_PLAY);
                this.reply(msg, reply);

                PlayArgument a = (PlayArgument)msg.obj;
                try {
                    this.service.player.play(a.path, a.offset);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    // TODO: The handler must return an error to a client.
                    return;
                }

                String fmt = "Play: %s from %d";
                Log.i(LOG_TAG, String.format(fmt, a.path, a.offset));
            }
        }

        private AudioService service;
        private SparseArray<MessageHandler> handlers;

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

        private void initializeHandlers(AudioService service) {
            this.service = service;
            this.handlers = new SparseArray<MessageHandler>();
            this.handlers.put(MSG_PLAY, new PlayHandler(service));
            this.handlers.put(MSG_PAUSE, new PauseHandler(service));
            this.handlers.put(MSG_WHAT_TIME, new WhatTimeHandler(service));
        }
    }

    // Message to the service.
    public static final int MSG_PAUSE = 0x01;
    // Message from the service.
    public static final int MSG_COMPLETION = 0x10;
    // Message from/to the service.
    public static final int MSG_PLAY = 0x00;
    public static final int MSG_WHAT_TIME = 0x02;

    private static final String LOG_TAG = MainActivity.LOG_TAG;

    private IncomingHandler handler;
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
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4

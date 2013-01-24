package jp.gr.java_conf.neko_daisuki.android.view;

import java.util.HashMap;
import java.util.Map;

import android.view.MotionEvent;

public class MotionEventDispatcher {

    public interface Proc {

        public boolean run(MotionEvent event);
    }

    private class FakeProc implements Proc {

        public boolean run(MotionEvent event) {
            return false;
        }
    }

    private Map<Integer, Proc> map;

    public MotionEventDispatcher() {
        this.map = new HashMap<Integer, Proc>();
    }

    public void setDownProc(Proc proc) {
        this.setProc(MotionEvent.ACTION_DOWN, proc);
    }

    public void setUpProc(Proc proc) {
        this.setProc(MotionEvent.ACTION_UP, proc);
    }

    public void setMoveProc(Proc proc) {
        this.setProc(MotionEvent.ACTION_MOVE, proc);
    }

    public void removeDownProc() {
        this.removeProc(MotionEvent.ACTION_DOWN);
    }

    public void removeUpProc() {
        this.removeProc(MotionEvent.ACTION_UP);
    }

    public void removeMoveProc() {
        this.removeProc(MotionEvent.ACTION_MOVE);
    }

    public boolean dispatch(MotionEvent event) {
        return this.getProc(event.getActionMasked()).run(event);
    }

    private Proc getProc(int action) {
        Proc proc = this.map.get(action);
        return proc != null ? proc : new FakeProc();
    }

    private void setProc(int action, Proc proc) {
        this.map.put(action, proc);
    }

    private void removeProc(int action) {
        this.map.remove(action);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4

package jp.ddo.neko_daisuki.anaudioplayer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ViewFlipper;

public class MainActivity extends Activity
{
	private static final String log_tag = "An Audio Player";

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
		this.initializeFlipper();
		this.initializeDirList();
    }

	private void initializeDirList() {
		ListView view = (ListView)this.findViewById(R.id.dir_list);
		int layout = android.R.layout.simple_list_item_1;
		this.dirList = this.listMp3Dir(new File("/sdcard/u1"));
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

	private void initializeFlipper() {
		this.flipper = (ViewFlipper)this.findViewById(R.id.flipper);
		int[] next_buttons = { R.id.next0, R.id.next1, R.id.next2 };
		this.setClickListener(next_buttons, new NextFlipper(this.flipper));
		int[] previous_buttons = { R.id.prev0, R.id.prev1, R.id.prev2 };
		this.setClickListener(previous_buttons, new PreviousFlipper(this.flipper));
	}

	private void selectDir(int position) {
		int layout = android.R.layout.simple_list_item_1;
		String dir = this.dirList.get(position);
		this.files = (new File(dir)).list(new Mp3Filter());

		ListView view = (ListView)this.findViewById(R.id.file_list);
		view.setAdapter(new ArrayAdapter<String>(this, layout, this.files));
		view.setOnItemClickListener(new FileListListener(this));

		this.flipper.showNext();
	}

	private void selectFile(int position) {
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

	private List<String> dirList = null;
	private String[] files = new String[0];
	private ViewFlipper flipper;
}

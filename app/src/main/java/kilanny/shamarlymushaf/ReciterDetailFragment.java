package kilanny.shamarlymushaf;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RecoverySystem;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

/**
 * A fragment representing a single Reciter detail screen.
 * This fragment is either contained in a {@link ReciterListActivity}
 * in two-pane mode (on tablets) or a {@link ReciterDetailActivity}
 * on handsets.
 */
public class ReciterDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    private AsyncTask prevTask;
    /**
     * prevent user from download/delete single items while
     * download all is running
     */
    private boolean canDoSingleOperation = true;

    /**
     * The dummy content this fragment is presenting.
     */
    String mItem;

    public boolean isDownloadActive() {
        return prevTask != null && !prevTask.isCancelled();
    }

    public void setCanDoSingleOperation(boolean canDoSingleOperation) {
        this.canDoSingleOperation = canDoSingleOperation;
    }

    public void cancelActiveOperations() {
        if (prevTask != null) {
            if (!prevTask.isCancelled())
                prevTask.cancel(true);
        }
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReciterDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = getArguments().getString(ARG_ITEM_ID);
        }
    }

    public void setSurahProgress(int surah, int prog) {
        View rootView = getView();
        ListView listview = (ListView) rootView.findViewById(R.id.listview_reciter_detail);
        if (listview == null) return;
        View rowView = Utils.getViewByPosition(surah - 1, listview);
        TextView txt = (TextView) rowView.findViewById(R.id.itemProgressText);
        int max = getSurahAyahCount(surah);
        ProgressBar progress = (ProgressBar) rowView.findViewById(R.id.itemProgress);
        txt.setText(String.format("%d / %d", prog, max));
        progress.setProgress(prog);
    }

    private int getSurahAyahCount(int surah) {
        return Utils.AYAH_COUNT[surah - 1] + (surah == 1 ? 1 : 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_reciter_detail, container, false);

        if (mItem != null) {
            final ListView listview = (ListView)
                    rootView.findViewById(R.id.listview_reciter_detail);
            ArrayAdapter<Surah> adapter = new ArrayAdapter<Surah>(getActivity(),
                    R.layout.reciter_download_list_item, WelcomeActivity.surahs) {
                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    LayoutInflater inflater = (LayoutInflater) getActivity()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View rowView = inflater.inflate(R.layout.reciter_download_list_item,
                            parent, false);
                    TextView s = (TextView) rowView.findViewById(R.id.surahName);
                    s.setText(WelcomeActivity.surahs[position].name);
                    final TextView txt = (TextView) rowView.findViewById(R.id.itemProgressText);
                    final int calc = Utils.getNumDownloaded(getActivity(), mItem, position + 1),
                            max = getSurahAyahCount(position + 1);
                    txt.setText(String.format("%d / %d", calc, max));
                    final ProgressBar progress = (ProgressBar)
                            rowView.findViewById(R.id.itemProgress);
                    progress.setMax(max);
                    progress.setProgress(calc);
                    final ImageButton btn = (ImageButton) rowView.findViewById(R.id.download_item);
                    btn.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            if (!canDoSingleOperation) return;
                            if (prevTask != null) {
                                if (!prevTask.isCancelled())
                                    prevTask.cancel(true);
                                return;
                            }
                            prevTask = Utils.downloadSurah(getActivity(), mItem, position + 1,
                                    new RecoverySystem.ProgressListener() {
                                        @Override
                                        public void onProgress(int prog) {
                                            progress.setProgress(prog);
                                            txt.setText(String.format("%d / %d", prog, max));
                                        }
                                    }, new DownloadTaskCompleteListener() {
                                        @Override
                                        public void taskCompleted(int result) {
                                            prevTask = null;
                                        }
                                    });
                        }
                    });
                    rowView.findViewById(R.id.delete_item).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!canDoSingleOperation) return;
                            final String message = String.format("حذف تسجيل سورة %s للقارئ المحدد",
                                    WelcomeActivity.surahs[position].name);
                            Utils.showConfirm(getActivity(), "حذف سورة",
                                    "متأكد أنك تريد " + message + " ؟"
                                    , new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            final ProgressDialog show = new ProgressDialog(getActivity());
                                            show.setTitle(message);
                                            show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                            show.setIndeterminate(false);
                                            show.setCancelable(false);
                                            show.setMax(max);
                                            show.setProgress(0);
                                            show.show();
                                            new AsyncTask<Void, Integer, Void>() {
                                                @Override
                                                protected Void doInBackground(Void... params) {
                                                    File surahDir = Utils.getSurahDir(getActivity(),
                                                            mItem, position + 1);
                                                    if (surahDir.exists()) {
                                                        for (int i = 0; i <= max; ++i) {
                                                            File file = new File(Utils.getAyahFile(i, surahDir));
                                                            if (file.exists())
                                                                file.delete();
                                                            publishProgress(i);
                                                        }
                                                    } else publishProgress(max);
                                                    return null;
                                                }

                                                @Override
                                                protected void onProgressUpdate(final Integer... values) {
                                                    show.setProgress(values[0]);
                                                }

                                                @Override
                                                protected void onPostExecute(Void v) {
                                                    txt.setText(String.format("%d / %d", 0, max));
                                                    progress.setProgress(0);
                                                    show.dismiss();
                                                }
                                            }.execute();
                                        }
                                    }, null);
                        }
                    });
                    return rowView;
                }
            };
            listview.setAdapter(adapter);
        }

        return rootView;
    }
}

package kilanny.shamarlymushaf.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import java.io.File;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.ReciterDetailActivity;
import kilanny.shamarlymushaf.activities.ReciterListActivity;
import kilanny.shamarlymushaf.data.AyahFile;
import kilanny.shamarlymushaf.data.DownloadedAyat;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.Surah;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

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
    public static final int CURRENT_SURAH_NONE = 0;

    private AsyncTask prevTask;
    private ArrayAdapter<SurahDownload> adapter;
    private View mView;
    private SurahDownload[] arr;
    private boolean isLoading = false;
    private int currentDownloadSurah;

    /**
     * prevent user from download/delete single items while
     * download all is running
     */
    private boolean canDoSingleOperation = true;

    /**
     * The dummy content this fragment is presenting.
     */
    public String mItem;

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
        currentDownloadSurah = 0;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReciterDetailFragment() {
    }

    public void setCurrentDownloadSurah(int currentDownloadSurah) {
        if (adapter != null) { //early call before onCreateView() ?
            this.currentDownloadSurah = currentDownloadSurah;
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = getArguments().getString(ARG_ITEM_ID);
        }
    }

    public void setSurahProgress(int surah, int prog, boolean isCurrentDownload) {
        adapter.getItem(surah - 1).downloadedAyah = prog;
        arr[surah - 1].downloadedAyah = prog;
        if (isCurrentDownload)
            currentDownloadSurah = surah;
        adapter.notifyDataSetChanged();
    }

    private int getSurahAyahCount(QuranData quranData, int surah) {
        return quranData.surahs[surah - 1].ayahCount + (surah == 1 ? 1 : 0);
    }

    public void reload() {
        if (isLoading) return;
        isLoading = true;
        mView.findViewById(R.id.progressBarLoading).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.listLayout).setVisibility(View.GONE);
        AppExecutors.getInstance().executeOnCachedExecutor(() -> {
            DownloadedAyat.getInstance(getContext());
            for (int i = 0; i < 114; ++i) {
                adapter.getItem(i).downloadedAyah = Utils.getNumDownloaded(getActivity(), mItem, i + 1);
                arr[i].downloadedAyah = adapter.getItem(i).downloadedAyah;
            }
            Activity activity = getActivity();
            if (activity == null)
                return;
            activity.runOnUiThread(() -> {
                mView.findViewById(R.id.progressBarLoading).setVisibility(View.GONE);
                mView.findViewById(R.id.listLayout).setVisibility(View.VISIBLE);
                ListView listview = mView.findViewById(R.id.listview_reciter_detail);
                listview.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                isLoading = false;
            });
        });
    }

    class SurahDownload {
        public Surah surah;
        public int totalAyah, downloadedAyah;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_reciter_detail, container, false);
        mView = rootView;

        Boolean saveSoundsUri = Utils.isSaveSoundsUri(getContext());
        rootView.findViewById(R.id.txtRefreshTip).setVisibility(
                saveSoundsUri == null || !saveSoundsUri ? View.GONE : View.VISIBLE);

        if (mItem != null) {
            rootView.findViewById(R.id.progressBarLoading).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.listLayout).setVisibility(View.GONE);
            final QuranData quranData = QuranData.getInstance(getActivity());
            arr = new SurahDownload[quranData.surahs.length];
            for (int i = 0; i < quranData.surahs.length; ++i) {
                arr[i] = new SurahDownload();
                arr[i].surah = quranData.surahs[i];
                arr[i].totalAyah = getSurahAyahCount(quranData, i + 1);
            }
            adapter = new ArrayAdapter<SurahDownload>(getActivity(),
                    R.layout.reciter_download_list_item, arr) {
                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    View rowView;
                    if (convertView == null)
                        rowView = inflater.inflate(R.layout.reciter_download_list_item,
                                parent, false);
                    else
                        rowView = convertView;
                    final SurahDownload item = adapter.getItem(position);
                    TextView s = rowView.findViewById(R.id.surahName);
                    s.setText(item.surah.name);
                    s = rowView.findViewById(R.id.status);
                    s.setText(currentDownloadSurah == item.surah.index ? "تحميل..." : "");
                    final TextView txt = rowView.findViewById(R.id.itemProgressText);
                    final ProgressBar progress = rowView.findViewById(R.id.itemProgress);
                    progress.setMax(item.totalAyah);
                    txt.setText(String.format("%d / %d", item.downloadedAyah, item.totalAyah));
                    progress.setProgress(item.downloadedAyah);
                    final ImageButton btn = rowView.findViewById(R.id.download_item);
                    btn.setOnClickListener(view -> {
                        if (!canDoSingleOperation) return;
                        //user can make only one single surah download per time
                        //any other surah is clicked, cancel the previous surah
                        if (prevTask != null) {
                            if (!prevTask.isCancelled())
                                prevTask.cancel(true);
                            Toast.makeText(getActivity(),
                                    "يتم إيقاف التحميل...", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (Utils.isSaveSoundsUri(getActivity()) == null) {
                            Toast.makeText(getActivity(),
                                    "فضلا اختر حافظة تحميل التلاوات أولا",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        setCurrentDownloadSurah(position + 1);
                        prevTask = Utils.downloadSurah(getActivity(), mItem, position + 1,
                                prog -> setSurahProgress(position + 1, prog, true),
                                result -> {
                                    prevTask = null;
                                    if (result != Utils.DOWNLOAD_OK && result != Utils.DOWNLOAD_USER_CANCEL) {
                                        String text = result == Utils.DOWNLOAD_QUOTA_EXCEEDED ?
                                                "تم بلوغ الكمية القصوى للتحميل لهذا اليوم. نرجوا المحاولة غدا أو استخدام التحميل اللامحدود"
                                                : "فشل التحميل. تأكد من اتصالك بالشبكة ووجود مساحة كافية بجهازك";
                                        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
                                    } else if (result == Utils.DOWNLOAD_OK) {
                                        AnalyticsTrackers.getInstance(getContext())
                                                .sendDownloadRecites(mItem, position + 1);
                                    }
                                    setCurrentDownloadSurah(CURRENT_SURAH_NONE);
                                }, quranData);
                        Toast.makeText(getActivity(),
                                "يتم التحميل...", Toast.LENGTH_SHORT).show();
                    });
                    rowView.findViewById(R.id.delete_item).setOnClickListener(view -> {
                        if (!canDoSingleOperation) return;
                        if (prevTask != null) return;
                        final String message = String.format("حذف تسجيل سورة %s للقارئ المحدد",
                                item.surah.name);
                        Utils.showConfirm(getActivity(), "حذف سورة",
                                "متأكد أنك تريد " + message + " ؟"
                                , (dialog, which) -> {
                                    final ProgressDialog show = new ProgressDialog(getActivity());
                                    show.setTitle(message);
                                    show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    show.setIndeterminate(false);
                                    show.setCancelable(false);
                                    show.setMax(item.totalAyah);
                                    show.setProgress(0);
                                    show.show();
                                    new AsyncTask<Void, Integer, Void>() {
                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            Object surahDir = Utils.getSurahDir(getActivity(),
                                                    mItem, position + 1, false);
                                            if (surahDir != null) {
                                                DownloadedAyat downloadedAyat = DownloadedAyat.getInstance(getContext());
                                                for (int i = 0; i <= item.totalAyah; ++i) {
                                                    Object ayahFile = Utils.getAyahFile(i, surahDir);
                                                    int ayah = -1;
                                                    if (ayahFile instanceof File) {
                                                        File file = (File) ayahFile;
                                                        ayah = Integer.parseInt(file.getName());
                                                        if (file.exists())
                                                            file.delete();
                                                    } else if (ayahFile != null) {
                                                        AyahFile file = (AyahFile) ayahFile;
                                                        DocumentFile documentFile = file.get();
                                                        if (documentFile != null) {
                                                            ayah = Integer.parseInt(documentFile.getName());
                                                            documentFile.delete();
                                                        }
                                                    }
                                                    if (ayah >= 0) {
                                                        downloadedAyat.set(mItem, position + 1, ayah, false);
                                                    }
                                                    publishProgress(i);
                                                }
                                                downloadedAyat.save(getContext());
                                            }
                                            publishProgress(item.totalAyah);
                                            return null;
                                        }

                                        @Override
                                        protected void onProgressUpdate(final Integer... values) {
                                            show.setProgress(values[0]);
                                        }

                                        @Override
                                        protected void onPostExecute(Void v) {
                                            setSurahProgress(position + 1, 0, false);
                                            show.dismiss();
                                        }
                                    }.execute();
                                }, null);
                    });
                    return rowView;
                }
            };

            reload();
        }

        return rootView;
    }
}

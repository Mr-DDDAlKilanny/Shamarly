package kilanny.shamarlymushaf.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.ReciteZipItem;

public class ReciterDownloadAdapter extends ArrayAdapter<ReciteZipItem> {

    private final Function<ReciteZipItem, Void> mOnDownloadClick;
    private final Function<ReciteZipItem, Void> mOnImportClick;

    public ReciterDownloadAdapter(@NonNull Context context,
                                  Function<ReciteZipItem, Void> onDownloadClick,
                                  Function<ReciteZipItem, Void> onImportClick) {
        super(context, R.layout.list_item_recite_zip);
        mOnDownloadClick = onDownloadClick;
        mOnImportClick = onImportClick;

        addAll(ReciteZipItem.getAll(context));
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View rowView;
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) parent.getContext()
                    .getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            rowView = layoutInflater.inflate(R.layout.list_item_recite_zip, parent, false);
        } else
            rowView = convertView;
        final ReciteZipItem listItem = getItem(position);
        TextView txtReciteName = rowView.findViewById(R.id.txtReciteName);
        txtReciteName.setText(listItem.name);
        TextView txtRequiredSize = rowView.findViewById(R.id.txtRequiredSize);
        txtRequiredSize.setText(listItem.size);
        rowView.findViewById(R.id.btnDownload).setOnClickListener(view -> mOnDownloadClick.apply(listItem));
        rowView.findViewById(R.id.btnImport).setOnClickListener(view -> mOnImportClick.apply(listItem));
        return rowView;
    }
}

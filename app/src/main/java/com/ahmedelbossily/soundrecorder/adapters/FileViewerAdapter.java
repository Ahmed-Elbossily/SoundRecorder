package com.ahmedelbossily.soundrecorder.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmedelbossily.soundrecorder.DBHelper;
import com.ahmedelbossily.soundrecorder.R;
import com.ahmedelbossily.soundrecorder.RecordingItem;
import com.ahmedelbossily.soundrecorder.fragments.PlaybackFragment;
import com.ahmedelbossily.soundrecorder.listeners.OnDatabaseChangedListener;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class FileViewerAdapter extends RecyclerView.Adapter<FileViewerAdapter.RecordingsViewHolder> implements OnDatabaseChangedListener {

    private static final String LOG_TAG = "FileViewerAdapter";
    private DBHelper mDatabase;
    RecordingItem item;
    Context mContext;
    LinearLayoutManager llm;

    public FileViewerAdapter(Context context, LinearLayoutManager linearLayoutManager) {
        super();
        mContext = context;
        mDatabase = new DBHelper(mContext);
        mDatabase.setOnDatabaseChangedListener(this);
        llm = linearLayoutManager;
    }

    @Override
    public void onBindViewHolder(final FileViewerAdapter.RecordingsViewHolder holder, int position) {
        item = getItem(position);
        long itemDuration = item.getLength();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(itemDuration) - TimeUnit.MINUTES.toSeconds(minutes);

        holder.vName.setText(item.getName());
        holder.vLength.setText(String.format("%02d:%02d", minutes, seconds));
        holder.vDateAdded.setText(
                DateUtils.formatDateTime(
                    mContext,
                    item.getTime(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR)
        );
        // define an on click listener to open PlaybackFragment
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PlaybackFragment playbackFragment = new PlaybackFragment().newInstance(getItem(holder.getPosition()));
                    FragmentTransaction transaction = ((FragmentActivity) mContext).getSupportFragmentManager().beginTransaction();
                    playbackFragment.show(transaction, "dialog_playback");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "exception", e);
                }
            }
        });
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ArrayList<String> entries = new ArrayList<>();
                entries.add(mContext.getString(R.string.dialog_file_share));
                entries.add(mContext.getString(R.string.dialog_file_rename));
                entries.add(mContext.getString(R.string.dialog_file_delete));

                final CharSequence[] items = entries.toArray(new CharSequence[entries.size()]);

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(mContext.getString(R.string.dialog_title_options));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            shareFileDialog(holder.getPosition());
                        } if (item == 1) {
                            renameFileDialog(holder.getPosition());
                        } else if (item == 2) {
                            deleteFileDialog(holder.getPosition());
                        }
                    }
                });
                builder.setCancelable(true);
                builder.setNegativeButton(mContext.getString(R.string.dialog_action_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

                return false;
            }
        });
    }

    @Override
    public FileViewerAdapter.RecordingsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
        mContext = parent.getContext();
        return new RecordingsViewHolder(itemView);
    }

    public class RecordingsViewHolder extends RecyclerView.ViewHolder {
        protected TextView vName;
        protected TextView vLength;
        protected TextView vDateAdded;
        protected View cardView;

        public RecordingsViewHolder(View itemView) {
            super(itemView);
            vName = itemView.findViewById(R.id.file_name_text);
            vLength = itemView.findViewById(R.id.file_length_text);
            vDateAdded = itemView.findViewById(R.id.file_date_added_text);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }

    @Override
    public int getItemCount() {
        return mDatabase.getCount();
    }

    public RecordingItem getItem(int position) {
        return mDatabase.getItemAt(position);
    }

    @Override
    public void onNewDatabaseEntryAdded() {
        //item added to top of the list
        notifyItemInserted(getItemCount() - 1);
        llm.scrollToPosition(getItemCount() - 1);
    }

    @Override
    public void onDatabaseEntryRenamed() {

    }

    public void remove(int position) {
        File file = new File(getItem(position).getFilePath());
        file.delete();
        Toast.makeText(mContext, String.format(mContext.getString(R.string.toast_file_delete), getItem(position).getName()), Toast.LENGTH_SHORT).show();
        mDatabase.removeItemWithId(getItem(position).getId());
        notifyItemRemoved(position);
    }

    public void removeOutOfApp(String filePath) {

    }

    public void rename(int position, String name) {
        String mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFilePath += "/SoundRecorder/" + name;
        File file = new File(mFilePath);
        if (file.exists() && !file.isDirectory()) {
            //file name is not unique, cannot rename file.
            Toast.makeText(mContext,
                    String.format(mContext.getString(R.string.toast_file_exists), name),
                    Toast.LENGTH_SHORT).show();
        } else {
            //file name is unique, rename file
            File oldFilePath = new File(getItem(position).getFilePath());
            oldFilePath.renameTo(file);
            mDatabase.renameItem(getItem(position), name, mFilePath);
            notifyItemChanged(position);
        }
    }

    public void shareFileDialog(int position) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(getItem(position).getFilePath())));
        shareIntent.setType("audio/mp4");
        mContext.startActivity(Intent.createChooser(shareIntent, mContext.getText(R.string.send_to)));
    }

    public void renameFileDialog(final int position) {
        AlertDialog.Builder renameFileBuilder = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_rename_file, null);
        final EditText input = view.findViewById(R.id.new_name);
        renameFileBuilder.setTitle(mContext.getString(R.string.dialog_title_rename));
        renameFileBuilder.setCancelable(true);
        renameFileBuilder.setPositiveButton(mContext.getString(R.string.dialog_action_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                try {
                    String value = input.getText().toString().trim() + ".mp4";
                    rename(position, value);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "exception", e);
                }
            }
        });
        renameFileBuilder.setNegativeButton(mContext.getString(R.string.dialog_action_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        renameFileBuilder.setView(view);
        AlertDialog alertDialog = renameFileBuilder.create();
        alertDialog.show();
    }

    public void deleteFileDialog(final int position) {
        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(mContext);
        confirmDelete.setTitle(mContext.getString(R.string.dialog_title_delete));
        confirmDelete.setMessage(mContext.getString(R.string.dialog_text_delete));
        confirmDelete.setCancelable(true);
        confirmDelete.setPositiveButton(mContext.getString(R.string.dialog_action_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                try {
                    remove(position);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "exception", e);
                }
            }
        });
        confirmDelete.setNegativeButton(mContext.getString(R.string.dialog_action_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = confirmDelete.create();
        alertDialog.show();
    }

}

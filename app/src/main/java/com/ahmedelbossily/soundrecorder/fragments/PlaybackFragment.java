package com.ahmedelbossily.soundrecorder.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ahmedelbossily.soundrecorder.R;
import com.ahmedelbossily.soundrecorder.RecordingItem;
import com.melnykov.fab.FloatingActionButton;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by AhmedElbossily on 26/03/2018.
 */

public class PlaybackFragment extends DialogFragment {
    private static final String LOG_TAG = "PlaybackFragment";
    private static final String ARG_ITEM = "recording_item";
    private RecordingItem item;

    private Handler mHandler = new Handler();
    private MediaPlayer mMediaPlayer = null;

    private SeekBar mSeekBar = null;
    private FloatingActionButton mPlayButton = null;
    private TextView mFileNameTextView = null;
    private TextView mCurrentProgressTextView = null;
    private TextView mFileLengthTextView = null;

    //stores whether or not the mediaplayer is currently playing audio
    private boolean isPlaying = false;

    //stores minutes and seconds of the length of the file.
    long minutes = 0;
    long seconds = 0;

    public PlaybackFragment newInstance(RecordingItem item) {
        PlaybackFragment playbackFragment = new PlaybackFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ITEM, item);
        playbackFragment.setArguments(bundle);
        return playbackFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        item = getArguments().getParcelable(ARG_ITEM);
        long itemDuration = item.getLength();
        minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        seconds = TimeUnit.MILLISECONDS.toSeconds(itemDuration)
                - TimeUnit.MINUTES.toSeconds(minutes);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_media_playback, null);

        mFileNameTextView = view.findViewById(R.id.file_name_text_view);
        mFileLengthTextView = view.findViewById(R.id.file_length_text_view);
        mCurrentProgressTextView = view.findViewById(R.id.current_progress_text_view);
        mPlayButton = view.findViewById(R.id.fab_play);
        mSeekBar = view.findViewById(R.id.seekbar);

        ColorFilter filter = new LightingColorFilter(getResources().getColor(R.color.colorPrimary), getResources().getColor(R.color.colorPrimary));
        mSeekBar.getProgressDrawable().setColorFilter(filter);
        mSeekBar.getThumb().setColorFilter(filter);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mMediaPlayer != null && fromUser) {
                    mMediaPlayer.seekTo(progress);
                    mHandler.removeCallbacks(runnable);

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer.getCurrentPosition());
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer.getCurrentPosition())
                            - TimeUnit.MINUTES.toSeconds(minutes);
                    mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes,seconds));

                    updateSeekBar();

                } else if (mMediaPlayer == null && fromUser) {
                    prepareMediaPlayerFromPoint(progress);
                    updateSeekBar();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(mMediaPlayer != null) {
                    // remove message Handler from updating progress bar
                    mHandler.removeCallbacks(runnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    mHandler.removeCallbacks(runnable);
                    mMediaPlayer.seekTo(seekBar.getProgress());

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer.getCurrentPosition());
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer.getCurrentPosition())
                            - TimeUnit.MINUTES.toSeconds(minutes);
                    mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes,seconds));
                    updateSeekBar();
                }
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay(isPlaying);
                isPlaying = !isPlaying;
            }
        });

        mFileNameTextView.setText(item.getName());
        mFileLengthTextView.setText(String.format("%02d:%02d", minutes,seconds));

        builder.setView(view);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        //set transparent background
        Window window = getDialog().getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);

        //disable buttons from dialog
        AlertDialog alertDialog = (AlertDialog) getDialog();
        alertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
        alertDialog.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(false);
        alertDialog.getButton(Dialog.BUTTON_NEUTRAL).setEnabled(false);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMediaPlayer != null) {
            stopPlaying();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            stopPlaying();
        }
    }

    private void onPlay(boolean isPlaying) {
        if (!isPlaying) {
            //currently MediaPlayer is not playing audio
            if(mMediaPlayer == null) {
                startPlaying(); //start from beginning
            } else {
                resumePlaying(); //resume the currently paused MediaPlayer
            }

        } else {
            //pause the MediaPlayer
            pausePlaying();
        }
    }

    private void startPlaying() {
        mPlayButton.setImageResource(R.drawable.ic_media_pause);
        mMediaPlayer = new MediaPlayer();

        try {
            mMediaPlayer.setDataSource(item.getFilePath());
            mMediaPlayer.prepare();
            mSeekBar.setMax(mMediaPlayer.getDuration());

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlaying();
            }
        });

        updateSeekBar();

        //keep screen on while playing audio
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void prepareMediaPlayerFromPoint(int progress) {
        //set mediaPlayer to start from middle of the audio file

        mMediaPlayer = new MediaPlayer();

        try {
            mMediaPlayer.setDataSource(item.getFilePath());
            mMediaPlayer.prepare();
            mSeekBar.setMax(mMediaPlayer.getDuration());
            mMediaPlayer.seekTo(progress);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                }
            });

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        //keep screen on while playing audio
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void pausePlaying() {
        mPlayButton.setImageResource(R.drawable.ic_media_play);
        mHandler.removeCallbacks(runnable);
        mMediaPlayer.pause();
    }

    private void resumePlaying() {
        mPlayButton.setImageResource(R.drawable.ic_media_pause);
        mHandler.removeCallbacks(runnable);
        mMediaPlayer.start();
        updateSeekBar();
    }

    private void stopPlaying() {
        mPlayButton.setImageResource(R.drawable.ic_media_play);
        mHandler.removeCallbacks(runnable);
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;

        mSeekBar.setProgress(mSeekBar.getMax());
        isPlaying = !isPlaying;

        mCurrentProgressTextView.setText(mFileLengthTextView.getText());
        mSeekBar.setProgress(mSeekBar.getMax());

        //allow the screen to turn off again once audio is finished playing
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //updating mSeekBar
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null) {
                int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                mSeekBar.setProgress(mCurrentPosition);

                long minutes = TimeUnit.MILLISECONDS.toMinutes(mCurrentPosition);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(mCurrentPosition)
                        - TimeUnit.MINUTES.toSeconds(minutes);
                mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes, seconds));
                updateSeekBar();
            }
        }
    };

    private void updateSeekBar() {
        mHandler.postDelayed(runnable, 1000);
    }
}

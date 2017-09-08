package com.domain.company.audioplayer;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements MediaPlayer.OnSeekCompleteListener {

    private String filePath = "/storage/emulated/legacy/Music/song.mp3";// Environment.getExternalStoragePublicDirectory(               Environment.DIRECTORY_MUSIC)+"/song.mp3";
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isBacking = false;
    private boolean isForward = false;
    private boolean isBBacking = false;
    private boolean isFForward = false;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private MediaObserver observer = null;

    public void back(View view) {
        seek(-5000);
        isBacking = true;
    }

    public void fwd(View view) {
        if (!isPlaying) {
            startPlay();
        }
        seek(5000);
        isForward = true;
    }

    public void bback(View view) {
        seek(-60000);
        isBBacking = true;
    }

    public void ffwd(View view) {
        if (!isPlaying) {
            startPlay();
        }
        seek(60000);
        isFForward = true;
    }

    private void seek(int delta) {
        if (isBacking || isForward || isBBacking || isFForward || !isPlaying) {
            return;
        }

        unPause();

        toggleSeek(false, true);

        int np = mediaPlayer.getCurrentPosition() + delta;
        if (np >= mediaPlayer.getDuration()){
            np = mediaPlayer.getDuration()-1;
            togglePauseIfPlaying();
        }
        mediaPlayer.seekTo(np);
    }

    private void toggleSeek(boolean v, boolean all) {
        Button b = (Button) findViewById(R.id.back);
        b.setEnabled(v);

        b = (Button) findViewById(R.id.bback);
        b.setEnabled(v);

        if (all) {
            b = (Button) findViewById(R.id.fwd);
            b.setEnabled(v);

            b = (Button) findViewById(R.id.ffwd);
            b.setEnabled(v);
        }
    }

    public void play(View view) {

        if (isPlaying) {
            stopPlay();
        } else {
            startPlay();
        }
    }

    public void pause(View view) {
        togglePauseIfPlaying();
    }

    private void unPause() {
        if (isPlaying && isPaused) {
            isPaused = false;
            mediaPlayer.start();
        }
    }

    private void togglePauseIfPlaying() {
        if (isPlaying) {
            if (!isPaused) {
                isPaused = true;
                mediaPlayer.pause();
            } else if (isPaused) {
                isPaused = false;
                mediaPlayer.start();
            }
        }
    }


    private void stopPlay() {
        if (!isPlaying) {
            return;
        }

        TextView t = (TextView) findViewById(R.id.textView1);
        Button btnP = (Button) findViewById(R.id.play);
        Button btnPa = (Button) findViewById(R.id.pause);
        try {
            btnP.setText("P");
            toggleSeek(false, false);
            btnPa.setEnabled(false);
            mediaPlayer.stop();
            mediaPlayer.reset();
            observer.stop();
            observer = null;
            t.setText("ok stop ");
        } catch (Exception e) {
            e.printStackTrace();
            t.setText("fail stop " + e.getMessage());
        }

        isPlaying = false;
        isPaused = false;
    }

    private void startPlay() {
        if (isPlaying) {
            return;
        }

        TextView t = (TextView) findViewById(R.id.textView1);
        Button btnP = (Button) findViewById(R.id.play);
        Button btnPa = (Button) findViewById(R.id.pause);
        FileInputStream fis = null;
        try {

            fis = new FileInputStream(filePath);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            //mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            t.setText("ok play");
            isPlaying = true;
            isPaused = false;
            btnP.setText("S");
            toggleSeek(true, true);
            btnPa.setEnabled(true);

            observer = new MediaObserver();
            mediaPlayer.start();
            new Thread(observer).start();

        } catch (IOException e) {
            t.setText("fail play " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignore) {
                }
            }

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer.setOnSeekCompleteListener(this);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mPlayer) {
                stopPlay();
            }
        });

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            filePath = getIntent().getData().getPath();
            Log.d(TAG, "onCreate: " + filePath);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            stopPlay();
            mediaPlayer.release();
        } finally {
            mediaPlayer = null;
        }

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (isBacking) {
            isBacking = false;
        }
        if (isForward) {
            isForward = false;
        }

        if (isBBacking) {
            isBBacking = false;
        }
        if (isFForward) {
            isFForward = false;
        }

        toggleSeek(true, true);

    }


    private class MediaObserver implements Runnable {
        private AtomicBoolean stop = new AtomicBoolean(false);

        public void stop() {
            stop.set(true);
        }

        @Override
        public void run() {
            while (!stop.get()) {
                double per = 100 * (double) mediaPlayer.getCurrentPosition() / (double) mediaPlayer.getDuration();
                int curr = mediaPlayer.getCurrentPosition() / 1000;
                int total = mediaPlayer.getDuration() / 1000;
                TextView t = (TextView) findViewById(R.id.textView1);
                if (!stop.get()) {
                    update(t, String.format("%.2f%% %d/%d", per, curr, total));
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void update(final TextView t, final String value) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    t.setText(value);

                }
            });
        }
    }
}

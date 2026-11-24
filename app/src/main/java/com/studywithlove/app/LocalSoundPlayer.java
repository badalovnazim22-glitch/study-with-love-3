package com.studywithlove.app;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;

/** Короткие оригинальные звуковые сигналы, синтезируемые локально без сети и системного ToneGenerator. */
public final class LocalSoundPlayer {
    private static final int RATE = 22050;
    private LocalSoundPlayer() {}

    public static void play(Context context, boolean focusFinished, int volumePercent) {
        if (context == null) return;
        final short[] samples = create(focusFinished);
        final float volume = Math.max(0f, Math.min(1f, volumePercent / 100f));
        Thread worker = new Thread(() -> {
            AudioTrack track = null;
            try {
                track = new AudioTrack(AudioManager.STREAM_MUSIC, RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        samples.length * 2, AudioTrack.MODE_STATIC);
                if (track.getState() != AudioTrack.STATE_INITIALIZED) return;
                track.setVolume(volume);
                track.write(samples, 0, samples.length);
                track.play();
                long duration = samples.length * 1000L / RATE;
                Thread.sleep(duration + 80L);
            } catch (Exception ignored) {
            } finally {
                if (track != null) {
                    try { if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) track.stop(); } catch (Exception ignored) {}
                    try { track.release(); } catch (Exception ignored) {}
                }
            }
        }, "study-local-sound");
        worker.setDaemon(true);
        worker.start();
    }

    private static short[] create(boolean focus) {
        // Мягкая двух/трёхнотная мелодия с плавной атакой и затуханием.
        int[] notes = focus ? new int[]{659, 784, 988} : new int[]{784, 988};
        int[] lengths = focus ? new int[]{180, 190, 330} : new int[]{130, 230};
        int total = 0;
        for (int length : lengths) total += length * RATE / 1000;
        short[] out = new short[total];
        int offset = 0;
        for (int n = 0; n < notes.length; n++) {
            int count = lengths[n] * RATE / 1000;
            for (int i = 0; i < count; i++) {
                double t = i / (double) RATE;
                double edge = Math.min(1.0, Math.min(i / 500.0, (count - i) / 1100.0));
                double wave = Math.sin(2.0 * Math.PI * notes[n] * t)
                        + 0.18 * Math.sin(4.0 * Math.PI * notes[n] * t);
                out[offset + i] = (short) (Math.max(0, edge) * wave * 4800);
            }
            offset += count;
        }
        return out;
    }
}

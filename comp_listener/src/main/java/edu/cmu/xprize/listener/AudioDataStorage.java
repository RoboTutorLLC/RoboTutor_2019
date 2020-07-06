package edu.cmu.xprize.listener;

import java.util.Arrays;

public class AudioDataStorage {

    /**
     * audioData stores the data
     * finIndex is used to add to audioData without overwriting existing data
     * Volatile because it is accessed by the RecognizerThread but also by the _______ thread
     */
    public static volatile short[] audioData = new short[160 * 25 * 100]; // creates a buffer that can store 25 seconds worth of audio
    public static volatile int finIndex = 0;

    // I initialize the array and add to it

    public static synchronized void addAudioData(short[] buffer) {
        try {
            for (short s : buffer) {
                audioData[finIndex] = s;
                finIndex++;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // ??
            // Should I save the audio file that we have and start writing to a new file?
        }
    }

    public static synchronized void clearAudioData() {
        Arrays.fill(audioData, (short) 0);
        finIndex = 0;
    }

    public static void saveAudioData(String filepath) {

    }

}

package edu.cmu.xprize.listener;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AudioDataStorage {

    /**
     * audioData stores the data
     * finIndex is used to add to audioData without overwriting existing data
     * Volatile because it is a ccessed by the RecognizerThread but also by the _______ thread
     */
    public static final int MAX_LEN = 160* 60 * 100; // can store 60 seconds worth of audio
    public static volatile short[] audioData = new short[MAX_LEN];
    public static volatile int finIndex = 0;
    public static final int HEADER_LENGTH = 44;
    public static List<ListenerBase.HeardWord> segmentation = new ArrayList<ListenerBase.HeardWord>();
    static JSONObject storyData;
    static int currentSentence = 0;
    static int sampleRate;

    static FileOutputStream outputStream;
    static FileChannel outChannel;

    public static void initStoryData(String jsonData) {
        try {
            storyData = new JSONObject(jsonData);
        } catch (Exception e) {
            Log.wtf("AudioDataStorage", "Could not load storydata");
        }
    }

    // I initialize the array and add to it
    static synchronized void addAudioData(int index, short[] buffer) {
        try {
            for(int i = 0; i < index; i++) {
                audioData[finIndex + i] = buffer[i];
            }
            finIndex += index;
            Log.d("AudioDataStorage", "Successfully stored Audio Data");
        } catch (ArrayIndexOutOfBoundsException e) {
            // ??
            // Should I save the audio file that we have and start writing to a new file?
            Log.d("AudioDataStorage", "out of bounds. finIndex is " + finIndex);
        } catch (Exception e) {
            Log.d("AudioDataStorage", "Failed to capture audio");
        }
    }

    public static synchronized void clearAudioData() {
        Log.d("AudioDataStorage", "AudioData Cleared");
        Arrays.fill(audioData, (short) 0);
        finIndex = 0;
    }

    public static void saveAudioData(String fileName, String assetLocation) {
        Log.d("ADSSave", "attempting to save audiodata.");

        sampleRate = 16000;

        String completeFilePath = assetLocation + fileName + ".wav";

        Log.d("ADSSave", completeFilePath);

        // write segmentation to file
        try {
            FileOutputStream os = new FileOutputStream(assetLocation + "/" + fileName + ".seg");
            StringBuilder segData = new StringBuilder("");

            Log.d("Segprogress", "FileOutputStream Created at " + assetLocation + "/" + fileName + ".seg");
            for(ListenerBase.HeardWord word: segmentation) {
                segData.append(word.hypWord + "\t" + word.startFrame + "\t" + word.endFrame);
                segData.append("\n");
            }
            segData.deleteCharAt(segData.lastIndexOf("\n"));

            byte[] segBytes = segData.toString().getBytes();
            os.write(segBytes);
            os.close();

        } catch(IOException | NullPointerException e) {
            Log.wtf("AudioDataStorage", "Failed to write segmentation!");
            Log.d("SegmentationFail", Log.getStackTraceString(e));
        }

        // Update Storydata.json
        try {
            boolean isSentence = true;
            JSONObject sentenceData = storyData
                    .getJSONArray("data")
                    .getJSONObject(currentSentence)
                    .getJSONArray("text")
                    .getJSONArray(0)
                    .getJSONObject(0)
                    .getJSONArray("narration")
                    .getJSONObject(0);

            JSONArray segm = sentenceData.getJSONArray("segmentation");
            long finalEndTime = 0;
            for(ListenerBase.HeardWord heardWord : segmentation) {
                JSONObject segObj = new JSONObject();
                segObj.put("end", heardWord.endTime);
                segObj.put("start", heardWord.startTime);
                segObj.put("word", heardWord.hypWord);
                segm.put(segObj);
                finalEndTime = heardWord.endTime;
            }
            sentenceData.put("from", segmentation.get(0).startTime); // TODO: figure out what 'from' is supposed to represent
            sentenceData.put("audio", completeFilePath);
            sentenceData.put("until", finalEndTime);
            sentenceData.put("utterance", fileName);

            FileOutputStream outJson = new FileOutputStream(assetLocation + "storydata.json");
            outJson.write(storyData.toString().getBytes());
            outJson.close();
        } catch(JSONException e) {
            Log.wtf("ADSStoryData", "Failed to update storyData!");
            Log.d("StoryDataFail", Log.getStackTraceString(e));
        } catch(IOException e) {
            Log.d("ADSStoryDataFail", "Was not able to open file");
        }

    }

    static void updateHypothesis(ListenerBase.HeardWord[] heardWords) {
        Log.d("AudioDataStorageHyp", "Hypothesis Updated");
        segmentation.clear();
        segmentation.addAll(Arrays.asList(heardWords));
    }

    static void setSampleRate(int samplerate) {
        // samplerate is always 16000 from my experience
    }
}

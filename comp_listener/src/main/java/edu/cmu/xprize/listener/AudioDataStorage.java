package edu.cmu.xprize.listener;

import java.io.FileOutputStream;
import java.io.IOException;
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
     * Volatile because it is accessed by the RecognizerThread but also by the _______ thread
     */
    public static final int MAX_LEN = 160* 60 * 100; // can store 60 seconds worth of audio
    public static volatile short[] audioData = new short[MAX_LEN];
    public static volatile int finIndex = 0;
    public static final int HEADER_LENGTH = 44;
    public static List<ListenerBase.HeardWord> segmentation = new ArrayList<ListenerBase.HeardWord>();
    static JSONObject storyData;
    static int currentSentence = 0;

    public static void initStoryData(JSONObject JSONObj) {
        storyData = JSONObj;
    }

    // I initialize the array and add to it
    static synchronized void addAudioData(short[] buffer) {
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

    public static void saveAudioData(String fileName, String assetLocation) {

        String completeFilePath = assetLocation + "/" + fileName + ".wav";

        // Write the audio to file
        try {
            // Remove trailing silence
            short[] trimmedAudio = null;
            for(int x = audioData.length - 1; x > 0; x++) {
                if (audioData[x] != 0) {
                    trimmedAudio = new short[x + 1];
                    for(int y = 0; y<x; y++) {
                        trimmedAudio[y] = audioData[y];
                    }
                    break;
                }
            }


            FileOutputStream os = new FileOutputStream(completeFilePath);
            int dataLen = trimmedAudio.length;
            ByteBuffer dataBuffer = ByteBuffer.allocate(dataLen * 2 /* the data */ + 44 /* The header */);

            dataBuffer.order(ByteOrder.LITTLE_ENDIAN); // I *think* little endian is what .wav uses, not sure about .mp3

            int sampleRate = 16000;
            int channelNumber = 1;
            long bitRate = sampleRate * channelNumber * 16; // sampleRate times number of Channels times the number of bits per sample
            byte[] header = new byte[]{'R','I','F','F',
                    (byte) (dataLen & 0xff), (byte) (byte) ((dataLen >> 8) & 0xff), (byte) ((dataLen >> 16) & 0xff),(byte) ((dataLen >> 24) & 0xff),
                    'W','A','V','E','f','m','t',' ',
                    (byte) 16, 0, 0, 1, 0, (byte) channelNumber, 0,(byte) (sampleRate & 0xff), (byte) ((sampleRate >> 8) & 0xff), (byte) ((sampleRate >> 16) & 0xff), (byte) ((sampleRate >> 24) & 0xff),
                    (byte) ((bitRate / 8) & 0xff), (byte) (((bitRate / 8) >> 8) & 0xff), (byte) (((bitRate / 8) >> 16) & 0xff), (byte) (((bitRate / 8) >> 24) & 0xff), (byte) ((channelNumber * 16) / 8),
                    0, 16, 0, 'd', 'a', 't', 'a', (byte) (dataLen * 2 & 0xff), (byte) (((dataLen * 2) >> 8) &  0xff), (byte) (((dataLen * 2) >> 16) &  0xff), (byte) (((dataLen * 2) >> 24) &  0xff)
             };
            dataBuffer.put(header);

            ShortBuffer dataBufferShort = dataBuffer.asShortBuffer();
            dataBufferShort.put(trimmedAudio); // dataBuffer changes alongside dataBufferShort


            FileChannel out = os.getChannel();
            out.write(dataBuffer);
            out.close();
            os.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace(); // ?
        }

        // write segmentation to file
        try {
            FileOutputStream os = new FileOutputStream(assetLocation + "/" + fileName + ".seg");
            StringBuilder segData = new StringBuilder("");

            for(ListenerBase.HeardWord word: segmentation) {
                segData.append(word.hypWord + "\t" + word.startTime + "\t" + word.startTime);
                segData.append("\n");
            }
            segData.deleteCharAt(segData.lastIndexOf("\n"));

            byte[] segBytes = segData.toString().getBytes();
            os.write(segBytes);
            os.close();
        } catch(IOException | NullPointerException e) {
            e.printStackTrace();
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
            sentenceData.put("from", ""); // TODO: figure out what 'from' is supposed to represent
            sentenceData.put("audio", completeFilePath);
            sentenceData.put("until", finalEndTime);
            sentenceData.put("utterance", fileName);

        } catch(JSONException e) {

        }

        segmentation.clear();
    }

    static void updateHypothesis(ListenerBase.HeardWord[] heardWords) {
        segmentation.addAll(Arrays.asList(heardWords));
    }
}

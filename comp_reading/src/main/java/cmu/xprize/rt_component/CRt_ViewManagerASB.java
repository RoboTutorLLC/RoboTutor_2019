//*********************************************************************************
//
//    Copyright(c) 2016-2017  Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package cmu.xprize.rt_component;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.text.Html;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import cmu.xprize.util.CPersonaObservable;
import cmu.xprize.util.ILoadableObject;
import cmu.xprize.util.IScope;
import cmu.xprize.util.JSON_Helper;
import cmu.xprize.util.TCONST;
import cmu.xprize.rt_component.CASB_Seg;
import edu.cmu.xprize.listener.ListenerBase;


/**
 * This view manager provides student UX for the African Story Book format used
 * in the CMU XPrize submission
 */
public class CRt_ViewManagerASB implements ICRt_ViewManager, ILoadableObject {

    private Context                 mContext;

    private ListenerBase            mListener;
    private IVManListener           mOwner;
    private String                  mAsset;

    private CRt_Component           mParent;
    private ImageView               mPageImage;
    private TextView                mPageText;

    private ImageButton             mPageFlip;
    private ImageButton             mSay;

    // ASB even odd page management

    private ViewGroup               mOddPage;
    private ViewGroup               mEvenPage;

    private int                     mOddIndex;
    private int                     mEvenIndex;
    private int                     mCurrViewIndex;

    private String                  mCurrEffectiveVariant = "";
    private String                  mPrevEffectiveVariant;
    private String                  mCurrPrompt = "";
    private String                  mPrevPrompt;

    // state for the current storyName - African Story Book

    private String                  mCurrHighlight = "";
    private int                     mCurrPage;
    private int                     mCurrPara;
    private int                     mCurrLine;
    private int                     mCurrWord;
    private int                     mHeardWord;                          // The expected location of mCurrWord in heardWords - see PLRT version of onUpdate below

    private String                  speakButtonEnable = "DISABLE";
    private String                  speakButtonShow = "HIDE";
    private String                  pageButtonEnable = "DISABLE";
    private String                  pageButtonShow = "HIDE";

    private int                     mPageCount;
    private int                     mParaCount;
    private int                     mLineCount;
    private int                     mWordCount;
    private int                     attemptNum = 1;
    private boolean                 storyBooting;

    private String[]                wordsToDisplay;                      // current sentence words to display - contain punctuation
    private String[]                wordsToSpeak;                        // current sentence words to hear
    private ArrayList<String>       wordsToListenFor;                    // current sentence words to build language model
    private String                  hearRead;

    private CASB_Narration[]        rawNarration;                        // The narration segmentation info for the active sentence
    private String                  rawSentence;                         // currently displayed sentence that need to be recognized
    private CASB_Seg                narrationSegment;
    private String[]                splitSegment;
    private int                     splitIndex = TCONST.INITSPLIT;
    private boolean                 endOfSentence = false;
    private ArrayList<String>       spokenWords;
    private int                     utteranceNdx;
    private int                     segmentNdx;
    private String                  pagePrompt;

    private int                     numUtterance;
    private CASB_Narration          currUtterance;
    private CASB_Seg[]              segmentArray;
    private int                     numSegments;
    private int                     utterancePrev;
    private int                     segmentPrev;
    private int                     segmentCurr;

    private String                  completedSentencesFmtd = "";
    private String                  completedSentences = "";
    private String                  futureSentencesFmtd = "";
    private String                  futureSentences = "";
    private boolean                 showWords;
    private boolean                 showFutureWords;
    private boolean                 listenFutureContent = false;
    private String                  assetLocation;

    private ArrayList<String>       wordsSpoken;
    private ArrayList<String>       futureSpoken;

    // json loadable
    // ZZZ where the money gets loaded

    public String        license;
    public String        story_name;
    public String        authors;
    public String        illustrators;
    public String        language;
    public String        status;
    public String        copyright;
    public String        titleimage;

    public String        prompt;
    public String        parser;
    // ZZZ the money
    public CASB_data[]   data;

    static final String TAG = "CRt_ViewManagerASB";


    /**
     *
     * @param parent
     * @param listener
     */
    public CRt_ViewManagerASB(CRt_Component parent, ListenerBase listener) {
        mParent = parent;
        mContext = mParent.getContext();

        mOddPage = (ViewGroup)android.support.percent.PercentRelativeLayout.inflate(mContext, R.layout.asb_oddpage, null);
        mEvenPage = (ViewGroup)android.support.percent.PercentRelativeLayout.inflate(mContext, R.layout.asb_evenpage, null);

        mOddPage.setVisibility(View.GONE);
        mEvenPage.setVisibility(View.GONE);

        mOddIndex = mParent.addPage(mOddPage);
        mEvenIndex = mParent.addPage(mEvenPage);

        mListener = listener;
    }


    /**
     *   The startup sequence for a new storyName is:
     *   Set - storyBooting flag to inhibit startListening so the script can complete whatever
     *   preparation is required before the listener starts.  Otherwise you get junk hypotheses.
     *
     *   Once the script has completed its introduction etc. it calls nextLine to cause a line increment
     *   which resets storyBooting and enables the listener for the first sentence in the storyName.
     *
     * @param owner
     * @param assetPath
     */
    public void initStory(IVManListener owner, String assetPath, String location) {
        mOwner        = owner;
        mAsset        = assetPath; // ZZZ assetPath... TCONST.EXTERN
        storyBooting  = true;
        assetLocation = location;  // ZZZ assetLocation... contains storydata.json and images

        Log.d(TCONST.DEBUG_STORY_TAG, String.format("mAsset=%s -- assetLocation=%s", mAsset, assetLocation));

        seekToPage(TCONST.ZERO);

        //TODO: CHECK
        mParent.animatePageFlip(true, mCurrViewIndex);
    }

    /**
     *  NOTE: we reset mCurrWord - last parm in seekToStoryPosition
     *
     */
    public void startStory() {
        // reset boot flag to inhibit future calls
        //
        if (storyBooting) {
            mParent.setFeature(TCONST.FTR_STORY_STARTING, TCONST.DEL_FEATURE);
            storyBooting = false;
        }
    }

    @Override
    public void onDestroy() {
    }

    /**
     * From the script writers perspective there is only one say button and one pageflip button
     * Since there are actually two of each - one on each page view we share the state between them and
     * enforce updates so they are kept in sync with user expectations.
     *
     * @param control
     * @param command
     */
    public void setButtonState(View control, String command) {
        try {
            switch (command) {
                case "ENABLE":
                    control.setEnabled(true);
                    break;
                case "DISABLE":
                    control.setEnabled(false);
                    break;
                case "SHOW":
                    control.setVisibility(View.VISIBLE);
                    break;
                case "HIDE":
                    control.setVisibility(View.INVISIBLE);
                    break;
            }
        } catch(Exception e) {
            Log.d(TAG, "result:" + e);
        }
    }

    public void setSpeakButton(String command) {
        switch (command) {
            case "ENABLE":
                speakButtonEnable = command;
                break;
            case "DISABLE":
                speakButtonEnable = command;
                break;
            case "SHOW":
                speakButtonShow = command;
                break;
            case "HIDE":
                speakButtonShow = command;
                break;
        }
        // Ensure the buttons reflect the current states
        //
        updateButtons();
    }

    public void setPageFlipButton(String command) {
        switch (command) {
            case "ENABLE":
                Log.i("ASB", "ENABLE Flip Button");
                pageButtonEnable = command;
                break;
            case "DISABLE":
                Log.i("ASB", "DISABLE Flip Button");
                pageButtonEnable = command;
                break;
            case "SHOW":
                pageButtonShow = command;
                break;
            case "HIDE":
                pageButtonShow = command;
                break;
        }
        // Ensure the buttons reflect the current states
        //
        updateButtons();
    }

    private void updateButtons() {
        // Make the button states insensitive to the page - So the script does not have to
        // worry about timing of setting button states.
        //
        setButtonState(mPageFlip, pageButtonEnable);
        setButtonState(mPageFlip, pageButtonShow);

        setButtonState(mSay, speakButtonEnable);
        setButtonState(mSay, speakButtonShow);

        mPageFlip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TCONST.QGRAPH_MSG, "event.click: " + " CRt_ViewManagerASB: PAGEFLIP");

                mParent.onButtonClick(TCONST.PAGEFLIP_BUTTON);
            }
        });

        mSay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TCONST.QGRAPH_MSG, "event.click: " + " CRt_ViewManagerASB:onButtonClick SPEAKBUTTON");

                mParent.onButtonClick(TCONST.SPEAK_BUTTON);
            }
        });
    }

    /**
     *  This configures the target display components to be populated with data.
     *
     *  mPageImage - mPageText
     *
     */
    public void flipPage() {
        // Note that we use zero based indexing so page zero is first page - i.e. odd
        //
        if (mCurrPage % 2 == 0) {
            mCurrViewIndex = mOddIndex;
            mPageImage = (ImageView) mOddPage.findViewById(R.id.SpageImage);
            mPageText  = (TextView) mOddPage.findViewById(R.id.SstoryText);

            mPageFlip = (ImageButton) mOddPage.findViewById(R.id.SpageFlip);
            mSay      = (ImageButton) mOddPage.findViewById(R.id.Sspeak);
        } else {
            mCurrViewIndex = mEvenIndex;
            mPageImage = (ImageView) mEvenPage.findViewById(R.id.SpageImage);
            mPageText  = (TextView) mEvenPage.findViewById(R.id.SstoryText);

            mPageFlip = (ImageButton) mEvenPage.findViewById(R.id.SpageFlip);
            mSay      = (ImageButton) mEvenPage.findViewById(R.id.Sspeak);
        }
        // Ensure the buttons reflect the current states
        //
        updateButtons();
    }

    private void configurePageImage() {
        InputStream in;

        try {
            if (assetLocation.equals(TCONST.EXTERN)) {
                Log.d(TCONST.DEBUG_STORY_TAG, "loading image " + mAsset + data[mCurrPage].image);
                in = new FileInputStream(mAsset + data[mCurrPage].image); // ZZZ load image
            } else if (assetLocation.equals(TCONST.EXTERN_SHARED)) {
                Log.d(TCONST.DEBUG_STORY_TAG, "loading shared image " + mAsset + data[mCurrPage].image);
                in = new FileInputStream(mAsset + data[mCurrPage].image); // ZZZ load image
            } else {
                Log.d(TCONST.DEBUG_STORY_TAG, "loading image from asset" + mAsset + data[mCurrPage].image);
                in = JSON_Helper.assetManager().open(mAsset + data[mCurrPage].image); // ZZZ load image
            }

            // ALAN_HILL (5) here is how to load the image...... NEXT NEXT NEXT
            mPageImage.setImageBitmap(BitmapFactory.decodeStream(in));
        } catch (IOException e) {
            mPageImage.setImageBitmap(null);
            e.printStackTrace();
        }
    }

    private String[] splitWordOnChar(String[] wordArray, String splitChar) {
        ArrayList<String> wordList = new ArrayList<>();

        for (String word : wordArray) {
            String[] wordSplit = word.split(splitChar);

            if (wordSplit.length > 1) {
                for (int i1 = 0; i1 < wordSplit.length - 1; i1++) wordList.add(wordSplit[i1] + splitChar);
                wordList.add(wordSplit[wordSplit.length - 1]);
            } else {
                wordList.add(wordSplit[0]);
            }
        }

        return wordList.toArray(new String[wordList.size()]);
    }

    private String[] splitRawSentence(String rawSentence) {
        String[] sentenceWords = rawSentence.trim().split("\\s+");

        sentenceWords = stripLeadingTrailing(sentenceWords, "'");
        sentenceWords = splitWordOnChar(sentenceWords, "-");
        sentenceWords = splitWordOnChar(sentenceWords, "'");

        return sentenceWords;
    }

    /**
     * This cleans a raw sentence from the ASB.  This is very idiosyncratic to the ASB content.
     * ASB contains some apostrophes used as single quotes that otherwise confuse the layout
     *
     * We also need to have true apostrophes and hyphenated words split to maintain alignment with
     * the listener.  i.e the displayed words and spoken word arrays should be kept in alignment.
     *
     * @param rawSentence
     * @return
     */
    private String processRawSentence(String rawSentence) {
        String[] sentenceWords = splitRawSentence(rawSentence);
        StringBuilder sentence = new StringBuilder();

        for (int i1 = 0; i1 < sentenceWords.length; i1++) {
            if (sentenceWords[i1].endsWith("'") || sentenceWords[i1].endsWith("-")) sentence.append(sentenceWords[i1]);
            else sentence.append(sentenceWords[i1] + ((i1 < sentenceWords.length - 1)? TCONST.WORD_SPACE: TCONST.NO_SPACE));
        }

        return sentence.toString();
    }

    private String stripLeadingTrailing(String sentence, String stripChar) {
        if (sentence.startsWith(stripChar)) sentence = sentence.substring(1);
        if (sentence.endsWith(stripChar)) sentence = sentence.substring(0, sentence.length() - 1);

        return sentence;
    }

    private String[] stripLeadingTrailing(String[] wordArray, String stripChar) {
        ArrayList<String> wordList = new ArrayList<>();

        for (String word : wordArray) {
            if (word.startsWith(stripChar)) word = word.substring(1);
            if (word.endsWith(stripChar)) word = word.substring(0, word.length() - 1);

            wordList.add(word);
        }

        return wordList.toArray(new String[wordList.size()]);
    }

    /**
     * Reconfigure for a specific page / paragraph / line (seeks to)
     *
     * @param currPage
     * @param currPara
     * @param currLine
     * @param currWord
     */
    private void seekToStoryPosition(int currPage, int currPara, int currLine, int currWord) {
        Log.d(TAG, "seekToStoryPosition: currPage: " + currPage + " - currPara: " + currPara + " - currLine: " + currLine + " - currWord: " + currWord);

        showWords = !(mParent.testFeature(TCONST.FTR_USER_HIDE)) && !(mParent.testFeature(TCONST.FTR_USER_PROMPT));
        showFutureWords = hearRead.equals(TCONST.FTR_USER_HEAR) || !(mParent.testFeature(TCONST.FTR_USER_REVEAL));

        Log.d(TAG, "seekToStoryPosition: showWords = " + showWords + ", showFutureWords = " + showFutureWords);

        completedSentencesFmtd = "";
        completedSentences = "";
        futureSentencesFmtd = "";
        futureSentences = "";
        wordsSpoken = new ArrayList<>();
        futureSpoken = new ArrayList<>();

        // Optimization - Skip If seeking to the very first line
        //
        // Otherwise create 2 things:
        //
        // 1. A visually formatted representation of the words already spoken
        // 2. A list of words already spoken - for use in the Sphinx language model
        //
        if (!mCurrEffectiveVariant.equals("story.prompt") && (currPara > 0 || currLine > 0)) {
            // First generate all completed paragraphs in their entirety
            //
            for (int paraIndex = 0; paraIndex < currPara; paraIndex++) {
                int paraLength = data[currPage].text[paraIndex].length;
                for (int lineIndex = 0; lineIndex < paraLength; lineIndex++) {
                    rawSentence = data[currPage].text[paraIndex][lineIndex].sentence;
                    // Add the previous line to the list of spoken words used to build the
                    // language model - so it allows all on screen words to be spoken
                    //
                    for (String word : splitIntoWords(rawSentence)) wordsSpoken.add(word);
                    String variant = computeEffectiveVariant(currPage, paraIndex, lineIndex);
                    if (!variant.equals("story.prompt") && !variant.equals("story.hide")) completedSentences += processRawSentence(rawSentence) + TCONST.SENTENCE_SPACE;
                }
                if (paraIndex < currPara) completedSentences += "<br><br>";
            }

            // Then generate all completed sentences from the current paragraph
            //
            for (int lineIndex = 0; lineIndex < currLine; lineIndex++) {
                rawSentence = data[currPage].text[currPara][lineIndex].sentence;
                // Add the previous line to the list of spoken words used to build the
                // language model - so it allows all on screen words to be spoken
                //
                for (String word : splitIntoWords(rawSentence)) wordsSpoken.add(word);
                String variant = computeEffectiveVariant(currPage, currPara, lineIndex);
                if (!variant.equals("story.prompt") && !variant.equals("story.hide")) completedSentences += processRawSentence(rawSentence) + TCONST.SENTENCE_SPACE;
            }

            // Note that we add a space after the sentence.
            //
            completedSentencesFmtd = "<font color='#AAAAAA'>";
            completedSentencesFmtd += completedSentences;
            completedSentencesFmtd += "</font>";
        }

        // Generate the active line of text - target sentence
        // Reset the highlight
        mCurrHighlight = TCONST.EMPTY;

        mCurrPage = currPage;
        mCurrPara = currPara;
        mCurrLine = currLine;

        mPageCount = data.length;
        mParaCount = data[currPage].text.length;
        mLineCount = data[currPage].text[currPara].length;

        rawNarration = data[currPage].text[currPara][currLine].narration;
        rawSentence  = data[currPage].text[currPara][currLine].sentence;

        // Words that are used to build the display text - include punctuation etc.
        //
        // NOTE: wordsToSpeak is used in generating the active ASR listening model
        // so it must reflect the current sentence without punctuation!
        //
        // To keep indices into wordsToSpeak in sync with wordsToDisplay we break the words to
        // display if they contain apostrophes or hyphens into sub "words" - e.g. "thing's" -> "thing" "'s"
        // these are reconstructed by the highlight logic without adding spaces which it otherwise inserts
        // automatically.
        //
        wordsToDisplay = splitRawSentence(rawSentence);

        // TODO: strip word-final or -initial apostrophes as in James' or 'cause.
        // Currently assuming hyphenated expressions split into two Asr words.
        //
        wordsToSpeak = splitIntoWords(rawSentence);
        mCurrWord = currWord;
        mWordCount = wordsToSpeak.length;

        // If we are showing future content - i.e. we want the entire page to be visible but
        // only the "current" line highlighted.
        // Note we need ...Count vars initialized here
        //
        // Create 2 things:
        //
        // 1. A visually formatted representation of the words not yet spoken
        // 2. A list of future words to be spoken - for use in the Sphinx language model
        //
        // Generate all remaining sentences from the current paragraph
        //
        if (!mCurrEffectiveVariant.equals("story.prompt")) {
            for (int lineIndex = currLine + 1; lineIndex < mLineCount; lineIndex++) {
                rawSentence = data[currPage].text[currPara][lineIndex].sentence;
                // Add the previous line to the list of spoken words used to build the
                // language model - so it allows all on screen words to be spoken
                //
                for (String word : splitIntoWords(rawSentence)) futureSpoken.add(word);
                String variant = computeEffectiveVariant(currPage, currPara, lineIndex);
                if (!variant.equals("story.prompt") && !variant.equals("story.hide") && !variant.equals("story.reveal")) futureSentences += processRawSentence(rawSentence) + TCONST.SENTENCE_SPACE;
            }

            // Then generate all completed paragraphs in their entirety
            //
            for (int paraIndex = currPara + 1; paraIndex < mParaCount; paraIndex++) {
                // Add the paragraph break if not at the end
                //
                futureSentences += "<br><br>";
                int paraLength = data[currPage].text[paraIndex].length;
                for (int lineIndex = 0; lineIndex < paraLength; lineIndex++) {
                    rawSentence = data[currPage].text[paraIndex][lineIndex].sentence;
                    // Add the previous line to the list of spoken words used to build the
                    // language model - so it allows all on screen words to be spoken
                    //
                    for (String word : splitIntoWords(rawSentence)) futureSpoken.add(word);
                    String variant = computeEffectiveVariant(currPage, paraIndex, lineIndex);
                    if (!variant.equals("story.prompt") && !variant.equals("story.hide") && !variant.equals("story.reveal")) futureSentences += processRawSentence(rawSentence) + TCONST.SENTENCE_SPACE;
                }
            }

            // TODO: parameterize the color

            futureSentencesFmtd = "<font color='#AAAAAA'>";
            futureSentencesFmtd += futureSentences;
            futureSentencesFmtd += "</font>";
        }

        publishStateValues();
        UpdateDisplay();
    }

    private String[] splitIntoWords(String text) {
       return text.replace('-', ' ').replaceAll("['.!?,:;\"\\(\\)]", " ").toUpperCase(Locale.US).trim().split("\\s+");
    }

    private void initSegmentation(int _uttNdx) {
        Log.d(TAG, "initSegmentation: _uttNdx = " + _uttNdx);

        utteranceNdx  = _uttNdx;
        numUtterance  = rawNarration.length;
        currUtterance = rawNarration[utteranceNdx];
        segmentArray  = rawNarration[utteranceNdx].segmentation;
        if (segmentArray == null || segmentArray.length == 0) {
            String[] words = splitIntoWords(currUtterance.utterances);
            segmentArray = new CASB_Seg[words.length];
            for (int i = 0; i < words.length; i++) {
                segmentArray[i] = new CASB_Seg();
                segmentArray[i].start = 0;
                segmentArray[i].end = 0;
                segmentArray[i].word = words[i];
            }
        }

        segmentNdx    = 0;
        numSegments   = segmentArray.length;
        utterancePrev = utteranceNdx == 0 ? 0 : rawNarration[utteranceNdx - 1].until;
        segmentPrev   = utterancePrev;

        // Publish the current utterance within sentence
        //
        String filename = currUtterance.audio.toLowerCase();
        if (filename.endsWith(".wav") || filename.endsWith(".mp3")) filename = filename.substring(0, filename.length() - 4);
        mParent.publishValue(TCONST.RTC_VAR_UTTERANCE, filename);

        // NOTE: Due to inconsistencies in the segmentation data, you cannot depend on it
        // having precise timing information.  As a result the segment may timeout before the
        // audio has completed. To avoid this we use oncomplete in type_audio to push an
        // TRACK_SEGMENT back to this components queue.
    }

    private void trackNarration(boolean start) {
        Log.d(TAG, "trackNarration: start = " + start);

        if (start) {
            mHeardWord    = 0;
            splitIndex    = TCONST.INITSPLIT;
            endOfSentence = false;

            initSegmentation(0);

            spokenWords = new ArrayList<String>();

            // Tell the script to speak the new utterance
            //
            trackSegment();
        } else {
            // NOTE: The narration mode uses the ASR logic to simplify operation.  In doing this
            /// it uses the wordsToSpeak array to progressively highlight the on screen text based
            /// on the timing found in the segmentation data.
            //
            // Special processing to account for apostrophes and hyphenated words
            // Note the system listens for e.g. "WON'T" as [WON] [T] two words so if we provide "won't" then it "won't" match:)
            // and the narration will freeze
            // This is a kludge to account for the fact that segmentation data does not split words with
            // hyphens or apostrophes into separate "words" the way the wordstospeak does.
            // Without this the narration will get out of sync
            //
            if (splitIndex == TCONST.INITSPLIT) {
                splitSegment = narrationSegment.word.toUpperCase().split("[\\-']");
                splitIndex = 0;
            }
            if (splitIndex < splitSegment.length) spokenWords.add(splitSegment[splitIndex++]);

            // Update the display
            //
            onUpdate(spokenWords.toArray(new String[spokenWords.size()]));

            // If the segment word is complete continue to the next segment - note that this is
            // generally the case.  Words are not usually split by punctuation
            //
            if (splitIndex >= splitSegment.length) {
                splitIndex = TCONST.INITSPLIT;

                // sentences are built from an array of utterances which are build from an array
                // of segments (i.e. timed words)
                //
                // Note the last segment is not timed.  It is driven by the TRACK_COMPLETE event
                // from the audio mp3 playing.  This is required as the segmentation data is not
                // sufficiently accurate to ensure we don't interrupt a playing utterance.
                //
                if (++segmentNdx >= numSegments) {
                    // If we haven't consumed all the utterances (i.e "narrations") in the
                    // sentence prep the next
                    //
                    // NOTE: Prep the state and wait for the TRACK_COMPLETE event to invoke
                    // trackSegment to continue or terminate
                    //
                    if (++utteranceNdx < numUtterance) initSegmentation(utteranceNdx);
                    else endOfSentence = true;
                } else {
                    // All the segments except the last one are timed based on the segmentation data.
                    // i.e. the audio plays and this highlights words based on prerecorded durations.
                    //
                    postDelayedTracker();
                }
            } else {
                // If the segment word is split due to apostrophes or hyphens then consume them
                // before continuing to the next segment.
                //
                mParent.post(TCONST.TRACK_NARRATION, 0);
            }
        }
    }

    private void postDelayedTracker() {
        Log.d(TAG, "postDelayedTracker");

        narrationSegment = segmentArray[segmentNdx];
        segmentCurr = utterancePrev + narrationSegment.end;
        mParent.post(TCONST.TRACK_NARRATION, new Long((segmentCurr - segmentPrev) * 10));
        segmentPrev = segmentCurr;
    }

    private void trackSegment() {
        Log.d(TAG, "trackSegment");

        if (!endOfSentence) {
            // Tell the script to speak the new utterance
            //
            mParent.applyBehavior(TCONST.SPEAK_UTTERANCE);
            postDelayedTracker();
        } else {
            mParent.applyBehavior(TCONST.UTTERANCE_COMPLETE_EVENT);
        }
    }

    public void execCommand(String command, Object target) {
        switch (command) {
            case TCONST.START_NARRATION:
                trackNarration(true);
                break;

            case TCONST.NEXT_WORD:
                generateVirtualASRWord();
                break;

            case TCONST.NEXT_PAGE:
                nextPage();
                break;

            case TCONST.NEXT_SCENE:
                mParent.nextScene();
                break;

            case TCONST.TRACK_NARRATION:
                trackNarration(false);
                break;

            case TCONST.TRACK_SEGMENT:
                trackSegment();
                break;

            case TCONST.NEXT_NODE:
                mParent.nextNode();
                break;

            case TCONST.SPEAK_EVENT:
            case TCONST.UTTERANCE_COMPLETE_EVENT:
                mParent.applyBehavior(command);
                break;
        }
    }

    /**
     * Push the state out to the tutor domain.
     *
     */
    private void publishStateValues() {
        Log.d(TAG, "publishStateValues: mCurrWord = " + mCurrWord + ", mWordCount = " + mWordCount);

        String cumulativeState = TCONST.RTC_CLEAR;

        // ensure echo state has a valid value.
        //
        mParent.publishValue(TCONST.RTC_VAR_ECHOSTATE, TCONST.FALSE);
        mParent.publishValue(TCONST.RTC_VAR_PARROTSTATE, TCONST.FALSE);

        mParent.publishValue(TCONST.RTC_VAR_PROMPT, mCurrPrompt);

        mParent.setFeature(TCONST.FTR_PROMPT, mParent.testFeature(TCONST.FTR_USER_PROMPT) ? TCONST.ADD_FEATURE : TCONST.DEL_FEATURE);
        mParent.setFeature(TCONST.FTR_PLAY_PROMPT, mParent.testFeature(TCONST.FTR_USER_PROMPT) || mPrevEffectiveVariant.equals("story.prompt") || mCurrPrompt.equals(mPrevPrompt) ? TCONST.DEL_FEATURE : TCONST.ADD_FEATURE);

        // Set the scriptable flag indicating the current state.
        //
        if (mCurrWord >= mWordCount) {
            // In echo mode - After line has been echoed we switch to Read mode and
            // read the next sentence.
            //
            if (mParent.testFeature(TCONST.FTR_USER_ECHO) || mParent.testFeature(TCONST.FTR_USER_REVEAL) || mParent.testFeature(TCONST.FTR_USER_PARROT)) {
                if (hearRead.equals(TCONST.FTR_USER_READ)) {
                    // Read Mode - When user finishes reading switch to Narrate mode and
                    // narrate the same sentence - i.e. echo
                    //
                    if (!mParent.testFeature(TCONST.FTR_USER_PARROT)) mParent.publishValue(TCONST.RTC_VAR_ECHOSTATE, TCONST.TRUE);
                    mParent.retractFeature(TCONST.FTR_USER_READING);

                    Log.d("ISREADING", "NO");

                    cumulativeState = TCONST.RTC_LINECOMPLETE;
                    mParent.publishValue(TCONST.RTC_VAR_WORDSTATE, TCONST.LAST);

                    mListener.setPauseListener(true);
                } else {
                    // Narrate mode - switch back to READ and set line complete flags
                    //
                    if (mParent.testFeature(TCONST.FTR_USER_PARROT)) mParent.publishValue(TCONST.RTC_VAR_PARROTSTATE, TCONST.TRUE);
                    mParent.publishFeature(TCONST.FTR_USER_READING);

                    Log.d("ISREADING", "YES");

                    cumulativeState = TCONST.RTC_LINECOMPLETE;
                    mParent.publishValue(TCONST.RTC_VAR_WORDSTATE, TCONST.LAST);
                }
            } else {
                cumulativeState = TCONST.RTC_LINECOMPLETE;
                mParent.publishValue(TCONST.RTC_VAR_WORDSTATE, TCONST.LAST);
            }
        } else
            mParent.publishValue(TCONST.RTC_VAR_WORDSTATE, TCONST.NOT_LAST);

        if (mCurrLine >= mLineCount - 1) {
            cumulativeState = TCONST.RTC_PARAGRAPHCOMPLETE;
            mParent.publishValue(TCONST.RTC_VAR_LINESTATE, TCONST.LAST);
        } else
            mParent.publishValue(TCONST.RTC_VAR_LINESTATE, TCONST.NOT_LAST);

        if (mCurrPara >= mParaCount - 1) {
            cumulativeState = TCONST.RTC_PAGECOMPLETE;
            mParent.publishValue(TCONST.RTC_VAR_PARASTATE, TCONST.LAST);
        } else
            mParent.publishValue(TCONST.RTC_VAR_PARASTATE, TCONST.NOT_LAST);

        if (mCurrPage >= mPageCount - 1) {
            cumulativeState = TCONST.RTC_STORYCMPLETE;
            mParent.publishValue(TCONST.RTC_VAR_PAGESTATE, TCONST.LAST);
        } else
            mParent.publishValue(TCONST.RTC_VAR_PAGESTATE, TCONST.NOT_LAST);

        // Publish the cumulative state out to the scripting scope in the tutor
        //
        mParent.publishValue(TCONST.RTC_VAR_STATE, cumulativeState);
    }

    /**
     *  Configure for specific Page
     *  Assumes current storyName
     *
     * @param pageIndex
     */
    @Override
    public void seekToPage(int pageIndex) {
        mCurrPage = pageIndex;

        if (mCurrPage > mPageCount - 1) mCurrPage = mPageCount - 1;
        if (mCurrPage < TCONST.ZERO) mCurrPage = TCONST.ZERO;

        incPage(TCONST.ZERO);
    }

    @Override
    public void nextPage() {
        if (mCurrPage < mPageCount - 1) incPage(TCONST.INCR);

        // Actually do the page animation
        //
        mParent.animatePageFlip(true, mCurrViewIndex);
    }

    @Override
    public void prevPage() {
        if (mCurrPage > 0) incPage(TCONST.DECR);

        //TODO: CHECK
        mParent.animatePageFlip(false, mCurrViewIndex);
    }

    private void incPage(int direction) {
        mCurrPage += direction;

        // This configures the target display components to be populated with data.
        // mPageImage - mPageText
        //
        flipPage();
        configurePageImage();

        // Update the state vars
        // Note that this must be done after flip and configure so the target text and image views are defined
        // NOTE: we reset mCurrPara, mCurrLine and mCurrWord
        //
        setTutorFeatures(mCurrPage, TCONST.ZERO, TCONST.ZERO);
        seekToStoryPosition(mCurrPage, TCONST.ZERO, TCONST.ZERO, TCONST.ZERO);
    }

    /**
     *  Configure for specific Paragraph
     *  Assumes current page
     *
     * @param paraIndex
     */
    @Override
    public void seekToParagraph(int paraIndex) {
        mCurrPara = paraIndex;

        if (mCurrPara > mParaCount - 1) mCurrPara = mParaCount - 1;
        if (mCurrPara < TCONST.ZERO) mCurrPara = TCONST.ZERO;

        incPara(TCONST.ZERO);
    }

    @Override
    public void nextPara() {
        if (mCurrPara < mParaCount - 1) incPara(TCONST.INCR);
    }

    @Override
    public void prevPara() {
        if (mCurrPara > 0) incPara(TCONST.DECR);
    }

    // NOTE: we reset mCurrLine and mCurrWord
    private void incPara(int incr) {
        mCurrPara += incr;

        // Update the state vars
        //
        setTutorFeatures(mCurrPage, mCurrPara, TCONST.ZERO);
        seekToStoryPosition(mCurrPage, mCurrPara, TCONST.ZERO, TCONST.ZERO);
    }

    /**
     *  Configure for specific line
     *  Assumes current page and paragraph
     *
     * @param lineIndex
     */
    @Override
    public void seekToLine(int lineIndex) {
        mCurrLine = lineIndex;

        if (mCurrLine > mLineCount - 1) mCurrLine = mLineCount - 1;
        if (mCurrLine < TCONST.ZERO) mCurrLine = TCONST.ZERO;

        incLine(TCONST.ZERO);
    }

    @Override
    public void nextLine() {
        if (mCurrLine < mLineCount - 1) incLine(TCONST.INCR);
    }

    @Override
    public void prevLine() {
        if (mCurrLine > 0) incLine(TCONST.DECR);
    }

    /**
     *  NOTE: we reset mCurrWord - last parm in seekToStoryPosition
     *
     */
    private void incLine(int incr) {
        // reset boot flag to
        //
        if (storyBooting) {
            storyBooting = false;
        } else {
            mCurrLine += incr;

            // Update the state vars
            //
            setTutorFeatures(mCurrPage, mCurrPara, mCurrLine);
            seekToStoryPosition(mCurrPage, mCurrPara, mCurrLine, TCONST.ZERO);
        }
    }

    /**
     *  NOTE: we reset mCurrWord - last parm in seekToStoryPosition
     *
     */
    @Override
    public void echoLine() {
        mParent.publishValue(TCONST.RTC_VAR_ECHOSTATE, TCONST.FALSE);
        hearRead = TCONST.FTR_USER_HEAR;

        // Update the state vars
        //
        seekToStoryPosition(mCurrPage, mCurrPara, mCurrLine, TCONST.ZERO);
    }

    /**
     *
     */
    @Override
    public void parrotLine() {
        mParent.publishValue(TCONST.RTC_VAR_PARROTSTATE, TCONST.FALSE);
        hearRead = TCONST.FTR_USER_READ;

        // Update the state vars
        //
        seekToStoryPosition(mCurrPage, mCurrPara, mCurrLine, TCONST.ZERO);
    }

    /**
     *  Configure for specific word
     *  Assumes current page, paragraph and line
     *
     * @param wordIndex
     */
    @Override
    public void seekToWord(int wordIndex) {
        mCurrWord = wordIndex;
        mHeardWord = 0;

        if (mCurrWord > mWordCount - 1) mCurrWord = mWordCount - 1;
        if (mCurrWord < TCONST.ZERO) mCurrWord = TCONST.ZERO;

        seekToStoryPosition(mCurrPage, mCurrPara, mCurrLine, wordIndex);
        incWord(TCONST.ZERO);
    }

    @Override
    public void nextWord() {
        if (mCurrWord < mWordCount) incWord(TCONST.INCR);
    }

    @Override
    public void prevWord() {
        if (mCurrWord > 0) incWord(TCONST.DECR);
    }

    /**
     * We assume this has been bounds checked prior to call
     *
     * There is one optimization here - when simply moving through the sentence we do not rebuild
     * the entire state.  We just publish any change of state
     *
     * @param incr
     */
    private void incWord(int incr) {
        mCurrWord += incr;

        // For instances where we are advancing the word manually through a script it is required
        // that you reset the highlight and the FTR_WRONG so the next word is highlighted correctly
        //
        setHighLight(TCONST.EMPTY, false);
        mParent.UpdateValue(true);

        publishStateValues();
        UpdateDisplay();
    }

    /**
     * Reconfigure variant for a specific page / paragraph / line (seeks to)
     *
     * @param currPage
     * @param currPara
     * @param currLine
     */
    private void setTutorFeatures(int currPage, int currPara, int currLine) {
        Log.d(TAG, "setTutorFeatures: currPage = " + currPage + ", currPara = " + currPara + ", currLine = " + currLine);

        mPrevEffectiveVariant = mCurrEffectiveVariant;
        mCurrEffectiveVariant = computeEffectiveVariant(currPage, currPara, currLine);
        mParent.setTutorFeatures(mCurrEffectiveVariant);

        if (storyBooting) mParent.setFeature(TCONST.FTR_STORY_STARTING, TCONST.ADD_FEATURE);

        pagePrompt = data[currPage].prompt;
        mPrevPrompt = mCurrPrompt;

        if (prompt != null) mCurrPrompt = prompt;
        else if (mParent.testFeature(TCONST.FTR_USER_READ) || mParent.testFeature(TCONST.FTR_USER_ECHO) || mParent.testFeature(TCONST.FTR_USER_REVEAL)) mCurrPrompt = "Read to RoboTutor.mp3";
        else if (mParent.testFeature(TCONST.FTR_USER_HEAR) || mParent.testFeature(TCONST.FTR_USER_HIDE)) mCurrPrompt = "Listen.mp3";
        else if (mParent.testFeature(TCONST.FTR_USER_PARROT)) mCurrPrompt = "Repeat after RoboTutor.mp3";

        // Narration Mode (i.e. USER_HEAR) always narrates the story otherwise we
        // start with USER_READ where the student reads aloud and if USER_ECHO
        // is in effect we then toggle between READ and HEAR for each sentence.
        //
        if (mParent.testFeature(TCONST.FTR_USER_PROMPT) || mParent.testFeature(TCONST.FTR_USER_HEAR) || mParent.testFeature(TCONST.FTR_USER_HIDE) || mParent.testFeature(TCONST.FTR_USER_PARROT)) {
            hearRead = TCONST.FTR_USER_HEAR;
            mParent.setFeature(TCONST.FTR_USER_READING, TCONST.DEL_FEATURE);
        } else {
            hearRead = TCONST.FTR_USER_READ;
            mParent.setFeature(TCONST.FTR_USER_READING, TCONST.ADD_FEATURE);
        }

        Log.d(TAG, "setTutorFeatures: hearRead = " + hearRead);
    }

    private String computeEffectiveVariant(int currPage, int currPara, int currLine) {
        String pageVariant = data[currPage].variant;
        String sentenceVariant = data[currPage].text[currPara][currLine].variant;
        String currEffectiveVariant = (sentenceVariant != null) ? ("story." + sentenceVariant) : (pageVariant != null) ? ("story." + pageVariant) : mParent.getTutorVariant();

        Log.d(TAG, "computeEffectiveVariant: sentenceVariant = " + sentenceVariant + ", pageVariant = " + pageVariant + ", tutorVariant = " + mParent.getTutorVariant());
        Log.d(TAG, "setTutorFcomputeEffectiveVarianteatures: currEffectiveVariant = " + currEffectiveVariant);

        return currEffectiveVariant;
    }

    /**
     * This picks up listening from the last word - so it seeks to wherever we are in the
     * current sentence and listens from there.
     */
    public void continueListening() {
        if (hearRead.equals(TCONST.FTR_USER_HEAR)) mParent.applyBehavior(TCONST.NARRATE_STORY);
        if (hearRead.equals(TCONST.FTR_USER_READ)) startListening();
    }

    private void startListening() {
        // We allow the user to say any of the onscreen words but set the priority order of how we
        // would like them matched  Note that if the listener is not explicitly listening for a word
        // it will just ignore it if spoken.
        //
        // for the current target word.
        // 1. Start with the target word on the target sentence
        // 2. Add the words from there to the end of the sentence - just to permit them
        // 3. Add the words already spoken from the other lines - just to permit them
        //
        // "Permit them": So the language model is listening for them as possibilities.
        //
        wordsToListenFor = new ArrayList<>();

        for (int i1 = mCurrWord; i1 < wordsToSpeak.length; i1++) wordsToListenFor.add(wordsToSpeak[i1]);
        for (int i1 = 0; i1 < mCurrWord; i1++) wordsToListenFor.add(wordsToSpeak[i1]);
        for (String word : wordsSpoken) wordsToListenFor.add(word);

        // If we want to listen for all the words that are visible
        //
        if (listenFutureContent) for (String word : futureSpoken) wordsToListenFor.add(word);

        // Start listening
        //
        if (mListener != null) {
            // reset the relative position of mCurrWord in the incoming PLRT heardWords array
            mHeardWord = 0;
            mListener.reInitializeListener(true);
            mListener.updateNextWordIndex(mHeardWord);

            mListener.listenFor(wordsToListenFor.toArray(new String[wordsToListenFor.size()]), 0);
            mListener.setPauseListener(false);
        }
    }

    /**
     * Scipting mechanism to update target word highlight
     * @param highlight
     */
    @Override
    public void setHighLight(String highlight, boolean update) {
        mCurrHighlight = highlight;
        if (update) UpdateDisplay();
    }

    /**
     *  Update the displayed sentence
     */
    private void UpdateDisplay() {
        Log.d(TAG, "UpdateDisplay: hearRead = " + hearRead);

        mPageText.setText(Html.fromHtml(""));

        String fmtSentence = "";
        if (showWords) {
            for (int index = 0; index < wordsToDisplay.length; index++) {
                String styledWord = wordsToDisplay[index];                           // default plain

                if (index < mCurrWord) styledWord = "<font color='#00B600'>" + styledWord + "</font>";

                if (showFutureWords && index == mCurrWord) {
                    if (!mCurrHighlight.equals(TCONST.EMPTY)) styledWord = "<font color='" + mCurrHighlight + "'>" + styledWord + "</font>";
                    styledWord = "<u>" + styledWord + "</u>";
                }

                if (index > mCurrWord && hearRead.equals(TCONST.FTR_USER_HEAR)) styledWord = "<font color='#0000B6'>" + styledWord + "</font>";

                if (showFutureWords || index < mCurrWord) {
                    fmtSentence += styledWord + ((wordsToDisplay[index].endsWith("'") || wordsToDisplay[index].endsWith("-") || (index >= wordsToDisplay.length - 1)) ?  TCONST.NO_SPACE : TCONST.WORD_SPACE);
                }
            }
        }

        // Generate the text to be displayed
        //
        String content = completedSentencesFmtd + fmtSentence + TCONST.SENTENCE_SPACE + futureSentencesFmtd;
        mPageText.setText(Html.fromHtml(content));

        Log.d(TAG, "Story Sentence Text: " + content);

        if (showWords && (showFutureWords || mCurrWord > 0)) broadcastActiveTextPos(mPageText, wordsToDisplay);

        // Publish the current word / sentence / remaining words for use in scripts
        //
        if (mCurrWord < wordsToSpeak.length) {
            mParent.publishValue(TCONST.RTC_VAR_WORDVALUE, wordsToSpeak[mCurrWord]);
            mParent.publishValue(TCONST.RTC_VAR_REMAINING, TextUtils.join(" ", Arrays.copyOfRange(wordsToSpeak, mCurrWord, wordsToSpeak.length)));
            mParent.publishValue(TCONST.RTC_VAR_SENTENCE,  TextUtils.join(" ", wordsToSpeak));
        }
    }

    /**
     *
     * @param text
     * @param words
     * @return
     */
    private PointF broadcastActiveTextPos(TextView text, String[] words){
        PointF point = new PointF(0,0);
        int charPos = 0;
        int maxPos;

        try {
            Layout layout = text.getLayout();

            if (layout != null && mCurrWord < words.length) {
                // Point to the start of the Target sentence (mCurrLine)
                charPos = completedSentences.length();

                // Find the starting character of the current target word
                for (int i1 = 0; i1 <= mCurrWord; i1++) charPos += words[i1].length() + 1;

                // Look at the end of the target word
                charPos -= 1;

                // Note that sending a value greater than maxPos will corrupt the textView - so
                // guarantee this will never happen.
                //
                maxPos = text.getText().length();
                charPos = (charPos > maxPos) ? maxPos : charPos;

                point.x = layout.getPrimaryHorizontal(charPos);

                int y = layout.getLineForOffset(charPos);
                point.y = layout.getLineTop(y);

                CPersonaObservable.broadcastLocation(text, TCONST.LOOKAT, point);
            }
        } catch (Exception e) {
            Log.d(TAG, "broadcastActiveTextPos: " + e.toString());
        }

        return point;
    }

    /**
     * This is where we process words being narrated
     *
     */
    @Override
    public void onUpdate(String[] heardWords) {
        boolean result = true;
        String logString = "";

        for (int i = 0; i < heardWords.length; i++) logString += heardWords[i].toLowerCase() + " | ";

        Log.i("ASR", "Update Words Spoken: " + logString);

        while (mHeardWord < heardWords.length) {
            if (wordsToSpeak[mCurrWord].equals(heardWords[mHeardWord])) {
                nextWord();
                mHeardWord++;

                Log.i("ASR", "RIGHT");
                attemptNum = 0;
                result = true;
            } else {
                Log.e(TAG, "Input Error in narrator no match found - mCurrWord ->" + wordsToSpeak[mCurrWord] + " -> heardWords: " + heardWords[mHeardWord]);

                nextWord();
                mHeardWord++;

                attemptNum = 0;
                result = true;
            }
            mParent.updateContext(rawSentence, mCurrLine, wordsToSpeak, mCurrWord - 1, heardWords[mHeardWord - 1], attemptNum, false, result);
        }

        // Publish the outcome
        mParent.publishValue(TCONST.RTC_VAR_ATTEMPT, attemptNum);
        mParent.UpdateValue(result);
    }

    /**
     * This is where the incoming PLRT ASR data is processed.
     *
     * Provided the input matches the model sentence it continues in sequence through the sentence
     * words. If there is an error it seeks the listener to only "hear" words form the error to the
     * end of sentence and continues like this iteratively as required.  The goal is to eliminate the
     * word shuffling that Multi-Match does and simplify the process. Ultimately this should migrate
     * to using 2 simultaneous decoders one for the correct sentence and one for any other "distractor"
     * words. i.e. other words in the sentence in this case.
     *
     *  TODO: check if it is possible for the hypothesis to change between last update and final hyp
     */
    @Override
    public void onUpdate(ListenerBase.HeardWord[] heardWords, boolean finalResult) {
        boolean result = true;
        String logString = "";

        try {
            for (int i = 0; i < heardWords.length; i++) {
                if (heardWords[i] != null) logString += heardWords[i].hypWord.toLowerCase() + ":" + heardWords[i].iSentenceWord + " | ";
                else logString += "VIRTUAL | ";
            }

            while ((mCurrWord < wordsToSpeak.length) && (mHeardWord < heardWords.length)) {
                if (wordsToSpeak[mCurrWord].equals(heardWords[mHeardWord].hypWord)) {
                    nextWord();
                    mHeardWord++;
                    mListener.updateNextWordIndex(mHeardWord);

                    Log.i("ASR", "RIGHT");
                    attemptNum = 0;
                    result = true;
                    mParent.updateContext(rawSentence, mCurrLine, wordsToSpeak, mCurrWord - 1, heardWords[mHeardWord - 1].hypWord, attemptNum, heardWords[mHeardWord - 1].utteranceId == "", result);
                } else {
                    mListener.setPauseListener(true);

                    Log.i("ASR", "WRONG");
                    attemptNum++;
                    result = false;
                    mParent.updateContext(rawSentence, mCurrLine, wordsToSpeak, mCurrWord, heardWords[mHeardWord].hypWord, attemptNum, heardWords[mHeardWord].utteranceId == "", result);
                    break;
                }
            }

            Log.i("ASR", "Update New HypSet: " + logString + " - Attempt: " + attemptNum);

            // Publish the outcome
            mParent.publishValue(TCONST.RTC_VAR_ATTEMPT, attemptNum);
            mParent.UpdateValue(result);

            mParent.onASREvent(TCONST.RECOGNITION_EVENT);
        } catch (Exception e) {
            Log.e("ASR", "onUpdate Fault: " + e);
        }
    }

    public void generateVirtualASRWord() {
        mListener.setPauseListener(true);

        ListenerBase.HeardWord words[] = new ListenerBase.HeardWord[mHeardWord+1];
        words[mHeardWord] = new ListenerBase.HeardWord(wordsToSpeak[mCurrWord]);

        onUpdate(words, false);
        mListener.setPauseListener(false);
//        startListening();
    }

    /**
     * This is where incoming JSGF ASR data would be processed.
     *
     *  TODO: check if it is possible for the hypothesis to change between last update and final hyp
     */
    @Override
    public void onUpdate(String[] heardWords, boolean finalResult) {
//        String logString = "";
//
//        for (String hypWord : heardWords) logString += hypWord.toLowerCase() + ":";
//        Log.i("ASR", "New JSGF HypSet: " + logString);
//
//        mParent.publishValue(TCONST.RTC_VAR_ATTEMPT, attemptNum++);
//        mParent.onASREvent(TCONST.RECOGNITION_EVENT);
    }

    @Override
    public boolean endOfData() {
        return false;
    }


    //************ Serialization

    /**
     * Load the data source
     *
     * @param jsonData
     */
    @Override
    public void loadJSON(JSONObject jsonData, IScope scope) {
        JSON_Helper.parseSelf(jsonData, this, CClassMap.classMap, scope);
    }
}

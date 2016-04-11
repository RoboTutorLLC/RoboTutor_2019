//*********************************************************************************
//
//    Copyright(c) 2016 Carnegie Mellon University. All Rights Reserved.
//    Copyright(c) Kevin Willows All Rights Reserved
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

package cmu.xprize.robotutor.tutorengine.widgets.core;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import cmu.xprize.fw_component.CFingerWriter;
import cmu.xprize.fw_component.ITextSink;
import cmu.xprize.robotutor.tutorengine.CTutor;
import cmu.xprize.robotutor.tutorengine.CObjectDelegate;
import cmu.xprize.robotutor.tutorengine.ITutorLogManager;
import cmu.xprize.robotutor.tutorengine.ITutorNavigator;
import cmu.xprize.robotutor.tutorengine.ITutorObjectImpl;
import cmu.xprize.robotutor.tutorengine.ITutorSceneImpl;
import cmu.xprize.robotutor.tutorengine.graph.vars.IScriptable2;

public class TFingerWriter extends CFingerWriter implements ITutorObjectImpl {

    private CTutor          mTutor;
    private CObjectDelegate mSceneObject;

    private float aspect = 1.12f;  // w/h

    private static final String   TAG = "TFingerWriter";


    public TFingerWriter(Context context) {
        super(context);
        init(context, null);
    }

    public TFingerWriter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, null);
    }

    public TFingerWriter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, null);
    }


    @Override
    public void init(Context context, AttributeSet attrs) {
        mSceneObject = new CObjectDelegate(this);
        mSceneObject.init(context, attrs);

    }


    @Override
    public void onDestroy() {
        mSceneObject.onDestroy();
    }


    public void setDataSource(String dataSource) {

    }


    @Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
    {
        int finalWidth, finalHeight;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec );

        int originalWidth  = MeasureSpec.getSize(widthMeasureSpec);
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);

        finalWidth  = (int)(originalHeight * aspect);
        finalHeight = originalHeight;

        setMeasuredDimension(finalWidth, finalHeight);

//        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
//                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
//        super.onMeasure(
//                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
    }


    @Override
    protected void updateLinkedView(int linkedViewID) {
        // If we are linked to a textSink then send it the new character
        if(linkedViewID != -1) {
            ITextSink linkedView = (ITextSink)mTutor.getViewById(linkedViewID, null);

            if(linkedView == null) {
                Log.e(TAG, "FingerWriter Component does not have LinkView");
                System.exit(1);
            }

            try {
                // If there is a hypothesis then return the most likely
                //
                if(_recChars.size() > 0)
                    linkedView.addChar(_recChars.get(0));
            }
            catch(Exception e) {
                Log.d(TAG, "FingerWriter: probable empty result" + e);

                // Send special unrecognized sequence
                linkedView.addChar("???");
            }
        }
    }




    //************************************************************************
    //************************************************************************
    // Tutor scriptiable methods  Start


    public void setRecognizer(String recogId) {
        super.setRecognizer(recogId);
    }

    public void setRecognizer(String recogId, String subset) {
        super.setRecognizer(recogId, subset);
    }


    public void enable(Boolean enable) {
        enableFW(enable);
    }


    public void personaWatch(Boolean enable) {

        super.personaWatch(enable);
    }


    public void onStartWriting(String symbol) {
        super.onStartWriting(symbol);
    }


    public void onRecognitionComplete(String symbol) {
        super.onRecognitionComplete(symbol);
    }


    protected void applyEventNode(String nodeName) {
        IScriptable2 obj = null;

        if(nodeName != null && !nodeName.equals("")) {
            try {
                obj = mTutor.getScope().mapSymbol(nodeName);
                obj.applyNode();

            } catch (Exception e) {
                // TODO: Manage invalid Behavior
                e.printStackTrace();
            }
        }
    }

    // Tutor methods  End
    //************************************************************************
    //************************************************************************



    @Override
    public void setName(String name) {
        mSceneObject.setName(name);
    }

    @Override
    public String name() {
        return mSceneObject.name();
    }

    @Override
    public void setParent(ITutorSceneImpl mParent) {
        mSceneObject.setParent(mParent);
    }

    @Override
    public void setTutor(CTutor tutor) {
        mTutor = tutor;
        mSceneObject.setTutor(tutor);
    }

    @Override
    public void setNavigator(ITutorNavigator navigator) {
        mSceneObject.setNavigator(navigator);
    }

    @Override
    public void setLogManager(ITutorLogManager logManager) {
        mSceneObject.setLogManager(logManager);
    }

    @Override
    public CObjectDelegate getimpl() {
        return mSceneObject;
    }

    @Override
    public void zoomInOut(Float scale, Long duration) {
        mSceneObject.zoomInOut(scale, duration);
    }
}

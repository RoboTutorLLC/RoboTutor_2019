package cmu.xprize.comp_picmatch;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONObject;

import cmu.xprize.comp_logging.CErrorManager;
import cmu.xprize.util.ILoadableObject;
import cmu.xprize.util.IScope;
import cmu.xprize.util.JSON_Helper;
import cmu.xprize.util.TCONST;

/**
 * Generated automatically w/ code written by Kevin DeLand
 */

public class CPicMatch_Component extends RelativeLayout implements ILoadableObject {

    // views
    protected ConstraintLayout Scontent;

    protected TextView promptView;
    protected TextView[] optionViews;

    // DataSource Variables
    protected   int                   _dataIndex = 0;
    protected String level;
    protected String task;
    protected String layout;
    protected String prompt;
    protected String[] images;


    // json loadable
    public String bootFeatures;
    public int rows;
    public int cols;
    public CPicMatch_Data[] dataSource;


    // View Things
    protected Context mContext;

    private LocalBroadcastManager bManager;


    static final String TAG = "CPicMatch_Component";


    public CPicMatch_Component(Context context) {
        super(context);
        init(context, null);
    }


    public CPicMatch_Component(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    public CPicMatch_Component(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {

        mContext = context;

        inflate(getContext(), R.layout.picmatch_layout, this);

        Scontent = (ConstraintLayout) findViewById(R.id.Scontent);

        promptView = ((TextView) findViewById(R.id.prompt));

        optionViews = new TextView[4];
        optionViews[0] = findViewById(R.id.option_1);
        optionViews[1] = findViewById(R.id.option_2);
        optionViews[2] = findViewById(R.id.option_3);
        optionViews[3] = findViewById(R.id.option_4);

    }

    // ALAN_HILL (2) this is called by animator_graph
    public void next() {

        try {
            if (dataSource != null) {
                updateDataSet(dataSource[_dataIndex]);

                _dataIndex++;

            }
        } catch (Exception e) {
            CErrorManager.logEvent(TAG, "Data Exhuasted: call past end of data", e, false);
        }

    }

    public boolean dataExhausted() {
        return _dataIndex >= dataSource.length;
    }

    protected void updateDataSet(CPicMatch_Data data) {

        // first load dataset into fields
        loadDataSet(data);

        updateStimulus();

    }

    /**
     * Loads from current dataset into the private DataSource fields
     *
     * @param data the current element in the DataSource array.
     */
    protected void loadDataSet(CPicMatch_Data data) {
        level = data.level;
        task = data.task;
        layout = data.layout;
        prompt = data.prompt;
        images = data.images;

    }

    /**
     * Resets the view for the next task.
     */
    protected void resetView() {


    }

    /**
     * Point at a view
     */
    public void pointAtSomething() {
        View v = findViewById(R.id.prompt);

        int[] screenCoord = new int[2];

        PointF targetPoint = new PointF(screenCoord[0] + v.getWidth()/2,
                screenCoord[1] + v.getHeight()/2);
        Intent msg = new Intent(TCONST.POINTAT);
        msg.putExtra(TCONST.SCREENPOINT, new float[]{targetPoint.x, targetPoint.y});

        bManager.sendBroadcast(msg);
    }


    /**
     * Updates the stimulus.
     */
    protected void updateStimulus() {

        promptView.setText(prompt);

        for (int i = 0; i < optionViews.length; i++) {
            optionViews[i].setText(images[i]);

            optionViews[i].setOnClickListener(new StudentChoiceListener(i));
        }

    }

    class StudentChoiceListener implements View.OnClickListener {

        int _index;
        StudentChoiceListener(int index) {
            this._index = index;
        }

        @Override
        public void onClick(View view) {
            retractFeature("FTR_CORRECT");
            retractFeature("FTR_WRONG");

            if(prompt.equals(images[_index])) {
                publishFeature("FTR_CORRECT"); // ALAN_HILL (3) search animator graph for this term
            } else {
                publishFeature("FTR_WRONG"); // ALAN_HILL (3) search animator graph for this term
            }


            applyBehavior("STUDENT_CHOICE_EVENT"); // ALAN_HILL (3) search animator graph for this term
        }
    };

    // Must override in TClass
    // TClass domain where TScope lives providing access to tutor scriptables
    //
    public boolean applyBehavior(String event){ return false;}

    // Overridden in TClass
    public void publishFeature(String feature) {}

    // Overridden in TClass
    public void retractFeature(String feature) {}

    /**
     * Load the data source
     *
     * @param jsonData
     */
    @Override
    public void loadJSON(JSONObject jsonData, IScope scope) {

        JSON_Helper.parseSelf(jsonData, this, CClassMap.classMap, scope);
        _dataIndex = 0;
    }

    public ConstraintLayout getContainer() {
        return Scontent;
    }
}

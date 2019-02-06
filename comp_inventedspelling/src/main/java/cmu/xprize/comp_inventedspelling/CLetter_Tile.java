package cmu.xprize.comp_inventedspelling;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Generated automatically w/ code written by Kevin DeLand
 */

@SuppressLint("AppCompatCustomView")
public class CLetter_Tile extends TextView {

    //region Class Variables

    private boolean _isBlank;
    private String _letter;
    private String _phoneme;
    private int _index;
    private CInventedSpellingComponent _parent;
    private boolean _isLocked;
    private boolean _isCorrect; // mark true when it's been marked as correct and moved to the top row

    //endregion

    //region Constructors

    public CLetter_Tile(Context context) {

        super(context);
        init(context, null);
    }

    public CLetter_Tile(Context context, AttributeSet attrs) {

        super(context, attrs);
        init(context, attrs);
    }
    public CLetter_Tile(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CLetter_Tile(Context context, String letter, String phoneme, int letterIndex, CInventedSpellingComponent parent) {

        super(context);
        _isBlank = letter == null;
        _letter = letter;
        _phoneme = phoneme;
        _index = letterIndex;
        _parent = parent;
        _isLocked = false;
        _isCorrect = false;


        Log.d("SPELLING_DEBUG", "changing color");
        this.setText(_isBlank ? "" : " " + _letter + " ");
        this.setId(letterIndex);
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP_CONST.LETTER_TILE_TEXT_SIZE);
        this.setTextColor(Color.BLUE);
        this.setBackgroundResource(_isBlank ? R.drawable.letter_button_empty : R.drawable.letter_button);//: Color.rgb(240, 200, 65));
        this.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        this.setPadding(
            SP_CONST.LETTER_TILE_PADDING,
            SP_CONST.LETTER_TILE_PADDING,
            SP_CONST.LETTER_TILE_PADDING,
            SP_CONST.LETTER_TILE_PADDING
        );

        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        llp.setMargins(50, 0, 0, 0);
        llp.width = SP_CONST.LETTER_TILE_WIDTH;
        llp.height = SP_CONST.LETTER_TILE_HEIGHT;
        llp.gravity = Gravity.CENTER_HORIZONTAL;
        this.setLayoutParams(llp);
    }

    protected void init(Context context, AttributeSet attrs) { }

    //endregion

    protected void indicateError() {
        this.setBackgroundResource(R.drawable.letter_button_wrong);//;setBackgroundColor(Color.rgb(240, 100, 100));
    }

    protected void revertColor() {
        this.setBackgroundResource(R.drawable.letter_button);//setBackgroundColor(Color.rgb(240, 200, 65));
    }

    protected void lock() { _isLocked = true; }

    protected void unlock() {
        _isLocked = false;
    }

    protected void markCorrect() { _isCorrect = true; }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d("ddd", "Touching: " + _isLocked);
            if (!_isLocked && !_isBlank && !_isCorrect) {
                _parent.onLetterTouch(_letter, _phoneme, _index, this);
            }
        }

        return true;
    }
}

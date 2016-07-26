package cmu.xprize.asm_component;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;


/**
 * Horizontal Alley that has the text on the left and its associated dotbag on the right
 */
public class CAsm_Alley extends LinearLayout {

    private CAsm_Text SText;
    private CAsm_DotBag SdotBag;

    private int digitIndex;
    private int val;
    private int id;

    private String operation;
    private String image;

    private boolean clickable;

    float scale = getResources().getDisplayMetrics().density;
    final int textSize = (int)(ASM_CONST.textSize*scale);
    final int rightPadding = (int)(ASM_CONST.rightPadding*scale);

    static final String TAG = "CAsm_Alley";

    public CAsm_Alley(Context context) {

        super(context);
        init(context, null);
    }

    public CAsm_Alley(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }


    public CAsm_Alley(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        createText();
        createDotBag();

        //setClipChildren(false);
        //setClipToPadding(false);
    }

    public void update(int val, String image, int id, String operation, boolean clickable, int numSlots) {

        this.id = id;
        this.val = val;
        this.operation = operation;
        this.clickable = clickable;
        this.digitIndex = numSlots;
        this.image = image;

        resetText();

        SText.update(id, val, operation, numSlots);

        boolean drawBorder = (id != ASM_CONST.ANIMATOR);
        SdotBag.setDrawBorder(drawBorder);


    }

    private void createText() {

        SText = new CAsm_Text(getContext());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, rightPadding, 0);
        SText.setLayoutParams(lp);
        addView(SText, 0);
    }

    private void createDotBag() {
        // TODO: figure out why it won't show up unless updated

        SdotBag = new CAsm_DotBag(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        SdotBag.setLayoutParams(lp);
        addView(SdotBag, 1);

    }

    public void nextDigit() {

        Integer cols;

        digitIndex--;
        SText.performNextDigit();

        if (id == ASM_CONST.RESULT) {
            cols = 0;
        }
        else {
            cols = SText.getDigit(digitIndex);
            cols = (cols != null)?cols:0;
        }
        SdotBag.update(1, cols, image, clickable);

    }


    public Integer getNum() {return SText.getNum();}

    public CAsm_DotBag getDotBag() {
        return SdotBag;
    }

    public CAsm_Text getText() {return SText;}

    public void resetText() {SText.resetAllValues();}

    public Integer getCurrentDigit() {return SText.getDigit(digitIndex);}


}

package com.hixos.smartwp.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hixos.smartwp.R;

/**
 * Created by Luca on 23/12/13.
 */
public class SearchBox extends FrameLayout implements View.OnClickListener {
    public interface OnSearchActionListener{
        public void onSearch(String query);
        public void onDismiss();
    }

    private RelativeLayout mRootLayout;

    private LinearLayout mSearchArea;
    private ImageView mSearchCancelButton;
    private EditText mSearchEditText;
    private ProgressBar mProgressBar;

    private boolean mCollapsed = true;

    private OnSearchActionListener mListener;

    private boolean mShowProgress = true;

    public SearchBox(Context context) {
        super(context);
        init();
    }

    public SearchBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SearchBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mRootLayout = (RelativeLayout)inflater.inflate(R.layout.hixos_search_box, null);
        addView(mRootLayout);
        mSearchArea = (LinearLayout)mRootLayout.findViewById(R.id.frame_search_box);
        mSearchCancelButton = (ImageView)mRootLayout.findViewById(R.id.imageview_search_cancel);
        mSearchCancelButton.setOnClickListener(this);
        mSearchEditText = (EditText)mRootLayout.findViewById(R.id.edittext_search_box);
        mProgressBar = (ProgressBar)mRootLayout.findViewById(R.id.progress);

        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(mListener != null && !mSearchEditText.getText().toString().trim().equals("")){
                    mListener.onSearch(mSearchEditText.getText().toString());
                    InputMethodManager inputManager = (InputMethodManager) getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
                    if(mShowProgress) showProgress();
                    return true;
                }
                return false;
            }
        });
    }

    public void expand(){
        updateViewStatus(false);
    }

    public void collapse(){
        updateViewStatus(true);
        if(mListener != null) mListener.onDismiss();
    }

    public void setOnSearchActionListener(OnSearchActionListener listener){
        mListener = listener;
    }

    public void showProgressBarOnSearch(boolean show){
        mShowProgress = show;
    }

    public void showProgress(){
        mSearchCancelButton.setVisibility(GONE);
        mProgressBar.setVisibility(VISIBLE);
    }

    public void hideProgress(){
        mProgressBar.setVisibility(GONE);
        mSearchCancelButton.setVisibility(VISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.imageview_search_cancel:
                if(!mCollapsed){
                    if(mListener != null) mListener.onDismiss();
                    if(!mSearchEditText.getText().toString().trim().equals("")){
                        mSearchEditText.setText("");
                        break;
                    }
                }
                updateViewStatus(!mCollapsed);
                break;
        }
    }

    private void updateViewStatus(boolean collapse){
        if(mCollapsed == collapse) return;
        mCollapsed = collapse;
        if(collapse){
            mSearchCancelButton.setImageResource(R.drawable.ic_action_search);

            mSearchArea.setPivotX(mSearchArea.getWidth());
            ObjectAnimator animator = ObjectAnimator.ofFloat(mSearchArea, "scaleX", 1f, 0f);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    mSearchArea.setVisibility(INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            animator.setDuration(175);
            animator.start();

            mSearchEditText.setText("");
            InputMethodManager inputManager = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
        }else{
            mSearchArea.setVisibility(VISIBLE);
            mSearchArea.setPivotX(mSearchArea.getWidth());
            ObjectAnimator animator = ObjectAnimator.ofFloat(mSearchArea, "scaleX", 0f, 1f);
            animator.setDuration(175);
            animator.start();
            mSearchCancelButton.setImageResource(R.drawable.ic_action_search_cancel);
            mSearchEditText.requestFocus();
            InputMethodManager inputManager = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(mSearchEditText, 0);
        }
    }
}

package com.hixos.smartwp.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Window;

import com.hixos.smartwp.R;

/**
 * Created by Luca on 20/11/13.
 */
public class ProgressDialogFragment extends DialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true);
        setCancelable(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog().getWindow();
        int size = getResources().getDimensionPixelSize(R.dimen.dialog_crop_progress);
        window.setLayout(size, size);
        window.setGravity(Gravity.CENTER);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_crop_progress, null));
        return builder.create();
    }
}

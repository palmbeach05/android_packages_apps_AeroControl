package com.aero.control.helpers.Android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.aero.control.R;

/* JADX INFO: loaded from: classes.dex */
public class AboutDialog extends DialogFragment {
    private Context mContext;
    private Drawable mIcon;
    private View mLayout;
    private String mNegativeText;
    private String mNeutralText;
    private boolean mPayPalIcons = false;
    private String mPositiveText;
    private String mTitle;

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void setTitle(int i) {
        this.mTitle = this.mContext.getText(i).toString();
    }

    public void setView(View v) {
        this.mLayout = v;
    }

    public void setIcon(Drawable d) {
        this.mIcon = d;
    }

    public void setPayPalIcons(boolean b) {
        this.mPayPalIcons = b;
    }

    public void setPositiveButton(int i) {
        this.mPositiveText = this.mContext.getText(i).toString();
    }

    public void setNeutralButton(int i) {
        this.mNeutralText = this.mContext.getText(i).toString();
    }

    @Override // android.app.DialogFragment
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle(this.mTitle).setIcon(this.mIcon).setView(this.mLayout).setPositiveButton(this.mPositiveText, new DialogInterface.OnClickListener() { // from class: com.aero.control.helpers.Android.AboutDialog.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog2, int which) {
                Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=46VQEKBETN36U");
                Intent intent = new Intent("android.intent.action.VIEW", uri);
                AboutDialog.this.startActivity(intent);
            }
        }).setNeutralButton(this.mNeutralText, new DialogInterface.OnClickListener() { // from class: com.aero.control.helpers.Android.AboutDialog.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog2, int which) {
                Uri uri = Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=quarx%40yandex%2eru&lc=DE&no_note=0&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest");
                Intent intent = new Intent("android.intent.action.VIEW", uri);
                AboutDialog.this.startActivity(intent);
            }
        }).create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: com.aero.control.helpers.Android.AboutDialog.3
            @Override // android.content.DialogInterface.OnShowListener
            public void onShow(DialogInterface dialogInterface) {
                if (AboutDialog.this.mPayPalIcons) {
                    Button positive = dialog.getButton(-1);
                    Button neutral = dialog.getButton(-3);
                    Drawable drawable = AboutDialog.this.mContext.getResources().getDrawable(R.drawable.paypal);
                    positive.setCompoundDrawablesWithIntrinsicBounds(drawable, (Drawable) null, (Drawable) null, (Drawable) null);
                    neutral.setCompoundDrawablesWithIntrinsicBounds(drawable, (Drawable) null, (Drawable) null, (Drawable) null);
                    positive.setCompoundDrawablePadding(5);
                    neutral.setCompoundDrawablePadding(5);
                }
            }
        });
        return dialog;
    }
}

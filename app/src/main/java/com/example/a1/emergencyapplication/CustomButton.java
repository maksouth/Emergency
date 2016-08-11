package com.example.a1.emergencyapplication;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by andrey on 03.06.16.
 */
public class CustomButton extends LinearLayout {
    public TextView name;
    ImageView icon;

    RelativeLayout click;


    public CustomButton(Context context) {
        super(context);
        init(context);
    }

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        inflate(context, R.layout.image_text_button, this);
        icon = (ImageView) findViewById(R.id.icon);
        name = (TextView) findViewById(R.id.name);
        click = (RelativeLayout) findViewById(R.id.click);

    }
    public void setClick(OnClickListener onClickListener){
        click.setOnClickListener(onClickListener);
    }
    public void setValues(int idName, int idRes){
        icon.setImageDrawable(getResources().getDrawable(idRes));
        name.setText(getResources().getText(idName));
    }
    public void setTextColor(int id){
        name.setTextColor(getResources().getColor(id));
    }
}

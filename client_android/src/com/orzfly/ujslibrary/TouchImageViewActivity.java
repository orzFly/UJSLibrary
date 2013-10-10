// https://github.com/MikeOrtiz/TouchImageView/raw/master/src/com/example/touch/TouchImageViewActivity.java
	
package com.orzfly.ujslibrary;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class TouchImageViewActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        Intent intent = getIntent();
        
        TouchImageView touch = new TouchImageView(this);
        touch.setMaxZoom(4f);
        touch.setBackgroundColor(Color.BLACK);
        LibraryAPI.getIconCache().get(intent.getStringExtra("url"), touch);
        
        setContentView(touch);
    }
}
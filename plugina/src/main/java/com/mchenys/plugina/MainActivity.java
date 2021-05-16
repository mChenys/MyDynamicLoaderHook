package com.mchenys.plugina;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.util.Log;

public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e("MainActivity", "mResources:" + (getResources() == null ? "" : getResources().hashCode()));
        Log.e("MainActivity", "mBase:" + (getBaseContext() == null ? "" : getBaseContext().hashCode()));
    }
}
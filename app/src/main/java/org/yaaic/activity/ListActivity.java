/*
Yaaic - Yet Another Android IRC Client

Copyright 2009-2015 Sebastian Kaspari

This file is part of Yaaic.

Yaaic is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Yaaic is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Yaaic.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.yaaic.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.yaaic.R;
import org.yaaic.Yaaic;
import org.yaaic.YaaicApplication;
import org.yaaic.model.Server;

/**
 * "About" dialog activity.
 */
public class ListActivity extends Activity {
    private static final String TAG = "ListActivity";
    private String[] channels;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        channels = getResources().getStringArray(R.array.list_channels);


        setContentView(R.layout.channel_list);

        populateChannels();
        ((YaaicApplication)this.getApplication()).sendHit(TAG);
    }

    public void populateChannels() {
        LinearLayout serverContainer = (LinearLayout) findViewById(R.id.channel_list_container);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);

        for (final String channel : channels) {
            final TextView serverView = (TextView) getLayoutInflater().inflate(R.layout.item_channel_list, drawer, false);
            serverView.setText(channel);

            serverView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select(serverView.getText().toString());
                }
            });

            serverContainer.addView(serverView, 0);
        }
    }

    public void select(String channel) {
        Log.v("Enter to channel", channel);
        Intent intent = new Intent();
        intent.putExtra("channel", channel);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}

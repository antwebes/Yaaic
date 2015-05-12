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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.yaaic.R;
import org.yaaic.YaaicApplication;
import org.yaaic.listener.ServicesListener;
import org.yaaic.model.Broadcast;
import org.yaaic.receiver.ServicesReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * "About" dialog activity.
 */
public class NickServActivity extends ActionBarActivity implements ServicesListener{
    private static final String TAG = "Yaaic/NickservActivity";
    private ServicesReceiver servicesReceiver;

    private int server_id = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nick_serv);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        View v = LayoutInflater.from(this).inflate(R.layout.item_done_discard, toolbar, false);

        toolbar.addView(v);

        setSupportActionBar(toolbar);

        v.findViewById(R.id.actionbar_discard).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        v.findViewById(R.id.actionbar_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChange();
            }
        });


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            server_id = extras.getInt("server_id");
            ((EditText) findViewById(R.id.nickserv_field)).setText(extras.getString("nick"));
            ((EditText) findViewById(R.id.nickserv_password_field)).requestFocus();
        }

        servicesReceiver = new ServicesReceiver(server_id, this);
        this.registerReceiver(servicesReceiver, new IntentFilter(Broadcast.NICKSERV_MESSAGE));

        Log.v("Nickserv", "OnCreate view to server " + server_id);


        populateData();
    }

    public void onChange() {
        Intent intent = new Intent();
        intent.putExtra("nick", ((EditText) findViewById(R.id.nickserv_field)).getText().toString());
        intent.putExtra("password", ((EditText) findViewById(R.id.nickserv_password_field)).getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void populateData() {
        List<String> data = ((YaaicApplication) this.getApplication()).getNickservData();

        if(data == null) return;

        for(String d : data) {
            if(d.contains("Contraseña aceptada - Has sido reconocido")) {
                List<String> mulldata = new ArrayList<>();
                ((YaaicApplication) this.getApplication()).setNickservData(mulldata);
                this.finish();
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();

    }

    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(servicesReceiver);
    }


    @Override
    public void onNickservMessage(String target) {
        Log.v("NickserActivity", "Mensaje performed......... " + target);
        populateData();
    }

    @Override
    public void onNickservInitialize(String target) {
        //noting to do...
    }
}

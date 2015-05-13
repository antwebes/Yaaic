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
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.yaaic.R;
import org.yaaic.Yaaic;
import org.yaaic.YaaicApplication;
import org.yaaic.fragment.ConversationFragment;
import org.yaaic.fragment.OverviewFragment;
import org.yaaic.fragment.SettingsFragment;
import org.yaaic.irc.IRCBinder;
import org.yaaic.irc.IRCService;
import org.yaaic.model.Extra;
import org.yaaic.model.Server;
import org.yaaic.model.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.yaaic.adapter.ExpandableListAdapter;
import org.yaaic.tools.UIHelper;


/**
 * The main activity of Yaaic. We'll add, remove and replace fragments here.
 */
public class MainActivity extends AppCompatActivity implements YaaicActivity, ServiceConnection {
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private IRCBinder binder;

    public static String TAG = "MainActivity";


    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initializeToolbar();
        initializeDrawer();

        if (savedInstanceState == null) {
            onOverview(null);
        }

        onCreateExpandableList();
    }

    public void initializeToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void initializeDrawer() {
        drawer = (DrawerLayout) findViewById(R.id.drawer);
        toggle = new ActionBarDrawerToggle(this, drawer, toolbar, 0, 0);

        drawer.setDrawerListener(toggle);

        LinearLayout serverContainer = (LinearLayout) findViewById(R.id.server_container);

        for (final Server server : Yaaic.getInstance().getServersAsArrayList()) {
            TextView serverView = (TextView) getLayoutInflater().inflate(R.layout.item_drawer_server, drawer, false);
            serverView.setText(server.getTitle());

            serverView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onServerSelected(server);

                    drawer.closeDrawers();
                }
            });

            serverContainer.addView(serverView, 0);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        toggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, IRCService.class);
        intent.setAction(IRCService.ACTION_BACKGROUND);
        startService(intent);

        bindService(intent, this, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (binder != null && binder.getService() != null) {
            binder.getService().checkServiceStatus();
        }

        unbindService(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }

        return false;
    }

    @Override
    public void onServerSelected(Server server) {
        Bundle arguments = new Bundle();

        if (server.getStatus() == Status.DISCONNECTED && !server.mayReconnect()) {
            server.setStatus(Status.PRE_CONNECTING);

            arguments.putBoolean(Extra.CONNECT, true);
        }

        arguments.putInt(Extra.SERVER_ID, server.getId());

        ConversationFragment fragment = new ConversationFragment();
        fragment.setArguments(arguments);

        switchToFragment(fragment, ConversationFragment.TRANSACTION_TAG);

    }

    public void onOverview(View view) {
        switchToFragment(new OverviewFragment(), OverviewFragment.TRANSACTION_TAG);
    }

    public void onSettings(View view) {
        switchToFragment(new SettingsFragment(), SettingsFragment.TRANSACTION_TAG);
    }

    private void switchToFragment(Fragment fragment, String tag) {
        drawer.closeDrawers();

        getFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                        R.animator.card_flip_left_in, R.animator.card_flip_left_out)
                .replace(R.id.container, fragment, tag)
                .commit();
    }

    public void onAbout(View view) {
        drawer.closeDrawers();

        startActivity(new Intent(this, AboutActivity.class));
    }

    @Override
    public IRCBinder getBinder() {
        return binder;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public void setToolbarTitle(String title) {
        //toolbar.setTitle(title);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder = (IRCBinder) service;

        autoConnectServers();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        binder = null;
    }


    private void onCreateExpandableList() {
        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        // Listview Group click listener
        expListView.setOnGroupClickListener(new OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                // Toast.makeText(getApplicationContext(),
                // "Group Clicked " + listDataHeader.get(groupPosition),
                // Toast.LENGTH_SHORT).show();
                return false;
            }
        });


        // Listview on child click listener
        expListView.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                String clicked = listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
                if(getString(R.string.about_label) == clicked) {
                    onAbout(v);
                }

                if(getString(R.string.advanced_label) == clicked) {
                    onSettings(v);
                }

                if(getString(R.string.manage_accounts_label) == clicked) {
                    onOverview(v);
                }

                if(getString(R.string.close_application) == clicked) {
                    onExit();
                }

                if(getString(R.string.register_label) == clicked) {
                    onRegister();
                }

                return false;
            }
        });
    }

    private void onRegister() {
        drawer.closeDrawers();

        startActivity(new Intent(this, RegisterActivity.class));
    }

    private void onExit() {
        // shutdown logic
        for (final Server server : Yaaic.getInstance().getServersAsArrayList()) {

            if (binder != null && server.getStatus() == Status.CONNECTED) {
                server.clearConversations();
                server.setStatus(Status.DISCONNECTED);
                server.setMayReconnect(false);
                binder.getService().getConnection(server.getId()).quitServer();
            }
        }



        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(startMain);


        UIHelper.killApp(true);
    }

 /*
 * Preparing the list data
 */
    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // Adding child data
        listDataHeader.add(getString(R.string.mi_account));
        listDataHeader.add(getString(R.string.navigation_settings));

        // Adding child data
        List<String> myAccount = new ArrayList<String>();
        myAccount.add(getString(R.string.manage_accounts_label));
        myAccount.add(getString(R.string.register_label));
        myAccount.add(getString(R.string.close_application));


        List<String> settings = new ArrayList<String>();

        settings.add(getString(R.string.advanced_label));
        settings.add(getString(R.string.about_label));


        listDataChild.put(listDataHeader.get(0), myAccount); // Header, Child data
        listDataChild.put(listDataHeader.get(1), settings);
    }

    public void autoConnectServers() {
        int connected = 0;
        ArrayList<Server> servers = Yaaic.getInstance().getAutoconnectServersAsArrayList();
        if(servers.size() == 0) {
            drawer.closeDrawers();

            startActivity(new Intent(this, AddServerActivity.class));
        } else {
            for (final Server server : servers) {

                if (binder != null && server.getStatus() == Status.DISCONNECTED) {
                    binder.connect(server);
                    server.setStatus(Status.CONNECTING);
                    connected++;
                }

                if (connected == 1) onServerSelected(server);
            }
        }

    }
}

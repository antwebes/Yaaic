/*
Yaaic - Yet Another Android IRC Client

Copyright 2009-2013 Sebastian Kaspari

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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.yaaic.R;
import org.yaaic.Yaaic;
import org.yaaic.YaaicApplication;
import org.yaaic.db.Database;
import org.yaaic.exception.ValidationException;
import org.yaaic.model.Authentication;
import org.yaaic.model.Extra;
import org.yaaic.model.Identity;
import org.yaaic.model.RegisterValidation;
import org.yaaic.model.Server;
import org.yaaic.model.Settings;
import org.yaaic.model.Status;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Add a new server to the list
 *
 * @author Sebastian Kaspari <se import android.util.Log;bastian@yaaic.org>
 */
public class AddServerActivity extends ActionBarActivity implements OnClickListener
{
    private static final int REQUEST_CODE_CHANNELS       = 1;
    private static final int REQUEST_CODE_COMMANDS       = 2;
    private static final int REQUEST_CODE_ALIASES        = 3;
    private static final int REQUEST_CODE_AUTHENTICATION = 4;

    private Server server;
    private Authentication authentication;
    private ArrayList<String> aliases;
    private ArrayList<String> channels;
    private ArrayList<String> commands;

    ProgressDialog mDialog;
    private boolean executing = false;

    public static String TAG = "AddServerActivity";

    private Context context;
    /**
     * On create
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        executing = false;

        context = this.getApplicationContext();

        setContentView(R.layout.activity_add_server);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        View v =LayoutInflater.from(this).inflate(R.layout.item_done_discard, toolbar, false);

        toolbar.addView(v);

        setSupportActionBar(toolbar);

        // If I include the below bit, then the DrawerToggle doesn't function
        // I don't know how to switch it back and forth

        authentication = new Authentication();
        aliases = new ArrayList<String>();
        channels = new ArrayList<String>();
        commands = new ArrayList<String>();

        findViewById(R.id.aliases).setOnClickListener(this);
        findViewById(R.id.channels).setOnClickListener(this);
        findViewById(R.id.commands).setOnClickListener(this);
        findViewById(R.id.authentication).setOnClickListener(this);
        v.findViewById(R.id.actionbar_discard).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel(v);
            }
        });

        v.findViewById(R.id.actionbar_done).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!executing) {
                    executing = true;
                    onSave(v);
                }

            }
        });

        Spinner spinner = (Spinner) findViewById(R.id.charset);
        String[] charsets = getResources().getStringArray(R.array.charsets);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, charsets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(Extra.SERVER)) {
            setTitle(R.string.edit_server_label);

            // Request to edit an existing server
            Database db = new Database(this);
            this.server = db.getServerById(extras.getInt(Extra.SERVER));
            aliases.addAll(server.getIdentity().getAliases());
            this.channels = db.getChannelsByServerId(server.getId());
            this.commands = db.getCommandsByServerId(server.getId());
            this.authentication = server.getAuthentication();
            db.close();

            // Set server values
            ((EditText) findViewById(R.id.title)).setText(server.getTitle());
            ((EditText) findViewById(R.id.host)).setText(server.getHost());
            ((EditText) findViewById(R.id.port)).setText(String.valueOf(server.getPort()));
            ((EditText) findViewById(R.id.password)).setText(server.getPassword());

            ((EditText) findViewById(R.id.nickname)).setText(server.getIdentity().getNickname());
            ((EditText) findViewById(R.id.ident)).setText(server.getIdentity().getIdent());
            ((EditText) findViewById(R.id.realname)).setText(server.getIdentity().getRealName());
            ((CheckBox) findViewById(R.id.useSSL)).setChecked(server.useSSL());
            ((CheckBox) findViewById(R.id.autoconnect)).setChecked(server.getAutoconnect());

            // Select charset
            if (server.getCharset() != null) {
                for (int i = 0; i < charsets.length; i++) {
                    if (server.getCharset().equals(charsets[i])) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        }

        // Disable suggestions for host name
        if (android.os.Build.VERSION.SDK_INT >= 5) {
            EditText serverHostname = (EditText) findViewById(R.id.host);
            serverHostname.setInputType(0x80000);
        }

        Uri uri = getIntent().getData();
        if (uri != null && uri.getScheme().equals("irc")) {
            // handling an irc:// uri

            ((EditText) findViewById(R.id.host)).setText(uri.getHost());
            if (uri.getPort() != -1) {
                ((EditText) findViewById(R.id.port)).setText(String.valueOf(uri.getPort()));
            }
            if (uri.getPath() != null) {
                channels.add(uri.getPath().replace('/', '#'));
            }
            if (uri.getQuery() != null) {
                ((EditText) findViewById(R.id.password)).setText(String.valueOf(uri.getQuery()));
            }
        }

        ((YaaicApplication)this.getApplication()).sendHit(TAG);
    }

    /**
     * On activity result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK) {
            return; // ignore everything else
        }

        switch (requestCode) {
            case REQUEST_CODE_ALIASES:
                aliases.clear();
                aliases.addAll(data.getExtras().getStringArrayList(Extra.ALIASES));
                break;

            case REQUEST_CODE_CHANNELS:
                channels = data.getExtras().getStringArrayList(Extra.CHANNELS);
                break;

            case REQUEST_CODE_COMMANDS:
                commands = data.getExtras().getStringArrayList(Extra.COMMANDS);
                break;

            case REQUEST_CODE_AUTHENTICATION:
                authentication.setSaslUsername(data.getExtras().getString(Extra.SASL_USER));
                authentication.setSaslPassword(data.getExtras().getString(Extra.SASL_PASSWORD));
                authentication.setNickservPassword(data.getExtras().getString(Extra.NICKSERV_PASSWORD));
                break;
        }
    }

    /**
     * On click add server or cancel activity
     */
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.aliases:
                Intent aliasIntent = new Intent(this, AddAliasActivity.class);
                aliasIntent.putExtra(Extra.ALIASES, aliases);
                startActivityForResult(aliasIntent, REQUEST_CODE_ALIASES);
                break;

            case R.id.authentication:
                Intent authIntent = new Intent(this, AuthenticationActivity.class);
                authIntent.putExtra(Extra.NICKSERV_PASSWORD, authentication.getNickservPassword());
                authIntent.putExtra(Extra.SASL_USER, authentication.getSaslUsername());
                authIntent.putExtra(Extra.SASL_PASSWORD, authentication.getSaslPassword());
                startActivityForResult(authIntent, REQUEST_CODE_AUTHENTICATION);
                break;

            case R.id.channels:
                Intent channelIntent = new Intent(this, AddChannelActivity.class);
                channelIntent.putExtra(Extra.CHANNELS, channels);
                startActivityForResult(channelIntent, REQUEST_CODE_CHANNELS);
                break;

            case R.id.commands:
                Intent commandsIntent = new Intent(this, AddCommandsActivity.class);
                commandsIntent.putExtra(Extra.COMMANDS, commands);
                startActivityForResult(commandsIntent, REQUEST_CODE_COMMANDS);
                break;
        }
    }

    public void onCancel(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }


    /**
     * Try to save server.
     */

    public void onSave(View view) {
        try {
            validateServer();
            validateIdentity();

        } catch(ValidationException e) {
            executing = false;
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onSaveDone() {
            if (server == null) {
                addServer();
            } else {
                updateServer();
            }
            setResult(RESULT_OK);
            finish();
    }

    /**
     * Add server to database
     */
    private void addServer()
    {
        Settings settings = new Settings(this);
        Database db = new Database(this);

        Identity identity = getIdentityFromView();
        long identityId = db.addIdentity(
            identity.getNickname(),
            identity.getIdent(),
            identity.getRealName(),
            identity.getAliases()
            );

        Server server = getServerFromView();
        server.setTitle(identity.getNickname());
        server.setAuthentication(authentication);

        long serverId = db.addServer(server, (int) identityId);

        channels.add(settings.getDefaultChannel());
        
        db.setChannels((int) serverId, channels);
        db.setCommands((int) serverId, commands);

        db.close();

        server.setId((int) serverId);
        server.setIdentity(identity);
        server.setAutoJoinChannels(channels);
        server.setConnectCommands(commands);

        Yaaic.getInstance().addServer(server);
    }

    /**
     * Update server
     */
    private void updateServer()
    {
        Settings settings = new Settings(this);
        Database db = new Database(this);

        int serverId = this.server.getId();
        int identityId = db.getIdentityIdByServerId(serverId);

        Server server = getServerFromView();
        server.setAuthentication(authentication);

        Identity identity = getIdentityFromView();
        server.setTitle(identity.getNickname());

        db.updateServer(serverId, server, identityId);

        db.updateIdentity(
            identityId,
            identity.getNickname(),
            identity.getIdent(),
            identity.getRealName(),
            identity.getAliases()
            );

        channels.add(settings.getDefaultChannel());

        db.setChannels(serverId, channels);
        db.setCommands(serverId, commands);

        db.close();

        server.setId(this.server.getId());
        server.setIdentity(identity);
        server.setAutoJoinChannels(channels);
        server.setConnectCommands(commands);

        Yaaic.getInstance().updateServer(server);
    }

    /**
     * Populate a server object from the data in the view
     *
     * @return The server object
     */
    private Server getServerFromView()
    {
        Settings settings = new Settings(this);


        //String title = ((EditText) findViewById(R.id.title)).getText().toString().trim();
        String host =  settings.getServer();
        int port = settings.getPort();
        String password = ((EditText) findViewById(R.id.password)).getText().toString().trim();
        String charset = "UTF-8"; //((Spinner) findViewById(R.id.charset)).getSelectedItem().toString();
        Boolean useSSL = false; //((CheckBox) findViewById(R.id.useSSL)).isChecked();

        Boolean autoConnect = ((CheckBox) findViewById(R.id.autoconnect)).isChecked();


        Server server = new Server();
        server.setHost(host);
        server.setPort(port);
        server.setPassword(password);
        //server.setTitle(title);
        server.setCharset(charset);
        server.setUseSSL(useSSL);
        server.setStatus(Status.DISCONNECTED);
        server.setAutoconnect(autoConnect);

        return server;
    }

    /**
     * Populate an identity object from the data in the view
     *
     * @return The identity object
     */
    private Identity getIdentityFromView()
    {
        String nickname = ((EditText) findViewById(R.id.nickname)).getText().toString().trim();
        String ident = ((EditText) findViewById(R.id.ident)).getText().toString().trim();
        String realname = ((EditText) findViewById(R.id.realname)).getText().toString().trim();


        Identity identity = new Identity();
        identity.setNickname(nickname);
        identity.setIdent(ident);
        identity.setRealName(realname);

        identity.setAliases(aliases);

        return identity;
    }

    /**
     * Validate the input for a server
     *
     * @throws ValidationException
     */
    private void validateServer() throws ValidationException
    {
        String title = ((EditText) findViewById(R.id.title)).getText().toString();
        String host = ((EditText) findViewById(R.id.host)).getText().toString();
        String port = ((EditText) findViewById(R.id.port)).getText().toString();
        String charset = ((Spinner) findViewById(R.id.charset)).getSelectedItem().toString();
        /*
        if (title.trim().equals("")) {
            throw new ValidationException(getResources().getString(R.string.validation_blank_title));
        }
        */
        /*
        if (host.trim().equals("")) {
            // XXX: We should use some better host validation
            throw new ValidationException(getResources().getString(R.string.validation_blank_host));
        }
        */
        /*
        try {
            Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new ValidationException(getResources().getString(R.string.validation_invalid_port));
        }
        */

        try {
            "".getBytes(charset);
        }
        catch (UnsupportedEncodingException e) {
            throw new ValidationException(getResources().getString(R.string.validation_unsupported_charset));
        }

        Database db = new Database(this);
        if (db.isTitleUsed(title) && (server == null || !server.getTitle().equals(title))) {
            db.close();
            throw new ValidationException(getResources().getString(R.string.validation_title_used));
        }
        db.close();
    }

    /**
     * Validate the input for a identity
     *
     * @throws ValidationException
     */
    private void validateIdentity() throws ValidationException
    {
        String nickname = ((EditText) findViewById(R.id.nickname)).getText().toString();
        String ident = ((EditText) findViewById(R.id.ident)).getText().toString();
        String realname = ((EditText) findViewById(R.id.realname)).getText().toString();

        if (nickname.trim().equals("")) {
            throw new ValidationException(getResources().getString(R.string.validation_blank_nickname));
        }

        if (ident.trim().equals("")) {
            //throw new ValidationException(getResources().getString(R.string.validation_blank_ident));
        }

        if (realname.trim().equals("")) {
            //throw new ValidationException(getResources().getString(R.string.validation_blank_realname));
        }

        // RFC 1459:  <nick> ::= <letter> { <letter> | <number> | <special> }
        // <special>    ::= '-' | '[' | ']' | '\' | '`' | '^' | '{' | '}'
        // Chars that are not in RFC 1459 but are supported too:
        // | and _
        Pattern nickPattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9^\\-`\\[\\]{}|_\\\\]*$");
        if (!nickPattern.matcher(nickname).matches()) {
            throw new ValidationException(getResources().getString(R.string.validation_invalid_nickname));
        }

        // We currently only allow chars, numbers and some special chars for ident
        Pattern identPattern = Pattern.compile("^[a-zA-Z0-9\\[\\]\\-_/]+$");
        if (!identPattern.matcher(ident).matches()) {
            throw new ValidationException(getResources().getString(R.string.validation_invalid_ident));
        }

        if(((EditText) findViewById(R.id.password)).getVisibility() != View.VISIBLE) {
            validateRegisterNick(nickname);
        } else {
            onSaveDone();
        }


    }

    private void validateRegisterNick(String nickname) {
        mDialog = new ProgressDialog(this);
        mDialog.setTitle(getString(R.string.check_nick_title));
        mDialog.setMessage(getString(R.string.check_nick_description));
        mDialog.show();

        new RegisterValidator(nickname).execute();

    }

     public class RegisterValidator extends AsyncTask<Void, Void, RegisterValidation> {
        private String nick;

        public RegisterValidator(String nick) {
            this.nick = nick;
        }

        @Override
        protected RegisterValidation doInBackground(Void... params) {
            final String url = getString(R.string.api_user_is_register_endpoint) + this.nick;
            try {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("Cache-Control:",  "no-cache, private");
                headers.set("Connection", "Close");

                Log.i("ApiCheck","Call Verify");

                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

                HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

                ResponseEntity<RegisterValidation> validation = restTemplate.exchange(url, HttpMethod.GET, entity, RegisterValidation.class);


                return validation.getBody();
            }catch(HttpClientErrorException e) {
                if(e.getStatusCode().toString().equals("400")) {
                    Log.e("ApiError", e.getMessage(), e);
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        Log.i("ApiCheck", "Error response: " + e.getResponseBodyAsString());
                        RegisterValidation validation = mapper.readValue(e.getResponseBodyAsString(), RegisterValidation.class);
                        validation.setCode(34);
                        Log.i("ApiCheck","400 user already register");
                        return validation;
                    } catch (IOException e1) {
                        Log.e("ApiError", e1.getMessage(), e1);
                        return null;
                    }
                }
            } catch (HttpServerErrorException e) {
                Log.e("ApiError", e.getMessage(), e);
            } catch (Exception e) {
                Log.e("ApiError", e.getMessage(), e);
            }


            return null;
        }

        @Override
        protected void onPostExecute(RegisterValidation validation) {
            mDialog.dismiss();
            executing = false;

            Log.i("ApiCheck","onPostExecute");

            if(validation == null) return;

            if (validation.isValid()) {
                onSaveDone();
                return;
            }

            Log.i("ApiCheck","validationCode" + validation.getCode());

            if(validation.getCode() == 34) {
                Log.i("ApiCheck","Nick register");

                ((EditText) findViewById(R.id.password)).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.password_label)).setVisibility(View.VISIBLE);
                ((EditText) findViewById(R.id.password)).setError(getString(R.string.check_nick_has_password));
            }

        }
    }
}



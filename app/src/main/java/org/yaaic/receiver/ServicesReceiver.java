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
package org.yaaic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.yaaic.listener.ConversationListener;
import org.yaaic.listener.ServicesListener;
import org.yaaic.model.Broadcast;
import org.yaaic.model.Extra;

/**
 * A channel receiver for receiving channel updates
 * 
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class ServicesReceiver extends BroadcastReceiver
{
    private final ServicesListener listener;
    private final int serverId;

    /**
     * Create a new channel receiver
     *
     * @param serverId Only listen on channels of this server
     * @param listener
     */
    public ServicesReceiver(int serverId, ServicesListener listener)
    {
        this.listener = listener;
        this.serverId = serverId;
    }

    /**
     * On receive broadcast
     * 
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        int serverId = intent.getExtras().getInt(Extra.SERVER);
        if (serverId != this.serverId) {
            return;
        }

        String action = intent.getAction();

        if (action.equals(Broadcast.NICKSERV_MESSAGE)) {
            listener.onNickservMessage(intent.getExtras().getString(Extra.CONVERSATION));
        } else if (action.equals(Broadcast.NICKSERV_INITIALIZE)) {
            listener.onNickservInitialize(intent.getExtras().getString(Extra.CONVERSATION));
        }

    }
}

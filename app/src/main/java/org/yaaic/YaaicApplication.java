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
package org.yaaic;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

/**
 * Application implementation for Yaaic.
 */
public class YaaicApplication extends Application {

    private List <String> nickservData = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        Yaaic.getInstance().loadServers(this);
    }

    public List<String> getNickservData() {
        return nickservData;
    }

    public void setNickservData(List<String> nickservData) {
        this.nickservData = nickservData;
    }
}

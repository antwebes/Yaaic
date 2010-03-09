/*
 Yaaic - Yet Another Android IRC Client

Copyright 2009 Sebastian Kaspari

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
package org.yaaic.command;

import org.yaaic.irc.IRCService;
import org.yaaic.model.Channel;
import org.yaaic.model.Server;

/**
 * Base class for commands
 * 
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public abstract class BaseCommand
{
	/**
	 * Execute the command
	 * 
	 * @param params The params given (0 is the command itself)
	 * @param server The server object
	 * @param channel The channel object or null if no channel is selected
	 * @param service The service with all server connections
	 */
	public abstract void execute(String[] params, Server server, Channel channel, IRCService service);
	
	/**
	 * Get the usage description for this command
	 * 
	 * @return The usage description
	 */
	public abstract String getUsage();

	/**
	 * How much params does this command need?
	 * 
	 * Default: 0
	 * 
	 * @return The number of params needed
	 */
	public abstract int needsParams();
}

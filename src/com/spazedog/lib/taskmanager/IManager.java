/*
 * This file is part of the TaskManager Project: https://github.com/spazedog/taskmanager
 *  
 * Copyright (c) 2013 Daniel Bergløv
 *
 * TaskManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * TaskManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public License
 * along with TaskManager. If not, see <http://www.gnu.org/licenses/>
 */

package com.spazedog.lib.taskmanager;

public interface IManager {
	public final static String TAG = null;
	
	public void addTask(String aTag, ITask aReceiver);
	public void removeTask(String aTag);
	public ITask getTask(String aTag);
	
	public void addDaemon(String aTag, IDaemon aReceiver);
	public void removeDaemon(String aTag);
	public IDaemon getDaemon(String aTag);
	
	public Boolean isUIAttached();
}

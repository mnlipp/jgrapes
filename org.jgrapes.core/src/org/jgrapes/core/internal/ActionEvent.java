/*
 * Ad Hoc Polling Application
 * Copyright (C) 2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.core.internal;

import java.util.concurrent.Callable;

import org.jgrapes.core.Event;

/**
 * 
 */
public abstract class ActionEvent<T> extends Event<T> {

	private static Object defaultCriterion = new Object();

	static <V> ActionEvent<V> create(Callable<V> action) {
		return new CallableActionEvent<>(action);
	}
	
	static ActionEvent<Void> create(Runnable action) {
		return new RunnableActionEvent(action);
	}
	
	abstract void execute() throws Exception;
	
	@Override
	public boolean isEligibleFor(Object criterion) {
		return criterion == defaultCriterion;
	}

	@Override
	public Object defaultCriterion() {
		return defaultCriterion;
	}

	private static class CallableActionEvent<V> extends ActionEvent<V> {
		private Callable<V> action;
		
		public CallableActionEvent(Callable<V> callable) {
			this.action = callable;
		}

		void execute() throws Exception {
			setResult(action.call());
		}
	}
	
	private static class RunnableActionEvent extends ActionEvent<Void> {
		private Runnable action;
		
		public RunnableActionEvent(Runnable action) {
			this.action = action;
		}

		void execute() throws Exception {
			action.run();
		}
		

	}
}

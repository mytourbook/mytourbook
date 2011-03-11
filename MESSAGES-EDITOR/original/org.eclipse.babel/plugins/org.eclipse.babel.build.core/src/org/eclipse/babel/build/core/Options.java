/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.babel.build.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Options {
	public static class UnknownOption extends RuntimeException{
		private static final long serialVersionUID = 5843082921291728978L;
		
		public UnknownOption(String name) {
			super(name);
		}
	}
	
	public static class UnsetMandatoryOption extends RuntimeException{
		private static final long serialVersionUID = 4112908586458361303L;

		public UnsetMandatoryOption(String name){
			super(name);
		}
	}
	
	public static class MissingArgument extends RuntimeException{
		private static final long serialVersionUID = 4845067566513625318L;
		
		public MissingArgument(String message){
			super(message);
		}
	}
	
	private class Option {
		private final String name;
		private String value = null;
		private final boolean needsArg;
		private final boolean mandatory;
		
		public Option(String specifier){
			mandatory = specifier.endsWith("!");
			needsArg = specifier.endsWith("=") || specifier.endsWith("=!");
			name = specifier.substring(0, specifier.length() - (needsArg ? 1 : 0) - (mandatory ? 1 : 0));
		}
	}
	
	private final Map<String, Option> values = new HashMap<String, Option>();
	private final List<String> params = new LinkedList<String>();
	
	public Options(String... opts) {
		for(String opt : opts){
			Option o = new Option(opt);
			values.put(o.name, o);
		}
	}
	
	public Options parse(String... args) {
		for(int i = 0; i < args.length; i++){
			Option opt = values.get(args[i]);
			
			if(opt == null){
				params.add(args[i]);
			} else {
				if(opt.needsArg){
					if(i < args.length - 1){
						opt.value = args[i + 1];
					} else {
						throw new MissingArgument(args[i]);
					}
					i++;
				} else {
					opt.value = "";
				}
			}
		}
		
		for(String name : values.keySet()){
			Option opt = values.get(name);
			if(opt.mandatory && opt.value == null){
				throw new UnsetMandatoryOption(name);
			}
		}
		
		return this;
	}
	
	public boolean isSet(String name){
		try{
			return values.get(name).value != null;
		} catch (NullPointerException ex){
			throw new UnknownOption(name);
		}
	}

	public List<String> getParams() {
		return params;
	}

	public String get(String name) {
		return get(name, null);
	}

	public String get(String name, String default_) {
		try{
			String value = values.get(name).value;
			if(value == null){
				return default_;
			}
			return value;
		} catch (NullPointerException ex){
			throw new UnknownOption(name);
		}
	}
}
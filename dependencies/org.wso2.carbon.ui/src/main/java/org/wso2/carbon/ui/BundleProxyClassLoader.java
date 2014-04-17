/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.ui;

import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
 * A BundleProxyClassLoader wraps a bundle and uses the various Bundle methods to produce a ClassLoader.
 */
public class BundleProxyClassLoader extends ClassLoader {
	private Bundle bundle;
	private ClassLoader parent;

	public BundleProxyClassLoader(Bundle bundle) {
		this.bundle = bundle;
	}

	public BundleProxyClassLoader(Bundle bundle, ClassLoader parent) {
		super(parent);
		this.parent = parent;
		this.bundle = bundle;
	}

	public Enumeration findResources(String name) throws IOException {
		return bundle.getResources(name);
	}

	public URL findResource(String name) {
		return bundle.getResource(name);
	}

	public Class findClass(String name) throws ClassNotFoundException {
		return bundle.loadClass(name);
	}

	public URL getResource(String name) {
		return (parent == null) ? findResource(name) : super.getResource(name);
	}

	protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class clazz = (parent == null) ? findClass(name) : super.loadClass(name, false);
		if (resolve) {
			super.resolveClass(clazz);
		}
		
		return clazz;
	}
}
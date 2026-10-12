/*
 * Copyright (c) 2013-2026 Hutool Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.hutool.v7.core.io.resource;

import cn.hutool.v7.core.compress.ZipUtil;
import cn.hutool.v7.core.io.IORuntimeException;

import java.io.Serial;
import java.net.URI;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * Jar包资源对象
 *
 * @author Looly
 */
public class JarResource extends UrlResource {
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 构造
	 *
	 * @param uri JAR的URI
	 */
	public JarResource(final URI uri) {
		super(uri);
	}

	/**
	 * 构造
	 *
	 * @param url JAR的URL
	 */
	public JarResource(final URL url) {
		super(url);
	}

	/**
	 * 构造
	 *
	 * @param url  JAR的URL
	 * @param name 资源名称
	 */
	public JarResource(final URL url, final String name) {
		super(url, name);
	}

	/**
	 * 获取URL对应的{@link JarFile}对象
	 *
	 * @return {@link JarFile}
	 * @throws IORuntimeException IO异常
	 */
	public JarFile getJarFile() throws IORuntimeException {
		return getJarFile(true);
	}

	/**
	 * 获取URL对应的{@link JarFile}对象
	 * <p>
	 * 当{@code useCaches}为{@code false}时，返回不与其他URL连接共享的独立{@link JarFile}实例，
	 * 由调用方负责关闭；当{@code useCaches}为{@code true}时，可能返回URL连接缓存中的共享实例，
	 * 调用方不应关闭该实例，否则会影响其他持有同一实例的调用方。
	 *
	 * @param useCaches 是否使用URL连接缓存
	 * @return {@link JarFile}
	 * @throws IORuntimeException IO异常
	 * @since 7.0.0
	 */
	public JarFile getJarFile(final boolean useCaches) throws IORuntimeException {
		return ZipUtil.ofJar(getUrl(), useCaches);
	}
}

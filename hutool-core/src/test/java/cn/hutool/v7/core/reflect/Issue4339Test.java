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

package cn.hutool.v7.core.reflect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * issue#4339 测试：ClassScanner扫描jar时不应关闭URL连接缓存中共享的JarFile
 */
public class Issue4339Test {

	/**
	 * 用于放入测试jar的静态内部类
	 */
	public static class Issue4339Bean {
	}

	/**
	 * 测试ClassScanner扫描jar后不会关闭共享的JarFile实例（issue#4339）
	 */
	@Test
	public void scanNotCloseSharedJarFileTest(@TempDir Path tempDir) throws Exception {
		// 构建一个包含 Issue4339Bean.class 的 jar 文件
		final Path jarPath = tempDir.resolve("test-issue4339.jar");
		final String classEntryName = "cn/hutool/v7/core/reflect/Issue4339Test$Issue4339Bean.class";

		try (final JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
			// 写入目录条目
			jos.putNextEntry(new JarEntry("cn/"));
			jos.closeEntry();
			jos.putNextEntry(new JarEntry("cn/hutool/"));
			jos.closeEntry();
			jos.putNextEntry(new JarEntry("cn/hutool/v7/"));
			jos.closeEntry();
			jos.putNextEntry(new JarEntry("cn/hutool/v7/core/"));
			jos.closeEntry();
			jos.putNextEntry(new JarEntry("cn/hutool/v7/core/reflect/"));
			jos.closeEntry();

			// 写入 class 文件条目
			jos.putNextEntry(new JarEntry(classEntryName));
			try (final InputStream classStream = Issue4339Test.class.getResourceAsStream("Issue4339Test$Issue4339Bean.class")) {
				assertNotNull(classStream, "无法读取 Issue4339Bean 的 class 字节");
				classStream.transferTo(jos);
			}
			jos.closeEntry();
		}

		// 构造 jar: URL 并验证连接缓存会返回同一个 JarFile 实例
		final URL jarUrl = jarPath.toUri().toURL();
		final URL sharedUrl = new URL("jar:" + jarUrl + "!/");

		final JarURLConnection con1 = (JarURLConnection) sharedUrl.openConnection();
		final JarFile sharedJarFile1 = con1.getJarFile();

		final JarURLConnection con2 = (JarURLConnection) sharedUrl.openConnection();
		final JarFile sharedJarFile2 = con2.getJarFile();

		assertSame(sharedJarFile1, sharedJarFile2, "默认缓存下两次 getJarFile() 应返回同一实例");

		URLClassLoader loader = null;
		try {
			// 使用 URLClassLoader 扫描该 jar
			loader = new URLClassLoader(new URL[]{jarUrl}, null);
			final ClassScanner scanner = new ClassScanner("cn.hutool.v7.core.reflect", null);
			scanner.setClassLoader(loader);
			final Set<Class<?>> result = scanner.scan();

			// 断言扫描结果包含 Issue4339Bean
			assertEquals(1, result.size(), "应恰好扫到一个类");
			assertEquals("cn.hutool.v7.core.reflect.Issue4339Test$Issue4339Bean",
				result.iterator().next().getName(),
				"扫到的类名应为 Issue4339Test$Issue4339Bean");

			// 关键断言：共享的 JarFile 仍然可用（未被 scanJar 的 finally 关闭）
			assertTrue(sharedJarFile1.entries().hasMoreElements(),
				"共享 JarFile 调用 entries() 不应抛出异常");
			assertNotNull(sharedJarFile1.getEntry(classEntryName),
				"共享 JarFile 应能正常获取条目");
		} finally {
			// 清理：关闭共享的 JarFile 和 URLClassLoader（Windows 下不关会导致 @TempDir 删不掉）
			if (loader != null) {
				try {
					loader.close();
				} catch (final IOException ignored) {
				}
			}
			try {
				sharedJarFile1.close();
			} catch (final IOException ignored) {
			}
		}
	}
}

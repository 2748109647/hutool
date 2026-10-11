package cn.hutool.core.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cn.hutool.core.io.IoUtil;

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

/**
 * issue#4339 测试：ClassScanner扫描jar后不应关闭共享的JarFile
 */
public class Issue4339Test {

	/**
	 * 用于放入测试jar的静态内部类
	 */
	public static class Issue4339Bean {
	}

	/**
	 * 验证ClassScanner扫描jar后不会关闭连接缓存中共享的JarFile
	 */
	@Test
	public void scanNotCloseSharedJarFileTest(@TempDir Path tempDir) throws Exception {
		// 构建一个包含 Issue4339Bean.class 的 jar 文件
		Path jarPath = tempDir.resolve("test-issue4339.jar");
		try (InputStream classStream = Issue4339Test.class.getResourceAsStream("Issue4339Test$Issue4339Bean.class")) {
			Assertions.assertNotNull(classStream, "无法读取 Issue4339Bean 的 class 字节");
			byte[] classBytes = IoUtil.readBytes(classStream);

			try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
				// 目录条目（每级都要有，类加载器靠目录条目找到包资源）
				jos.putNextEntry(new JarEntry("cn/"));
				jos.closeEntry();
				jos.putNextEntry(new JarEntry("cn/hutool/"));
				jos.closeEntry();
				jos.putNextEntry(new JarEntry("cn/hutool/core/"));
				jos.closeEntry();
				jos.putNextEntry(new JarEntry("cn/hutool/core/lang/"));
				jos.closeEntry();

				// class 条目
				jos.putNextEntry(new JarEntry("cn/hutool/core/lang/Issue4339Test$Issue4339Bean.class"));
				jos.write(classBytes);
				jos.closeEntry();
			}
		}

		// 构造 jar URL，验证默认缓存下两次 getJarFile 返回同一实例
		URL jarUrl = jarPath.toUri().toURL();
		URL sharedUrl = new URL("jar:" + jarUrl + "!/");

		JarURLConnection conn1 = (JarURLConnection) sharedUrl.openConnection();
		JarFile sharedJar1 = conn1.getJarFile();

		JarURLConnection conn2 = (JarURLConnection) sharedUrl.openConnection();
		JarFile sharedJar2 = conn2.getJarFile();

		Assertions.assertSame(sharedJar1, sharedJar2, "默认缓存下两次 getJarFile 应返回同一实例");

		URLClassLoader loader = null;
		try {
			// 用 URLClassLoader 扫描该 jar
			loader = new URLClassLoader(new URL[]{jarUrl}, null);
			ClassScanner scanner = new ClassScanner("cn.hutool.core.lang", null);
			scanner.setClassLoader(loader);
			Set<Class<?>> classes = scanner.scan();

			// 断言扫到了 Issue4339Bean
			Assertions.assertEquals(1, classes.size(), "应恰好扫到一个类");
			Class<?> found = classes.iterator().next();
			Assertions.assertEquals(
				"cn.hutool.core.lang.Issue4339Test$Issue4339Bean",
				found.getName(),
				"扫到的类名应为 Issue4339Test$Issue4339Bean"
			);

			// 关键断言：扫描后共享的 JarFile 仍可正常使用（未被 scanJar 关闭）
			Assertions.assertTrue(sharedJar1.entries().hasMoreElements(),
				"共享 JarFile 的 entries 应仍可遍历");
			Assertions.assertNotNull(
				sharedJar1.getEntry("cn/hutool/core/lang/Issue4339Test$Issue4339Bean.class"),
				"共享 JarFile 应仍能读取条目"
			);
		} finally {
			// 清理：关闭共享 JarFile 和 URLClassLoader（Windows 下不关会导致 @TempDir 删不掉）
			if (sharedJar1 != null) {
				sharedJar1.close();
			}
			if (loader != null) {
				loader.close();
			}
		}
	}
}

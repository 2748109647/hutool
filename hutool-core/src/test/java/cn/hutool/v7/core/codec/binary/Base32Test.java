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

package cn.hutool.v7.core.codec.binary;

import cn.hutool.v7.core.util.ByteUtil;
import cn.hutool.v7.core.util.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base32Test {

	@Test
	public void encodeAndDecodeTest(){
		final String a = "伦家是一个非常长的字符串";
		final String encode = Base32.encode(a);
		assertEquals("4S6KNZNOW3TJRL7EXCAOJOFK5GOZ5ZNYXDUZLP7HTKCOLLMX46WKNZFYWI======", encode);

		String decodeStr = Base32.decodeStr(encode);
		assertEquals(a, decodeStr);

		// 支持小写模式解码
		decodeStr = Base32.decodeStr(encode.toLowerCase());
		assertEquals(a, decodeStr);
	}

	@Test
	public void hexEncodeAndDecodeTest(){
		final String a = "伦家是一个非常长的字符串";
		final String encode = Base32.encodeHex(ByteUtil.toUtf8Bytes(a));
		assertEquals("SIUADPDEMRJ9HBV4N20E9E5AT6EPTPDON3KPBFV7JA2EBBCNSUMADP5OM8======", encode);

		String decodeStr = Base32.decodeStrHex(encode);
		assertEquals(a, decodeStr);

		// 支持小写模式解码
		decodeStr = Base32.decodeStrHex(encode.toLowerCase());
		assertEquals(a, decodeStr);
	}

	@Test
	public void encodeAndDecodeRandomTest(){
		final String a = RandomUtil.randomLettersAndNumbersLower(RandomUtil.randomInt(1000));
		final String encode = Base32.encode(a);
		final String decodeStr = Base32.decodeStr(encode);
		assertEquals(a, decodeStr);
	}

	@Test
	public void decodeTest(){
		final String a = "伦家是一个非常长的字符串";
		final String decodeStr = Base32.decodeStr("4S6KNZNOW3TJRL7EXCAOJOFK5GOZ5ZNYXDUZLP7HTKCOLLMX46WKNZFYWI");
		assertEquals(a, decodeStr);
	}

	@Test
	public void rfc4648VectorsTest(){
		// RFC 4648 第 10 节的 Base32 测试向量
		assertEquals("", Base32.encode(""));
		assertEquals("MY======", Base32.encode("f"));
		assertEquals("MZXQ====", Base32.encode("fo"));
		assertEquals("MZXW6===", Base32.encode("foo"));
		assertEquals("MZXW6YQ=", Base32.encode("foob"));
		assertEquals("MZXW6YTB", Base32.encode("fooba"));
		assertEquals("MZXW6YTBOI======", Base32.encode("foobar"));

		assertArrayEquals("f".getBytes(), Base32.decode("MY======"));
		assertArrayEquals("foobar".getBytes(), Base32.decode("MZXW6YTBOI======"));
	}

	@Test
	public void decodeSingleCharTest(){
		// 单字符输入的输出缓冲区长度为 0（5bit 不足以组成一个字节），解码不应越界，应返回空数组
		assertArrayEquals(new byte[0], Base32.decode("M"));
		assertArrayEquals(new byte[0], Base32.decode("M="));
		assertArrayEquals(new byte[0], Base32.decode("M======="));
		// 小写同上
		assertArrayEquals(new byte[0], Base32.decode("m"));
	}
}

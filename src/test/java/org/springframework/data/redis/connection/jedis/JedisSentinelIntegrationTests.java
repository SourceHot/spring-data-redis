/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.connection.jedis;

import static org.assertj.core.api.Assertions.*;

import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.SettingsUtils;
import org.springframework.data.redis.connection.AbstractConnectionIntegrationTests;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.jedis.extension.JedisConnectionFactoryExtension;
import org.springframework.data.redis.test.condition.EnabledOnRedisSentinelAvailable;
import org.springframework.data.redis.test.extension.RedisSentinel;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Christoph Strobl
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@ExtendWith(JedisConnectionFactoryExtension.class)
@EnabledOnRedisSentinelAvailable
public class JedisSentinelIntegrationTests extends AbstractConnectionIntegrationTests {

	private static final RedisServer SLAVE_0 = new RedisServer("127.0.0.1", 6380);
	private static final RedisServer SLAVE_1 = new RedisServer("127.0.0.1", 6381);

	public JedisSentinelIntegrationTests(@RedisSentinel JedisConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	@Test
	public void testEvalReturnSingleError() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> connection.eval("return redis.call('expire','foo')", ReturnType.BOOLEAN, 0));
	}

	@Test
	public void testEvalArrayScriptError() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(super::testEvalArrayScriptError);
	}

	@Test
	public void testEvalShaNotFound() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> connection.evalSha("somefakesha", ReturnType.VALUE, 2, "key1", "key2"));
	}

	@Test
	public void testEvalShaArrayError() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(super::testEvalShaArrayError);
	}

	@Test
	public void testRestoreBadData() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(super::testRestoreBadData);
	}

	@Test
	public void testRestoreExistingKey() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(super::testRestoreExistingKey);
	}

	@Test
	public void testExecWithoutMulti() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(super::testExecWithoutMulti);
	}

	@Test
	public void testErrorInTx() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(super::testErrorInTx);
	}

	@Test // DATAREDIS-330
	void shouldReadMastersCorrectly() {

		List<RedisServer> servers = (List<RedisServer>) connectionFactory.getSentinelConnection().masters();
		assertThat(servers).hasSize(1);
		assertThat(servers.get(0).getName()).isEqualTo(SettingsUtils.getSentinelMaster());
	}

	@Test // DATAREDIS-330
	void shouldReadSlavesOfMastersCorrectly() {

		RedisSentinelConnection sentinelConnection = connectionFactory.getSentinelConnection();

		List<RedisServer> servers = (List<RedisServer>) sentinelConnection.masters();
		assertThat(servers).hasSize(1);

		Collection<RedisServer> slaves = sentinelConnection.slaves(servers.get(0));
		assertThat(slaves).hasSize(2).contains(SLAVE_0, SLAVE_1);
	}

	@Test // DATAREDIS-552
	void shouldSetClientName() {

		RedisSentinelConnection sentinelConnection = connectionFactory.getSentinelConnection();
		Jedis jedis = (Jedis) ReflectionTestUtils.getField(sentinelConnection, "jedis");

		assertThat(jedis.clientGetname()).isEqualTo("jedis-client");
	}

}

/*-
 * #%L
 * HAPI FHIR JPA Server - HFQL Driver
 * %%
 * Copyright (C) 2014 - 2023 Smile CDR, Inc.
 * %%
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
 * #L%
 */
package ca.uhn.fhir.jpa.fql.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

class JdbcConnection implements Connection {
	private final String myServerUrl;
	private boolean myClosed;
	private HfqlRestClient myClient;
	private String myUsername;
	private String myPassword;

	public JdbcConnection(String theServerUrl) {
		myServerUrl = theServerUrl;
	}

	@Override
	public Statement createStatement() {
		return new JdbcStatement(this);
	}

	@Override
	public PreparedStatement prepareStatement(String sql)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public CallableStatement prepareCall(String sql)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public String nativeSQL(String sql)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAutoCommit(boolean autoCommit)  {
		// nothing
	}

	@Override
	public boolean getAutoCommit()  {
		return false;
	}

	@Override
	public void commit() {
		// nothing
	}

	@Override
	public void rollback()  {
		// nothing
	}

	@Override
	public void close() {
		myClosed = true;
	}

	@Override
	public boolean isClosed() {
		return myClosed;
	}

	@Override
	public DatabaseMetaData getMetaData() {
		return new JdbcDatabaseMetadata(this, getClient());
	}

	@Override
	public void setReadOnly(boolean readOnly)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void setCatalog(String catalog)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getCatalog()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTransactionIsolation(int level)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTransactionIsolation()  {
		return Connection.TRANSACTION_READ_COMMITTED;
	}

	@Override
	public SQLWarning getWarnings()  {
		return null;
	}

	@Override
	public void clearWarnings() {
		// nothing
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Class<?>> getTypeMap() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setHoldability(int holdability)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getHoldability()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public Savepoint setSavepoint()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public Savepoint setSavepoint(String name)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public void rollback(Savepoint savepoint)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			 {
		throw new UnsupportedOperationException();
	}

	@Override
	public PreparedStatement prepareStatement(
			String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public CallableStatement prepareCall(
			String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public Clob createClob()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public Blob createBlob()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public NClob createNClob()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public SQLXML createSQLXML()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isValid(int timeout)  {
		return true;
	}

	@Override
	public void setClientInfo(String name, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setClientInfo(Properties properties) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getClientInfo(String name)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public Properties getClientInfo()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSchema(String schema)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSchema()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public void abort(Executor executor)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNetworkTimeout()  {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap(Class<T> theInterface)  {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrapperFor(Class<?> theInterface)  {
		throw new UnsupportedOperationException();
	}

	public HfqlRestClient getClient() {
		if (myClient == null) {
			myClient = new HfqlRestClient(myServerUrl, myUsername, myPassword);
		}
		return myClient;
	}

	public void setUsername(String theUsername) {
		myUsername = theUsername;
	}

	public void setPassword(String thePassword) {
		myPassword = thePassword;
	}
}

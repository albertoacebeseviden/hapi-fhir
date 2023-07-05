/*-
 * #%L
 * HAPI FHIR JPA Server - Firely Query Language
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

import ca.uhn.fhir.jpa.fql.executor.IHfqlExecutionResult;
import ca.uhn.fhir.jpa.fql.util.HfqlConstants;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

class JdbcStatement implements Statement {
	private int myMaxRows;
	private final JdbcConnection myConnection;

	private int myFetchSize;
	private JdbcResultSet myResultSet;

	public JdbcStatement(JdbcConnection theConnection) {
		myConnection = theConnection;
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		execute(sql);
		return getResultSet();
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		throw new SQLException();
	}

	@Override
	public void close() throws SQLException {
		// ignored
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return 0;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		// ignored
	}

	@Override
	public int getMaxRows() {
		return myMaxRows;
	}

	@Override
	public void setMaxRows(int theMaxRows) {
		myMaxRows = theMaxRows;
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		// ignored
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return 0;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		// ignored
	}

	@Override
	public void cancel() throws SQLException {
		// ignored
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException {
		// ignored
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		// ignored
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		if (getFetchSize() > 0) {
			// FIXME: move to client#execute method
			myConnection.getClient().setFetchSize(getFetchSize());
		}

		Integer limit = null;
		if (getMaxRows() > 0) {
			limit = getMaxRows();
		}

		Parameters input = new Parameters();
		input.addParameter(HfqlConstants.PARAM_ACTION, new CodeType(HfqlConstants.PARAM_ACTION_SEARCH));
		input.addParameter(HfqlConstants.PARAM_QUERY, new StringType(sql));
		if (limit != null) {
			input.addParameter(HfqlConstants.PARAM_LIMIT, new IntegerType(limit));
		}
		input.addParameter(HfqlConstants.PARAM_FETCH_SIZE, new IntegerType(myFetchSize));

		IHfqlExecutionResult result = myConnection.getClient().execute(input, true);

		myResultSet = new JdbcResultSet(result, this);
		return true;
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return myResultSet;
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return 0;
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		// ignored
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return ResultSet.FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int theFetchSize) throws SQLException {
		myFetchSize = theFetchSize;
	}

	@Override
	public int getFetchSize() throws SQLException {
		return myFetchSize;
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public int getResultSetType() throws SQLException {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public void addBatch(String sql) throws SQLException {
		throw new SQLException();
	}

	@Override
	public void clearBatch() throws SQLException {
		throw new SQLException();
	}

	@Override
	public int[] executeBatch() throws SQLException {
		return new int[0];
	}

	@Override
	public Connection getConnection() throws SQLException {
		return myConnection;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		throw new SQLException();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		throw new SQLException();
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLException();
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLException();
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		throw new SQLException();
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		throw new SQLException();
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		throw new SQLException();
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		throw new SQLException();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return false;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		// ignored
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return false;
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		// ignored
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}
}
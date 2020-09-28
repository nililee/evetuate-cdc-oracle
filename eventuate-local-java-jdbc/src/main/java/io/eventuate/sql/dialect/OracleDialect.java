package io.eventuate.sql.dialect;

import org.springframework.core.Ordered;

public class OracleDialect extends DefaultEventuateSqlDialect implements Ordered {

	@Override
	public boolean supports(String driver) {
		return "oracle.jdbc.OracleDriver".equals(driver);
	}

	// another approach if the below sql doesn't work:
	// SELECT (SYSDATE - TO_DATE('01-01-1970 00:00:00', 'DD-MM-YYYY HH24:MI:SS')) * 24 * 60 * 60 * 1000 FROM DUAL
	@Override
	public String getCurrentTimeInMillisecondsExpression() {
		return "SELECT (SYSDATE - CAST(TO_TIMESTAMP_TZ('01-01-1970 00:00:00+00:00','DD-MM-YYYY HH24:MI:SS TZH:TZM') "
				+ "as date)) * 24 * 60 * 60 * 1000 FROM DUAL";
	}

	@Override
	public String addLimitToSql(String sql, String limitExpression) {
		return String.format("%s FETCH NEXT %s ROWS ONLY", sql, limitExpression);
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE;
	}

	@Override
	public String jsonColumnToString(Object object, EventuateSchema eventuateSchema, String unqualifiedTable,
			String column, EventuateJdbcStatementExecutor eventuateJdbcStatementExecutor) {
		if (object instanceof String)
			return (String) object;
		else if (object instanceof SerialClob)
			return clobToString((SerialClob) object);
		else
			throw new IllegalArgumentException(String.format("Unsupported oracle type of column %s", column));
	}

	private String clobToString(SerialClob clob) {
		StringBuilder sb = new StringBuilder();
		try {
			Reader reader = clob.getCharacterStream();
			BufferedReader br = new BufferedReader(reader);
			String line;
			while (null != (line = br.readLine())) {
				sb.append(line);
			}
			br.close();
		} catch (SQLException | IOException e) {
			throw new RuntimeException("Cannot read CLOB: " + e.getMessage());
		}
		return sb.toString();
	}
}

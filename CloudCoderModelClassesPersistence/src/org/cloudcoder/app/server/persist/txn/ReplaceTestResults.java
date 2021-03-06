// CloudCoder - a web-based pedagogical programming environment
// Copyright (C) 2011-2013, Jaime Spacco <jspacco@knox.edu>
// Copyright (C) 2011-2013, David H. Hovemeyer <david.hovemeyer@gmail.com>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.cloudcoder.app.server.persist.txn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.cloudcoder.app.server.persist.util.AbstractDatabaseRunnableNoAuthException;
import org.cloudcoder.app.shared.model.TestResult;

/**
 * Transaction to replace {@link TestResult}s associated with a submission.
 * (Useful if a submission needs to be retested.)
 */
public class ReplaceTestResults extends
		AbstractDatabaseRunnableNoAuthException<Boolean> {
	private final int submissionReceiptId;
	private final TestResult[] testResults;

	/**
	 * Constructor.
	 * 
	 * @param submissionReceiptId the event id of the submission receipt
	 * @param testResults         the test results that should replace the old test results
	 */
	public ReplaceTestResults(int submissionReceiptId,
			TestResult[] testResults) {
		this.submissionReceiptId = submissionReceiptId;
		this.testResults = testResults;
	}

	@Override
	public Boolean run(Connection conn) throws SQLException {
		// Delete old test results (if any)
		PreparedStatement delTestResults = prepareStatement(
				conn,
				"delete from " + TestResult.SCHEMA.getDbTableName() + " where submission_receipt_event_id = ?");
		delTestResults.setInt(1, submissionReceiptId);
		delTestResults.executeUpdate();
		
		// Insert new test results
		Queries.doInsertTestResults(testResults, submissionReceiptId, conn, this);
		
		return true;
	}

	@Override
	public String getDescription() {
		return "store test results";
	}
}
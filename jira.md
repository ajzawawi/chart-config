Feature: Pipeline Agent – file ingest, validation, upload, and archival

Scenario: Startup creates folders and processes backlog

  Given a valid HOCON config with one or more Sources (inbox/output/archive/s3)
  When the agent starts
  Then it creates missing directories for each Source
  And registers a watcher on each inbox
  And processes any pre-existing candidate files in the inbox exactly once (oldest first)

Scenario: Ignore unsupported files
  Given a Source configured with allowedExtensions [".txt"]
  When a file with an unsupported extension appears in the inbox
  Then the file is ignored and no outputs are produced

Scenario: Accept file when file-level validations pass
  Given a text file where
    And the first non-empty line matches "<headerPrefix> YYYYDDD"
    And the second non-empty line matches expectedColumns using columnDelimiter
    And the last non-empty line matches "<trailerPrefix> <count>"
    And <count> equals the number of data rows between header row and trailer (blank lines ignored)
  When the agent validates the file
  Then the file is accepted

Scenario: Reject when header control is missing
  Given a file whose first non-empty line does not match "<headerPrefix> YYYYDDD"
  When the agent validates the file
  Then the file is rejected
  And a reject report is written to the reject folder
  And the original file is gzip-archived under archive/rejected
  And nothing is uploaded to MinIO

Scenario: Reject when column header row is missing or mismatched
  Given a file where the second non-empty line is missing or does not match expectedColumns
  When the agent validates the file
  Then the file is rejected (or flagged with header mismatch)
  And a reject report is written
  And the original is gzip-archived under archive/rejected
  And nothing is uploaded to MinIO

Scenario: Reject when trailer line is missing or malformed
  Given a file whose last non-empty line is missing or not "<trailerPrefix> <digits>"
  When the agent validates the file
  Then the file is rejected
  And a reject report is written
  And the original is gzip-archived under archive/rejected
  And nothing is uploaded to MinIO

Scenario: Reject when trailer count mismatches actual data rows
  Given a file where "<trailerPrefix> <count>" does not equal actual data-row count
  When the agent validates the file
  Then the file is rejected
  And a reject report is written
  And the original is gzip-archived under archive/rejected
  And nothing is uploaded to MinIO

Scenario: Row-level validation produces clean CSV and row-errors TSV
  Given an accepted file and configured RowRules (delimiter, expectedCols, per-column rules)
  When the agent validates each data row
  Then valid rows are written to a clean CSV in output/clean (with optional header)
  And invalid rows are excluded from the clean CSV
  And each invalid row is recorded in an errors TSV in output/errors with row number, column index, code, and message

Scenario: Upload only clean results to MinIO
  Given a successfully written clean CSV for an accepted file
  When uploading to MinIO
  Then only the clean CSV is uploaded
  And object keys are generated per Source using the configured scheme (e.g., prefix/template)
  And uploaded objects are tagged with at least outType=clean and source=<SourceName> (if tagging enabled)

Scenario: Archive original only after successful upload
  Given an accepted file
  When the clean CSV upload to MinIO succeeds
  Then the original input file is gzip-archived under archive/accepted
  And the inbox no longer contains the original file

Scenario: Do not archive original if upload fails
  Given an accepted file
  When the clean CSV upload to MinIO fails
  Then the original input file is not archived to accepted/rejected
  And the failure is logged
  And the file is handled by the failure path (e.g., archived under archive/failed)

Scenario: Rejected files are archived without upload
  Given a rejected file
  When post-processing runs
  Then no objects are uploaded to MinIO
  And the original file is gzip-archived under archive/rejected
  And a reject report exists locally

Scenario: Concurrency handles bursts without dropping files
  Given a Source configured with a bounded worker pool and queue
  When many candidate files arrive in a short burst
  Then the watcher remains responsive
  And files are queued and processed up to the configured concurrency
  And all queued files are processed exactly once

Scenario: Graceful shutdown
  Given the agent is running
  When it receives a shutdown signal
  Then all watchers are stopped
  And in-flight tasks are allowed to finish (or shut down gracefully)
  And the agent exits cleanly

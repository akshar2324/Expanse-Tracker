# Bank statement sample sources

Researched: 2026-11-06. This is a source catalog, not downloaded statements or a claim of parser support.

## Fictional statement with matching exports

- Source: https://getutilly.com/guides/sample-bank-statement-pdf
- PDF link advertised by the source: https://getutilly.com/sample/bank.pdf
- CSV link advertised by the source: https://getutilly.com/sample/bank/csv
- The page also offers Excel, QBO, OFX, QIF, IIF, and Xero exports.
- The publisher explicitly describes the bank/customer as fictional. This is a US-style sample, not an Indian bank statement.
- Published validation targets: 31 transactions; opening balance USD 12418.36; credits USD 29597.43; debits USD 28575.27; closing balance USD 13440.52.
- Use: compare extraction across PDF and CSV, reconcile balances, test separate withdrawal/deposit columns.
- Verification: source page inspected. Direct PDF/CSV fetches failed in the web tool; file contents have not been validated or downloaded locally.

## Indian bank-style CSV fixtures

- Repository: https://github.com/Siddharthk17/TEAM-ATROPOS
- Samples directory: https://github.com/Siddharthk17/TEAM-ATROPOS/tree/main/samples
- The README lists hdfc_statement.csv, sbi_statement.csv, and icici_statement.csv as demo samples.
- Advertised layouts: HDFC Txn Date with Debit/Credit; SBI Transaction Date with Withdrawal (Dr)/Deposit (Cr); ICICI Value Date with Amount (INR).
- Use: candidate fixtures for column mapping and bank-layout variations.
- Verification: repository README inspected; samples-directory fetch failed. Individual sample contents, redaction, license, and exact headers have not been verified. Do not treat these as bank-issued examples or copy the repository's runtime services into AkSpend.

## Official bank layout reference

- Publisher: Royal Bank of Scotland.
- PDF: https://www.rbs.co.uk/content/dam/cwf/rbs/downloads/global/docs/rbs05996.pdf
- The Revolve account guide includes an Understanding your statement section and an illustrated statement.
- Use: document-layout reference. This is a guide, not a standalone transaction fixture or an Indian bank format.
- Verification: PDF opened and guide contents inspected through the web tool; not downloaded locally.

## Additional references

- https://irecord.regnum.co.in/import-templates.html
  Third-party import-template catalog describing bank templates. These are software import templates, not verified bank exports. Individual downloads were not validated.
- https://github.com/MalayPalace/Bank-Statement-Utility
  Parser project describing HDFC, Kotak, SBI, BOB, and IDBI support. Search result inspected only; no sample-file availability verified.
- https://github.com/emilgeo/india2actual
  Candidate parser reference for Indian PDF/CSV/Excel formats. Search result inspected only; no sample files verified.

## Test cases to collect next

- Separate debit/credit columns and signed amount columns.
- Amount plus explicit DR/CR marker.
- dd/MM/yyyy, dd-MM-yyyy, dd-MMM-yyyy, ISO dates, and ambiguous dates.
- Indian digit grouping, quoted commas, blank cells, zeroes, CR balances, and negative balances.
- Opening/closing balance rows that must not become transactions.
- Statement headers, footers, repeated page headers, multiline narrations, and reverse chronological order.
- UPI, NEFT, IMPS, RTGS, cash movement, card repayment, fees, interest, refunds, and reversals.
- Duplicate exports, overlapping periods, and matching transfers across accounts.
- Credit card statements with purchases, repayments, refunds, minimum due, and total due.
- Text PDF, scanned PDF, and password-protected PDF as separate capabilities. Finding a sample does not establish AkSpend support for these.

Use fictional or intentionally anonymized fixtures. Label generated bank-style samples as synthetic and record their expected transaction count and balance reconciliation.

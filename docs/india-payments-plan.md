# AkSpend: Indian spending and payment tracking plan

Research date: 27 November 2026. Status: proposed product and implementation plan; no app behavior changed.

## 1. Product direction

Make AkSpend a single offline record of money available, money spent, money owed, and upcoming payments across Indian payment methods. Keep recording usable without notification access, bank connectivity, or internet. Payment execution and live bank/biller connections are outside this plan because they conflict with the repository's offline product requirements.

The central design rule: separate **account/funding source**, **payment method**, **payment app**, and **purpose**. A purchase can be Groceries, funded by a RuPay credit card, paid through UPI, using PhonePe. It is one purchase.

## 2. What the research supports

| Evidence | Product implication |
| --- | --- |
| A 2024 Indian UPI study received 276 responses, retained 235 valid responses, and interviewed 20 people. Participants described reviewing transaction histories and using notes, spreadsheets, split-expense tools, and finance apps. [Study, sections 3 and 5](https://arxiv.org/html/2401.09937v3) | Combine an understandable activity history, quick manual entry, imports, and shared-expense records. |
| The same study reports frequent self-reported overspending and demand for category/time summaries. Its sample leaned toward students, professionals, and men; it is not representative of all Indians or causal evidence. [Study](https://arxiv.org/html/2401.09937v3) | Prioritize a daily total and useful budgets; validate needs with broader users before making nationwide claims. |
| NPCI documents UPI Lite as an on-device balance and permits multiple balances across UPI apps. [NPCI launch circular, historical design](https://www.npci.org.in/uploads/UPI_OC_138_Introduction_of_On_Device_wallet_UPI_Lite_for_Small_Value_Transactions_a0930bbc3d.pdf) | Model each actual Lite balance separately; a top-up moves money rather than consuming it. |
| SBI's FAQ says Lite purchases do not appear individually in the bank statement; loads do. Some limits on that FAQ are outdated. [SBI FAQ](https://sbi.bank.in/web/customer-care/upi1) | Bank imports alone cannot recover every Lite purchase. Provide quick entry and a reconciliation workflow. |
| RBI's December 2024 amendment sets UPI Lite limits at ₹1,000 per payment and ₹5,000 balance. [RBI amendment](https://www.rbi.org.in/Scripts/NotificationUser.aspx?Id=12752&Mode=0) | Do not hard-code provider limits as ledger validation. Historical records and provider variations must remain recordable. |
| NPCI supports RuPay credit cards on UPI for merchant payments, with restrictions on other transaction types. [NPCI](https://www.npci.org.in/product/rupay/credit-card-on-upi) | A UPI label must never imply that a bank deposit funded the payment. |
| RBI permits pre-sanctioned bank credit lines through UPI. [RBI directive, updated February 2025](https://www.rbi.org.in/scripts/NotificationUser.aspx?Id=12532&Mode=0) | Credit line is a liability account; UPI is its payment method. |
| UPI Circle allows an authorized secondary person to spend from the primary person's account. [NPCI](https://www.npci.org.in/product/upi-circle) | Record who paid/initiated and whose account funded it, without duplicating the household expense. |
| Bharat Connect lists utilities, rent, recharge, transport, insurance, education, subscriptions, loan and card repayments. [Official categories](https://www.bharat-connect.com/customers/) | A bills hub needs different accounting treatments, rather than making every bill an expense. |
| NCMC can include a stored-value offline wallet as well as debit-card functionality. [IOB FAQ](https://www.iob.bank.in/en/Contact-Less-Cards-NCMC-Cards-FAQs) | Keep transit stored value separate from the linked bank balance. |
| Google Play restricts SMS permissions; money-management use is a listed exception subject to declaration and policy requirements. [Google Play policy](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en) | Preserve paste/share and file imports; do not promise unrestricted background SMS access. |

These sources establish payment mechanics and some observed habits. They do not establish how every Indian keeps records, nor a reliable percentage using notebooks versus apps. The workflow priorities below are recommendations to validate.

## 3. Complete coverage map

P1 = first usable release after accounting foundation; P2 = next release; P3 = advanced coverage.

| Area | What users can record | Phase |
| --- | --- | --- |
| Cash | Cash on hand, purchases, salary, withdrawals, deposits, cash discrepancies | P1 |
| Bank accounts | Multiple savings/current accounts, opening balances, salary, interest, fees, transfers | P1 |
| UPI | Bank-funded purchases, person transfers, reference number, merchant, app used | P1 |
| UPI Lite / Lite X | Separate balances by actual wallet/app, loads, auto-load records, purchases, closure returns, missing entries | P1 for Lite; P3 for explicit Lite X label |
| Debit cards | Card identity/last four, linked bank, online/POS/contactless usage; no duplicate card balance | P1 |
| Credit cards | RuPay/Visa/Mastercard/Amex/etc. labels, purchases, outstanding, statements, due dates, repayments, refunds, fees | P1 |
| RuPay card through UPI | Same credit account; UPI method; optional app label | P1 |
| Bank transfers | IMPS, NEFT, RTGS, net banking; own-account transfer versus expense/payment to someone else | P1 |
| Wallets / prepaid | Actual prepaid balances, loads, spends, refunds, restricted balance notes | P1 basic; P2 restrictions |
| Bills | Electricity, water, gas/LPG, rent, society fees, broadband, mobile, DTH, education, insurance, taxes, donations | P2 |
| Auto-debits | UPI AutoPay, NACH/ECS, card standing instructions; schedules, expected amounts, confirmation and reminders | P2 |
| FASTag | Separate stored balance where applicable, tolls, top-ups, refunds, fees, refundable deposits | P2 |
| Metro / NCMC / transit cards | Wallet top-ups, individual rides, passes, pending loads, balance reconciliation | P2 basic; P3 complex offline settlement |
| Loans / EMI / BNPL | Borrowing, outstanding principal, schedules, principal/interest/fees/tax split, partial/early payment | P2 |
| Credit line / overdraft on UPI | Draws/purchases, repayments, limits, statement balance and charges | P2 |
| Friends / family / udhaar | Lent/borrowed money, split bills, unequal shares, partial settlements, reimbursements | P2 |
| UPI Circle / household | Funding owner, spender/member, personal versus household scope; all stored locally | P2 |
| Gift / meal / store cards | Purchased or received balances, restrictions, expiry, partial redemption, split tender | P3 |
| Cheque / DD / AePS / USSD / UPI 123PAY | Method labels, reference, pending/cleared/bounced state, bank/cash effect | P3 |
| Digital rupee / e-RUPI | Optional distinct wallet or restricted voucher tracking; no payment integration | P3 |
| Savings / investments | FD/RD, SIP, PPF/NPS and broker contributions as asset moves; withdrawals, fees, income distinguished | P3; recording only |
| Foreign payments | Settled INR amount first; optional original amount/currency, user-entered exchange rate, markup and tax | P3 |

An app name is optional metadata. For example, an app's UPI interface and its prepaid wallet are different things. Only create a balance when the user actually holds money or debt there.

## 4. Accounting rules and concrete examples

Use positive integer paise for amounts, with explicit posting direction and asset/liability account roles. Show credit-card outstanding as money owed and keep available credit out of available cash.

| Event | Correct result |
| --- | --- |
| Load ₹500 into Lite; buy tea for ₹20 | Bank −₹500, Lite +₹480; spending ₹20. |
| Withdraw ₹2,000 at ATM; spend ₹300 cash | Bank −₹2,000, cash +₹1,700; spending ₹300. ATM fee, if any, is a separate expense. |
| Buy ₹1,200 using RuPay credit via UPI | Card owed +₹1,200; spending +₹1,200; bank unchanged. |
| Repay that ₹1,200 card bill | Bank −₹1,200; card owed −₹1,200; no new spending. |
| Receive ₹200 refund for the purchase | Reduce linked category spending and card liability by ₹200; preserve purchase and refund history. |
| Move ₹5,000 between own banks | Balances change; income and spending do not. |
| Pay a ₹1,000 dinner; own share ₹400 | Spending ₹400, receivable ₹600; repayment clears receivable rather than becoming salary/income. |
| Borrow ₹10,000 | Bank +₹10,000, loan owed +₹10,000; income unchanged. |
| Pay EMI ₹3,000 comprising ₹2,500 principal and ₹500 interest | Bank −₹3,000; debt −₹2,500; expense ₹500. Show the full ₹3,000 in cash-outflow/commitment views. |
| Convert a previously recorded purchase to EMI | Reclassify/restructure the debt; never record the purchase a second time. |
| Failed payment with no debit | No balance or spending effect. |
| Failed payment where bank was debited, reversal pending | Reflect observed debit against a recovery/clearing balance; no completed purchase expense; clear when returned. |
| Pay ₹600 using ₹100 wallet and ₹500 card | One ₹600 purchase, two funding postings. |
| Put ₹2,000 into an owned investment account | Asset transfer, distinct from consumption; budget can optionally track saving contributions separately. |

Additional rules:

- Loan proceeds and card payments never become normal income/expenses merely because imports say credit/debit.
- Category refunds default to the refund date, with an explicit link to the original expense and clear gross/refund/net reporting. Closed historical months do not silently change.
- Cashback received as real money is distinguished from promotional points and purchase discounts. Choose and document treatment; do not count points as spendable cash.
- Pending authorizations affect a separate reserved/available estimate; posted settlement replaces the authorization rather than adding another purchase.
- Reconciliation differences are explicit adjustments with reason and date, never silently invented expenses.
- Cheques post only when cleared, or use an explicit clearing account if recording an observed bank debit.
- Recurring schedules create expected bills/drafts; actual confirmation/import creates the posted payment. Match the two to prevent duplicates.
- Forecasts distinguish consumption, bank cash flow, liabilities, and future commitments, including EMI principal and card repayments only once.

## 5. Screens and everyday workflows

Keep primary navigation compact: Home, Activity, Accounts, Plan, More. Put specialist features inside the relevant account or Plan screen.

**Home:** money available across cash/bank/spendable wallets; card/loan amounts owed separately; today's and month's spending; bills due; imports awaiting review. Label balances as recorded and show the last reconciliation date. Do not present a guessed safe-to-spend figure as a bank balance.

**Quick add:** amount → expense/income/transfer/refund → funding account → category. Default date to today. Remember recent choices without overriding explicit input. Offer a cash shortcut and account-specific Lite shortcut. Advanced fields reveal method, app, reference, person, split funding and notes only when needed.

**Accounts:** group Cash, Banks, Stored balances, and Credit & loans. Debit cards sit under their linked bank; credit cards have their own liability balance. Optional setup presets include UPI Lite, prepaid wallet, FASTag, and transit card.

**Card detail:** current recorded outstanding, limit and estimated utilization, statement history, total/minimum due entered or imported, due date, billed/unbilled transactions, repayments and refunds. Issuer statement is authoritative: do not calculate minimum due, interest-free eligibility, interest or payment allocation from guessed universal rules. Allow overpaid cards to have a credit balance and support shared issuer limits.

**Lite/wallet detail:** last checked balance, loads, purchases, returns and a missing-spend prompt. If only top-ups were imported, show incomplete coverage. Offer either individual purchase entries or an explicit unallocated spending adjustment, and match later imports against that adjustment to avoid counting twice.

**Plan:** bills, subscriptions, EMI schedules, household/people dues, category budgets and savings commitments. Local reminders must tolerate Android delivery limitations and remain visible inside the app.

**Activity and review:** filters by funding account, method, app, category, person, status, reference and date. Explain suggested matches for bank SMS + app notification + statement entries. Matching by amount/date alone is a suggestion, not proof: two identical tea purchases must survive.

**Reports:** spending by category and method, actual cash flow, asset/liability balances and debt trend. A date/coverage indicator explains missing imports. Support Indian number grouping, paise, month and April–March financial-year filters; never label the report a tax filing.

## 6. Import and privacy design

- Reuse existing on-device SMS paste/share, optional notification review and CSV preview.
- No universal bank/API feed is assumed. Manual entry and file imports cover methods without usable notifications, including Lite and offline transit.
- Normalize observations into drafts with account hint, amount, direction, event time, source, reference if present, and match confidence.
- Group observations of the same event. Support debits and credits on different dates for transfers and repayments.
- Support pending → posted → reversed updates without multiplying balance effects.
- Let users confirm or correct ambiguous transfers, refunds, liability funding, and partial payments before posting.
- Retain necessary structured provenance and deduplication identifiers only; preserve the existing policy of not saving raw message text.
- No OTP, PIN, CVV, login credentials, or full card number. Last four digits and a user label are sufficient for identification.
- File imports, migrations and export/restore stay off the main thread. Financial data remains on-device unless explicitly exported/shared.
- PDF statement parsing and on-device receipt OCR are later feasibility work, not promised P1 features. Passwords remain ephemeral; no upload service.

## 7. Existing implementation and gaps

Repository inspection found:

- Existing accounts: Cash, Bank, Wallet, Savings and Credit Card, currently selected as string types.
- Transactions already have account, payment method and paired transfer IDs. Transfer writes are atomic.
- Existing manual entry, notification/SMS review, CSV import, category memory, budgets, recurring transactions, simple debts, reconciliation and backups.
- `Models.kt` persists money as `Double`; account records have no card cycle/limit or wallet linkage metadata.
- Recurring transactions do not currently carry an account field. Debts have an amount and resolved flag, without a partial-settlement ledger.
- A Credit Card account label does not establish a statement/repayment/EMI subsystem. These need dedicated behavior and tests.

Relevant implementation areas: `data/model/Models.kt`, `data/db/AppDatabase.kt`, `data/db/FinanceDao.kt`, `data/repository/FinanceRepository.kt`, `data/backup/BackupCodec.kt`, `sms/`, `ui/viewmodel/`, account/transaction screens, and `insights/`.

## 8. Implementation sequence

### P0 — financial foundation

1. Write balance/sign/reporting contracts and acceptance fixtures before schema changes.
2. Move persisted money to `Long` paise across transactions, opening balances, budgets, goals, recurring amounts, debts and reconciliation. Parse decimal text with `BigDecimal`; use explicit HALF_UP rounding only at documented legacy/import boundaries. Reject fractional paise in normal entry, overflow and non-finite legacy values.
3. Add stable account and method identifiers, preserve legacy display labels/custom methods, and add optional account linkage/card metadata. Debit cards are instruments linked to assets; credit cards/loans are liabilities.
4. Introduce a logical event with balanced postings and category allocations, supporting transfer, refund, split funding, receivable, liability and clearing entries. Keep that complexity out of ordinary entry screens. Migrate existing paired transfers without double-posting; retain old rows/IDs through a compatibility layer until all readers migrate.
5. Add data migrations and a versioned backup upgrade path. Preserve legacy amounts after documented rounding; do not infer debt signs from the Credit Card label alone. Ask for a reconciliation when legacy intent is ambiguous.
6. Update reports, widgets, exports, search, forecasting and import paths to the same financial contract.

Exit: opening balance + postings reconciles for assets and liabilities; old backups restore; transfers stay out of income/spending; no money path falls back to floating point arithmetic.

### P1 — cash, UPI, Lite and credit cards

1. Deliver funding-account/method/app separation with compatible existing records.
2. Add Lite wallet presets, loads/returns, purchases and explicit incomplete-data handling.
3. Add credit-card outstanding, statements, recorded total/minimum due, due-date reminders, repayments and linked refunds.
4. Correct RuPay-on-UPI funding and recognize internal bank/card/wallet movements in import review.
5. Refresh Home and Accounts; improve quick add and transaction matching.

Exit: the Lite, ATM, RuPay, refund and repayment examples in section 4 pass through manual entry, imports, reports, export/restore and account reconciliation.

### P2 — bills, commitments and shared money

1. Extend recurring schedules with funding account, mandate metadata and expected-versus-confirmed state.
2. Add local bill/subscription hub, due calendar and forecast commitments.
3. Add FASTag/transit balances and reconciliation.
4. Extend debts into partial settlements and shared bills; add household ownership/spender fields for Circle-style records.
5. Add loan/BNPL/credit-line balances, EMI principal/interest splits and conversion of existing card purchases.

Exit: cash flow includes future repayments without double-counting consumption; split bills and partial settlements reconcile exactly.

### P3 — advanced coverage

Add gift/meal cards and expiry, voucher records, digital rupee, cheque clearing, optional uncommon method labels, investment transfers, and original-currency metadata. Evaluate offline PDF/OCR after a sanitized fixture study. Do not add live payment execution, cloud sync, credential collection or telemetry.

Do not assign calendar estimates until P0 migration and import feasibility are scoped. Ship each phase as focused, independently reviewable changes.

## 9. Validation plan

- Financial tests: paise precision, Indian grouping/locale input, negative and zero handling, upper bounds, transfers, liability signs, refunds across statement/month boundaries, credit balances, split tender and allocation rounding.
- Import tests: duplicate SMS/app/CSV observations, repeated same-amount purchases, missing references, ambiguous funding, delayed settlement, partial refunds, reversals and pending debit recovery.
- Card/loan tests: partial and excess repayments, shared limits, statement boundary dates, February/month-end due dates, fees/tax splits and EMI conversion without repeated expense.
- Persistence tests: every supported Room upgrade, atomic event edits/deletes/undo, backup round trip including every new field, legacy backups and malformed input. Fail safely without destructive migration.
- ViewModel/UI tests: draft restoration after process/configuration changes, account selection, staged review, empty/partial coverage, accessibility, reduced motion, permission-denied and airplane-mode operation.
- Validate upcoming/due/overdue states and forecasts against confirmed versus expected payments; background reminder delivery is not guaranteed.
- Before any implementation PR: `./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` and `./scripts/repo_guard.sh`.
- This document-only change does not require an Android build.

## 10. User research still needed

Run 8–12 consent-based interviews and a short diary study across salaried people, students, cash-heavy households, self-employed users, card users and people sharing household expenses. This is exploratory research, not a representative survey.

Ask participants to reconstruct a typical day without collecting account numbers or private statements: where did they pay; where is the record; which payments are missed; how do they recognize top-ups and repayments; what do they want to know before payday?

Test five tasks with fictional balances: record a Lite purchase, pay a credit bill, split a dinner, resolve a failed debit, and find next month's commitments. Measure correct account choice, correct spending total, duplicate mistakes and time to complete. Define success thresholds after an initial baseline; keep research outside the shipped app with no telemetry.

Recommended first implementation milestone: **P0 accounting foundation, then P1 cash + bank-funded UPI + UPI Lite + credit-card purchase/repayment/refund workflows.** This covers the requested core while preventing incorrect spending totals.

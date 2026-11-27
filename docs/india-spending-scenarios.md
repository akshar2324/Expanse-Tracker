# Indian spending: fictional people, payment journeys and a checked monthly ledger

Prepared 27 November 2026. This is a product simulation, not national spending statistics, a claim about an average income, or a prediction of a particular person's behavior. All people, income, balances, purchases and charges are fictional. November's complete month includes future simulated days. Prices are illustrative and vary by place, date and household.

The goal is to show what someone buys, which app or physical surface they might use, which balance actually changes, where a record is available, and what AkSpend should show. Coverage is broad and representative; no finite list captures every household.

## 1. The everyday distinction AkSpend must preserve

A person can browse a food app, pay through Google Pay, fund that payment with a bank account, and then record it in AkSpend. These are four roles, not four expenses.

| Role | Fictional examples | What AkSpend records |
| --- | --- | --- |
| Shopping/service surface | Local shop; Amazon/Flipkart; Swiggy/Zomato; Blinkit/Zepto; Uber/Ola; IRCTC; telecom/provider app | Merchant, purpose, order/reference when available |
| Payment surface | PhonePe, Google Pay, Paytm, BHIM, bank app, issuer app, POS terminal, ATM, cash | Optional app/surface and payment method |
| Funding source | Bank, cash, UPI Lite, prepaid wallet, credit card, credit line, gift balance | The account whose money or debt changes |
| Existing personal record | Memory, notebook, notes, spreadsheet, bank/app history, shared-expense app | A habit to accommodate; do not assume an API or import is available |
| AkSpend record | Confirmed ledger event with optional import observations | One financial event even if several apps mention it |

App names are illustrative choices, not endorsements, a required installation list, or a promise of every app/bank/merchant combination. Some people use one payment app and cash; the fictional full ledger deliberately exercises more methods for testing.

Research basis: Indian study participants described app history, notes, spreadsheets, finance apps and split-expense tools; this does not establish national prevalence. [Study, sections 3 and 5](https://arxiv.org/html/2401.09937v3). NPCI lists payment apps in its [ecosystem statistics](https://www.npci.org.in/what-we-do/upi/upi-ecosystem-statistics). Google documents Lite balance, purchases, auto-load and closure returns in its [Lite help](https://support.google.com/pay/india/answer/13327133?hl=en). The [Bharat Connect categories](https://www.bharat-connect.com/categories/) cover bills, recharges, transport and debt repayments. These sources establish mechanics and examples; the fictional amounts below are our assumptions.

## 2. Eight people to simulate instead of one national average

The monthly allocations below are illustrative uses of net take-home/support. 'Allocated' includes saving and principal payments, so it is not the same as consumption expense. They are separate alternative profiles, not additions to the detailed ledger.

| Person | Monthly inflow | Illustrative monthly allocations, summing to inflow | Likely simulated surfaces | Recordkeeping challenge |
| --- | ---: | --- | --- | --- |
| Student in shared accommodation | ₹12,000 support | PG/rent 5,000; food 3,000; travel 800; phone 300; study 700; leisure 1,200; reserve 1,000 | One UPI app, cash, college portal, shared-expense app | Parents' support is income to student; shared dinner reimbursement is settlement, not extra income |
| Single salaried renter | ₹50,000 salary | Housing/utilities 15,000; food 7,000; travel 3,000; family support 3,000; shopping/health/leisure 7,000; EMI 3,000; saving/reserve 12,000 | One/two UPI apps, bank/issuer app, shopping and food apps | Bank, Lite and card records scattered; repayments look like expenses |
| Family household, combined scope | ₹80,000 salaries | Housing/utilities 24,000; food 12,000; education 8,000; travel 6,000; health/insurance 5,000; loan payment 10,000; leisure/gifts 3,000; reserve 12,000 | Bank/UPI apps, school portal, cash, biller apps | Spouse transfers are internal; family member purchases can be duplicated |
| Cash-heavy daily-wage household | ₹18,000 irregular receipts | Housing/utilities 5,000; food 6,000; travel 1,500; school/health 2,000; phone 300; debt payment 1,200; reserve 2,000 | Cash, occasional UPI, recharge shop, ATM/BC outlet | No digital trail for small purchases; irregular income and lender dues |
| Self-employed worker, personal scope | ₹35,000 owner draw | Rent/utilities 10,000; food 6,000; travel 3,000; family 4,000; health/phone 2,000; debt 3,000; reserve 7,000 | UPI/bank, cash, work-expense notes | ₹35,000 is a transfer if business is inside the ledger; income here assumes business is outside personal scope |
| Retired couple | ₹30,000 pension | Housing/utilities 5,000; food 7,000; medicine 5,000; travel 1,000; family/gifts 2,000; insurance 2,000; reserve 8,000 | Bank app, cash, pharmacy, utility portal | Caregiver pays on behalf, pension timing, medical reimbursement |
| Small-town family with remittances | ₹25,000 support | Food 8,000; housing/utilities 4,000; education 3,000; travel 2,000; health 2,000; phone 500; seasonal reserve 5,500 | Cash, UPI, bank/BC outlet, local kirana credit | Family remittance scope, udhaar balances, cash withdrawal fees |
| Credit/EMI-heavy employee | ₹45,000 salary | Housing/utilities 12,000; food 6,000; travel 3,000; current purchases 4,000; card/loan cash payments 15,000; reserve 5,000 | Issuer/lender app, UPI, bank, shopping app | Cash payments and new card purchases overlap economically; separately show consumption, principal and prior-period bills |

For each profile, run both 'manual-only with no permissions' and 'optional imports' versions. Also test shared phone, multiple SIM/bank accounts, interrupted internet, low literacy, large text and reduced motion.

## 3. A day in the salaried renter's life

| Time | What happens | Surface | Funding source | Correct AkSpend result |
| --- | --- | --- | --- | --- |
| 08:00 | Tea ₹20 | Google Pay Lite at shop QR | Lite balance | Food ₹20; no additional bank debit |
| 09:00 | Commute ₹40 return total | Transit gate/card | Transit wallet | Travel ₹40; may need manual entry |
| 13:00 | Office lunch ₹90 | PhonePe | Bank | Food ₹90 |
| 18:30 | Pharmacy ₹450 | UPI app with eligible RuPay credit selection | Credit card | Health ₹450; card owed rises |
| 20:00 | Dinner for three ₹1,800, own share ₹600 | Google Pay | Bank | Food ₹600 and receivable ₹1,200 |
| 21:00 | Friend sends ₹800 back | PhonePe/bank alert | Bank receives | Reduce receivable; not salary |
| 22:00 | Check money | AkSpend | All recorded accounts | Spend ₹1,200; receivable ₹400; day cash/bank/wallet outflow ₹1,150; card debt +₹450 |

This is a standalone composite day assembled from scenarios, not one particular date in the monthly CSV. The net outflow equals purchases 20+40+90+1800 minus repayment 800, with 450 charged to credit. Net assets fall by own spending 1,200.

## 4. Full month: exact synthetic ledger

The CSV contains every generated event, app/surface, method, balance changes, expense effect and explanation. It is a research/test fixture, not an import file promised compatible with the current app. Day and amount fields are intentionally synthetic. Split tender is two rows grouped by note; repeated daily events remain distinct.

Opening card owed is ₹6,000 and opening loan owed is ₹20,000. They are pre-existing liabilities. All amounts are calculated in integer paise; positive account values are assets, negative card/loan values are debt. Opening asset balances are assumed user-entered and correct.

| Result | Value |
| --- | ---: |
| Recorded events | 121 |
| Salary and realized cashback | ₹50,050.00 |
| Net personal spending after refunds | ₹33,215.60 |
| Opening assets less liabilities | ₹5,300.00 |
| Closing assets less liabilities | ₹22,134.40 |
| Change in assets less liabilities | ₹16,834.40 |

Salary is ₹50,000; realized independent cashback is ₹50. Net spending includes own shares and interest/fees, excludes investments, recoverable deposits, loan principal and internal transfers. It is not a forecast of typical Indian spending.

| Account | Opening | Closing |
| --- | ---: | ---: |
| Salary bank | ₹15,000.00 | ₹21,383.40 |
| Second bank | ₹3,000.00 | ₹4,650.00 |
| Cash | ₹2,000.00 | ₹1,890.00 |
| UPI Lite | ₹0.00 | ₹900.00 |
| Prepaid wallet | ₹300.00 | ₹450.00 |
| Transit wallet | ₹500.00 | ₹160.00 |
| FASTag | ₹500.00 | ₹820.00 |
| Investments | ₹10,000.00 | ₹13,000.00 |
| Friend receivable | ₹0.00 | ₹400.00 |
| Pending recovery | ₹0.00 | ₹0.00 |
| Refundable deposit | ₹0.00 | ₹500.00 |
| Credit card | ₹-6,000.00 | ₹-4,019.00 |
| Loan | ₹-20,000.00 | ₹-18,000.00 |

| Spending category | Net amount |
| --- | ---: |
| Housing | ₹12,000.00 |
| Groceries | ₹3,435.00 |
| Food | ₹3,410.00 |
| Shopping | ₹3,050.00 |
| Family support | ₹3,000.00 |
| Transport | ₹2,150.00 |
| Utilities | ₹1,649.00 |
| Health | ₹1,050.00 |
| Gifts | ₹700.00 |
| Household | ₹650.00 |
| Personal care | ₹550.00 |
| Entertainment | ₹549.00 |
| Interest and fees | ₹500.00 |
| Communication | ₹299.00 |
| Travel | ₹100.00 |
| Donation | ₹100.00 |
| Fees | ₹23.60 |

### Day-by-day events

Daily tea, weekday lunch and commute are present individually in the CSV. The table below groups them only for readability.

| Day | Other events and surfaces | Net personal spending that day |
| --- | --- | ---: |
| 1 | Salary credited (Bank app); Monthly rent (PhonePe); Load UPI Lite (Google Pay); Load transit wallet (Transit app / station) | ₹12,020.00 |
| 2 | Family support outside tracked household (Google Pay); Groceries at kirana (PhonePe) | ₹4,600.00 |
| 3 | Pay previous card statement (Issuer / bank app); Electricity bill (Bill payment app) | ₹1,100.00 |
| 4 | Broadband bill (Provider app); Move money to second bank (Bank app) | ₹849.00 |
| 5 | Mobile recharge (Telecom app); Online clothes order (Shopping app) | ₹2,849.00 |
| 6 | Vegetables (None); Auto-rickshaw (None) | ₹530.00 |
| 7 | Food delivery (Food delivery app); Medicine from local pharmacy (PhonePe) | ₹790.00 |
| 8 | Withdraw cash (ATM); ATM fee as actually charged (Bank app) | ₹43.60 |
| 9 | Haircut (None) | ₹350.00 |
| 10 | Subscription renewal (Streaming app); Replenish UPI Lite (Google Pay) | ₹299.00 |
| 11 | Partial return from clothes order (Shopping / issuer app) | ₹-650.00 |
| 12 | Groceries by debit card (None / merchant terminal); Dinner paid for three people (Google Pay) | ₹1,730.00 |
| 13 | Friend repays first part (PhonePe) | ₹150.00 |
| 14 | Bus fare (None); Load FASTag (Issuer / bank app) | ₹50.00 |
| 15 | Toll deduction (Issuer alert); Loan EMI posted (Lender / bank app) | ₹700.00 |
| 16 | Pending debit without merchant success (PhonePe) | ₹150.00 |
| 17 | Failed debit reversed (Bank app); Load prepaid wallet (Wallet app) | ₹150.00 |
| 18 | Wallet-funded purchase (Wallet app); Shoes, split cash and UPI (PhonePe); Shoes, cash portion (None) | ₹1,600.00 |
| 19 | Fuel (None / merchant terminal) | ₹1,150.00 |
| 20 | Replenish UPI Lite (Google Pay); Investment contribution at cost (Investment app) | ₹150.00 |
| 21 | Household repair (Google Pay) | ₹670.00 |
| 22 | Cinema (Ticketing app) | ₹420.00 |
| 23 | Clinic consultation (PhonePe) | ₹750.00 |
| 24 | Travel booking (Travel app) | ₹1,350.00 |
| 25 | Travel cancellation refund (Travel / issuer app) | ₹-950.00 |
| 26 | Groceries (Google Pay); Cashback actually credited (Bank app) | ₹875.00 |
| 27 | Refundable equipment deposit (PhonePe) | ₹150.00 |
| 28 | Festival / gift purchase (None) | ₹720.00 |
| 29 | Small donation (None) | ₹120.00 |
| 30 | Personal items (Bank app) | ₹500.00 |

## 5. More people's actual journeys to simulate

These are independent fictional vignettes, not additional rows in the salaried person's ledger.

| Person / day | Purchases and money movement | App or surface choices | AkSpend outcome |
| --- | --- | --- | --- |
| Student, weekday | Parent sends ₹5,000; mess ₹80; bus ₹20; printouts ₹15; snacks ₹40; pays ₹600 group meal with own ₹150 share | Bank/UPI app; cash bus; campus printer; optional shared-expense app | Support income ₹5,000 if parent is outside scope; own spend ₹305; friends owe ₹450. Transfer within one tracked family is not income. |
| Cash-paid worker, payday | Receives ₹900 wage; groceries ₹250; transport ₹60; sends ₹300 to family outside scope | Cash; bank/agent receipt for remittance, method recorded as actually used | Income ₹900; personal outflows ₹610; cash ₹290 remaining if all paid from cash, excluding separately charged fee. |
| Home manager, market day | Receives ₹4,000 household allocation; vegetables ₹350 cash; kirana ₹900 UPI; milk bought on credit ₹300 | Cash, PhonePe or another UPI app, notebook for kirana credit | Household transfer is neutral within scope; expense ₹1,550; ₹300 payable remains; later repayment is not groceries again. |
| Freelancer, work day | Client settles ₹8,000 invoice; travel ₹180; work materials ₹1,000; personal dinner ₹200 | Bank, UPI, cash, invoice notes | For cash-basis personal tracking record realized income once; separate work/personal tags. If invoice receivable already posted, receipt settles it instead of income again. |
| Retired couple, clinic day | Pension ₹30,000; consultation ₹700; medicine ₹1,200; daughter pays ₹500 taxi externally | Bank app, pharmacy terminal, cash; daughter's UPI app | Health ₹1,900; explicitly record taxi as gift-funded support or amount owed to daughter. Do not invent a debit in pension bank. |
| Remittance household, weekly shop | Receives ₹6,000 from family outside scope; withdraws ₹2,000; groceries ₹1,500 cash; school fee ₹1,000 UPI | Bank/BC outlet, cash, payment app | Income ₹6,000; spending ₹2,500; cash ₹500 and bank ₹3,000 remain before any actual charges. |
| Commuter with vehicle, weekend | FASTag load ₹500, toll ₹120, fuel ₹1,000, parking ₹50 cash, refundable parking token ₹100 | Issuer app, terminal, cash | Expense ₹1,170; FASTag ₹380 remains; recoverable token ₹100 stays an asset. |
| EMI-heavy employee, due day | Pays ₹4,000 card bill against recorded old purchases; ₹3,000 loan installment split ₹2,500 principal + ₹500 charges | Issuer/lender and bank apps | Bank outflow ₹7,000; current expense ₹500; card debt −₹4,000 and loan debt −₹2,500. |

For the worker and remittance examples, agent charges affect the total only when actually charged. Cash allocation examples assume zero opening balances unless stated. Income/expense policy and household scope must be visible and consistent.

## 6. Scenario catalogue: normal payments and things that go wrong

This table is a behavioral specification. Only section 4 is executed as a complete monthly ledger. App/surface names remain illustrative; actual eligibility, limits and provider support differ.

| # | Situation | Likely record surface | Correct behavior and important distinction |
| --- | --- | --- | --- |
| 1 | Tea ₹20 paid by bank-funded UPI | UPI app/bank | Food expense against bank; merchant app and bank alerts are observations of one event. |
| 2 | Tea ₹20 paid using Lite | Lite app history | Food expense against Lite; no fabricated bank debit. |
| 3 | Two identical ₹20 teas on same day | Lite/app history | Two purchases; amount/date matching cannot silently remove one. |
| 4 | Lite balance replenishes ₹500 | UPI app/bank | Bank-to-Lite transfer, including confirmed auto-load. |
| 5 | Lite has insufficient balance, user switches to bank UPI | UPI app | Record the funding source actually used; no universal fallback behavior assumed. |
| 6 | Close Lite and receive remaining balance | UPI app/bank | Transfer back to bank; closure return is not income. |
| 7 | Two separate Lite balances in different apps | Each app history | Separate actual balances; link each top-up to its true source. |
| 8 | Restore AkSpend backup onto a new phone | AkSpend backup | Restored records do not restore or move real Lite funds. Reconcile provider balance independently. |
| 9 | Bank transfer between owned accounts | Two bank histories | Atomic internal transfer; preserve both observations. |
| 10 | ATM withdrawal ₹2,000 | ATM/bank | Bank-to-cash transfer; show remaining cash until spent. |
| 11 | ATM debit but no cash dispensed | Bank/complaint receipt | Pending recovery, not cash received or personal expense; link resolution. |
| 12 | Deposit cash at bank or authorized outlet | Bank/receipt | Cash-to-bank transfer, plus actual fee if separately charged. |
| 13 | UPI cash withdrawal at ATM | ATM/UPI/bank | Method changes; bank-to-cash accounting remains the same. |
| 14 | Debit-card supermarket purchase | POS receipt/bank | Expense from linked bank; debit card is not an extra funded wallet. |
| 15 | Credit-card supermarket purchase | POS/issuer | Expense and debt now; bank changes on repayment. |
| 16 | Eligible RuPay credit-card UPI purchase | UPI/issuer | Card-funded expense; retain UPI method and actual card account. |
| 17 | Card purchase offered EMI conversion | Issuer | Reclassify existing liability and create schedule; do not buy the product twice in reports. |
| 18 | Statement generated | Issuer statement | Snapshot of billed items; statement total is not a new expense event. |
| 19 | Full card bill paid | Bank/issuer | Reduce bank and liability; match delayed card-side settlement. |
| 20 | Only part or minimum due paid | Bank/issuer | Reduce debt by actual posted amount; do not mark fully settled or infer interest waived. |
| 21 | Card overpayment | Bank/issuer | Allow positive credit balance; do not clamp to zero. |
| 22 | Card payment still pending at issuer | Bank and issuer disagree temporarily | In-transit settlement linked to debit; no premature available-credit claim. |
| 23 | Card fee, interest, GST or surcharge | Statement | Separate confirmed charges and categories; do not guess a universal rate. |
| 24 | Parent directly pays user's card bill | Parent confirmation/issuer | If parent outside scope, support income + debt settlement; if within scope, internal movement. |
| 25 | Refund arrives after statement/due date | Merchant/issuer | Linked refund on actual date; do not automatically reduce an authoritative statement's required payment. |
| 26 | Same issuer cards share one limit | Issuer statement | One shared limit group; do not sum limits as independent capacity. |
| 27 | Prepaid wallet recharge ₹1,000 | Wallet/bank | Transfer into stored value. |
| 28 | App-based UPI versus that app's wallet | Payment detail | Use true source: app brand alone cannot decide bank versus wallet. |
| 29 | Shopping paid ₹200 gift credit + ₹800 card | Merchant/issuer | One ₹1,000 purchase, two funding postings; gift-credit acquisition policy determines its earlier source. |
| 30 | Cashback promised, coins or points earned | Rewards screen | No spendable bank income until realized; optional noncash rewards tracking. |
| 31 | Independent cashback credited to bank | Bank | Realized cashback income under chosen policy; avoid also subtracting it from purchase. |
| 32 | Merchant returns money to store credit | Merchant credit balance | Reverse linked purchase into restricted asset; do not invent a bank refund. Consistent asset policy avoids spending twice on later redemption. |
| 33 | Gift credit expires | Merchant record | Explicit asset write-off/loss; no imaginary cash movement. |
| 34 | Online order placed but no money charged | Order page | Optional planned purchase only; no confirmed financial posting. |
| 35 | Cash-on-delivery order | Merchant/delivery receipt | Post actual cash or UPI used at delivery; order's COD label does not prove cash payment. |
| 36 | Split cash + UPI purchase | Receipt and UPI | Funding parts sum to one purchase; category amount appears once. |
| 37 | Refund requested or return picked up | Merchant status | Pending expected refund; do not increase actual balance before receipt. |
| 38 | Partial return | Merchant/bank | Reverse only refunded part and preserve remaining cost. |
| 39 | Cancellation with retained fee | Merchant/issuer | Purchase minus actual refund leaves retained fee; reclassify rather than add fee twice. |
| 40 | Duplicate debit, one delivery | Bank and merchant | One expense plus duplicate-debit recovery claim, then linked reversal. |
| 41 | Payment fails with no bank debit | UPI app | Failed attempt only, zero balance effect. |
| 42 | Pending status with an actual bank debit | Bank/UPI | Hold in pending clearing/recovery until evidence establishes outcome; not available cash. |
| 43 | Merchant paid successfully after pending | Bank/merchant | Move pending posting to purchase without another bank debit. |
| 44 | Failed debit reverses | Bank | Clear recovery, not ordinary income. |
| 45 | AutoPay reminder received | Mandate/app alert | Upcoming obligation only. |
| 46 | AutoPay succeeds and manual retry also debits | Bank/provider | Detect actual duplicate; keep recovery claim until refund, rather than deleting a real debit. |
| 47 | AutoPay fails and fee charged | Bank/provider | Failed bill remains due; confirmed failure fee is an expense. |
| 48 | Utility payment made through Bharat Connect channel | Payment receipt/biller | One utility expense; channel/reference optional, not a second balance. |
| 49 | Mobile/DTH recharge | Telecom or payment app | Service expense by default; distinguish genuinely refundable stored money from a consumed service plan. |
| 50 | FASTag top-up then toll deduction | Issuer history | Transfer on top-up; transport expense on toll. No identical recharge/toll double count. |
| 51 | FASTag fee or refundable deposit | Issuer receipt | Fee expense separate from recoverable deposit and usable wallet balance. |
| 52 | Transit top-up online, physical activation pending | Transit receipt/gate | Track value in transit until loaded if deducted; never assume immediately usable wallet funds. |
| 53 | NCMC bank use versus offline wallet use | Card/issuer/transit | Same physical card can access different balances; identify which one funded each event. |
| 54 | Monthly transit pass | Transit provider | Default expense when purchased under simple tracking policy; avoid also deducting a fare expense per covered ride. |
| 55 | Personal loan disbursed ₹20,000 | Lender/bank | Bank asset +₹20,000 and liability +₹20,000; no salary income. |
| 56 | Loan/BNPL installment | Lender statement | Principal settles liability; confirmed interest/fees/tax are expenses. |
| 57 | Early/partial loan repayment | Lender statement | Use confirmed allocation and actual charges; update schedule without guessing amortization. |
| 58 | Credit-line-funded UPI | UPI/lender | Purchase against liability; draws and repayments are not deposit-account income. |
| 59 | Kirana udhaar purchase | Shop notebook/user entry | Groceries expense + payable; repayment later clears payable. |
| 60 | Lend ₹1,000 to friend | UPI + own note | Bank-to-receivable; not personal consumption. |
| 61 | Borrow ₹1,000 from friend | UPI + own note | Cash/bank and payable increase; not salary. |
| 62 | Group dinner paid by one person | Receipt/shared-expense record | Own share expense; others' shares receivable; partial repayments clear receivable. |
| 63 | Friend pays user's meal | Shared-expense record | Own share expense + payable; later UPI repayment does not repeat food expense. |
| 64 | Flatmate rent paid upfront | Rent receipt/shared record | Own rent + recoverable shares; retain overdue balance across month end. |
| 65 | Settlement nets several old debts | Shared ledger/bank | Allocate one payment to multiple claims; do not create fresh income/expense. |
| 66 | UPI Circle family member spends | Funding account/app history | One household expense tagged with spender and funding owner; no extra child bank debit invented. |
| 67 | Send family support | Bank | Expense when outside tracked household; transfer when inside. Record scope explicitly. |
| 68 | Received family support | Bank/cash | Support income when sender outside scope; internal transfer when inside. |
| 69 | Work travel reimbursed | Receipt/employer bank credit | Recoverable work advance cleared on reimbursement if employer owes it; do not mix with personal travel consumption. |
| 70 | Employer meal benefit | Benefit balance/provider | Restricted asset under explicit benefit policy; expense on actual use, not bank income invented from a voucher. |
| 71 | Refundable rent/security deposit | Lease/payment | Recoverable asset; refund clears it; agreed forfeiture is recognized loss/expense. |
| 72 | FD/RD/SIP/PPF/NPS contribution | Bank/investment statement | Asset transfer; not spending. Cost ledger is distinct from live market valuation. |
| 73 | Deposit interest or investment distribution received | Statement | Separate realized income from principal returned; no guessed rate or automatic live prices. |
| 74 | Hospital deposit and final invoice | Hospital receipt | Advance/deposit first; settle actual charge and refund/recovery, avoiding both deposit and full invoice as expense. |
| 75 | Insurance claim after medical spending | Insurer/bank | Link recovery; keep gross cost, covered portion and own cost understandable under chosen reimbursement policy. |
| 76 | Festival/wedding cash gift sent or received | Cash/user note | Sent gift expense, received gift income outside household scope; no assumed matching amount. |
| 77 | Cheque issued but not cleared | Cheque note/bank | Commitment until actual clearing under cash-basis view; avoid treating both issue and clearance as expenses. |
| 78 | Cheque bounced | Bank | Undo/resolve pending attempt; post only actually charged fee and preserve unpaid obligation. |
| 79 | Cash short at month end | Count/user confirmation | Reconciliation discrepancy; explicit loss/unallocated spend with explanation, not invented merchant details. |
| 80 | Foreign purchase, later settled INR differs | Issuer statement | Pending estimate separate from posted INR; explicit original currency/settled amount/actual markup and taxes. |
| 81 | e-rupee wallet top-up and payment | Provider history | Real wallet balance transfer then purchase; optional record support, not payment integration. |
| 82 | e-RUPI restricted voucher redemption | Voucher/provider receipt | Track restriction and usage with explicit benefit policy; no ordinary bank balance implied. |
| 83 | UPI 123PAY, USSD or AePS event | Bank/authorized receipt | Actual bank/cash movement decides accounting; channel label does not create an extra account. |
| 84 | Payment disputed as fraud | Bank/dispute record | Preserve real debit and recovery/dispute status; chargeback provisional credit can be reversed. Never promise recovery. |
| 85 | Overseas family remittance | Bank/provider | Actual received INR and separately charged fees; original currency optional. Household scope still applies. |
| 86 | Invoice receipt already recorded as receivable | Invoice/bank | Settle claim, not second income. Distinguish cash-basis income mode from invoice accounting. |
| 87 | Last-month expense refunded this month | Bank/merchant | Current-date linked refund, transparent gross/refund/net views; never silently rewrite a closed period. |
| 88 | Payment late at night, posted next day | Bank/merchant | Preserve event and posted dates, timezone and references; do not deduplicate by same calendar day alone. |

## 7. Where automation helps, and where a person must fill gaps

| Source | Can help establish | Cannot safely establish alone |
| --- | --- | --- |
| Bank SMS/notification | Debit/credit amount, date, account hint, reference when supplied | Purpose, own versus group share, universal merchant identity, every individual Lite purchase |
| UPI-app notification | Payment/app context and sometimes source hints | Authoritative full account balance or unique funding source in every message |
| Card statement CSV | Posted purchases, repayments, refunds, actual charges and dates | A user's manual cash purchases; current issuer balance after statement cutoff |
| Bank statement CSV | Posted bank movements | Whether each debit is shopping, loan principal, own transfer, deposit or group bill |
| Lite/provider history | Individual purchases and wallet loads where exposed | A universal export or background read API; don't promise one |
| Cash receipt/manual entry | Actual cash purchases/income and allocation | Automatic discovery of every cash event |
| Shared-expense notes | Who owes whom and own share | Proof that a bank settlement actually completed |
| Order confirmation | Intent, item and merchant | Posted payment, fulfilled delivery or completed refund |

AkSpend remains offline, with optional existing on-device notifications/SMS paste/share and CSV review. No bank login, OTP, PIN, CVV, screen scraping promise, remote inbox access or cloud service is needed for these simulations. Imports are observations to review; they are not a guarantee of complete spending coverage. Direct SMS permissions require applicable Google Play declarations/policy compliance; paste/share and manual workflows remain available. [Google Play policy](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en)

## 8. Acceptance outcomes for AkSpend

1. Show bank/cash/spendable wallets separately from investments, recoverable claims and debt. Available credit never inflates cash available.
2. A purchase changes spending once, regardless of how many notifications or receipts describe it.
3. Internal transfers, card payments, loan principal and top-ups stay outside consumption totals.
4. Refunds, debt settlements and reimbursements preserve original links and actual dates.
5. Explain missing data: unknown Lite transactions, unreviewed imports and unreconciled cash must not produce a false 'complete' report.
6. A person can complete all entry, review, reporting and backup workflows offline with no optional permissions.
7. The salaried-month closing balances match the CSV and satisfy opening net assets + income − net expenses = closing net assets.
8. All money is calculated in integer paise. The fixture checks each event's change in net assets, non-negative non-credit balances and whole-month reconciliation.

## 9. Product priorities from the simulation

Build the bank/cash/card/Lite accounting contract first; then daily entry and import matching; then statements, repayments and refunds; then bills, installments and shared spending. Ship specialist assets and rare channels behind optional account presets rather than placing 20 payment choices in everyone's first screen.

Use this catalogue for product review and focused tests when implementing. The 121-event CSV is generated and reconciled externally; it has not been executed through AkSpend's current UI or database. No production financial behavior has changed.

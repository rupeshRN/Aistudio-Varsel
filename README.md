# VarSel Expense Tracker

An intelligent, privacy-first personal finance and expense tracking Android application. VarSel automates financial tracking through high-precision, on-device bank statement parsing (PDF/OCR), dynamic category assignment, reconciliation, and granular analytics—operating completely offline with zero external server dependencies.

---

## 🌟 Key Features

### 1. Smart On-Device Bank Statement Ingestion
- **Automated Bank Detection:** Uses an ultra-fast **Heuristic Metadata Scanner** to inspect RBI 4-letter IFSC code prefixes (`IDIB`, `ICIC`, `HDFC`, `UTIB`, `SBIN`, etc.) and institutional branding in document headers, correctly identifying issuing banks while preventing counterpart narration pollution (e.g., third-party UPI VPAs).
- **Multi-Bank Parser Support:** Specialized parsing engines for:
  - **Indian Bank** (Passbook layouts, multi-format dates, and single/dual amount columns)
  - **ICICI Bank** (Tabular e-statements, transaction narrations, running balances)
  - **HDFC Bank** (Detailed transaction tables and account summaries)
  - *Extensible architecture* ready for additional scheduled commercial and regional banks.
- **Password-Protected PDF Decryption:** Seamless in-memory decryption for password-protected bank e-statements.
- **High-Accuracy Data Extraction:** Extracts transaction dates, merchant/counterparty narrations, withdrawal (debit), deposit (credit), and running ledger balances.

### 2. Intelligent Reconciliation & Verification
- **Mathematical Balance Reconciliation:** Verifies whether the calculated net delta (`Credits - Debits`) reconciles accurately against reported opening and closing statement balances.
- **Statement Snapshots:** Auditable import history preserving original file hashes, transaction counts, and detected discrepancies.
- **Transaction Fingerprinting:** Generates deterministic hashes to identify and prevent duplicate transactions across overlapping statements.

### 3. Expense Tracking & Financial Management
- **Transaction Categorization Engine:** Real-time keyword rules, regex matching, and adaptive self-learning categorization.
- **Custom Categorization Rules:** Create priority-based user rules with auto-assign actions.
- **Budgets & Spending Limits:** Category-based monthly budgets with real-time progress indicators and spending alerts.
- **Recurring Transactions & Subscriptions:** Track repeating expenses, upcoming bills, and cash flow obligations.
- **Loan & Debt Accounts:** Manage principal balances, interest rates, EMI schedules, and linked payoff transactions.
- **Financial Analytics & Reporting:** Interactive breakdowns by category, income vs. expense cash flows, monthly trends, and spending patterns.

---

## 🔒 Data Handling & Privacy

Your financial data is sensitive, personal, and strictly yours. VarSel is engineered with a **local-first privacy architecture**:

- **100% On-Device Processing:** All PDF decryption, text normalization, table extraction, and balance calculations occur entirely on your device.
- **No Cloud Storage of Statements:** Your uploaded bank statements and PDF documents are processed in ephemeral memory and never uploaded to any remote server or cloud storage.
- **Local Persistence via Room (SQLite):** Your transaction history, accounts, budgets, and categorization rules are stored locally in an encrypted Room SQLite database on your device.
- **Zero Third-Party Telemetry:** We do not track, profile, monetize, or sell your personal financial records, transaction histories, account numbers, or balances.
- **Device-Level Security:** Database files are stored in internal sandboxed application storage accessible only by the VarSel app.

---

## ⚖️ Legal Disclaimer & Terms of Use

### Financial & Advisory Disclaimer
VarSel is an informational and organizational tool designed solely for personal expense tracking and budgeting. **VarSel is not a bank, certified financial planner, investment adviser, tax consultant, or financial institution.** The insights, analytics, balance summaries, and categorizations provided by the application do not constitute professional financial, investment, accounting, or legal advice. 

### Statement Accuracy & Reconciliation
While VarSel utilizes advanced deterministic parsing, heuristic normalization, and balance reconciliation algorithms, digital bank statements vary widely in formatting, layout versions, OCR accuracy, and scan quality. Users are solely responsible for reviewing and confirming the accuracy of all imported transactions, amounts, dates, and balance figures before relying on them for tax, legal, or commercial purposes.

### Trademarks & Third-Party Brand Attribution
All product names, logos, bank names (including but not limited to Indian Bank, ICICI Bank, HDFC Bank, State Bank of India, Axis Bank, etc.), and registered trademarks referenced in this project belong to their respective holders. Their reference in this software is strictly for the purpose of identification, file format compatibility, and interoperability under fair use principles. VarSel is an independent software project and is neither affiliated with, endorsed by, nor sponsored by any of the financial institutions mentioned.

### Terms of Use & "As-Is" License
This application is provided on an **"AS IS" and "AS AVAILABLE"** basis without warranties of any kind, whether express, statutory, or implied, including but not limited to warranties of merchantability, fitness for a particular purpose, non-infringement, or error-free operation. In no event shall the authors, contributors, or copyright holders be liable for any claims, financial discrepancies, damages, data loss, or expenses arising from the use or inability to use this software.

---

## 🤝 Support & Contribution

If you encounter issues, have questions regarding bank statement formats, or wish to suggest new bank layout support:

- **Bug Reports & Issues:** Submit detailed issue descriptions or sample anonymized statement snippets via the issue tracker.
- **Feature Requests:** Open a discussion for requested features, chart visualizations, or parser templates.
- **Feedback:** Reach out through the repository contact or project maintainers.

### ❤️ Donation & Sponsorship

VarSel is an open, independent, community-driven project built to give users true sovereignty over their personal financial data without subscriptions or intrusive ads. 

If VarSel helps you organize your finances, save time, or track your expenses, consider supporting ongoing development:

- **GitHub Sponsors:** Sponsor the project on GitHub.
- **UPI (India):** Support directly via UPI payments.
- **Buy Me a Coffee / Ko-fi:** Contribute to support continuous parser development, performance tuning, and new bank integrations.

*Every contribution helps keep this project independent, maintained, and privacy-respecting.*

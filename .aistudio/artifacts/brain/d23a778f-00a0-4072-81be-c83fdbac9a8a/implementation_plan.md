# Implementation Plan: Robust Bank Statement Detection & Disambiguation

## Confirmed Decisions from Phase 1
- **Official Branding Over Raw IFSC**: Primary header branding cues (e.g., "ICICI BANK", "HDFC BANK", "STATE BANK OF INDIA") take precedence over any standalone IFSC matches.
- **Header Boundary & Narration Isolation**: Raw transaction narrations must never influence institution detection. The document scanner will strictly isolate pre-transaction metadata (terminating scanning at transaction table header lines such as "Date", "Particulars", "Withdrawal", etc.) and require contextual anchoring for IFSC codes (e.g., preceded by "IFSC", "Branch Code", or "RTGS/NEFT IFSC").
- **Historical Account Number Matching**: When header branding and contextual IFSC are ambiguous or inconclusive, the detector will extract the statement's account number (or last 4 digits) and cross-reference against previously imported statements stored in Room (`statement_snapshots` and `transactions`).
- **Detailed Actionable Error**: If the account number is completely unrecognized and the bank cannot be conclusively determined, the app will throw a detailed, user-facing error showing extracted metadata (detected account number, ambiguous tokens) and actionable instructions.

---

## 1. Overview & Core Concept
Currently, `HeuristicMetadataScanner` scans the first 45 lines of raw text for any 11-character regex matching `[A-Z]{4}0[A-Z0-9]{6}` and maps the prefix directly to Indian banks. In real-world ICICI or HDFC statements, account summary headers can be as short as 10–15 lines, causing lines 16–45 to include initial transaction rows (e.g., `NEFT-IOBA0001234-...` or `UPI/5031.../IOB/`). Consequently, an ICICI statement was mistakenly identified as Indian Overseas Bank before any supported bank parser was even evaluated.

This plan introduces a multi-tiered, tamper-resistant bank identification pipeline:
1. **Pre-Transaction Boundary Truncation**: Discard all text lines occurring after the transaction ledger header row.
2. **Contextual Token & Brand Scoring**: Weigh explicit bank trademarks, branch headers, and official logos/letterhead texts.
3. **Branch-Anchored IFSC Only**: Reject unanchored IFSC patterns in narration lines; only accept IFSC tokens preceded by branch metadata labels.
4. **Account History Fingerprinting**: Cross-match the parsed account number against known account IDs in the local database.
5. **Detailed Failure Diagnostics**: Provide structured errors when a statement cannot be safely parsed.

---

## 2. User Experience & Visual Design
When an ambiguous or unsupported statement is processed:
- **Success State**: Immediate, confident detection of the issuing bank without erroneous prompts. A badge confirms the verified institution (e.g., "ICICI Bank • Savings Account ending in 4321").
- **Account Disambiguation Banner**: When account history resolution successfully matches an ambiguous statement to a previously imported account, an informational banner appears:
  - *"Matched via previous statement for Account •••• 4321 (ICICI Bank)"*
- **Detailed Error Dialog (Unrecognized / Ambiguous Account)**:
  - **Title**: Statement Could Not Be Safely Identified
  - **Extracted Cues Display**:
    - **Detected Account Number**: `•••• 8910` (or `Not Found`)
    - **Branch / IFSC Cues**: `None in official header`
    - **Observed Counterparty Banks**: Shows excluded transaction mention (e.g. `IOBA mention in narration ignored`)
  - **Action**: Suggests verifying the PDF is an official digital statement or submitting feedback with sample headers.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Alternative Considered | Rationale |
| :--- | :--- | :--- |
| **Cut text at Table Header Boundary** | Scanning entire document or arbitrary line limit (45 lines) | Prevents counterparty bank mentions in transaction rows from polluting institution identity. |
| **Anchor IFSC to Branch Labels** | Any IFSC token in top lines | Transaction references frequently embed counterpart IFSCs in NEFT/RTGS codes. True bank IFSCs are always labeled (e.g., "RTGS/NEFT IFSC Code: ICIC0000001"). |
| **Room Database Account Match Fallback** | Asking user via popup modal on every ambiguity | Minimizes user fatigue for recurring statement imports by leveraging prior confirmed account metadata. |
| **Strict Reject on Mismatch** | Guessing the closest bank name | Prevents corrupting user financial balances and categorization by running the wrong parser schema. |

---

## 4. Technical Architecture & Implementation Steps

### Architectural Flow

```
[Raw PDF Text]
      │
      ▼
┌──────────────────────────────────────────────┐
│       Pre-Transaction Boundary Cutter        │
│  (Stops at "Date | Particulars | Amount...") │
└──────────────────────┬───────────────────────┘
                       │ Header Text Only
                       ▼
┌──────────────────────────────────────────────┐
│          Header Brand & Cue Matcher          │
│   (Checks "ICICI BANK", "HDFC BANK", etc.)   │
└──────────────────────┬───────────────────────┘
                       │
          ┌────────────┴────────────┐
          ▼                         ▼
   [Brand Found]             [Brand Inconclusive]
          │                         │
          │                         ▼
          │         ┌──────────────────────────────┐
          │         │    Contextual IFSC Anchor    │
          │         │ ("IFSC Code: ...", not NEFT) │
          │         └──────────────┬───────────────┘
          │                        │
          │           ┌────────────┴────────────┐
          │           ▼                         ▼
          │    [Branch IFSC]            [No Valid IFSC]
          │           │                         │
          │           │                         ▼
          │           │         ┌──────────────────────────────┐
          │           │         │ Account History DB Matcher   │
          │           │         │ (Match acc no in Room DB)    │
          │           │         └──────────────┬───────────────┘
          │           │                        │
          ▼           ▼           ┌────────────┴────────────┐
      ┌───────────────────┐       ▼                         ▼
      │ Run Bank Parser   │  [DB Match]           [Unrecognized Acc]
      │ (e.g., ICICI)     │       │                         │
      └───────────────────┘       ▼                         ▼
                              Run Parser            Detailed Error
                                                    (Diagnostic Info)
```

### Planned File Changes

1. **`HeuristicMetadataScanner.kt`**:
   - Add `isolateHeaderSection(text: String): String` to detect common ledger header markers (`Date`, `Particulars`, `Description`, `Chq / Ref No`, `Withdrawal`, `Deposit`, `Balance`, etc.) and slice off transaction rows.
   - Refactor `detectBankFromIfsc()`: Only accept IFSC matches that are preceded by branch labels like `IFSC`, `Branch Code`, `RTGS/NEFT IFSC`, `NEFT IFSC`, `Branch IFSC`.
   - Implement `detectPrimaryBrand(headerText: String): SupportedBank?` to prioritize official trademarks ("ICICI BANK", "HDFC BANK", "STATE BANK OF INDIA", "AXIS BANK", "KOTAK") before checking generic IFSC dictionaries.

2. **`BankDetector.kt`**:
   - Refactor detection hierarchy:
     1. Evaluate primary header branding cues.
     2. Evaluate registered supported parsers (`canParse()` against header and structure).
     3. Evaluate contextual branch IFSC.
     4. Fall back to `AccountHistoryMatcher` (using `StatementSnapshotDao` / `TransactionDao`).
     5. If all fail, return `BankDetectionResult.UnrecognizedAccount` with diagnostic details (detected account digits, scanned lines, excluded counterparty names).

3. **`AccountHistoryMatcher.kt` (New Domain Helper)**:
   - Queries `StatementSnapshotDao.getAllSnapshots()` to retrieve existing accounts and their corresponding `bankName`.
   - Extracts account numbers from the header using regex patterns (`Account No`, `A/c No`, `Account Number: XXXXXXXXXXXX`) and checks for exact or last-4 matches.

4. **`BankDetectorTest.kt`**:
   - Add test case with an ICICI statement snippet containing an IOB transaction (`NEFT-IOBA0000123-TRANSFER TO MR X`). Verify it detects as `ICICI Bank` and NOT `Indian Overseas Bank`.
   - Add test case with missing branding but matching previously imported account number.
   - Add test case verifying detailed diagnostic error when an unknown account with no branding is presented.

5. **Build & Test Verification**:
   - Run `compile_applet` and `gradle :app:testDebugUnitTest` via `run_command` to ensure zero compilation or regression failures.

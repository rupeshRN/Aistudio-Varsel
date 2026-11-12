# Data Quality Dashboard & Homepage Health Indicator

A comprehensive ledger health system that continuously audits all 14 financial data quality rules, presents a compact one-liner status pill on the home screen, and provides a dedicated Data Quality Dashboard with one-tap interactive quick-fix resolutions.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed the following design and product decisions during Phase 1:
> - **Homepage Presentation**: Compact status pill showing issue count with direct tap to the full dashboard.
> - **Dashboard Entry Points**: Tapping the homepage status pill and via a dedicated tool card in the **More / Financial Tools** menu.
> - **Resolution Model**: Interactive quick-fix actions (e.g. auto-merge duplicates, instant categorisation, balance remainder, adjust date, or dismiss).

---

## 1. Overview & Core Concept

Personal finance trackers suffer from subtle ledger rot over time: duplicate statement imports, missing transfer counterpart legs, broken split sums, orphan event allocations, and forgotten recurring subscriptions.

This feature introduces an automated **Data Quality Audit Engine** that runs offline against local Room entities, validating the 14 specific criteria specified in the user's specification:

1. **Possible duplicate**: Same account, date, amount, and similar description.
2. **Exact duplicate**: Same account, date, amount, reference number, and normalized description.
3. **Uncategorised item**: Category is blank, "Unknown", or UNCAT.
4. **Unmatched transfer**: Transfer side without counterpart leg within 0–3 days.
5. **Unusual amount**: More than 3x the merchant's / category's median transaction.
6. **Missing recurring item**: Active recurring subscription not observed within due date plus grace period.
7. **Stale account**: Account with no statement import or transaction activity for 30–60+ days.
8. **Old receivable**: Split / loan receivable outstanding for more than 30 days.
9. **Allocation error**: Participant shares do not equal the event's total cost.
10. **Orphan mapping**: Event allocation or split item pointing to a deleted or non-existent transaction.
11. **Over-settlement**: Repayments exceeding the participant's outstanding amount.
12. **Broken split**: Split items do not sum to parent source transaction amount.
13. **Balance inconsistency**: Ledger calculated closing balance differs from recorded snapshot ending balance.
14. **Future-date anomaly**: Imported transaction timestamp is in the future, excluding scheduled entries.

---

## 2. User Experience & Visual Design

### Homepage Experience
- Located cleanly below the greeting header on `DashboardScreen`:
  - **Healthy State**: `[✓ 100% Healthy • All 14 checks passing →]` (Subtle M3 primary container pill).
  - **Attention State**: `[⚠ 3 items need review • Tap to inspect →]` (M3 tertiary container / amber highlight with pulse dot).
- Minimum 48dp touch target with full ripple feedback.

```
┌─────────────────────────────────────────────────────────────┐
│ Good Morning, Alex                                    [⚙]   │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ⚠ 3 items need review • Tap to inspect               →  │ │  <-- Compact Status Pill
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Total Net Worth                                         │ │
│ │ ₹1,42,850.00                                   [👁]     │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Dedicated Data Quality Dashboard (`QualityDashboardScreen`)
- **Health Score Hero**: Circular visual progress meter showing overall score (0–100%) and summary breakdown (Critical Errors, Warnings, Clean Checks).
- **Category Filter Tabs**: All Issues, Duplicates, Ledger & Balances, Splits & Events, Accounts.
- **Interactive Check Cards**:
  - Check type badge (e.g. `EXACT DUPLICATE`, `BROKEN SPLIT`, `UNCATEGORISED`).
  - Clear diagnosis with affected amounts, accounts, and dates.
  - Action buttons:
    - `Fix / Resolve`: Triggers relevant resolution modal (e.g., category picker, merge confirmation, auto-rebalance).
    - `Dismiss / Ignore`: Suppresses this instance and updates health score with undo toast.
- **Empty / Clean State**: Cheerful celebration illustration and badge when all 14 checks are green.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Continuous Reactive Room Flow vs. Manual Audit**
  - *Chosen Approach*: Reactive Room Flows that re-evaluate rules asynchronously in the background via Kotlin Coroutines whenever transactions, snapshots, or events are updated.
  - *Why*: The homepage one-liner always reflects real-time ledger health without requiring the user to press a manual "Scan" button.
- **Decision 2: Non-Destructive Resolution**
  - *Chosen Approach*: All quick-fix actions are confirmed via focused bottom sheets or dialogs with explicit before/after previews, and dismissed items can be reviewed/restored from a "Dismissed" filter tab.
  - *Why*: Protects against accidental deletion of valid transactions.
- **Decision 3: Zero Network / 100% Offline**
  - *Chosen Approach*: All 14 checks execute entirely on-device via Room queries and Kotlin algorithms. No external API calls or permissions needed.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Data Quality Architecture                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   Room Database DAOs                                                            │
│   ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐              │
│   │  TransactionDao  │  │StatementSnapshot │  │  FinancialEvent  │  ...         │
│   └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘              │
│            │                     │                     │                        │
│            ▼                     ▼                     ▼                        │
│   ┌──────────────────────────────────────────────────────────────┐              │
│   │                 DataQualityAuditEngine                       │              │
│   │  - Evaluates all 14 Quality Check Rules                      │              │
│   │  - Computes Health Score (0-100%)                            │              │
│   │  - Produces structured QualityCheckResult items              │              │
│   └──────────────────────────────┬───────────────────────────────┘              │
│                                  │                                              │
│                                  ▼                                              │
│   ┌──────────────────────────────────────────────────────────────┐              │
│   │                 DataQualityRepository                        │              │
│   │  - Manages active issues & dismissed issue persistence       │              │
│   │  - Applies quick-fix mutations (merge, categorize, rebalance)│              │
│   └──────────────────────────────┬───────────────────────────────┘              │
│                                  │                                              │
│            ┌─────────────────────┴─────────────────────┐                        │
│            ▼                                           ▼                        │
│   ┌─────────────────────────────┐             ┌─────────────────────────────┐   │
│   │    DashboardViewModel       │             │   DataQualityViewModel      │   │
│   │  - Exposes qualitySummary   │             │  - Full issue lists         │   │
│   │    for Home Health Pill     │             │  - Category filters         │   │
│   └──────────────┬──────────────┘             │  - Action dispatchers       │   │
│                  │                            └──────────────┬──────────────┘   │
│                  ▼                                           ▼                  │
│   ┌─────────────────────────────┐             ┌─────────────────────────────┐   │
│   │  DashboardScreen (Home)     │             │  QualityDashboardScreen     │   │
│   │  - DataQualityPill (1-line) │             │  - Full Interactive Center  │   │
│   └─────────────────────────────┘             └─────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Component & State Mapping

| Component | State Flow / Interaction | Quick-Fix Handlers |
| :--- | :--- | :--- |
| `DataQualityPill` | `DashboardUiState.qualitySummary` | Clicking navigates to `AppDestination.QualityDashboard` (`"data_quality"`) |
| `MoreScreen` | ToolHubCard "Data Quality & Health" | Clicking navigates to `"data_quality"` |
| `QualityDashboardScreen` | `DataQualityViewModel.uiState` | Filter by category, tap check to open action sheet, swipe/button to dismiss |
| `QuickFixCategorizeSheet` | Selected category ID | Invokes `viewModel.resolveCategorization(txId, category)` |
| `QuickFixDuplicateSheet` | Comparison of primary & duplicate | Invokes `viewModel.resolveDuplicateMerge(keepId, deleteId)` |
| `QuickFixSplitSheet` | Difference amount & child list | Invokes `viewModel.autoBalanceSplit(parentId)` |
| `QuickFixDateDialog` | Adjusted valid timestamp | Invokes `viewModel.correctDate(txId, newDate)` |

---

## 5. Verification & Test Plan

1. **Unit Testing (`DataQualityAuditEngineTest`)**:
   - Test each of the 14 rules independently with synthetic fixtures:
     - Exact & possible duplicates detection.
     - Uncategorised transactions with blank or UNKNOWN category.
     - Unmatched transfer detection when no opposite sign entry exists within 3 days.
     - Unusual amount when >3x merchant median.
     - Missing recurring item beyond due date + grace window.
     - Stale accounts with last update > 30 days.
     - Old receivables > 30 days.
     - Allocation & split sum discrepancies.
     - Orphan mappings pointing to missing parent/child IDs.
     - Over-settlement exceeding balance.
     - Balance inconsistency against snapshot closing balance.
     - Future date timestamps.
2. **Repository & ViewModel Testing (`DataQualityViewModelTest`)**:
   - Verify quick-fix resolution mutations update Room correctly.
   - Verify dismissal persists and adjusts overall health score.
3. **Compilation & JVM Test Run**:
   - Run `compile_applet` and `gradle :app:testDebugUnitTest` to ensure 100% pass rate.

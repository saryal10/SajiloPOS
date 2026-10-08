# SajiloPOS

**Offline-first Point of Sale & inventory management for small businesses in Nepal** — retail, restaurant and public transport — built Android-first for phones and vendor-supplied tablets.

SajiloPOS is designed for the Nepali micro-entrepreneur: it keeps selling through load shedding, prints to cheap Bluetooth thermal printers, accepts eSewa / Fonepay / Khalti QR payments, and produces IRD-audit-ready invoices — with no monthly subscription and no server dependency.

<p align="center">
  <b>Sell</b> · <b>Insights</b> · <b>Stock</b> · <b>History</b> · <b>Settings</b>
</p>

---

## Table of contents

- [Why it exists](#why-it-exists)
- [Feature overview](#feature-overview)
- [Screens](#screens)
- [Technology](#technology)
- [Architecture](#architecture)
- [Design system](#design-system)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [Data model](#data-model)
- [Testing](#testing)
- [What works today vs. what is stubbed](#what-works-today-vs-what-is-stubbed)
- [Roadmap](#roadmap)
- [Known gaps](#known-gaps)
- [Licensing](#licensing)

---

## Why it exists

Traditional POS hardware in Nepal costs more than the margin on a first month of sales. SajiloPOS removes that barrier:

| Traditional POS | SajiloPOS |
| --- | --- |
| Expensive proprietary terminal | Any Android phone (min SDK 24) |
| Server required — stops when internet drops | Local SQLite, sells offline, syncs later |
| Proprietary printers only | Standard 58 mm / 80 mm ESC/POS thermal printers |
| Cloud subscription | One-time app, no recurring fee |
| English-only UI | Nepali product names (Devanagari) throughout |

Because the whole ledger lives on-device, a shop can ring up sales, print receipts and count stock with **no connectivity at all**.

---

## Feature overview

### Catalog & inventory
- Product catalog with English + Nepali names, categories, units (`pcs`, `kg`, `pkt`, `plate`, `ticket`)
- Barcode scanning via **CameraX + ZXing** (EAN/UPC/Code-128/QR); unknown barcodes are captured as new items so a sale is never blocked
- Stock decrement on every sale, low-stock thresholds, "sold out" / "N left" states on product tiles
- Quick stock adjust (±1 / +5 / +10), margin % per product, catalog value at cost price
- Manual barcode entry and demo barcode presets for testing

### Payments
- **Cash** with quick-tender chips tuned to Nepali denominations and live change calculation
- **eSewa / Fonepay / Khalti** dynamic QR generated per invoice amount, with a settlement-verification flow (stock only decrements after confirmation)
- Offline-safe: the sale is settled locally, never blocked on a gateway round-trip

### Receipts
- Itemized thermal-slip preview that mirrors a real 58 mm / 80 mm printout
- Business header, PAN/VAT, invoice sequence, channel (table / bus / counter), VAT summary, transaction reference and an IRD verification QR
- Print via Android `PrintManager`, share as text, or copy the raw **ESC/POS byte plan** for a paired Bluetooth printer
- Reprint any past receipt from History

### Insights & predictive restocking *(Phase 2)*
- Revenue, order count, average ticket, gross profit and margin % for Today / 7 days / 30 days
- Gap-free daily sales trend chart (zero-sale days are shown, not hidden)
- Payment-mix donut with per-rail share
- Top sellers by revenue
- **Predictive restocking**: 14-day sales velocity → days of cover → suggested order quantity against a supplier lead time, with one-tap restock
- Stock value at cost price, plus a count of items needing reorder

### Industry modes
One switch re-shapes the entire workflow, catalog and ticket format:

| Mode | What you get |
| --- | --- |
| **Retail & grocery** | Barcode-first selling, category filters, low-stock alerts |
| **Restaurant & cafe** | Table service (status, seats, running tab), kitchen items, optional 10% service charge |
| **Public transport** | Route & stop fare matrix, one-tap ticket issuing, statutory Student (−45%) / Senior (−50%) concessions |

### Localization & compliance
- Nepal VAT (13%) applied at checkout, optional restaurant service charge
- PAN/VAT number on every invoice; sequential daily invoice numbers (`INV-YYYYMMDD-0001`)
- Nepali lakh/crore digit grouping (`रू 1,25,000`) and Nepali month/day labels

---

## Screens

| Screen | Purpose |
| --- | --- |
| **Sell** | Catalog + cart. Dual-pane on tablets, floating cart bar on phones. |
| **Insights** | KPIs, trend chart, payment mix, top sellers, smart restock. |
| **Stock** | Full inventory, filters, margins, quick adjust, add/edit product. |
| **History** | Every sale with search + payment filters; tap to reprint. |
| **Settings** | Business profile, tax toggles, paper width, industry mode, hardware guide. |

Adaptive by default: a navigation rail on tablets (≥ 840 dp), a bottom bar on phones, and a dual-pane POS layout from 680 dp upward.

---

## Technology

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (BOM 2024.09.00) |
| Architecture | MVVM — `ViewModel` → `Repository` → Room `DAO` |
| Async | Coroutines + Flow (reactive Room queries) |
| Persistence | Room 2.7.0 (SQLite), versioned with an explicit migration |
| Camera / scanning | CameraX 1.5.0 + ZXing core 3.5.3 |
| Printing | Android `PrintManager`, WebView receipt, ESC/POS generation |
| QR rendering | Custom Compose `Canvas` renderer — no network, no ML Kit download |
| Build | AGP 9.1.1, KSP, Gradle Kotlin DSL, version catalog |
| Min / target SDK | 24 (Android 7.0) / 36 |
| Tests | JUnit 4, Robolectric, Compose UI test deps |

Deliberately dependency-light: no image loader, no charting library, no font-downloading at runtime — everything the app needs is bundled, so first launch works fully offline.

---

## Architecture

```
UI (Compose screens + components)
        │  StateFlow / immutable snapshots
        ▼
PosViewModel  ── analytics assembly, cart maths, payment orchestration
        │
        ▼
PosRepository ── settings (SharedPreferences) + aggregation façade
        │
        ├── ProductDao      · TransactionDao   · AnalyticsDao
        ▼
Room / SQLite (sajilo_pos_database)
```

Key decisions:
- **Single source of truth** — screens receive plain data + lambdas; no component touches the database.
- **Structured line items** — `sale_items` stores every sold line (unit price *after* concession, cost for margin) instead of parsing receipt text, so analytics are real SQL aggregations.
- **Pure analytics core** — `AnalyticsEngine` has no Android dependencies, so it is unit-testable and reusable.
- **Derived money maths** — cart totals, VAT, service charge and concessions are computed in the view model, never stored twice.

---

## Design system

Deliberately airy and high-legibility for fast counter use.

- **Typography** — bundled variable fonts, fully offline: **Manrope** for headings and money, **Inter** for body text. Full 17-style scale with tightened heading tracking and generous line heights.
- **Palette** — "Himalayan Paper": white cards on `#F7F9FA` paper, one pine-green brand accent (`#0E7C63`), marigold highlight (`#E9A13B`), hairline `#E7ECEF` borders. A matching dark theme is included.
- **Spacing** — a single `PosSpace` ladder (4 → 44 dp); 20 dp screen gutters, 20 dp card radius, no heavy shadows.
- **Components** — `PosCard`, `SectionHeader`, `StatTile`, `SoftPill`, `PosSearchField`, `PrimaryButton` / `GhostButton`, `StatusPill`, `EmptyState`, plus charts in `Charts.kt`.
- **Accessibility** — every interactive element has a content description or role, semantic test tags throughout, and TalkBack-readable charts (column-based bars with real text, not canvas-only).

---

## Getting started

### Requirements
- JDK 17+ (JDK 21 recommended)
- Android Studio with **Android SDK 36** installed
- A device or emulator on **Android 7.0 (API 24)** or newer

### Open & run
1. Open the project folder in Android Studio (Gradle sync runs automatically).
2. Select the `app` run configuration and press **Run**.

> **Note on the Gradle wrapper:** this repository currently ships only
> `gradle/wrapper/gradle-wrapper.properties`. The `gradlew` scripts and
> `gradle-wrapper.jar` are missing (also true of the original scaffold), so a
> command-line `./gradlew` build will not work until they are regenerated:
> ```bash
> gradle wrapper --gradle-version 9.3.1
> ```
> Android Studio will use its own bundled Gradle in the meantime.

### Configuration
All business settings live in the app (Settings tab) and persist to `SharedPreferences`:

- Shop name, PAN/VAT number, address, phone, currency symbol
- VAT on/off and rate, service charge on/off and rate
- Default thermal paper width (58 mm / 80 mm)
- Active industry mode

### Optional integrations
- `google-services.json` (Firebase) is intentionally **git-ignored**; the build tolerates its absence via `googleServices.missing.passthrough=true`.
- Release signing reads `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` from the environment — no key material in the repo.

---

## Project structure

```
SajiloPOS/
├── app/src/main/java/com/example/
│   ├── MainActivity.kt                  App shell: nav rail/bar, top bar, modals
│   ├── data/
│   │   ├── analytics/Analytics.kt       Pure analytics + restock maths
│   │   ├── local/                       Room DB, DAOs, projections, demo catalog
│   │   ├── model/PosModels.kt           Entities + domain models
│   │   └── repository/PosRepository.kt  Data access façade
│   └── ui/
│       ├── components/                  DesignSystem, Charts, scanner, payment, receipt, QR
│       ├── screens/                     Pos, Insights, Inventory, Transactions, Settings
│       ├── theme/                       Color, Type, Shape, Theme
│       ├── util/Format.kt               Money, dates and Nepali formatting
│       └── viewmodel/PosViewModel.kt    State, cart maths, orchestration
├── app/src/main/res/font/               Bundled Inter + Manrope (variable)
├── app/src/test/…                       Analytics + formatting unit tests
├── third_party_fonts/                   OFL licence texts
└── gradle/libs.versions.toml            Version catalog
```

---

## Data model

| Entity | Purpose |
| --- | --- |
| `products` | Catalog: names, barcode, category, price, cost, stock, unit, industry mode |
| `transactions` | Sale header: invoice no, timestamp, subtotal, service charge, VAT, total, payment method/status, reference, cash tendered & change, items summary |
| `sale_items` | Sale lines: product, qty, list price, unit price after concession, line total, cost total |

Sales are written in one transaction with their line items, and stock is decremented only after the payment is settled.

---

## Testing

```bash
./gradlew test          # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (device required)
```

Covered today: `AnalyticsEngineTest` (gap-free series, payment-mix shares, restock
prediction maths, urgency buckets, formatting) plus the existing cart/concession
and Robolectric smoke tests. Analytics logic is pure Kotlin precisely so it can be
tested without an emulator.

---

## What works today vs. what is stubbed

Honest status, so expectations are clear:

**Fully working**
Catalog & inventory, barcode scanning, cart and money maths, VAT/service charge,
cash checkout with change, QR generation and rendering, local sale recording,
sequential invoices, stock decrement, all five screens, analytics and restock
predictions, receipt preview/print/share/ESC/POS copy, settings persistence.

**Simulated / stubbed** (UI and flows are complete, the network call is not)
- Payment settlement: the Fonepay/eSewa/Khalti verification runs a timed state
  machine that resolves to `205_VERIFIED_OK` instead of calling a live gateway.
- Bluetooth thermal printing: receipts print through Android `PrintManager`;
  direct BLE/SPP socket writes are not implemented (the ESC/POS byte plan is
  generated and copyable for integration).

**Not started**
Cloud sync, KOT printing, order hold/recall, customer profiles, IRD period reports.

---

## Roadmap

Aligned with the requirement analysis document:

- **Phase 1 — Retail (MVP)** ·camera scanning, local SQLite, printer binding, QR verification → *implemented as an offline-first MVP*
- **Phase 2 — Transport & hospitality** ·KOT kitchen tickets, table split-bill, route fare matrices → *tables, concessions and ticketing shipped; KOT pending*
- **Phase 2 — Smart analytics** ·sales trends and predictive restocking → **shipped**
- **Phase 3 — Tax & compliance** ·automated IRD period reporting, e-filing export → *invoice format ready; reporting pending*
- **Cross-cutting** ·optional cloud sync of the local ledger, rugged tablet provisioning

---

## Known gaps

1. **Gradle wrapper jar is missing** — see [Getting started](#getting-started).
2. **Payment gateway calls are simulated**, not live (needs merchant credentials).
3. **No BLE printer socket** — printing relies on `PrintManager`.
4. **Namespace is `com.example`** — rename to your own domain before publishing.
5. **Version 1.0 / versionCode 1** — bump for releases.

---

## Licensing

Application code is private/proprietary unless stated otherwise. Bundled third-party
assets keep their own licences:

| Asset | Licence |
| --- | --- |
| Inter (variable) | SIL Open Font License 1.1 — `third_party_fonts/OFL-Inter.txt` |
| Manrope (variable) | SIL Open Font License 1.1 — `third_party_fonts/OFL-Manrope.txt` |
| ZXing | Apache 2.0 |
| Jetpack Compose / AndroidX | Apache 2.0 |

---

Built for Nepali small businesses. Say hello at the counter. 🇳🇵
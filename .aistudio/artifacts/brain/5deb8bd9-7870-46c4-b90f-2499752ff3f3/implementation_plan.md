# Tip & Split Calculator

A modern, elegant Android dining companion built with Jetpack Compose that allows users to quickly calculate tips, handle tax, customize percentages with instant breakout displays, and split bills seamlessly using either quick even splitting (with round-up) or detailed itemized assignment.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and calculation choices have been tailored based on your answers:

- **Confirmed Decision 1 (Tax & Tip Calculation)**: Configurable calculation basis switch allowing tip percentage to be calculated either on the **Pre-Tax Subtotal** (industry standard) or the **Post-Tax Subtotal** (subtotal + tax).
- **Confirmed Decision 2 (Split Mode Options)**: Both split models supported via an intuitive mode switch:
  1. *Quick Even Split*: Quick headcount stepper (1 to 20+ people) with an instant "Round up total / per-person" toggle.
  2. *Itemized Split*: Add bill items with prices and assign them to specific people or mark them as shared, automatically distributing tax and tip proportionally.
- **Confirmed Decision 3 (Visual Identity)**: Modern Dining Receipt aesthetic featuring clean Material 3 elevated cards, receipt scalloped/dashed dividers, warm parchment tones with sage/emerald accents, and monospace numbers for crisp tabular alignment.

---

### 1. Overview & Core Concept

- **What It Does**: Computes tip amounts, tax, total bills, and split breakdowns in real time as the user adjusts inputs. Offers preset tip chips (10%, 15%, 18%, 20%, 25%) plus a continuous slider and custom numeric input. Clearly presents the standalone tip, combined total, and individual shares.
- **Target Audience / Persona**: Diners, friends having group lunches/dinners, travelers, and event organizers who want swift, accurate, and transparent bill calculation without mental math friction.
- **Key Value**: Eliminates dinner table calculation confusion with instant feedback, clear itemized breakdowns, and zero awkward rounding discrepancies.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Quick Calculation Flow**:
   - User enters Subtotal and Tax (or tax rate %).
   - User selects tip % via quick chips (15%, 18%, 20%) or adjusts custom percentage slider/stepper.
   - User selects tip calculation basis ("Before Tax" vs "After Tax").
   - Summary card immediately reflects: Tip Amount, Tax Amount, Subtotal, and Grand Total.
2. **Even Split Flow**:
   - User sets number of people with stepper buttons (`-` / `+`).
   - Per-person share updates dynamically.
   - User can tap "Round Up Share" to round each person's payment to the next whole dollar (or nearest 50¢), displaying the small resulting extra tip adjustment.
3. **Itemized Split Flow**:
   - User switches to "Itemized Split" tab.
   - User can add diner names (e.g., Alice, Bob, Charlie).
   - User adds bill line items (e.g., "Tacos $14", "Drinks $22") and checks off who ordered them (or selects "Shared by All").
   - App computes subtotal, assigns proportional tax and tip, and produces individual payment summaries ready to copy or share.

#### Visual Identity & Theme
- **Aesthetic Direction**: Modern Dining Receipt — minimal, tactile, and warm. Evokes the clarity of an artisanal café ticket combined with Material 3 fluidity.
- **Color Palette**:
  - Primary / Accent: Warm Bistro Forest Green (`0xFF1B4332`, `0xFF2D6A4F`) conveying value and trust.
  - Secondary Accent: Warm Amber / Terracotta (`0xFFD97736`) for highlights and active chips.
  - Background: Soft cream paper tint (`0xFFFBFBF9`) in light mode, deep slate charcoal (`0xFF121413`) in dark mode.
  - Surface: Card white (`0xFFFFFFFF`) with subtle tonal elevation and dashed/serrated border divider styling.
- **Typography & Hierarchy**:
  - Display / Totals: Prominent bold tabular figures (`sp` scaled) for unmistakable glanceability.
  - Section Labels: M3 Title Medium in subtle muted ink tones.
  - Receipt Items: Clean Sans-Serif with Monospace tabular figures for all dollar amounts.
- **Interactive Feedback & Motion**:
  - Smooth animated transitions (`AnimatedContent` and `animateFloatAsState`) whenever switching split modes or adjusting tip percentages.
  - Haptic feedback touch indicators on stepper increment/decrement and tip presets.
  - Touch targets strictly minimum 48dp for effortless one-handed use while dining.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Pre-Tax vs. Post-Tax Calculation Basis**
  - *Chosen Approach*: Prominent segment toggle directly beneath the tax input ("Pre-tax tip" vs "Post-tax tip").
  - *Why*: In many regions diners tip solely on food/beverage subtotal, while in others people calculate on the register slip total. Giving a one-tap switch avoids friction.
  - *Alternatives Considered*: Hiding the toggle in a settings screen (rejected because users frequently need to switch based on who they dine with).

- **Decision 2: Dual-Mode Splitting (Quick Even vs. Itemized)**
  - *Chosen Approach*: A top tab or segmented button in the Split section lets users toggle seamlessly between "Quick Split" and "Itemized Split".
  - *Why*: Satisfies both the 5-second quick lunch split and the complex group dinner where one person had steak and another only had a soup.
  - *Alternatives Considered*: Forcing all calculations through an itemized builder (rejected because it adds too much friction for casual bills).

- **Decision 3: Round-Up Mechanism**
  - *Chosen Approach*: A dedicated "Round up" chip that rounds each person's share to the nearest dollar and clearly indicates the slight tip variance.
  - *Why*: Makes splitting cash or sending peer-to-peer payments (Venmo, Cash App) simple without dealing with odd cents.

- **Decision 4: State Architecture**
  - *Chosen Approach*: Clean Jetpack Compose `ViewModel` with reactive `StateFlow` holding the calculation models.
  - *Why*: Ensures instantaneous, zero-latency reactive recalculation across all fields as any digit or slider changes.

---

### 4. Technical Architecture & Data Strategy

#### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────┐
│                      MainActivity                      │
│                           │                            │
│                 TipCalculatorApp (Scaffold)            │
│                           │                            │
│                 TipCalculatorViewModel                 │
│              (StateFlow<TipCalculatorUiState>)        │
└───────────────────────────┬────────────────────────────┘
                            │
       ┌────────────────────┼────────────────────┐
       ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐     ┌──────────────┐
│  BillInput   │    │  TipConfig   │     │ SplitSection │
│  Card        │    │  Card        │     │ Card         │
├──────────────┤    ├──────────────┤     ├──────────────┤
│ - Subtotal   │    │ - Quick %    │     │ [Quick|Item] │
│ - Tax Amount │    │ - Custom %   │     │ - People +/- │
│ - Pre/Post   │    │ - Tip Slider │     │ - Round Up   │
│   Tax Toggle │    │ - Breakout   │     │ - Item List  │
└──────────────┘    └──────────────┘     └──────────────┘
                            │
                            ▼
              ┌───────────────────────────┐
              │   ReceiptSummaryCard      │
              ├───────────────────────────┤
              │ - Standalone Tip Amount   │
              │ - Subtotal & Tax Line     │
              │ - Grand Total             │
              │ - Per-Person Breakdown    │
              │ - One-Tap Share / Copy    │
              └───────────────────────────┘
```

#### Data Model & State

```kotlin
data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val assignedPersonIds: Set<String> = emptySet(), // empty = shared
)

data class Person(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
)

enum class TipBasis {
    PRE_TAX,
    POST_TAX
}

enum class SplitMode {
    EVEN,
    ITEMIZED
}

data class TipCalculatorUiState(
    val subtotalText: String = "",
    val taxText: String = "",
    val tipBasis: TipBasis = TipBasis.PRE_TAX,
    val tipPercentage: Double = 18.0,
    val splitMode: SplitMode = SplitMode.EVEN,
    val splitCount: Int = 1,
    val roundUpShare: Boolean = false,
    val people: List<Person> = listOf(Person(name = "Person 1"), Person(name = "Person 2")),
    val items: List<BillItem> = emptyList(),
)
```

#### Calculated Summary Model
- `subtotal`: parsed from user input (default 0.0).
- `tax`: parsed from user input (default 0.0).
- `tipBase`: `subtotal` if `PRE_TAX`, else `subtotal + tax`.
- `tipAmount`: `tipBase * (tipPercentage / 100)`.
- `grandTotal`: `subtotal + tax + tipAmount`.
- `evenShare`: `grandTotal / splitCount`, with optional ceiling rounding.
- `itemizedShares`: proportional allocation of shared items, itemized subtotals, tax, and tip calculated per person.

---

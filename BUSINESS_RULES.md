# Compensation rules v1

Inputs are nonnegative EUR monetary amounts with at most 16 integer digits and two decimal places. CoverageRate is between 0 and 1; REST accepts at most six fractional digits. AlreadyPaid cannot exceed policyLimit.

Damage means new damage for a distinct claim. AlreadyPaid means prior payouts reducing the policy's aggregate available limit. Redelivery of the same claim is not new damage; inbox/idempotency protection is planned.

```text
coveredDamage = damage × coverageRate
remainingLimit = policyLimit − alreadyPaid
amount = roundHalfEven(min(max(coveredDamage − deductible, 0), remainingLimit), 2)
```

Use BigDecimal; never double/float for money. Coverage precedes the deductible. This is a versioned educational-product rule, not a universal insurance formula.

POST `/api/v1/compensations/preview` requires a bearer JWT with scope `compensation:preview`:

```json
{"damage":20000,"coverageRate":0.9,"deductible":5000,"policyLimit":100000,"alreadyPaid":0}
```

Response:

```json
{"amount":13000.00,"currency":"EUR","ruleVersion":"coverage-before-deductible-v1"}
```

401: missing/invalid token. 403: insufficient scope. 400: invalid terms. Preview does not create a claim, payout, or database record. Damage calculation and policy APIs are implemented as separate demo services. FX conversion, fees and real transfers remain outside current behavior.

The event flow loads trusted policy terms; complete claim ownership enforcement remains to be implemented. Clients cannot select financial policy terms. Aggregate policy limits must be protected against concurrent claims before reliable concurrent claim processing; see ADR-008.

Demo investment weights and shock coefficients are implemented below; final product strategies, physical models and fee formulas remain undecided. Real market observations and simulated prices must remain distinguishable. Future allocations must sum to the available amount, respect strategy constraints, and retain inputs, rule versions, snapshot references, and explanations.

## Current demo allocations — 2026-10-05

FULL_PAYOUT assigns all compensation to payout; FULL_INVESTMENT assigns all to investment. HYBRID assigns half to payout with HALF_EVEN rounding to cents and assigns the exact remainder to investment. The event adapter currently selects HYBRID.

Conservative weights: 70% bonds, 20% gold, 10% BTC. Moderate: 40% bonds, 30% gold, 20% BTC, 10% ETH. Aggressive: 60% BTC, 30% ETH, 10% gold. Each allocation rounds to cents; the rounding remainder adjusts gold so totals equal the available investment amount. These are implemented educational rules, not finalized investment recommendations.

Damage multipliers are currently 10000 EUR per acceleration unit, 15000 EUR per water-depth unit and 500 EUR per wind-speed unit; unknown sensor types use a 5000 EUR fallback. Nonfinite/negative readings are rejected. No physical/geographic loss model or catastrophe threshold is claimed.

Payments use amountToPay; investments use amountToInvest and preserve the originating compensation ID. Policies provide trusted coverage/deductible/limit inputs, but aggregate prior payouts are not updated atomically. Duplicate financial effects and concurrency protection remain unfinished.

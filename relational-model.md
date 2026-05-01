# Relational Model

```text
                          ┌────────┐
                          │ Player │
                          └───┬────┘
                              │ 1
                              │
                              │ N
                              ▼
┌──────────┐ 1          N ┌──────┐ 1          N ┌────────┐
│ Paytable │ ───────────> │ Spin │ ───────────> │ Tumble │
└────┬─────┘              └──┬───┘              └────────┘
     │ 1                     │ ▲
     │                       │ │ parentSpin 1:N
     │ N                     └─┘ free spin children
     ▼
┌──────────────┐
│ SymbolConfig │
└──────────────┘
```

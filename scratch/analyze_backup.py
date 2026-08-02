import json
from datetime import datetime
from collections import Counter

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

db = data.get('database', {})
txs = db.get('transactions', [])
accounts = db.get('account_balances', [])

print(f"Total Transactions: {len(txs)}")
print("\n--- Account Balances ---")
for a in accounts:
    print(f"Bank: {a.get('bankName')}, Account: {a.get('accountNumber')}, Balance: {a.get('balance')}, Type: {a.get('accountType')}")

# Sum of transactions
incomes = [t for t in txs if t.get('transactionType') == 'INCOME']
expenses = [t for t in txs if t.get('transactionType') == 'EXPENSE']
transfers = [t for t in txs if t.get('transactionType') == 'TRANSFER']
credits = [t for t in txs if t.get('transactionType') == 'CREDIT']

print(f"\n--- Transactions Summary ---")
print(f"Incomes: {len(incomes)}, Sum: {sum(float(t['amount']) for t in incomes)}")
print(f"Expenses: {len(expenses)}, Sum: {sum(float(t['amount']) for t in expenses)}")
print(f"Transfers: {len(transfers)}, Sum: {sum(float(t['amount']) for t in transfers)}")
print(f"Credits: {len(credits)}, Sum: {sum(float(t['amount']) for t in credits)}")

# Check duplicate transaction checks (e.g. by same date, amount, description)
tx_signatures = []
for t in txs:
    sig = (t.get('dateTime'), t.get('amount'), t.get('transactionType'), t.get('bankName'))
    tx_signatures.append(sig)

dups = [item for item, count in Counter(tx_signatures).items() if count > 1]
print(f"\nDuplicate Transactions: {len(dups)}")
for d in dups[:10]:
    print(d)

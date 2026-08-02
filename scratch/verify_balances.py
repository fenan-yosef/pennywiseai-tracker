import json
from datetime import datetime

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

db = data.get('database', {})
txs = db.get('transactions', [])

# CBE Analysis
cbe_incomes = [t for t in txs if t.get('bankName') == 'Commercial Bank of Ethiopia' and t.get('transactionType') == 'INCOME']
cbe_expenses = [t for t in txs if t.get('bankName') == 'Commercial Bank of Ethiopia' and t.get('transactionType') == 'EXPENSE']

cbe_inc_sum = sum(float(t['amount']) for t in cbe_incomes)
cbe_exp_sum = sum(float(t['amount']) for t in cbe_expenses)

print("--- CBE Cash Flow ---")
print(f"Total Incomes: {cbe_inc_sum} ({len(cbe_incomes)} txs)")
print(f"Total Expenses: {cbe_exp_sum} ({len(cbe_expenses)} txs)")
print(f"Net Flow (Inc - Exp): {cbe_inc_sum - cbe_exp_sum}")
print(f"Starting Balance: 8693.36")
print(f"Expected Ending Balance: {8693.36 + cbe_inc_sum - cbe_exp_sum}")
print(f"Actual Ending Balance: 38900.27")
print(f"Discrepancy: {38900.27 - (8693.36 + cbe_inc_sum - cbe_exp_sum)}")

# BOA Analysis
boa_incomes = [t for t in txs if t.get('bankName') == 'Bank of Abyssinia' and t.get('transactionType') == 'INCOME']
boa_expenses = [t for t in txs if t.get('bankName') == 'Bank of Abyssinia' and t.get('transactionType') == 'EXPENSE']

boa_inc_sum = sum(float(t['amount']) for t in boa_incomes)
boa_exp_sum = sum(float(t['amount']) for t in boa_expenses)

print("\n--- BOA Cash Flow ---")
print(f"Total Incomes: {boa_inc_sum} ({len(boa_incomes)} txs)")
print(f"Total Expenses: {boa_exp_sum} ({len(boa_expenses)} txs)")
print(f"Net Flow (Inc - Exp): {boa_inc_sum - boa_exp_sum}")
print(f"Starting Balance: 73946.73")
print(f"Expected Ending Balance: {73946.73 + boa_inc_sum - boa_exp_sum}")
print(f"Actual Ending Balance: 21753.78")
print(f"Discrepancy: {21753.78 - (73946.73 + boa_inc_sum - boa_exp_sum)}")

import json

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

db = data.get('database', {})
txs = db.get('transactions', [])

print("--- TOP 10 LARGEST INCOMES ---")
incomes = sorted([t for t in txs if t.get('transactionType') == 'INCOME'], key=lambda x: float(x['amount']), reverse=True)
for t in incomes[:15]:
    print(f"Date: {t.get('dateTime')}, Amount: {t.get('amount')}, Bank: {t.get('bankName')}, Msg: {t.get('smsBody')[:120]}")

print("\n--- TOP 10 LARGEST EXPENSES ---")
expenses = sorted([t for t in txs if t.get('transactionType') == 'EXPENSE'], key=lambda x: float(x['amount']), reverse=True)
for t in expenses[:15]:
    print(f"Date: {t.get('dateTime')}, Amount: {t.get('amount')}, Bank: {t.get('bankName')}, Msg: {t.get('smsBody')[:120]}")

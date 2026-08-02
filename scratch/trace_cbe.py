import json
import re

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

db = data.get('database', {})
txs = db.get('transactions', [])

cbe_txs = sorted([t for t in txs if t.get('bankName') == 'Commercial Bank of Ethiopia'], key=lambda x: x.get('dateTime'))

print("--- CBE TRANSACTIONS WITH BALANCES ---")
for i, t in enumerate(cbe_txs):
    sms = t.get('smsBody', '')
    # Check for balance patterns in CBE messages
    bal_match = re.search(r'balance\s*(?:is)?\s*(?:ETB)?\s*([\d,]+\.\d{2})', sms, re.IGNORECASE)
    bal = bal_match.group(1) if bal_match else "None"
    
    # Only print those that contain balance, or first/last ones
    if bal != "None" or i < 5 or i > len(cbe_txs) - 5:
        print(f"[{i}] Date: {t.get('dateTime')}, Type: {t.get('transactionType')}, Amount: {t.get('amount')}, MsgBal: {bal}, Msg: {sms[:80]}")

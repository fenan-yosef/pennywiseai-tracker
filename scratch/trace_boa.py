import json
import re

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

db = data.get('database', {})
txs = db.get('transactions', [])

# Get all BOA transactions sorted by datetime
boa_txs = sorted([t for t in txs if t.get('bankName') == 'Bank of Abyssinia'], key=lambda x: x.get('dateTime'))

print("--- BOA TRANSACTIONS CHRONOLOGICAL ---")
for t in boa_txs:
    # Try to extract balance from smsBody
    sms = t.get('smsBody', '')
    bal_match = re.search(r'(?:Available Balance|Balance|Bal):\s*(?:ETB)?\s*([\d,]+\.\d{2})', sms, re.IGNORECASE)
    bal = bal_match.group(1) if bal_match else "None"
    print(f"Date: {t.get('dateTime')}, Type: {t.get('transactionType')}, Amount: {t.get('amount')}, MsgBal: {bal}, Msg: {sms[:80]}")

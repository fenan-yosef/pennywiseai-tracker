import json

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

db = data.get('database', {})
accounts = db.get('account_balances', [])

# Find the latest entry for each bank/account based on timestamp
latest_by_key = {}
for a in accounts:
    key = (a.get('bankName'), a.get('accountNumber'))
    ts = a.get('timestamp', '')
    if key not in latest_by_key or ts > latest_by_key[key].get('timestamp', ''):
        latest_by_key[key] = a

print("--- Actual Latest Balances (Sorted by Timestamp) ---")
total = 0.0
for key, a in latest_by_key.items():
    print(f"Bank: {key[0]}, Account: {key[1]}, Balance: {a.get('balance')}, Timestamp: {a.get('timestamp')}")
    if a.get('balance'):
        total += float(a.get('balance'))
print(f"Sum of latest balances: {total}")

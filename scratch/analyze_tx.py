import json

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

if 'database' in data:
    db = data['database']
    txs = db.get('transactions', [])
    for t in txs:
        if t.get('amount') == '55988.00' or t.get('transactionHash', '').startswith('06819a0f'):
            print("TX DETAILS:")
            print(json.dumps(t, indent=2))
            
            # Find related balance update
            bals = db.get('account_balances', [])
            related_bals = [b for b in bals if b.get('transactionId') == t.get('id')]
            print("RELATED BALANCES:")
            print(json.dumps(related_bals, indent=2))

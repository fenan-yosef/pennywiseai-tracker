import json
from datetime import datetime

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

if 'database' in data:
    db = data['database']
    txs = db.get('transactions', [])
    print(f"Total transactions in backup: {len(txs)}")
    
    # Filter CBE transactions
    cbe_txs = [t for t in txs if t.get('bankName') == 'Commercial Bank of Ethiopia']
    print(f"CBE transactions: {len(cbe_txs)}")
    
    # Filter by date range: last 3 months (since May 1, 2026)
    cutoff = datetime(2026, 5, 1)
    
    recent_cbe = []
    for t in cbe_txs:
        dt_str = t.get('dateTime')
        # Format can be ISO: '2026-07-31T19:37:56.643'
        try:
            # truncate milliseconds if needed
            dt_clean = dt_str.split('.')[0]
            dt = datetime.strptime(dt_clean, "%Y-%m-%dT%H:%M:%S")
            if dt >= cutoff:
                recent_cbe.append((dt, t))
        except Exception as e:
            print("Error parsing date:", dt_str, e)
            
    print(f"Recent CBE transactions (since 2026-05-01): {len(recent_cbe)}")
    
    # Sort by date desc
    recent_cbe.sort(key=lambda x: x[0], reverse=True)
    
    # Print summary
    print("\nRECENT CBE EXPENSES/INCOMES:")
    for dt, t in recent_cbe[:50]:
        print(f"{dt.strftime('%Y-%m-%d %H:%M:%S')} | {t.get('transactionType')} | Amount: {t.get('amount')} {t.get('currency')} | Merchant: {t.get('merchantName')} | Hash: {t.get('transactionHash')[:8]}")

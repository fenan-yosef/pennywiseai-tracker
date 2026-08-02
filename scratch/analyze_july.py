import json
from datetime import datetime

backup_path = r"c:\Users\HPZBOOK\StudioProjects\pennywiseai-tracker\PennyWise_Backup_2026_08_02_072141.pennywisebackup"

with open(backup_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

if 'database' in data:
    db = data['database']
    txs = db.get('transactions', [])
    
    # Let's count totals for July 2026 (since that's last month)
    july_incomes = []
    july_expenses = []
    july_transfers = []
    july_credits = []
    
    start_date = datetime(2026, 7, 1)
    end_date = datetime(2026, 7, 31, 23, 59, 59)
    
    for t in txs:
        if t.get('bankName') == 'Commercial Bank of Ethiopia' or t.get('bankName') == 'Bank of Abyssinia':
            dt_str = t.get('dateTime').split('.')[0]
            dt = datetime.strptime(dt_str, "%Y-%m-%dT%H:%M:%S")
            if start_date <= dt <= end_date:
                amount = float(t.get('amount'))
                tt = t.get('transactionType')
                if tt == 'INCOME':
                    july_incomes.append((dt, amount, t.get('smsBody')))
                elif tt == 'EXPENSE':
                    july_expenses.append((dt, amount, t.get('smsBody')))
                elif tt == 'TRANSFER':
                    july_transfers.append((dt, amount, t.get('smsBody')))
                elif tt == 'CREDIT':
                    july_credits.append((dt, amount, t.get('smsBody')))

    print(f"July Incomes Count: {len(july_incomes)}, Sum: {sum(x[1] for x in july_incomes)}")
    print(f"July Expenses Count: {len(july_expenses)}, Sum: {sum(x[1] for x in july_expenses)}")
    print(f"July Transfers Count: {len(july_transfers)}, Sum: {sum(x[1] for x in july_transfers)}")
    print(f"July Credits Count: {len(july_credits)}, Sum: {sum(x[1] for x in july_credits)}")

import sys, json
import pandas as pd
from pathlib import Path

# Paths
base = Path('Models/Gemini2.5/xml-w-link')
source = base / 'ref-diff.tsv'
out = base / 'ref-cases.json'

# Load TSV with BOM handling
if not source.exists():
    raise SystemExit(f'Missing source TSV: {source}')

df = pd.read_csv(source, sep='\t', encoding='utf-8-sig')

# Helpers
import re
def split_ptrs(val):
    if pd.isna(val) or str(val).strip() == '':
        return []
    return [s.strip() for s in str(val).split(',') if s.strip()]

def get_checked(ref_text):
    m = re.search(r'checked="(\d+)"', str(ref_text))
    return m.group(1) if m else None

cases = {}
for _, row in df.iterrows():
    before_ptrs = split_ptrs(row['ptr before inspection'])
    after_ptrs = split_ptrs(row['ptr after inspection'])
    b_len, a_len = len(before_ptrs), len(after_ptrs)
    same_ptrs = before_ptrs == after_ptrs

    if b_len == 0 and a_len == 0:
        case = 'no ptr before or after'
    elif b_len == 0 and a_len > 0:
        case = 'ptrs added'
    elif b_len > 0 and a_len == 0:
        case = 'ptrs removed'
    elif b_len > 1 and a_len == 1:
        case = 'multi -> single'
    elif b_len > 1 and a_len > 1:
        case = 'multi -> multi (changed)' if not same_ptrs else 'multi unchanged'
    elif b_len == 1 and a_len > 1:
        case = 'single -> multi'
    elif b_len == 1 and a_len == 1:
        case = 'single unchanged' if same_ptrs else 'single -> single (changed)'
    else:
        case = 'other'

    entry = {
        'file': row['file name'],
        'location': row['location'],
        'xml_id': row['xml id'],
        'ptr_before': before_ptrs,
        'ptr_after': after_ptrs,
        'checked_before': get_checked(row['ref before inspection']),
        'checked_after': get_checked(row['ref after inspection']),
    }
    cases.setdefault(case, []).append(entry)

out.write_text(json.dumps(cases, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'wrote {out}')

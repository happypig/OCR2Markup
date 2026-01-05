import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")

before_dir = Path("Models/Gemini2.5/xml-w-link/link-b4-check")
after_dir = Path("Models/Gemini2.5/xml-w-link/link-after-check")
ns_xml = "{http://www.w3.org/XML/1998/namespace}"

def load_refs(path: Path):
    data = path.read_bytes()
    last_err = None
    for encoding in ("xml", "utf-8-sig", "utf-16", "utf-16le", "utf-16be"):
        try:
            if encoding == "xml":
                tree = ET.ElementTree(ET.fromstring(data))
            else:
                text = data.decode(encoding, errors="replace")
                tree = ET.ElementTree(ET.fromstring(text))
            break
        except Exception as e:
            last_err = e
            continue
    else:
        raise last_err

    root = tree.getroot()
    refs = {}
    for ref in root.iter("ref"):
        xml_id = ref.attrib.get(f"{ns_xml}id") or ref.attrib.get("xml:id") or ref.attrib.get("id")
        if xml_id:
            refs[xml_id] = ref
    return refs

def clean(text: str) -> str:
    return " ".join(text.split()) if text else ""

rows = []
for dirpath, _, filenames in os.walk(before_dir):
    for fname in filenames:
        if not fname.lower().endswith(".xml"):
            continue
        before_path = Path(dirpath) / fname
        rel_dir = Path(os.path.relpath(dirpath, before_dir))
        after_path = after_dir / rel_dir / fname

        try:
            before_refs = load_refs(before_path)
        except Exception as e:
            print(f"Error parsing {before_path}: {e}", file=sys.stderr, flush=True)
            continue

        after_refs = {}
        if after_path.exists():
            try:
                after_refs = load_refs(after_path)
            except Exception as e:
                print(f"Error parsing {after_path}: {e}", file=sys.stderr, flush=True)

        for xml_id, ref_before in before_refs.items():
            ptr_before = [p.attrib.get("href", "") for p in ref_before.findall("ptr")]
            ref_before_str = clean(ET.tostring(ref_before, encoding="unicode", method="xml"))
            ref_after = after_refs.get(xml_id)
            if ref_after is not None:
                ptr_after = [p.attrib.get("href", "") for p in ref_after.findall("ptr")]
                ref_after_str = clean(ET.tostring(ref_after, encoding="unicode", method="xml"))
            else:
                ptr_after = []
                ref_after_str = ""

            if len(ptr_before) > 1 or ptr_before != ptr_after:
                result = "yes" if ptr_after else "no"
                rows.append([
                    fname,
                    "" if rel_dir == Path(".") else str(rel_dir),
                    xml_id,
                    ref_before_str,
                    ", ".join(ptr_before),
                    ref_after_str,
                    ", ".join(ptr_after),
                    result,
                ])

header = [
    "file name",
    "location",
    "xml id",
    "ref before inspection",
    "ptr before inspection",
    "ref after inspection",
    "ptr after inspection",
    "result in ptr list",
]
print("\t".join(header))
for r in rows:
    print("\t".join(r))

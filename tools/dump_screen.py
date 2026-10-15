#!/usr/bin/env python3
import subprocess
import xml.etree.ElementTree as ET
import json
import sys
import os

ADB_PATH = "/home/tuncay/Android/Sdk/platform-tools/adb"

def run_adb(cmd):
    full_cmd = f"{ADB_PATH} {cmd}"
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def dump_ui():
    print("⏳ Telefonda mevcut ekran yakalanıyor...")
    run_adb("shell uiautomator dump /sdcard/nomore_dump.xml")
    temp_xml = "/tmp/nomore_dump.xml"
    run_adb(f"pull /sdcard/nomore_dump.xml {temp_xml}")
    
    if not os.path.exists(temp_xml):
        print("❌ Ekran dökümü alınamadı. Telefonun ekranının açık olduğundan emin olun.")
        sys.exit(1)
        
    return temp_xml

def parse_node(elem):
    attrib = elem.attrib
    node_data = {
        "class": attrib.get("class", ""),
        "id": attrib.get("resource-id", ""),
        "desc": attrib.get("content-desc", ""),
        "text": attrib.get("text", ""),
        "selected": attrib.get("selected", "") == "true",
        "clickable": attrib.get("clickable", "") == "true",
        "bounds": attrib.get("bounds", "")
    }
    
    children = []
    for child in elem:
        children.append(parse_node(child))
        
    if children:
        node_data["children"] = children
        
    return node_data

def main():
    screen_name = sys.argv[1] if len(sys.argv) > 1 else "screen"
    xml_path = dump_ui()
    
    tree = ET.parse(xml_path)
    root = tree.getroot()
    
    # JSON verisine dönüştür
    snapshot_json = parse_node(root)
    
    # Önemli alt bar sekmelerini ve düğümleri tespit et
    tabs = []
    def find_tabs(node):
        r_id = node.get("id", "")
        desc = node.get("desc", "")
        if "tab" in r_id or desc in ["Home", "Reels", "Search", "Profile", "Ana Sayfa", "Profil"] or "clips" in r_id:
            tabs.append({
                "id": r_id,
                "desc": desc,
                "selected": node.get("selected"),
                "clickable": node.get("clickable"),
                "class": node.get("class")
            })
        for child in node.get("children", []):
            find_tabs(child)
            
    find_tabs(snapshot_json)
    
    output_dir = "app/src/test/resources/fixtures"
    os.makedirs(output_dir, exist_ok=True)
    out_file = f"{output_dir}/{screen_name}.json"
    
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(snapshot_json, f, ensure_ascii=False, indent=2)
        
    print(f"✅ Ekran dökümü başarıyla kaydedildi: {out_file}")
    print("\n🔍 Tespit Edilen Önemli Sekmeler / Düğümler:")
    print("--------------------------------------------------")
    for t in tabs:
        sel = " [SEÇİLİ ★]" if t['selected'] else ""
        print(f"• ID: {t['id']:<35} | Açıklama: '{t['desc']}'{sel}")
    print("--------------------------------------------------")

if __name__ == "__main__":
    main()

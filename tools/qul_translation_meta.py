#!/usr/bin/env python3
"""Pisahkan unduhan terjemahan QUL (data-src/translation/*.db) menjadi satu file per terjemahan,
lengkap dengan metadata (id QUL, judul, bahasa, deskripsi).

Latar belakang: server QUL memberi nama file sama untuk terjemahan berjudul sama, dan unduhan
berikutnya BERISI unduhan sebelumnya (ditambahkan di belakang). Skrip ini:
  1. mengambil daftar publik + halaman tiap resource (cache di data-src/.qul-cache/),
  2. memecah tiap db menjadi blok terjemahan (kemunculan ke-n tiap ayah_key = blok ke-n),
  3. mencocokkan tiap blok ke id resource lewat teks 73:4 (cadangan: ayat lain untuk kandidat
     dengan slug judul yang sama),
  4. membuang duplikat (id sama -> ambil blok terlengkap),
  5. menulis data-src/translation-split/<id>-<slug>.db dan index.json.

Contoh:
    python tools/qul_translation_meta.py
    python tools/qul_translation_meta.py --offline   # hanya cache
    python tools/qul_translation_meta.py --dry-run   # tidak menulis keluaran
    python tools/qul_translation_meta.py --footnotes # pecah juga data-src/translation-footnote/ (varian dengan catatan kaki)

Mode --footnotes (dijalankan SETELAH mode biasa): tiap blok di data-src/translation-footnote/*.db dicocokkan ke id QUL
dengan membandingkan teksnya (tag catatan kaki dibuang) dengan berkas simple hasil pemecahan di translation-split/.
Keluaran: data-src/translation-footnote-split/<id>-<slug>.db (kolom footnotes dipertahankan) dan index.json.
"""
import argparse
import difflib
import html
import json
import re
import sqlite3
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "data-src" / "translation"
OUT = ROOT / "data-src" / "translation-split"
FN_SRC = ROOT / "data-src" / "translation-footnote"
FN_OUT = ROOT / "data-src" / "translation-footnote-split"
CACHE = ROOT / "data-src" / ".qul-cache"
BASE = "https://qul.tarteel.ai/resources/translation"
UA = "Mozilla/5.0 (X11; Linux x86_64) qul_translation_meta/1.0"
TOTAL = 6236
PREVIEW = (73, 4)


def norm(s):
    return " ".join((s or "").split())


def loose(s):
    """Kunci longgar: hanya huruf (buang angka catatan kaki, tanda baca, spasi)."""
    return "".join(ch for ch in (s or "") if ch.isalpha())


def same(a, b):
    """Teks pratinjau QUL bisa terpotong dan memuat angka catatan kaki; cocokkan secara longgar."""
    a, b = loose(a), loose(b)
    if len(a) < 6 or len(b) < 6:
        return a == b and a != ""
    return a.startswith(b) or b.startswith(a)


def slugify(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")


class Fetcher:
    def __init__(self, offline):
        self.offline = offline
        self.last = 0.0
        self.fetched = 0

    def get(self, url, cache_file):
        if cache_file.exists():
            return cache_file.read_text(encoding="utf-8")
        if self.offline:
            return None
        wait = 1.0 - (time.time() - self.last)
        if wait > 0:
            time.sleep(wait)
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        for attempt in range(3):
            try:
                with urllib.request.urlopen(req, timeout=30) as r:
                    body = r.read().decode("utf-8")
                break
            except Exception as e:  # noqa: BLE001
                print(f"  gagal ambil {url}: {e} (percobaan {attempt + 1})")
                time.sleep(2 * (attempt + 1))
        else:
            self.last = time.time()
            return None
        self.last = time.time()
        self.fetched += 1
        CACHE.mkdir(parents=True, exist_ok=True)
        cache_file.write_text(body, encoding="utf-8")
        return body


def parse_listing(page):
    """Kembalikan {id: {title, language}} dari kartu di halaman daftar."""
    out = {}
    hits = [(m.start(), int(m.group(1))) for m in re.finditer(r'href="/resources/translation/(\d+)"', page)]
    for i, (pos, rid) in enumerate(hits):
        if rid in out:
            continue
        end = hits[i + 1][0] if i + 1 < len(hits) else len(page)
        seg = page[pos:end]
        t = re.search(r"<span>([^<]+)</span>", seg)
        title = html.unescape(t.group(1)).strip() if t else ""
        lang = ""
        for m in re.finditer(r'<a class="tag[^>]*>\s*<span>([^<]+)</span>', seg):
            v = html.unescape(m.group(1)).strip()
            if v not in ("Translation", "Ayah by Ayah"):
                lang = v
                break
        out[rid] = {"title": title, "language": lang}
    return out


def parse_resource(page):
    m = re.search(r'<meta name="description" content="([^"]*)"', page)
    preview = norm(html.unescape(m.group(1))) if m else None
    d = re.search(r'<div class="text-gray-700">\s*(<p>.*?)</div>', page, re.S)
    desc = ""
    if d:
        ps = re.findall(r"<p>(.*?)</p>", d.group(1), re.S)
        desc = "\n".join(norm(html.unescape(re.sub(r"<[^>]+>", "", p))) for p in ps)
    return preview, desc


def split_blocks(path):
    """Pecah satu db menjadi blok: list of (list rows). Kolom sura/ayah di beberapa file rusak, pakai ayah_key."""
    con = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
    try:
        rows = con.execute("SELECT sura, ayah, ayah_key, text FROM translation ORDER BY rowid").fetchall()
    finally:
        con.close()
    # Urutan baris di file tidak bisa dipercaya (ada yang terurut per-string sura: 1,10,100,...),
    # jadi blok = kemunculan ke-n tiap ayah_key (unduhan yang ditambahkan = lapisan berikutnya).
    layers, seen = [], {}
    for r in rows:
        n = seen.get(r[2], 0)
        seen[r[2]] = n + 1
        while len(layers) <= n:
            layers.append([])
        layers[n].append(r)
    blocks = [sorted(l, key=lambda r: tuple(int(x) for x in r[2].split(":"))) for l in layers]
    return blocks


def plain(raw):
    """Teks varian with-footnote-tags (terbungkus JSON, berisi tag <sup>/<a>) menjadi teks polos."""
    t = raw or ""
    if t.startswith('"') and t.endswith('"'):
        try:
            t = json.loads(t)
        except ValueError:
            pass
    return html.unescape(re.sub(r"<[^>]+>", "", t))


def split_footnotes():
    """Pecah data-src/translation-footnote/*.db per terjemahan; cocokkan ke id lewat berkas simple di translation-split/."""
    simple = {}
    for p in sorted(OUT.glob("*.db")):
        con = sqlite3.connect(f"file:{p}?mode=ro", uri=True)
        simple[int(p.name.split("-")[0])] = {k: t for k, t in con.execute("SELECT ayah_key, text FROM translation")}
        con.close()
    index_simple = {e["id"]: e for e in json.loads((OUT / "index.json").read_text(encoding="utf-8"))}
    probes = ["1:1", "1:2", "2:1", "2:255", "3:1", "4:1", "18:10", "36:1", "55:1", "73:4", "73:5", "100:1", "112:1", "112:2", "113:1", "114:1", "114:6", "2:2", "2:3", "5:3"]
    best = {}  # rid -> (rows, file, blok, skor)
    unresolved = []
    for p in sorted(FN_SRC.glob("*.db")):
        con = sqlite3.connect(f"file:{p}?mode=ro", uri=True)
        rows = con.execute("SELECT sura, ayah, ayah_key, text, footnotes FROM translation ORDER BY rowid").fetchall()
        con.close()
        layers, seen = [], {}
        for r in rows:
            n = seen.get(r[2], 0)
            seen[r[2]] = n + 1
            while len(layers) <= n:
                layers.append([])
            layers[n].append(r)
        for bi, layer in enumerate(layers):
            layer.sort(key=lambda r: tuple(int(x) for x in r[2].split(":")))
            texts = {r[2]: plain(r[3]) for r in layer}
            scores = []
            for rid, st in simple.items():
                hit = [same(plain(st.get(k)), texts[k]) for k in probes if k in texts and k in st]
                if hit:
                    scores.append((sum(hit) / len(hit), rid))
            scores.sort(reverse=True)
            # Pemenang jelas: skor >= 0.6 dan unggul >= 0.1 dari peringkat kedua (teks simple kadang beda sedikit dari varian catatan kaki).
            top = [scores[0][1]] if scores and scores[0][0] >= 0.6 and (len(scores) < 2 or scores[0][0] - scores[1][0] >= 0.1) else []
            if len(top) != 1:
                unresolved.append(f"{p.name}#blok{bi} ({len(layer)} baris, skor tertinggi {scores[:3]})")
                continue
            rid = top[0]
            if rid not in best or len(layer) > best[rid][0]:
                best[rid] = (len(layer), p.name, layer, bi, scores[0][0])
    print(f"Blok footnote tak terselesaikan: {len(unresolved)}")
    for u in unresolved:
        print("  -", u)
    index = []
    for rid in sorted(best):
        n, src, layer, bi, sc = best[rid]
        e = index_simple.get(rid, {})
        index.append({"id": rid, "title": e.get("title", ""), "language": e.get("language", ""), "rows": n,
                      "complete": n == TOTAL, "source_file": src, "block": bi, "match": round(sc, 3)})
    FN_OUT.mkdir(parents=True, exist_ok=True)
    for old in FN_OUT.glob("*.db"):
        old.unlink()
    for rid in sorted(best):
        slug = slugify(index_simple.get(rid, {}).get("title", "")) or "resource"
        con = sqlite3.connect(FN_OUT / f"{rid}-{slug}.db")
        con.execute("CREATE TABLE translation(sura INTEGER, ayah INTEGER, ayah_key TEXT, text TEXT, footnotes TEXT)")
        con.executemany("INSERT INTO translation VALUES (?,?,?,?,?)", best[rid][2])
        con.commit()
        con.close()
    (FN_OUT / "index.json").write_text(json.dumps(index, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"Ditulis {len(index)} db footnote + index.json di {FN_OUT}")
    for e in index:
        if not e["complete"] or e["match"] < 0.95:
            print(f"  perhatikan: {e['id']} {e['title']} baris={e['rows']} cocok={e['match']}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--offline", action="store_true", help="hanya pakai cache, tanpa jaringan")
    ap.add_argument("--dry-run", action="store_true", help="jangan tulis keluaran")
    ap.add_argument("--footnotes", action="store_true", help="pecah data-src/translation-footnote/ (butuh translation-split/ sudah ada)")
    args = ap.parse_args()
    if args.footnotes:
        split_footnotes()
        return
    t0 = time.time()
    f = Fetcher(args.offline)

    listing_page = f.get(BASE, CACHE / "listing.html")
    if not listing_page:
        sys.exit("Daftar resource tidak tersedia (cache kosong / gagal ambil).")
    listing = parse_listing(listing_page)
    print(f"Resource di daftar: {len(listing)}")

    # info[id] = {preview: {ayah_key: teks}, description}
    info = {}

    def load(rid, key="73:4"):
        """Muat (dan simpan) teks pratinjau ayat `key` untuk resource rid."""
        rec = info.setdefault(rid, {"preview": {}, "description": ""})
        if key in rec["preview"]:
            return rec["preview"][key]
        url = f"{BASE}/{rid}" + ("" if key == "73:4" else "?ayah=" + key.replace(":", "%3A"))
        page = f.get(url, CACHE / f"r{rid}_{key.replace(':', '-')}.html")
        if page is None:
            return None
        pv, desc = parse_resource(page)
        rec["preview"][key] = pv
        if desc and not rec["description"]:
            rec["description"] = desc
        return pv

    for n, rid in enumerate(sorted(listing), 1):
        load(rid)
        if f.fetched and n % 25 == 0:
            print(f"  ... {n}/{len(listing)} halaman")

    def match_73_4(t):
        return [rid for rid in listing if same(info[rid]["preview"].get("73:4"), t)]

    slugs = {rid: slugify(v["title"]) for rid, v in listing.items()}

    def candidates(stem):
        base = re.sub(r"-\d+$", "", stem)
        base = re.sub(r"-simple$", "", base)
        return [rid for rid, s in slugs.items() if s and (base == s or base.startswith(s + "-") or s.startswith(base))]

    best = {}  # rid -> (rows, file, blockindex)
    unresolved = []
    nblocks = 0
    files = sorted(SRC.glob("*.db"))
    for p in files:
        for bi, rows in enumerate(split_blocks(p)):
            nblocks += 1
            texts = {r[2]: norm(r[3]) for r in rows}
            rid = None
            why = ""
            t = texts.get("73:4")
            if t:
                ids = match_73_4(t)
                if len(ids) == 1:
                    rid = ids[0]
                elif len(ids) > 1:
                    cands = [i for i in ids if i in candidates(p.stem)]
                    why = f"73:4 ambigu {ids}"
                    pool = cands or ids
                else:
                    why = "73:4 tidak cocok"
                    pool = candidates(p.stem)
            else:
                why = "tanpa 73:4"
                pool = candidates(p.stem)
            if rid is None:
                if why == "73:4 tidak cocok" and not pool:
                    # tak ada kandidat dari nama file: ambil yang paling mirip teks 73:4
                    sc = sorted(((difflib.SequenceMatcher(None, loose(t), loose(info[i]["preview"].get("73:4"))).ratio(), i)
                                 for i in listing if info[i]["preview"].get("73:4")), reverse=True)
                    pool = [i for r_, i in sc[:4] if r_ > 0.4]
                keys = [r[2] for r in rows]
                probes = [k for k in ("1:1", "1:2", "2:1", "2:2", "3:1", "4:1") if k in texts] or keys[:3]
                hit = list(pool)
                for key in probes:
                    if len(hit) <= 1:
                        break
                    hit = [i for i in hit if same(load(i, key), texts.get(key))]
                if len(hit) == 1:
                    rid = hit[0]
                if rid is None:
                    unresolved.append(f"{p.name}#blok{bi} ({len(rows)} baris, {why}; kandidat {pool})")
                    continue
            if rid not in best or len(rows) > best[rid][0]:
                best[rid] = (len(rows), p.name, rows)

    no_file = sorted(set(listing) - set(best))
    print(f"File db: {len(files)}, blok terjemahan: {nblocks}")
    print(f"Resource tercocok (unik): {len(best)}")
    print(f"Blok tak terselesaikan: {len(unresolved)}")
    for u in unresolved:
        print("  -", u)
    print(f"Id di daftar tanpa file: {len(no_file)}")
    for rid in no_file:
        print(f"  - {rid} {listing[rid]['title']} [{listing[rid]['language']}]")
    unknown = [r for r in best if r not in listing]
    if unknown:
        print("Id cocok tapi tidak di daftar:", unknown)

    index = []
    for rid in sorted(best):
        n, src, rows = best[rid]
        meta = listing.get(rid, {"title": "", "language": ""})
        index.append({"id": rid, "title": meta["title"], "language": meta["language"],
                      "description": info[rid]["description"], "rows": n,
                      "complete": n == TOTAL, "source_file": src})
    if args.dry_run:
        print("[dry-run] tidak menulis keluaran.")
    else:
        OUT.mkdir(parents=True, exist_ok=True)
        for old in OUT.glob("*.db"):
            old.unlink()
        for rid in sorted(best):
            name = f"{rid}-{slugs.get(rid) or 'resource'}.db"
            con = sqlite3.connect(OUT / name)
            con.execute("CREATE TABLE translation(sura INTEGER, ayah INTEGER, ayah_key TEXT, text TEXT)")
            con.executemany("INSERT INTO translation VALUES (?,?,?,?)", best[rid][2])
            con.commit()
            con.close()
        (OUT / "index.json").write_text(json.dumps(index, ensure_ascii=False, indent=1), encoding="utf-8")
        print(f"Ditulis {len(index)} db + index.json di {OUT}")
    print(f"Lengkap: {sum(i['complete'] for i in index)}, parsial: {sum(not i['complete'] for i in index)}")
    print(f"Waktu: {time.time() - t0:.0f} dtk (ambil jaringan: {f.fetched})")


if __name__ == "__main__":
    main()

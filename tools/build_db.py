#!/usr/bin/env python3
"""Bangun app/src/main/assets/quran.db dari file QUL di data-src/.

Pakai: python tools/build_db.py [data_src_dir] [output_db]
Butuh: pip install fonttools (membaca cmap font nama surah).
"""
import html
import json
import re
import sqlite3
import sys
from pathlib import Path

from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parent.parent
SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "data-src"
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "app/src/main/assets/quran.db"

DATA_VERSION = 7

TRANSLATIONS = [
    # id, lang, nama tampil, sumber, folder
    ("id-kemenag", "id", "Kemenag RI", "Kementerian Agama RI", "quran-id-with-footnote-tags"),
    ("id-sabiq", "id", "The Sabiq Company", "The Sabiq Company", "the-sabiq-company-with-footnote-tags"),
    ("id-kfqpc", "id", "King Fahad Quran Complex", "King Fahad Quran Complex", "king-fahad-quran-complex-with-footnote-tags"),
    ("en-sahih", "en", "Saheeh International", "Saheeh International", "en-sahih-international-with-footnote-tags"),
]

# Rekonstruksi manual untuk ayat yang penanda catatannya rusak di sumber (urutan = urutan id catatan).
TEXT_OVERRIDES = {
    ("id-kfqpc", 1, 2): "Segala puji<sup>1</sup> bagi Allah, Tuhan<sup>2</sup> semesta alam.",
    ("id-kfqpc", 2, 194): (
        "Bulan haram dengan bulan haram<sup>1</sup>, dan pada sesuatu yang patut dihormati<sup>2</sup>, "
        "berlaku hukum kisas. Oleh sebab itu, barang siapa yang menyerang kamu, maka seranglah ia, "
        "seimbang dengan serangannya terhadapmu. Bertakwalah kepada Allah dan ketahuilah bahwa Allah "
        "beserta orang-orang yang bertakwa."
    ),
}

# Transliterasi Sabiq memakai '>' sebagai tanda panjang (Sala>m, Lu>t).
SABIQ_FIXES = [("a&gt;", "ā"), ("u&gt;", "ū"), ("i&gt;", "ī")]

SURAHS = """\
الفاتحة|Al-Fatihah
البقرة|Al-Baqarah
آل عمران|Ali 'Imran
النساء|An-Nisa'
المائدة|Al-Ma'idah
الأنعام|Al-An'am
الأعراف|Al-A'raf
الأنفال|Al-Anfal
التوبة|At-Taubah
يونس|Yunus
هود|Hud
يوسف|Yusuf
الرعد|Ar-Ra'd
إبراهيم|Ibrahim
الحجر|Al-Hijr
النحل|An-Nahl
الإسراء|Al-Isra'
الكهف|Al-Kahf
مريم|Maryam
طه|Taha
الأنبياء|Al-Anbiya'
الحج|Al-Hajj
المؤمنون|Al-Mu'minun
النور|An-Nur
الفرقان|Al-Furqan
الشعراء|Asy-Syu'ara'
النمل|An-Naml
القصص|Al-Qasas
العنكبوت|Al-'Ankabut
الروم|Ar-Rum
لقمان|Luqman
السجدة|As-Sajdah
الأحزاب|Al-Ahzab
سبأ|Saba'
فاطر|Fatir
يس|Yasin
الصافات|As-Saffat
ص|Sad
الزمر|Az-Zumar
غافر|Ghafir
فصلت|Fussilat
الشورى|Asy-Syura
الزخرف|Az-Zukhruf
الدخان|Ad-Dukhan
الجاثية|Al-Jasiyah
الأحقاف|Al-Ahqaf
محمد|Muhammad
الفتح|Al-Fath
الحجرات|Al-Hujurat
ق|Qaf
الذاريات|Az-Zariyat
الطور|At-Tur
النجم|An-Najm
القمر|Al-Qamar
الرحمن|Ar-Rahman
الواقعة|Al-Waqi'ah
الحديد|Al-Hadid
المجادلة|Al-Mujadalah
الحشر|Al-Hasyr
الممتحنة|Al-Mumtahanah
الصف|As-Saff
الجمعة|Al-Jumu'ah
المنافقون|Al-Munafiqun
التغابن|At-Tagabun
الطلاق|At-Talaq
التحريم|At-Tahrim
الملك|Al-Mulk
القلم|Al-Qalam
الحاقة|Al-Haqqah
المعارج|Al-Ma'arij
نوح|Nuh
الجن|Al-Jinn
المزمل|Al-Muzzammil
المدثر|Al-Muddassir
القيامة|Al-Qiyamah
الإنسان|Al-Insan
المرسلات|Al-Mursalat
النبأ|An-Naba'
النازعات|An-Nazi'at
عبس|'Abasa
التكوير|At-Takwir
الانفطار|Al-Infitar
المطففين|Al-Mutaffifin
الانشقاق|Al-Insyiqaq
البروج|Al-Buruj
الطارق|At-Tariq
الأعلى|Al-A'la
الغاشية|Al-Gasyiyah
الفجر|Al-Fajr
البلد|Al-Balad
الشمس|Asy-Syams
الليل|Al-Lail
الضحى|Ad-Duha
الشرح|Asy-Syarh
التين|At-Tin
العلق|Al-'Alaq
القدر|Al-Qadr
البينة|Al-Bayyinah
الزلزلة|Az-Zalzalah
العاديات|Al-'Adiyat
القارعة|Al-Qari'ah
التكاثر|At-Takasur
العصر|Al-'Asr
الهمزة|Al-Humazah
الفيل|Al-Fil
قريش|Quraisy
الماعون|Al-Ma'un
الكوثر|Al-Kausar
الكافرون|Al-Kafirun
النصر|An-Nasr
المسد|Al-Masad
الإخلاص|Al-Ikhlas
الفلق|Al-Falaq
الناس|An-Nas
"""

SCHEMA = """
CREATE TABLE surahs (
    id INTEGER PRIMARY KEY, name_ar TEXT NOT NULL, name_latin TEXT NOT NULL,
    ayah_count INTEGER NOT NULL, first_page INTEGER NOT NULL, name_glyph INTEGER NOT NULL,
    place TEXT, revelation_order INTEGER
);
CREATE TABLE surah_info (
    lang TEXT NOT NULL, surah INTEGER NOT NULL, name TEXT NOT NULL, short_text TEXT NOT NULL, text TEXT NOT NULL,
    PRIMARY KEY (lang, surah)
) WITHOUT ROWID;
CREATE TABLE transliteration (
    surah INTEGER NOT NULL, ayah INTEGER NOT NULL, text TEXT NOT NULL, PRIMARY KEY (surah, ayah)
) WITHOUT ROWID;
CREATE TABLE juz (id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, page INTEGER NOT NULL);
CREATE TABLE hizb (id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, page INTEGER NOT NULL);
CREATE TABLE rub (id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, page INTEGER NOT NULL);
CREATE TABLE manzil (id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, page INTEGER NOT NULL);
CREATE TABLE ruku (
    id INTEGER PRIMARY KEY, surah_ruku INTEGER NOT NULL, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, page INTEGER NOT NULL
);
CREATE TABLE sajda (
    id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, page INTEGER NOT NULL, type TEXT NOT NULL
);
CREATE TABLE page_lines (
    page INTEGER NOT NULL, line INTEGER NOT NULL, type TEXT NOT NULL, centered INTEGER NOT NULL,
    first_word INTEGER, last_word INTEGER, surah INTEGER,
    PRIMARY KEY (page, line)
) WITHOUT ROWID;
CREATE TABLE words (
    id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, pos INTEGER NOT NULL,
    text TEXT NOT NULL, page INTEGER NOT NULL, line INTEGER NOT NULL
);
CREATE INDEX words_ayah ON words (surah, ayah);
CREATE TABLE ayahs (
    surah INTEGER NOT NULL, ayah INTEGER NOT NULL, text_ar TEXT NOT NULL, page INTEGER NOT NULL,
    PRIMARY KEY (surah, ayah)
) WITHOUT ROWID;
CREATE INDEX ayahs_page ON ayahs (page);
CREATE TABLE translations (id TEXT PRIMARY KEY, lang TEXT NOT NULL, name TEXT NOT NULL, source TEXT NOT NULL);
CREATE TABLE translation_texts (
    tr TEXT NOT NULL, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, text TEXT NOT NULL,
    PRIMARY KEY (tr, surah, ayah)
) WITHOUT ROWID;
CREATE TABLE footnotes (
    tr TEXT NOT NULL, surah INTEGER NOT NULL, ayah INTEGER NOT NULL, idx INTEGER NOT NULL,
    label INTEGER, text TEXT NOT NULL,
    PRIMARY KEY (tr, surah, ayah, idx)
) WITHOUT ROWID;
"""

SUP = re.compile(r'<sup foot_note="(\d+)">(\d+)</sup>')


def surah_name_glyphs():
    """Glyph 'سورة X' berurutan kode, dimulai dari surah 22 (Al-Hajj) lalu berputar.

    Kode sama untuk font nama surah (daftar) dan font header berwarna (bingkai di halaman).
    """
    cmap = TTFont(next((SRC / "surah-names").glob("*.ttf"))).getBestCmap()
    codes = sorted(c for c in cmap if c >= 0xE000)[:114]
    return {sid: codes[(sid - 22) % 114] for sid in range(1, 115)}


def clean_note(text: str) -> str:
    text = re.sub(r"</p>\s*<p>", "\n\n", text)
    text = re.sub(r"</?p>", "", text)
    return html.unescape(text).strip()


def build_translation(db, tr_id, folder):
    src = next((SRC / folder).glob("*.db"))
    rows = sqlite3.connect(src).execute(
        "SELECT sura, ayah, text, footnotes FROM translation ORDER BY sura, ayah"
    ).fetchall()
    stats = {"overrides": 0, "unreferenced": 0}
    for surah, ayah, raw_text, raw_notes in rows:
        text = json.loads(raw_text)
        notes = json.loads(raw_notes) if raw_notes else {}
        ids = list(notes)  # urutan id naik = urutan kemunculan

        override = TEXT_OVERRIDES.get((tr_id, surah, ayah))
        if override is not None:
            text, labels = override, {i: n + 1 for n, i in enumerate(ids)}
            stats["overrides"] += 1
        else:
            if tr_id == "id-sabiq":
                for old, new in SABIQ_FIXES:
                    text = text.replace(old, new)
            labels = {}
            for note_id, label in SUP.findall(text):
                labels[note_id] = int(label)
            text = SUP.sub(lambda m: f"<sup>{m.group(2)}</sup>", text)
            text = html.unescape(text)

        assert "foot_note" not in text and "&lt;" not in text, (tr_id, surah, ayah, text)
        db.execute("INSERT INTO translation_texts VALUES (?,?,?,?)", (tr_id, surah, ayah, text.strip()))
        for idx, note_id in enumerate(ids, 1):
            label = labels.get(note_id)
            if label is None:
                stats["unreferenced"] += 1
            db.execute(
                "INSERT INTO footnotes VALUES (?,?,?,?,?,?)",
                (tr_id, surah, ayah, idx, label, clean_note(notes[note_id])),
            )
    return len(rows), stats


MAKKI = re.compile(r"makk?iyy?ah|(?:turun|diturunkan) di (?:mekah|makkah)", re.I)
MADANI = re.compile(r"madaniyy?ah|(?:turun|diturunkan) di madinah", re.I)
SURAH_INFO_SOURCES = {"id": "surah-info-id", "en": "surah-info-en"}


def clean_info_html(html_text: str) -> str:
    """Sisakan tag p, h1-h3, strong, em, a, ol, li; buang tag lain (span, s) dan rapikan entitas."""
    html_text = re.sub(r"</?(?:span|s)\b[^>]*>", "", html_text)
    html_text = re.sub(r"<a\b[^>]*href=\"([^\"]*)\"[^>]*>", lambda m: f'<a href="{m.group(1)}">', html_text)
    return html_text.replace("&nbsp;", " ").strip()


def detect_place(info_text: str):
    """Makki atau madani dari penyebutan pertama dalam teks info bahasa Indonesia."""
    plain = re.sub(r"<[^>]+>", " ", info_text)
    makki, madani = MAKKI.search(plain), MADANI.search(plain)
    if makki and madani:
        return "makki" if makki.start() < madani.start() else "madani"
    return "makki" if makki else "madani" if madani else None


def load_surah_info(db):
    places = {}
    for lang, folder in SURAH_INFO_SOURCES.items():
        src = sqlite3.connect(next((SRC / folder).glob("*.db")))
        rows = src.execute("SELECT surah_number, surah_name, text, short_text FROM surah_infos ORDER BY surah_number").fetchall()
        assert len(rows) == 114, (lang, len(rows))
        for number, name, text, short in rows:
            db.execute("INSERT INTO surah_info VALUES (?,?,?,?,?)", (lang, number, name, (short or "").strip(), clean_info_html(text)))
            if lang == "id":
                places[number] = detect_place(text)
        print(f"surah_info {lang}: {len(rows)}")
    meta = sqlite3.connect(next((SRC / "meta").glob("*surah-name*.sqlite")))
    rows = meta.execute("SELECT id, revelation_place, revelation_order FROM chapters ORDER BY id").fetchall()
    assert len(rows) == 114
    official = {n: ("makki" if place == "makkah" else "madani") for n, place, _ in rows}
    # Metadata resmi jadi sumber utama; deteksi dari teks hanya untuk pemeriksaan silang.
    disagree = [n for n in official if places.get(n) and places[n] != official[n]]
    print("tempat turun beda dengan tebakan teks (memakai metadata):", disagree)
    for number, place, order in rows:
        db.execute("UPDATE surahs SET place=?, revelation_order=? WHERE id=?", (official[number], order, number))
    print("tempat turun:", {k: sum(1 for v in official.values() if v == k) for k in ("makki", "madani")})


def load_transliteration(db):
    """Varian kedua tiap ayat (gaya baca, jumlah kata sama dengan teks Arab); varian pertama gaya Tanzil dibuang."""
    src = sqlite3.connect(next((SRC / "translit-tajweed-simple").glob("*.db")))
    rows = src.execute("SELECT sura, ayah, text FROM translation ORDER BY sura, ayah, rowid").fetchall()
    per_ayah = {}
    for surah, ayah, text in rows:
        per_ayah.setdefault((surah, ayah), []).append(text)
    assert len(per_ayah) == 6236 and all(len(v) == 2 for v in per_ayah.values())
    for (surah, ayah), variants in sorted(per_ayah.items()):
        db.execute("INSERT INTO transliteration VALUES (?,?,?)", (surah, ayah, variants[1].strip()))
    print("transliterasi:", len(per_ayah))


def verse_page(db, key: str):
    surah, ayah = (int(x) for x in key.split(":"))
    page = db.execute("SELECT page FROM ayahs WHERE surah=? AND ayah=?", (surah, ayah)).fetchone()[0]
    return surah, ayah, page


def load_markers(db):
    """Juz, hizb, rub, manzil, ruku, dan sajdah dari metadata QUL (titik awal tiap bagian)."""
    meta = SRC / "meta"
    for table, number_col in (("juz", "juz_number"), ("hizb", "hizb_number"), ("rub", "rub_number"), ("manzil", "manzil_number")):
        src = sqlite3.connect(meta / f"quran-metadata-{table}.sqlite")
        # tabel sumber bernama sama dengan kunci kecuali hizb yang jamak
        src_table = {"hizb": "hizbs"}.get(table, table)
        rows = src.execute(f"SELECT {number_col}, first_verse_key FROM {src_table} ORDER BY {number_col}").fetchall()
        for number, key in rows:
            db.execute(f"INSERT INTO {table} VALUES (?,?,?,?)", (number, *verse_page(db, key)))
        print(f"{table}: {len(rows)}")
    src = sqlite3.connect(meta / "quran-metadata-ruku.sqlite")
    rows = src.execute("SELECT ruku_number, surah_ruku_number, first_verse_key FROM ruku ORDER BY ruku_number").fetchall()
    for number, surah_ruku, key in rows:
        db.execute("INSERT INTO ruku VALUES (?,?,?,?,?)", (number, surah_ruku, *verse_page(db, key)))
    print(f"ruku: {len(rows)}")
    src = sqlite3.connect(meta / "quran-metadata-sajda.sqlite")
    rows = src.execute("SELECT sajdah_number, verse_key, sajdah_type FROM sajdah ORDER BY sajdah_number").fetchall()
    for number, key, kind in rows:
        db.execute("INSERT INTO sajda VALUES (?,?,?,?,?)", (number, *verse_page(db, key), kind))
    print(f"sajda: {len(rows)}")


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.unlink(missing_ok=True)
    db = sqlite3.connect(OUT)
    db.executescript(SCHEMA)

    layout = sqlite3.connect(next((SRC / "layout").glob("*.db")))
    words_src = sqlite3.connect(next((SRC / "qpc-v4/db").glob("*.db")))
    hafs = sqlite3.connect(next((SRC / "qpc-hafs").glob("*.db")))

    word_page = {}
    for page, line, typ, centered, first, last, surah in layout.execute(
        "SELECT page_number, line_number, line_type, is_centered, first_word_id, last_word_id, surah_number "
        "FROM pages ORDER BY page_number, line_number"
    ):
        first = int(first) if first != "" else None
        last = int(last) if last != "" else None
        surah = int(surah) if surah != "" else None
        db.execute("INSERT INTO page_lines VALUES (?,?,?,?,?,?,?)", (page, line, typ, centered, first, last, surah))
        if first is not None:
            for wid in range(first, last + 1):
                word_page[wid] = (page, line)

    for wid, surah, ayah, pos, text in words_src.execute("SELECT id, surah, ayah, word, text FROM words ORDER BY id"):
        page, line = word_page[wid]
        db.execute("INSERT INTO words VALUES (?,?,?,?,?,?,?)", (wid, surah, ayah, pos, text, page, line))

    for surah, ayah, text in hafs.execute("SELECT surah, ayah, text FROM verses ORDER BY surah, ayah"):
        page = db.execute("SELECT page FROM words WHERE surah=? AND ayah=? ORDER BY pos LIMIT 1", (surah, ayah)).fetchone()[0]
        db.execute("INSERT INTO ayahs VALUES (?,?,?,?)", (surah, ayah, text, page))

    names = [line.split("|") for line in SURAHS.strip().splitlines()]
    assert len(names) == 114
    glyphs = surah_name_glyphs()
    for sid, (name_ar, latin) in enumerate(names, 1):
        count, first_page = db.execute(
            "SELECT COUNT(*), MIN(page) FROM ayahs WHERE surah=?", (sid,)
        ).fetchone()
        db.execute("INSERT INTO surahs VALUES (?,?,?,?,?,?,NULL,NULL)", (sid, name_ar, latin, count, first_page, glyphs[sid]))

    load_markers(db)
    load_surah_info(db)
    load_transliteration(db)

    for tr_id, lang, name, source, folder in TRANSLATIONS:
        db.execute("INSERT INTO translations VALUES (?,?,?,?)", (tr_id, lang, name, source))
        n, stats = build_translation(db, tr_id, folder)
        print(f"{tr_id}: {n} ayat, {stats}")

    db.execute(f"PRAGMA user_version = {DATA_VERSION}")
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"OK -> {OUT} ({OUT.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()

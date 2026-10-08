#!/usr/bin/env python3
"""Bangun app/src/main/assets/quran.db dari file QUL di data-src/.

Pakai: python tools/build_db.py [data_src_dir] [output_db]
      python tools/build_db.py --explore-only   # hanya tabel penjelajahan, di quran.db yang sudah ada
Butuh: pip install fonttools (membaca cmap font nama surah).
"""
import html
import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXPLORE_ONLY = "--explore-only" in sys.argv
sys.argv = [a for a in sys.argv if a != "--explore-only"]
SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "data-src"
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "app/src/main/assets/quran.db"

DATA_VERSION = 10

# Terjemahan tidak lagi dibundel di quran.db; semuanya paket unduhan (tools/build_translation_packs.py). Tabel
# translations/translation_texts/footnotes tetap dibuat kosong agar skema aplikasi tidak berubah. TEXT_OVERRIDES dan
# SABIQ_FIXES di bawah tetap dipakai oleh parser paket.

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
    text TEXT NOT NULL, page INTEGER NOT NULL, line INTEGER NOT NULL, is_end INTEGER NOT NULL DEFAULT 0
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

# Tabel opsional untuk penjelajahan tematik. Hanya dibuat bila folder sumbernya ada di data-src/ (lihat load_explore);
# tanpa folder itu quran.db sama persis seperti sebelumnya dan aplikasi menyembunyikan fiturnya.
EXPLORE_SCHEMA = {
    "topics": """
        CREATE TABLE topics (
            id INTEGER PRIMARY KEY, name TEXT NOT NULL, name_ar TEXT, description TEXT,
            is_ontology INTEGER NOT NULL DEFAULT 0, is_thematic INTEGER NOT NULL DEFAULT 0, parent_id INTEGER
        );
        CREATE TABLE topic_ayahs (
            topic_id INTEGER NOT NULL, surah INTEGER NOT NULL, ayah INTEGER NOT NULL,
            PRIMARY KEY (topic_id, surah, ayah)
        ) WITHOUT ROWID;
        CREATE TABLE topic_links (
            topic_id INTEGER NOT NULL, related_id INTEGER NOT NULL,
            PRIMARY KEY (topic_id, related_id)
        ) WITHOUT ROWID;
        CREATE INDEX topic_ayahs_ayah ON topic_ayahs (surah, ayah);
        CREATE INDEX topics_parent ON topics (parent_id);
    """,
    "ayah_theme": """
        CREATE TABLE ayah_themes (
            id INTEGER PRIMARY KEY, surah INTEGER NOT NULL, ayah_from INTEGER NOT NULL, ayah_to INTEGER NOT NULL,
            theme TEXT NOT NULL, keywords TEXT
        );
        CREATE INDEX ayah_themes_surah ON ayah_themes (surah, ayah_from);
    """,
    "similar-ayah": """
        CREATE TABLE similar_ayahs (
            surah INTEGER NOT NULL, ayah INTEGER NOT NULL, sim_surah INTEGER NOT NULL, sim_ayah INTEGER NOT NULL,
            matched_words INTEGER, coverage INTEGER, score INTEGER, from_word INTEGER, to_word INTEGER
        );
        CREATE INDEX similar_ayahs_ayah ON similar_ayahs (surah, ayah, score DESC);
    """,
    "mutashabihat": """
        CREATE TABLE mutashabihat (
            id INTEGER PRIMARY KEY, source_surah INTEGER NOT NULL, source_ayah INTEGER NOT NULL,
            from_word INTEGER NOT NULL, to_word INTEGER NOT NULL, ayah_count INTEGER, occurrences INTEGER
        );
        CREATE TABLE mutashabihat_ayahs (
            phrase_id INTEGER NOT NULL, surah INTEGER NOT NULL, ayah INTEGER NOT NULL,
            from_word INTEGER NOT NULL, to_word INTEGER NOT NULL
        );
        CREATE INDEX mutashabihat_ayahs_ayah ON mutashabihat_ayahs (surah, ayah);
        CREATE INDEX mutashabihat_ayahs_phrase ON mutashabihat_ayahs (phrase_id);
    """,
    "morphology": """
        CREATE TABLE morph_roots (id INTEGER PRIMARY KEY, text_ar TEXT NOT NULL, text_en TEXT, words_count INTEGER);
        CREATE TABLE morph_lemmas (id INTEGER PRIMARY KEY, text TEXT NOT NULL, text_clean TEXT, words_count INTEGER);
        CREATE TABLE morph_stems (id INTEGER PRIMARY KEY, text TEXT NOT NULL, text_clean TEXT, words_count INTEGER);
        CREATE TABLE word_morph (
            surah INTEGER NOT NULL, ayah INTEGER NOT NULL, word INTEGER NOT NULL,
            root_id INTEGER, lemma_id INTEGER, stem_id INTEGER, pos TEXT,
            PRIMARY KEY (surah, ayah, word)
        ) WITHOUT ROWID;
        CREATE INDEX word_morph_root ON word_morph (root_id);
        CREATE INDEX word_morph_lemma ON word_morph (lemma_id);
    """,
}

SUP = re.compile(r'<sup foot_note="(\d+)">(\d+)</sup>')


def surah_name_glyphs():
    """Glyph 'سورة X' berurutan kode, dimulai dari surah 22 (Al-Hajj) lalu berputar.

    Kode sama untuk font nama surah (daftar) dan font header berwarna (bingkai di halaman).
    """
    from fontTools.ttLib import TTFont  # diimpor di sini agar modul ini bisa dipakai tanpa fontTools
    cmap = TTFont(next((SRC / "surah-names").glob("*.ttf"))).getBestCmap()
    codes = sorted(c for c in cmap if c >= 0xE000)[:114]
    return {sid: codes[(sid - 22) % 114] for sid in range(1, 115)}


def clean_note(text: str) -> str:
    text = re.sub(r"</p>\s*<p>", "\n\n", text)
    text = re.sub(r"</?p>", "", text)
    return html.unescape(text).strip()


def parse_translation_row(tr_id, surah, ayah, raw_text, raw_notes, stats):
    """Teks bersih (penanda catatan jadi <sup>n</sup>) dan daftar (idx, label, catatan) untuk satu ayat."""
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
    parsed = []
    for idx, note_id in enumerate(ids, 1):
        label = labels.get(note_id)
        if label is None:
            stats["unreferenced"] += 1
        parsed.append((idx, label, clean_note(notes[note_id])))
    return text.strip(), parsed


def build_translation(db, tr_id, folder):
    src = next((SRC / folder).glob("*.db"))
    rows = sqlite3.connect(src).execute(
        "SELECT sura, ayah, text, footnotes FROM translation ORDER BY sura, ayah"
    ).fetchall()
    stats = {"overrides": 0, "unreferenced": 0}
    for surah, ayah, raw_text, raw_notes in rows:
        text, notes = parse_translation_row(tr_id, surah, ayah, raw_text, raw_notes, stats)
        db.execute("INSERT INTO translation_texts VALUES (?,?,?,?)", (tr_id, surah, ayah, text))
        for idx, label, note in notes:
            db.execute("INSERT INTO footnotes VALUES (?,?,?,?,?,?)", (tr_id, surah, ayah, idx, label, note))
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


def explore_sources(folder):
    """Berkas SQLite (.db/.sqlite) di data-src/<folder>/; kosong bila folder tidak ada."""
    root = SRC / folder
    if not root.is_dir():
        return []
    return sorted(p for p in root.iterdir() if p.suffix in (".db", ".sqlite"))


def find_table(conn, *needed):
    """Nama tabel pertama yang memiliki semua kolom di `needed`, atau None. Nama tabel QUL tidak terdokumentasi."""
    for (name,) in conn.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall():
        cols = {r[1] for r in conn.execute(f'PRAGMA table_info("{name}")')}
        if all(c in cols for c in needed):
            return name
    return None


def split_key(key):
    """'2:255' -> (2, 255); '2:3:5' -> (2, 3, 5)."""
    return tuple(int(x) for x in str(key).strip().split(":"))


def check_ayah(surah, ayah):
    assert 1 <= surah <= 114 and ayah >= 1, (surah, ayah)


def topic_text(raw):
    """Deskripsi topik QUL (HTML dengan <topic data-id>) -> teks polos dan id topik yang ditautkan."""
    raw = raw or ""
    links = [int(x) for x in re.findall(r'<topic\b[^>]*data-id="(\d+)"', raw)]
    plain = html.unescape(re.sub(r"<[^>]+>", "", raw))
    return re.sub(r"\s+", " ", plain).strip(), links


def load_topics(db):
    """Topik dan konsep (qul.tarteel.ai/resources/ayah-topics/45): satu baris per topik, ayat sebagai daftar 's:a, s:a'.

    Induk: parent_id, bila kosong thematic_parent_id lalu ontology_parent_id. Topik terkait: tautan <topic data-id> di
    deskripsi ditambah kolom related_topics (id dipisah koma).
    """
    files = explore_sources("topics")
    if not files:
        return
    db.executescript(EXPLORE_SCHEMA["topics"])
    wanted = ("topic_id", "name", "arabic_name", "description", "thematic", "ontology",
              "parent_id", "thematic_parent_id", "ontology_parent_id", "ayahs", "related_topics")
    seen, links, related = set(), set(), set()
    for f in files:
        conn = sqlite3.connect(f)
        table = find_table(conn, "topic_id", "name", "ayahs")
        assert table, f"{f.name}: tabel dengan kolom topic_id/name/ayahs tidak ditemukan"
        have = {r[1] for r in conn.execute(f'PRAGMA table_info("{table}")')}
        sel = ", ".join(c if c in have else "NULL" for c in wanted)
        for tid, name, name_ar, desc, thematic, ontology, p1, p2, p3, ayahs, rel in conn.execute(f'SELECT {sel} FROM "{table}" ORDER BY topic_id'):
            text, desc_links = topic_text(desc)
            parent = next((p for p in (p1, p2, p3) if p not in (None, "", 0)), None)
            seen.add(tid)
            db.execute(
                "INSERT INTO topics VALUES (?,?,?,?,?,?,?)",
                (tid, (name or "").strip(), (name_ar or "").strip() or None, text or None,
                 1 if ontology in (1, "1", "t", "true", True) else 0, 1 if thematic in (1, "1", "t", "true", True) else 0, parent),
            )
            for key in re.findall(r"\d+:\d+", ayahs or ""):
                surah, ayah = split_key(key)
                check_ayah(surah, ayah)
                links.add((tid, surah, ayah))
            for other in desc_links + [int(x) for x in re.findall(r"\d+", rel or "")]:
                if other != tid:
                    related.add((tid, other))
    related = {(a, b) for a, b in related if b in seen}  # buang tautan ke topik yang tidak ada
    db.executemany("INSERT INTO topic_ayahs VALUES (?,?,?)", sorted(links))
    db.executemany("INSERT INTO topic_links VALUES (?,?)", sorted(related))
    print(f"topics: {len(seen)}, topic_ayahs: {len(links)}, topic_links: {len(related)}")


def load_ayah_themes(db):
    """Tema ayat (qul.tarteel.ai/resources/ayah-theme/62): kelompok ayat berurutan dengan satu tema."""
    files = explore_sources("ayah-theme")
    if not files:
        return
    db.executescript(EXPLORE_SCHEMA["ayah_theme"])
    n = 0
    for f in files:
        conn = sqlite3.connect(f)
        table = find_table(conn, "theme", "surah_number")
        assert table, f"{f.name}: tabel dengan kolom theme/surah_number tidak ditemukan"
        have = {r[1] for r in conn.execute(f'PRAGMA table_info("{table}")')}
        # Halaman QUL menulis nama kolom awal rentang secara tidak pasti (ayah_from / from_ayah); terima keduanya.
        start = next((c for c in ("ayah_from", "from_ayah") if c in have), None)
        end = next((c for c in ("ayah_to", "to_ayah") if c in have), None)
        assert start and end, f"{f.name}: kolom rentang ayat tidak dikenali ({sorted(have)})"
        kw = "keywords" if "keywords" in have else "NULL"
        for surah, a_from, a_to, theme, keywords in conn.execute(
            f'SELECT surah_number, {start}, {end}, theme, {kw} FROM "{table}" ORDER BY surah_number, {start}'
        ):
            check_ayah(surah, a_from)
            db.execute("INSERT INTO ayah_themes (surah, ayah_from, ayah_to, theme, keywords) VALUES (?,?,?,?,?)",
                       (surah, a_from, a_to, theme.strip(), (keywords or "").strip() or None))
            n += 1
    print(f"ayah_themes: {n}")


def load_similar_ayahs(db):
    """Ayat serupa (qul.tarteel.ai/resources/similar-ayah/74): satu baris per pasangan ayat sumber dan ayat cocok."""
    files = explore_sources("similar-ayah")
    if not files:
        return
    db.executescript(EXPLORE_SCHEMA["similar-ayah"])
    n = 0
    for f in files:
        conn = sqlite3.connect(f)
        table = find_table(conn, "verse_key", "matched_ayah_key", "score")
        assert table, f"{f.name}: tabel dengan kolom verse_key/matched_ayah_key/score tidak ditemukan"
        have = {r[1] for r in conn.execute(f'PRAGMA table_info("{table}")')}
        sel = ", ".join(c if c in have else "NULL" for c in ("matched_words_count", "coverage", "score", "match_words_range"))
        for key, other, count, coverage, score, rng in conn.execute(f'SELECT verse_key, matched_ayah_key, {sel} FROM "{table}"'):
            surah, ayah = split_key(key)
            sim_surah, sim_ayah = split_key(other)
            check_ayah(surah, ayah)
            check_ayah(sim_surah, sim_ayah)
            nums = [int(x) for x in re.findall(r"\d+", rng or "")]
            db.execute(
                "INSERT INTO similar_ayahs VALUES (?,?,?,?,?,?,?,?,?)",
                (surah, ayah, sim_surah, sim_ayah, count, coverage, score,
                 nums[0] if len(nums) >= 2 else None, nums[1] if len(nums) >= 2 else None),
            )
            n += 1
    print(f"similar_ayahs: {n}")


def load_mutashabihat(db):
    """Mutasyabihat (qul.tarteel.ai/resources/mutashabihat/73): phrases.json; phrase_verses.json hanya indeks balik."""
    path = SRC / "mutashabihat" / "phrases.json"
    if not path.exists():
        return
    db.executescript(EXPLORE_SCHEMA["mutashabihat"])
    phrases = json.loads(path.read_text(encoding="utf-8"))
    links = 0
    for pid, p in phrases.items():
        surah, ayah = split_key(p["source"]["key"])
        check_ayah(surah, ayah)
        db.execute(
            "INSERT INTO mutashabihat VALUES (?,?,?,?,?,?,?)",
            (int(pid), surah, ayah, p["source"]["from"], p["source"]["to"], p.get("ayahs"), p.get("count")),
        )
        for key, ranges in p["ayah"].items():
            s, a = split_key(key)
            check_ayah(s, a)
            for start, end in ranges:  # indeks kata 1-based, inklusif
                db.execute("INSERT INTO mutashabihat_ayahs VALUES (?,?,?,?,?)", (int(pid), s, a, start, end))
                links += 1
    print(f"mutashabihat: {len(phrases)}, mutashabihat_ayahs: {links}")


def load_morphology(db):
    """Akar, lema, dan stem per kata (qul.tarteel.ai/resources/morphology/75, 76, 77): tiap jenis berkas sendiri.

    Kolom pos (jenis kata) dibiarkan kosong: halaman "Word morphology" (morphology/78) belum bisa dibaca skemanya.
    """
    files = explore_sources("morphology")
    if not files:
        return
    db.executescript(EXPLORE_SCHEMA["morphology"])
    # (tabel kamus, kolom, tabel kata, kolom id, kolom lokasi yang mungkin, tabel tujuan, kolom word_morph)
    kinds = [
        ("roots", ("id", "arabic_trilateral"), ("word_roots", "root_words"), "root_id", "morph_roots", "root_id",
         "SELECT id, arabic_trilateral, english_trilateral, words_count FROM roots"),
        ("lemmas", ("id", "text"), ("word_lemmas", "lemma_words"), "lemma_id", "morph_lemmas", "lemma_id",
         "SELECT id, text, text_clean, words_count FROM lemmas"),
        ("stems", ("id", "text"), ("word_stems", "stem_words"), "stem_id", "morph_stems", "stem_id",
         "SELECT id, text, text_clean, words_count FROM stems"),
    ]
    counts = {}
    for f in files:
        conn = sqlite3.connect(f)
        tables = {r[0] for r in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        for dict_table, _, word_tables, id_col, target, column, query in kinds:
            word_table = next((t for t in word_tables if t in tables), None)  # QUL memakai dua penamaan
            if dict_table not in tables or word_table is None:
                continue
            db.executemany(f"INSERT INTO {target} VALUES (?,?,?,?)", conn.execute(query).fetchall())
            have = {r[1] for r in conn.execute(f"PRAGMA table_info({word_table})")}
            loc = "word_location" if "word_location" in have else "location"  # QUL memakai dua nama berbeda
            n = 0
            for wid, location in conn.execute(f"SELECT {id_col}, {loc} FROM {word_table}").fetchall():
                surah, ayah, word = split_key(location)
                check_ayah(surah, ayah)
                db.execute(
                    f"INSERT INTO word_morph (surah, ayah, word, {column}) VALUES (?,?,?,?) "
                    f"ON CONFLICT (surah, ayah, word) DO UPDATE SET {column} = excluded.{column}",
                    (surah, ayah, word, wid),
                )
                n += 1
            counts[target] = n
    assert counts, f"data-src/morphology/ ada, tetapi tidak berisi tabel roots/lemmas/stems: {[f.name for f in files]}"
    print("morfologi:", counts)


def load_explore(db):
    """Data penjelajahan tematik; tiap sumber opsional dan dilewati bila foldernya tidak ada."""
    for loader in (load_topics, load_ayah_themes, load_similar_ayahs, load_mutashabihat, load_morphology):
        loader(db)


EXPLORE_TABLES = ["topic_links", "topic_ayahs", "topics", "ayah_themes", "similar_ayahs", "mutashabihat_ayahs", "mutashabihat",
                  "word_morph", "morph_roots", "morph_lemmas", "morph_stems"]


def explore_only():
    """Buka quran.db yang sudah ada, bangun ulang hanya tabel penjelajahan dari data-src/, lalu VACUUM.

    Tabel dasar tidak disentuh, jadi build penuh (butuh semua sumber dasar) tidak perlu diulang.
    """
    assert OUT.exists(), f"{OUT} belum ada; jalankan build penuh dulu"
    db = sqlite3.connect(OUT)
    for table in EXPLORE_TABLES:
        db.execute(f"DROP TABLE IF EXISTS {table}")
    load_explore(db)
    db.execute(f"PRAGMA user_version = {DATA_VERSION}")
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"OK (explore saja) -> {OUT} ({OUT.stat().st_size / 1e6:.1f} MB)")


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
        db.execute("INSERT INTO words VALUES (?,?,?,?,?,?,?,0)", (wid, surah, ayah, pos, text, page, line))
    # Kata terakhir tiap ayat adalah penanda nomor ayat (lingkaran berwarna di font V4).
    db.execute("UPDATE words SET is_end = 1 WHERE id IN (SELECT MAX(id) FROM words GROUP BY surah, ayah)")

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
    load_explore(db)

    db.execute(f"PRAGMA user_version = {DATA_VERSION}")
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"OK -> {OUT} ({OUT.stat().st_size / 1e6:.1f} MB)")


if __name__ == "__main__":
    explore_only() if EXPLORE_ONLY else main()

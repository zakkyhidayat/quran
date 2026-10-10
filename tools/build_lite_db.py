"""Bangun quran.db versi lite dari quran.db penuh: hanya data yang dibaca varian lite.

Dipanggil Gradle saat membangun varian lite (lihat app/build.gradle.kts); hasilnya tidak masuk git.
Teks Qur'an (words, ayahs, page_lines) dan pembagian juz/hizb/dst. disalin apa adanya. Yang dibuang:
- tabel penjelajahan opsional (aplikasi memeriksa sqlite_master dan menyembunyikan fiturnya bila tidak ada);
- isi surah_info dan transliteration (tabelnya tetap ada, kosong, supaya kueri lama tidak galat).

    python tools/build_lite_db.py <quran.db penuh> <keluaran>
"""
import shutil
import sqlite3
import sys

OPTIONAL_TABLES = [
    "topics", "topic_ayahs", "topic_links", "ayah_themes", "similar_ayahs", "mutashabihat", "mutashabihat_ayahs",
    "morph_roots", "morph_lemmas", "morph_stems", "word_morph",
]
EMPTIED_TABLES = ["surah_info", "transliteration"]
# Tabel yang wajib tetap utuh; jumlah barisnya dibandingkan dengan sumber.
KEPT_TABLES = ["surahs", "ayahs", "words", "page_lines", "juz", "hizb", "rub", "manzil", "ruku", "sajda"]


def main(src: str, dst: str) -> None:
    shutil.copyfile(src, dst)
    db = sqlite3.connect(dst)
    before = {t: db.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0] for t in KEPT_TABLES}
    for t in OPTIONAL_TABLES:
        db.execute(f"DROP TABLE IF EXISTS {t}")
    for t in EMPTIED_TABLES:
        db.execute(f"DELETE FROM {t}")
    db.commit()
    db.execute("VACUUM")
    after = {t: db.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0] for t in KEPT_TABLES}
    db.close()
    if before != after or before["ayahs"] != 6236:
        sys.exit(f"isi tabel inti berubah: {before} -> {after}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])

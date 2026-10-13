#!/usr/bin/env python3
"""Create release metadata for the Qur'an database and page fonts."""
import argparse
import gzip
import hashlib
import json
import sqlite3
import zipfile
from pathlib import Path


def asset(path: Path, name: str) -> dict:
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
            size += len(chunk)
    return {"file": name, "bytes": size, "sha256": digest.hexdigest()}


def database_version(path: Path) -> int:
    with gzip.open(path, "rb") as source:
        header = source.read(100)
    if header[:16] != b"SQLite format 3\x00":
        raise ValueError(f"{path} is not a compressed SQLite database")
    with gzip.open(path, "rb") as source:
        with sqlite3.connect(":memory:") as db:
            db.deserialize(source.read())
            version = db.execute("PRAGMA user_version").fetchone()[0]
            if db.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
                raise ValueError(f"{path} failed SQLite integrity_check")
            if db.execute("SELECT COUNT(*) FROM ayahs").fetchone()[0] != 6236:
                raise ValueError(f"{path} does not contain all ayahs")
    return version


def verify_fonts(path: Path) -> None:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        expected = {f"p{page}.ttf" for page in range(1, 605)}
        if len(names) != 604 or set(names) != expected:
            raise ValueError(f"{path} must contain exactly p1.ttf through p604.ttf")
        for name in names:
            with archive.open(name) as font:
                if font.read(4) not in (b"\x00\x01\x00\x00", b"OTTO"):
                    raise ValueError(f"{path}:{name} is not a valid TrueType/OpenType font")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--full", required=True, type=Path)
    parser.add_argument("--fonts", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    full_version = database_version(args.full)
    verify_fonts(args.fonts)
    manifest = {
        "version": 1,
        "database_version": full_version,
        "databases": {
            "full": asset(args.full, args.full.name),
        },
        "fonts": asset(args.fonts, "page-fonts.zip"),
    }
    args.output.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()

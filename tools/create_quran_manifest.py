#!/usr/bin/env python3
"""Create release metadata for the full and Lite Qur'an database downloads."""
import argparse
import gzip
import hashlib
import json
import sqlite3
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


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--full", required=True, type=Path)
    parser.add_argument("--lite", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    full_version = database_version(args.full)
    lite_version = database_version(args.lite)
    if full_version != lite_version:
        raise ValueError(f"database versions differ: full={full_version}, lite={lite_version}")
    manifest = {
        "version": 1,
        "database_version": full_version,
        "databases": {
            "full": asset(args.full, args.full.name),
            "lite": asset(args.lite, args.lite.name),
        },
    }
    args.output.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()

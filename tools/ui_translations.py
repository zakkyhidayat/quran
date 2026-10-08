#!/usr/bin/env python3
"""Teks antarmuka untuk bahasa selain Inggris (values/) dan Indonesia (values-in/).

Sumber kebenaran untuk kunci adalah app/src/main/res/values/strings.xml. Skrip ini menulis values-<kode>/strings.xml
untuk tiap bahasa di LANGS dan melaporkan kunci yang belum diterjemahkan (teks Inggris dipakai Android sebagai cadangan).

Pakai:
  python tools/ui_translations.py          # tulis berkas, laporkan kunci yang kurang
  python tools/ui_translations.py --check  # hanya laporkan; keluar 1 bila ada yang kurang
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"

# Urutan kolom di T. Folder resource Android: values-<kode>.
LANGS = ["ar", "ur", "bn", "tr", "fa", "ms", "fr", "ru"]

# Istilah yang dipakai berulang.
P = {"ar": "ص", "ur": "ص", "bn": "পৃ.", "tr": "s.", "fa": "ص", "ms": "hlm.", "fr": "p.", "ru": "с."}
JUZ = {"ar": "الجزء", "ur": "پارہ", "bn": "পারা", "tr": "Cüz", "fa": "جزء", "ms": "Juzuk", "fr": "Juz", "ru": "Джуз"}
HIZB = {"ar": "الحزب", "ur": "حزب", "bn": "হিযব", "tr": "Hizb", "fa": "حزب", "ms": "Hizb", "fr": "Hizb", "ru": "Хизб"}
MANZIL = {"ar": "المنزل", "ur": "منزل", "bn": "মনজিল", "tr": "Menzil", "fa": "منزل", "ms": "Manzil", "fr": "Manzil", "ru": "Манзиль"}
RUB = {"ar": "الربع", "ur": "ربع", "bn": "রুবʼ", "tr": "Rub'", "fa": "ربع", "ms": "Rubu'", "fr": "Rub'", "ru": "Руб"}
RUKU = {"ar": "الركوع", "ur": "رکوع", "bn": "রুকু", "tr": "Rükû", "fa": "رکوع", "ms": "Ruku'", "fr": "Ruku", "ru": "Руку"}
PAGE = {"ar": "الصفحة", "ur": "صفحہ", "bn": "পৃষ্ঠা", "tr": "Sayfa", "fa": "صفحهٔ", "ms": "Halaman", "fr": "Page", "ru": "Страница"}
AYAHS = {"ar": "آية", "ur": "آیات", "bn": "আয়াত", "tr": "ayet", "fa": "آیه", "ms": "ayat", "fr": "versets", "ru": "аятов"}
AYAH = {"ar": "آية", "ur": "آیت", "bn": "আয়াত", "tr": "Ayet", "fa": "آیه", "ms": "Ayat", "fr": "Verset", "ru": "Аят"}


def per(fn):
    return [fn(code) for code in LANGS]


T = {
    "back": ["رجوع", "واپس", "ফিরে যান", "Geri", "بازگشت", "Kembali", "Retour", "Назад"],
    "jump_to_ayah": ["الانتقال إلى آية", "آیت پر جائیں", "আয়াতে যান", "Ayete git", "رفتن به آیه", "Pergi ke ayat", "Aller au verset", "Перейти к аяту"],
    "random_ayah": ["آية عشوائية", "بے ترتیب آیت", "এলোমেলো আয়াত", "Rastgele ayet", "آیهٔ تصادفی", "Ayat rawak", "Verset aléatoire", "Случайный аят"],
    "settings": ["الإعدادات", "ترتیبات", "সেটিংস", "Ayarlar", "تنظیمات", "Tetapan", "Paramètres", "Настройки"],
    "index_title": ["الفهرس", "فہرست", "সূচি", "Fihrist", "فهرست", "Indeks", "Index", "Оглавление"],
    "continue_reading": ["متابعة القراءة", "پڑھنا جاری رکھیں", "পড়া চালিয়ে যান", "Okumaya devam et", "ادامهٔ خواندن", "Teruskan membaca", "Reprendre la lecture", "Продолжить чтение"],
    "surah_summary": per(lambda c: f"%1$d {AYAHS[c]} • {P[c]} %2$d"),
    "surah_info_cd": ["معلومات سورة %1$s", "سورہ %1$s کی معلومات", "সূরা %1$s-এর তথ্য", "%1$s suresi bilgisi", "اطلاعات سورهٔ %1$s", "Maklumat surah %1$s", "Infos sur la sourate %1$s", "О суре %1$s"],
    "juz_n": per(lambda c: f"{JUZ[c]} %1$d"),
    "ref_place": per(lambda c: f"%1$s %2$d:%3$d • {P[c]} %4$d"),
    "juz_place": per(lambda c: f"{JUZ[c]} %1$d • %2$s"),
    "hide_rub": ["إخفاء الأرباع", "ربع چھپائیں", "রুবʼ লুকান", "Rub'ları gizle", "پنهان کردن ربع‌ها", "Sembunyikan rubu'", "Masquer les rub'", "Скрыть четверти"],
    "show_rub": ["إظهار الأرباع", "ربع دکھائیں", "রুবʼ দেখান", "Rub'ları göster", "نمایش ربع‌ها", "Tunjukkan rubu'", "Afficher les rub'", "Показать четверти"],
    "hizb_n": per(lambda c: f"{HIZB[c]} %1$d"),
    "rub_n": per(lambda c: f"{RUB[c]} %1$s"),
    "manzil_n": per(lambda c: f"{MANZIL[c]} %1$d"),
    "sajda_required": ["سجدة تلاوة (واجبة)", "سجدۂ تلاوت (واجب)", "তিলাওয়াতের সিজদা (ওয়াজিব)", "Tilavet secdesi (vacip)", "سجدهٔ تلاوت (واجب)", "Sujud tilawah (wajib)", "Prosternation de récitation (obligatoire)", "Земной поклон при чтении (обязательный)"],
    "sajda_recommended": ["سجدة تلاوة (مستحبة)", "سجدۂ تلاوت (مستحب)", "তিলাওয়াতের সিজদা (মুস্তাহাব)", "Tilavet secdesi (müstehap)", "سجدهٔ تلاوت (مستحب)", "Sujud tilawah (sunat)", "Prosternation de récitation (recommandée)", "Земной поклон при чтении (желательный)"],
    "sajda_ayah_required": ["آية سجدة (واجبة)", "آیتِ سجدہ (واجب)", "সিজদার আয়াত (ওয়াজিব)", "Secde ayeti (vacip)", "آیهٔ سجده (واجب)", "Ayat sajdah (wajib)", "Verset de prosternation (obligatoire)", "Аят земного поклона (обязательный)"],
    "sajda_ayah_recommended": ["آية سجدة (مستحبة)", "آیتِ سجدہ (مستحب)", "সিজদার আয়াত (মুস্তাহাব)", "Secde ayeti (müstehap)", "آیهٔ سجده (مستحب)", "Ayat sajdah (sunat)", "Verset de prosternation (recommandée)", "Аят земного поклона (желательный)"],
    "page_short": per(lambda c: f"{P[c]} %1$d"),
    "no_bookmarks": ["لا توجد علامات بعد", "ابھی کوئی بُک مارک نہیں", "এখনো কোনো বুকমার্ক নেই", "Henüz yer işareti yok", "هنوز نشانکی نیست", "Belum ada penanda", "Aucun signet pour l'instant", "Закладок пока нет"],
    "page_n": per(lambda c: f"{PAGE[c]} %1$d"),
    "bookmark_ayah_sub": per(lambda c: f"{AYAH[c]} • {P[c]} %1$d"),
    "bookmark_page_sub": ["صفحة • %1$s", "صفحہ • %1$s", "পৃষ্ঠা • %1$s", "Sayfa • %1$s", "صفحه • %1$s", "Halaman • %1$s", "Page • %1$s", "Страница • %1$s"],
    "delete": ["حذف", "حذف کریں", "মুছুন", "Sil", "حذف", "Padam", "Supprimer", "Удалить"],
    "tab_surah": ["السور", "سورتیں", "সূরা", "Sureler", "سوره‌ها", "Surah", "Sourates", "Суры"],
    "tab_juz": ["الأجزاء", "پارے", "পারা", "Cüzler", "اجزاء", "Juzuk", "Juz", "Джузы"],
    "tab_hizb": ["الأحزاب", "احزاب", "হিযব", "Hizbler", "احزاب", "Hizb", "Hizb", "Хизбы"],
    "tab_manzil": ["المنازل", "منازل", "মনজিল", "Menziller", "منازل", "Manzil", "Manzil", "Манзили"],
    "tab_sajdah": ["السجدات", "سجدے", "সিজদা", "Secdeler", "سجده‌ها", "Sajdah", "Prosternations", "Поклоны"],
    "tab_bookmark": ["العلامات", "بُک مارکس", "বুকমার্ক", "Yer işaretleri", "نشانک‌ها", "Penanda", "Signets", "Закладки"],
    "place_makki": ["مكية", "مکی", "মাক্কী", "Mekki", "مکی", "Makkiyah", "Mecquoise", "Мекканская"],
    "place_madani": ["مدنية", "مدنی", "মাদানী", "Medeni", "مدنی", "Madaniyah", "Médinoise", "Мединская"],
    "ayah_count": per(lambda c: f"%1$d {AYAHS[c]}"),
    "pages_range": per(lambda c: f"{P[c]} %1$d–%2$d"),
    "juz_range": per(lambda c: f"{JUZ[c]} %1$d–%2$d"),
    "ruku_count": ["%1$d ركوعات", "%1$d رکوع", "%1$d রুকু", "%1$d rükû", "%1$d رکوع", "%1$d ruku'", "%1$d ruku", "%1$d руку"],
    "revelation_order": ["ترتيب النزول %1$d", "ترتیبِ نزول %1$d", "অবতরণের ক্রম %1$d", "İniş sırası %1$d", "ترتیب نزول %1$d", "Urutan turun ke-%1$d", "Ordre de révélation %1$d", "Порядок ниспослания %1$d"],
    "read_surah": ["اقرأ هذه السورة", "یہ سورہ پڑھیں", "এই সূরা পড়ুন", "Bu sureyi oku", "خواندن این سوره", "Baca surah ini", "Lire cette sourate", "Читать эту суру"],
    "no_surah_info": ["لا توجد معلومات عن هذه السورة.", "اس سورہ کے بارے میں معلومات دستیاب نہیں۔", "এই সূরার তথ্য নেই।", "Bu sure için bilgi yok.", "اطلاعاتی دربارهٔ این سوره نیست.", "Tiada maklumat untuk surah ini.", "Aucune information pour cette sourate.", "Нет сведений об этой суре."],
    "ayah_meta": per(lambda c: f"{PAGE[c]} %1$d • {JUZ[c]} %2$d • {HIZB[c]} %3$d ({RUB[c]} %4$d/4) • {MANZIL[c]} %5$d • {RUKU[c]} %6$d"),
    "previous_ayah": ["الآية السابقة", "پچھلی آیت", "আগের আয়াত", "Önceki ayet", "آیهٔ قبلی", "Ayat sebelumnya", "Verset précédent", "Предыдущий аят"],
    "next_ayah": ["الآية التالية", "اگلی آیت", "পরের আয়াত", "Sonraki ayet", "آیهٔ بعدی", "Ayat seterusnya", "Verset suivant", "Следующий аят"],
    "remove_bookmark": ["إزالة العلامة", "بُک مارک ہٹائیں", "বুকমার্ক সরান", "Yer işaretini kaldır", "حذف نشانک", "Buang penanda", "Retirer le signet", "Удалить закладку"],
    "bookmark_ayah": ["وضع علامة على الآية", "آیت بُک مارک کریں", "আয়াত বুকমার্ক করুন", "Ayeti işaretle", "نشانک‌گذاری آیه", "Tanda ayat", "Ajouter le verset aux signets", "Добавить аят в закладки"],
    "copy": ["نسخ", "کاپی کریں", "কপি", "Kopyala", "کپی", "Salin", "Copier", "Копировать"],
    "share": ["مشاركة", "شیئر کریں", "শেয়ার", "Paylaş", "هم‌رسانی", "Kongsi", "Partager", "Поделиться"],
    "transliteration": ["النطق اللاتيني", "رومن تلفظ", "প্রতিবর্ণীকরণ", "Transliterasyon", "آوانگاری", "Transliterasi", "Translittération", "Транслитерация"],
    "surah_cd": ["سورة %1$s", "سورہ %1$s", "সূরা %1$s", "%1$s Suresi", "سورهٔ %1$s", "Surah %1$s", "Sourate %1$s", "Сура %1$s"],
    "open_surah_info": ["فتح معلومات السورة", "سورہ کی معلومات کھولیں", "সূরার তথ্য খুলুন", "Sure bilgisini aç", "باز کردن اطلاعات سوره", "Buka maklumat surah", "Ouvrir les infos de la sourate", "Открыть сведения о суре"],
    "ayah_cd": ["%1$s الآية %2$d. %3$s", "%1$s آیت %2$d۔ %3$s", "%1$s আয়াত %2$d। %3$s", "%1$s %2$d. ayet. %3$s", "%1$s آیهٔ %2$d. %3$s", "%1$s ayat %2$d. %3$s", "%1$s verset %2$d. %3$s", "%1$s, аят %2$d. %3$s"],
    "show_translation": ["عرض الترجمة", "ترجمہ دکھائیں", "অনুবাদ দেখান", "Meali göster", "نمایش ترجمه", "Tunjukkan terjemahan", "Afficher la traduction", "Показать перевод"],
    "juz_page": per(lambda c: f"{JUZ[c]} %1$s • {P[c]} %2$d"),
    "index_cd": ["قائمة السور والأجزاء", "سورتوں اور پاروں کی فہرست", "সূরা ও পারার তালিকা", "Sure ve cüz listesi", "فهرست سوره‌ها و اجزاء", "Senarai surah dan juzuk", "Liste des sourates et des juz", "Список сур и джузов"],
    "search": ["بحث", "تلاش", "খুঁজুন", "Ara", "جستجو", "Cari", "Rechercher", "Поиск"],
    "remove_page_bookmark": ["إزالة علامة الصفحة", "صفحے کا بُک مارک ہٹائیں", "পৃষ্ঠার বুকমার্ক সরান", "Sayfa işaretini kaldır", "حذف نشانک صفحه", "Buang penanda halaman", "Retirer le signet de la page", "Удалить закладку страницы"],
    "bookmark_page": ["وضع علامة على الصفحة", "صفحہ بُک مارک کریں", "পৃষ্ঠা বুকমার্ক করুন", "Sayfayı işaretle", "نشانک‌گذاری صفحه", "Tanda halaman", "Ajouter la page aux signets", "Добавить страницу в закладки"],
    "next_surah_cd": ["السورة التالية: %1$s", "اگلی سورہ: %1$s", "পরের সূরা: %1$s", "Sonraki sure: %1$s", "سورهٔ بعدی: %1$s", "Surah seterusnya: %1$s", "Sourate suivante : %1$s", "Следующая сура: %1$s"],
    "previous_surah_cd": ["السورة السابقة: %1$s", "پچھلی سورہ: %1$s", "আগের সূরা: %1$s", "Önceki sure: %1$s", "سورهٔ قبلی: %1$s", "Surah sebelumnya: %1$s", "Sourate précédente : %1$s", "Предыдущая сура: %1$s"],
    "search_placeholder": ["ابحث عن آية أو سورة أو 2:255", "آیت، سورہ یا 2:255 تلاش کریں", "আয়াত, সূরা বা 2:255 খুঁজুন", "Ayet, sure veya 2:255 ara", "جستجوی آیه، سوره یا 2:255", "Cari ayat, surah atau 2:255", "Rechercher un verset, une sourate ou 2:255", "Поиск аята, суры или 2:255"],
    "clear": ["مسح", "صاف کریں", "মুছে ফেলুন", "Temizle", "پاک کردن", "Kosongkan", "Effacer", "Очистить"],
    "search_hint": [
        "اكتب كلمات من الترجمات المفعّلة، أو نصًا عربيًا، أو اسم سورة، أو مرجع آية مثل 2:255.",
        "فعال ترجموں کے الفاظ، عربی متن، سورہ کا نام یا 2:255 جیسا آیت کا حوالہ لکھیں۔",
        "সক্রিয় অনুবাদের শব্দ, আরবি পাঠ, সূরার নাম বা 2:255-এর মতো আয়াতের নম্বর লিখুন।",
        "Etkin meallerden kelimeler, Arapça metin, sure adı veya 2:255 gibi bir ayet numarası yazın.",
        "واژه‌ای از ترجمه‌های فعال، متن عربی، نام سوره یا نشانی آیه مانند 2:255 بنویسید.",
        "Taip perkataan daripada terjemahan aktif, teks Arab, nama surah atau rujukan ayat seperti 2:255.",
        "Saisissez des mots des traductions actives, du texte arabe, un nom de sourate ou une référence comme 2:255.",
        "Введите слова из включённых переводов, арабский текст, название суры или ссылку на аят, например 2:255.",
    ],
    "no_results": ["لا توجد نتائج.", "کوئی نتیجہ نہیں ملا۔", "কোনো ফলাফল নেই।", "Sonuç yok.", "نتیجه‌ای یافت نشد.", "Tiada hasil.", "Aucun résultat.", "Ничего не найдено."],
    "search_surah_sub": per(lambda c: f"%1$d • %2$d {AYAHS[c]} • {P[c]} %3$d"),
    "open_ayah": ["فتح الآية", "آیت کھولیں", "আয়াত খুলুন", "Ayeti aç", "باز کردن آیه", "Buka ayat", "Ouvrir le verset", "Открыть аят"],
    "surah_title": ["سورة %1$s", "سورہ %1$s", "সূরা %1$s", "%1$s Suresi", "سورهٔ %1$s", "Surah %1$s", "Sourate %1$s", "Сура %1$s"],
    "surah_label": ["السورة", "سورہ", "সূরা", "Sure", "سوره", "Surah", "Sourate", "Сура"],
    "ayah_label": ["الآية", "آیت", "আয়াত", "Ayet", "آیه", "Ayat", "Verset", "Аят"],
    "jump": ["انتقال", "جائیں", "যান", "Git", "برو", "Pergi", "Aller", "Перейти"],
    "cancel": ["إلغاء", "منسوخ", "বাতিল", "İptal", "لغو", "Batal", "Annuler", "Отмена"],
    "appearance": ["المظهر", "ظاہری شکل", "চেহারা", "Görünüm", "ظاهر", "Penampilan", "Apparence", "Оформление"],
    "theme": ["السمة", "تھیم", "থিম", "Tema", "پوسته", "Tema", "Thème", "Тема"],
    "theme_system": ["النظام", "سسٹم", "সিস্টেম", "Sistem", "سیستم", "Sistem", "Système", "Система"],
    "theme_light": ["فاتح", "روشن", "লাইট", "Açık", "روشن", "Cerah", "Clair", "Светлая"],
    "theme_dark": ["داكن", "تاریک", "ডার্ক", "Koyu", "تیره", "Gelap", "Sombre", "Тёмная"],
    "color_palette": ["لوحة الألوان", "رنگوں کا پیلیٹ", "রঙের প্যালেট", "Renk paleti", "پالت رنگ", "Palet warna", "Palette de couleurs", "Цветовая палитра"],
    "palette_dynamic": ["ديناميكية", "متحرک", "ডায়নামিক", "Dinamik", "پویا", "Dinamik", "Dynamique", "Динамическая"],
    "palette_original": ["الأصلية", "اصل", "আসল", "Orijinal", "اصلی", "Asal", "Originale", "Исходная"],
    "from_wallpaper": ["من الخلفية", "وال پیپر سے", "ওয়ালপেপার থেকে", "Duvar kâğıdından", "از تصویر زمینه", "Daripada kertas dinding", "Depuis le fond d'écran", "Из обоев"],
    "needs_android12": ["يتطلب Android 12+", "Android 12+ درکار ہے", "Android 12+ প্রয়োজন", "Android 12+ gerekir", "نیازمند Android 12+", "Memerlukan Android 12+", "Nécessite Android 12+", "Нужен Android 12+"],
    "classic_teal": ["أزرق مخضر كلاسيكي", "کلاسک ٹیل", "ক্লাসিক টিল", "Klasik camgöbeği", "سبزآبی کلاسیک", "Teal klasik", "Sarcelle classique", "Классический бирюзовый"],
    "contrast": ["تباين الألوان", "رنگوں کا تضاد", "রঙের কনট্রাস্ট", "Renk kontrastı", "کنتراست رنگ", "Kontras warna", "Contraste des couleurs", "Контрастность"],
    "contrast_standard": ["قياسي", "معیاری", "সাধারণ", "Standart", "استاندارد", "Standard", "Standard", "Обычная"],
    "contrast_medium": ["متوسط", "درمیانہ", "মাঝারি", "Orta", "متوسط", "Sederhana", "Moyen", "Средняя"],
    "contrast_high": ["عالٍ", "زیادہ", "উচ্চ", "Yüksek", "بالا", "Tinggi", "Élevé", "Высокая"],
    "contrast_note": ["ينطبق التباين على اللوحة الأصلية.", "تضاد صرف اصل پیلیٹ پر لاگو ہوتا ہے۔", "কনট্রাস্ট শুধু আসল প্যালেটে প্রযোজ্য।", "Kontrast yalnızca Orijinal palete uygulanır.", "کنتراست فقط برای پالت اصلی اعمال می‌شود.", "Kontras hanya untuk palet Asal.", "Le contraste s'applique à la palette Originale.", "Контрастность действует только для исходной палитры."],
    "amoled_sub": ["خلفية سوداء تمامًا في الوضع الداكن", "تاریک موڈ میں مکمل سیاہ پس منظر", "ডার্ক মোডে সম্পূর্ণ কালো পটভূমি", "Koyu modda tam siyah arka plan", "پس‌زمینهٔ کاملاً سیاه در حالت تیره", "Latar hitam pekat dalam mod gelap", "Fond noir pur en mode sombre", "Чисто чёрный фон в тёмной теме"],
    "translit_title": ["النطق بالحروف اللاتينية", "رومن تلفظ", "লাতিন প্রতিবর্ণীকরণ", "Latin harfli okunuş", "آوانگاری لاتین", "Transliterasi Rumi", "Translittération latine", "Латинская транслитерация"],
    "translit_sub": ["عرض النطق اللاتيني في لوحة الآية", "آیت شیٹ میں رومن تلفظ دکھائیں", "আয়াত শিটে লাতিন উচ্চারণ দেখান", "Ayet panelinde Latin harfli okunuşu göster", "نمایش تلفظ لاتین در برگهٔ آیه", "Tunjukkan sebutan Rumi dalam helaian ayat", "Afficher la prononciation latine dans la fiche du verset", "Показывать латинское произношение в карточке аята"],
    "tajweed_title": ["ألوان التجويد", "تجوید کے رنگ", "তাজবীদের রং", "Tecvid renkleri", "رنگ‌های تجوید", "Warna tajwid", "Couleurs du tajwid", "Цвета таджвида"],
    "tajweed_sub": ["تلوين الحروف حسب أحكام التجويد", "حروف کو تجوید کے قواعد کے مطابق رنگین دکھائیں", "তাজবীদের নিয়ম অনুযায়ী অক্ষর রঙিন দেখান", "Harfleri tecvid kurallarına göre renklendir", "رنگ‌آمیزی حروف بر اساس احکام تجوید", "Warnakan huruf mengikut hukum tajwid", "Colorer les lettres selon les règles du tajwid", "Раскрашивать буквы по правилам таджвида"],
    "translations": ["الترجمات", "ترجمے", "অনুবাদ", "Mealler", "ترجمه‌ها", "Terjemahan", "Traductions", "Переводы"],
    "delete_translation_cd": ["إزالة الترجمة %1$s", "ترجمہ %1$s ہٹائیں", "অনুবাদ %1$s সরান", "%1$s mealini kaldır", "حذف ترجمهٔ %1$s", "Buang terjemahan %1$s", "Retirer la traduction %1$s", "Убрать перевод %1$s"],
    "add_translation": ["إضافة ترجمة", "ترجمہ شامل کریں", "অনুবাদ যোগ করুন", "Meal ekle", "افزودن ترجمه", "Tambah terjemahan", "Ajouter une traduction", "Добавить перевод"],
    "close": ["إغلاق", "بند کریں", "বন্ধ", "Kapat", "بستن", "Tutup", "Fermer", "Закрыть"],
    "about": ["حول التطبيق", "تعارف", "সম্পর্কে", "Hakkında", "درباره", "Perihal", "À propos", "О приложении"],
    "version": ["الإصدار", "ورژن", "সংস্করণ", "Sürüm", "نسخه", "Versi", "Version", "Версия"],
    "data_source": ["مصدر البيانات", "ڈیٹا کا ماخذ", "ডেটার উৎস", "Veri kaynağı", "منبع داده", "Sumber data", "Source des données", "Источник данных"],
    "data_source_sub": [
        "Quranic Universal Library (Tarteel): تخطيط KFGQPC V4، والبيانات الوصفية، ومعلومات السور، والنطق اللاتيني، والترجمات.",
        "Quranic Universal Library (Tarteel): KFGQPC V4 لے آؤٹ، میٹا ڈیٹا، سورتوں کی معلومات، رومن تلفظ اور ترجمے۔",
        "Quranic Universal Library (Tarteel): KFGQPC V4 বিন্যাস, মেটাডেটা, সূরার তথ্য, প্রতিবর্ণীকরণ ও অনুবাদ।",
        "Quranic Universal Library (Tarteel): KFGQPC V4 düzeni, üst veriler, sure bilgileri, transliterasyon ve mealler.",
        "Quranic Universal Library (Tarteel): چیدمان KFGQPC V4، فراداده، اطلاعات سوره‌ها، آوانگاری و ترجمه‌ها.",
        "Quranic Universal Library (Tarteel): susun atur KFGQPC V4, metadata, maklumat surah, transliterasi dan terjemahan.",
        "Quranic Universal Library (Tarteel) : mise en page KFGQPC V4, métadonnées, infos des sourates, translittération et traductions.",
        "Quranic Universal Library (Tarteel): макет KFGQPC V4, метаданные, сведения о сурах, транслитерация и переводы.",
    ],
    "font": ["الخطوط", "فونٹ", "ফন্ট", "Yazı tipleri", "قلم‌ها", "Fon", "Polices", "Шрифты"],
    "font_sub": [
        "KFGQPC (مجمع الملك فهد لطباعة المصحف الشريف): خطوط صفحات V4 بالتجويد، وعناوين السور، وخط حفص العثماني. حقوق النشر لـ KFGQPC؛ مستخدمة دون تعديل.",
        "KFGQPC (شاہ فہد قرآن پرنٹنگ کمپلیکس): تجوید والے V4 صفحات کے فونٹ، سورتوں کے عنوان اور حفص عثمانی رسم الخط۔ جملہ حقوق KFGQPC؛ بغیر ترمیم کے استعمال۔",
        "KFGQPC (বাদশাহ ফাহাদ কুরআন মুদ্রণ কমপ্লেক্স): তাজবীদসহ V4 পৃষ্ঠার ফন্ট, সূরার শিরোনাম ও হাফস উসমানি লিপি। স্বত্ব KFGQPC-এর; অপরিবর্তিত ব্যবহৃত।",
        "KFGQPC (Kral Fahd Kur'an Basım Kompleksi): tecvidli V4 sayfa yazı tipleri, sure başlıkları ve Hafs Osmanlı hattı. Telif hakkı KFGQPC'ye aittir; değiştirilmeden kullanılır.",
        "KFGQPC (مجتمع ملک فهد برای چاپ قرآن): قلم‌های صفحهٔ V4 با تجوید، سرلوحهٔ سوره‌ها و خط حفص عثمانی. حق نشر از آنِ KFGQPC است؛ بدون تغییر استفاده شده.",
        "KFGQPC (Kompleks Percetakan Al-Quran Raja Fahd): fon halaman V4 bertajwid, kepala surah dan tulisan Hafs Uthmani. Hak cipta KFGQPC; digunakan tanpa pengubahsuaian.",
        "KFGQPC (Complexe du Roi Fahd pour l'impression du Coran) : polices des pages V4 avec tajwid, en-têtes des sourates et écriture Hafs uthmanie. Droits d'auteur KFGQPC ; utilisées sans modification.",
        "KFGQPC (Комплекс короля Фахда по печати Корана): шрифты страниц V4 с таджвидом, заголовки сур и османский шрифт Хафс. Авторские права KFGQPC; используются без изменений.",
    ],
    "selected": ["محدد", "منتخب", "নির্বাচিত", "Seçili", "انتخاب‌شده", "Dipilih", "Sélectionné", "Выбрано"],
    "language": ["اللغة", "زبان", "ভাষা", "Dil", "زبان", "Bahasa", "Langue", "Язык"],
    "language_system": ["لغة النظام", "سسٹم کی زبان", "সিস্টেমের ভাষা", "Sistem dili", "زبان سیستم", "Bahasa sistem", "Langue du système", "Язык системы"],
    "lang_name_id": ["الإندونيسية", "انڈونیشیائی", "ইন্দোনেশীয়", "Endonezce", "اندونزیایی", "Indonesia", "Indonésien", "Индонезийский"],
    "lang_name_en": ["الإنجليزية", "انگریزی", "ইংরেজি", "İngilizce", "انگلیسی", "Inggeris", "Anglais", "Английский"],
    "lang_name_ar": ["العربية", "عربی", "আরবি", "Arapça", "عربی", "Arab", "Arabe", "Арабский"],
    "lang_name_ur": ["الأردية", "اردو", "উর্দু", "Urduca", "اردو", "Urdu", "Ourdou", "Урду"],
    "lang_name_bn": ["البنغالية", "بنگالی", "বাংলা", "Bengalce", "بنگالی", "Benggali", "Bengali", "Бенгальский"],
    "lang_name_tr": ["التركية", "ترکی", "তুর্কি", "Türkçe", "ترکی", "Turki", "Turc", "Турецкий"],
    "lang_name_fa": ["الفارسية", "فارسی", "ফারসি", "Farsça", "فارسی", "Parsi", "Persan", "Персидский"],
    "lang_name_ms": ["الملايوية", "مالے", "মালয়", "Malayca", "مالایی", "Melayu", "Malais", "Малайский"],
    "lang_name_fr": ["الفرنسية", "فرانسیسی", "ফরাসি", "Fransızca", "فرانسوی", "Perancis", "Français", "Французский"],
    "lang_name_ru": ["الروسية", "روسی", "রুশ", "Rusça", "روسی", "Rusia", "Russe", "Русский"],
    "source_note": per(lambda c: {"ar": "المصدر", "ur": "ماخذ", "bn": "উৎস", "tr": "Kaynak", "fa": "منبع", "ms": "Sumber", "fr": "Source ", "ru": "Источник"}[c] + ": Quranic Universal Library (Tarteel)."),
    "translations_loading_cd": ["جارٍ تحميل الترجمات", "ترجمے لوڈ ہو رہے ہیں", "অনুবাদ লোড হচ্ছে", "Mealler yükleniyor", "در حال بارگیری ترجمه‌ها", "Memuatkan terjemahan", "Chargement des traductions", "Загрузка переводов"],
    "translations_catalog_error": [
        "تعذّر تحميل قائمة الترجمات المتاحة للتنزيل. تحقّق من الاتصال وحاول مرة أخرى.",
        "ڈاؤن لوڈ کے قابل ترجموں کی فہرست لوڈ نہیں ہو سکی۔ کنکشن چیک کر کے دوبارہ کوشش کریں۔",
        "ডাউনলোডযোগ্য অনুবাদের তালিকা লোড করা যায়নি। সংযোগ দেখে আবার চেষ্টা করুন।",
        "İndirilebilir meal listesi yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.",
        "فهرست ترجمه‌های قابل بارگیری بارگذاری نشد. اتصال را بررسی کنید و دوباره تلاش کنید.",
        "Senarai terjemahan yang boleh dimuat turun tidak dapat dimuatkan. Semak sambungan dan cuba lagi.",
        "Impossible de charger la liste des traductions téléchargeables. Vérifiez votre connexion et réessayez.",
        "Не удалось загрузить список доступных переводов. Проверьте подключение и повторите попытку.",
    ],
    "retry": ["إعادة المحاولة", "دوبارہ کوشش کریں", "আবার চেষ্টা করুন", "Tekrar dene", "تلاش دوباره", "Cuba lagi", "Réessayer", "Повторить"],
    "translation_download_failed": [
        "تعذّر تنزيل %1$s. تحقّق من الاتصال وحاول مرة أخرى.",
        "%1$s ڈاؤن لوڈ نہیں ہو سکا۔ کنکشن چیک کر کے دوبارہ کوشش کریں۔",
        "%1$s ডাউনলোড করা যায়নি। সংযোগ দেখে আবার চেষ্টা করুন।",
        "%1$s indirilemedi. Bağlantınızı kontrol edip tekrar deneyin.",
        "بارگیری %1$s انجام نشد. اتصال را بررسی کنید و دوباره تلاش کنید.",
        "%1$s tidak dapat dimuat turun. Semak sambungan dan cuba lagi.",
        "Impossible de télécharger %1$s. Vérifiez votre connexion et réessayez.",
        "Не удалось скачать %1$s. Проверьте подключение и повторите попытку.",
    ],
    "translation_downloading_cd": ["جارٍ تنزيل %1$s", "%1$s ڈاؤن لوڈ ہو رہا ہے", "%1$s ডাউনলোড হচ্ছে", "%1$s indiriliyor", "در حال بارگیری %1$s", "Memuat turun %1$s", "Téléchargement de %1$s", "Скачивается %1$s"],
    "translation_size_mb": ["%1$s م.ب", "%1$s MB", "%1$s MB", "%1$s MB", "%1$s مگابایت", "%1$s MB", "%1$s Mo", "%1$s МБ"],
    "translation_download_cd": ["تنزيل %1$s", "%1$s ڈاؤن لوڈ کریں", "%1$s ডাউনলোড করুন", "%1$s indir", "بارگیری %1$s", "Muat turun %1$s", "Télécharger %1$s", "Скачать %1$s"],
    "delete_downloaded_translation_cd": ["حذف الملف المنزَّل لـ %1$s", "%1$s کی ڈاؤن لوڈ فائل حذف کریں", "%1$s-এর ডাউনলোড করা ফাইল মুছুন", "%1$s için indirilen dosyayı sil", "حذف فایل بارگیری‌شدهٔ %1$s", "Padam fail muat turun %1$s", "Supprimer le fichier téléchargé de %1$s", "Удалить скачанный файл %1$s"],
    "translations_all_added": ["أُضيفت كل الترجمات المتاحة.", "تمام دستیاب ترجمے شامل ہو چکے ہیں۔", "সব উপলব্ধ অনুবাদ যোগ করা হয়েছে।", "Mevcut tüm mealler eklendi.", "همهٔ ترجمه‌های موجود افزوده شده‌اند.", "Semua terjemahan yang tersedia telah ditambah.", "Toutes les traductions disponibles ont été ajoutées.", "Все доступные переводы уже добавлены."],
    "translations_downloadable": ["متاحة للتنزيل", "ڈاؤن لوڈ کے لیے دستیاب", "ডাউনলোডের জন্য উপলব্ধ", "İndirilebilir", "قابل بارگیری", "Boleh dimuat turun", "Disponibles au téléchargement", "Доступны для скачивания"],
    "credits": ["شكر وتقدير", "اعترافات", "কৃতজ্ঞতা", "Katkıda bulunanlar", "سپاسگزاری", "Penghargaan", "Remerciements", "Благодарности"],
    "credits_sub": [
        "معظم موارد QUL أعدّها المجتمع: مجمع الملك فهد لطباعة المصحف الشريف، وتنزيل، وQuran.com، والمترجمون، وغيرهم كثير. اضغط لعرض القائمة كاملة.",
        "QUL کے زیادہ تر وسائل کمیونٹی نے تیار کیے: شاہ فہد قرآن پرنٹنگ کمپلیکس، تنزیل، Quran.com، مترجمین اور بہت سے دوسرے۔ پوری فہرست کے لیے تھپتھپائیں۔",
        "QUL-এর বেশিরভাগ উপকরণ সম্প্রদায়ের তৈরি: বাদশাহ ফাহাদ কুরআন মুদ্রণ কমপ্লেক্স, তানযিল, Quran.com, অনুবাদকগণ ও আরও অনেকে। পূর্ণ তালিকার জন্য ট্যাপ করুন।",
        "QUL kaynaklarının çoğu topluluk tarafından hazırlandı: Kral Fahd Kur'an Basım Kompleksi, Tanzil, Quran.com, mütercimler ve daha birçokları. Tam liste için dokunun.",
        "بیشتر منابع QUL را جامعه فراهم کرده است: مجتمع ملک فهد برای چاپ قرآن، تنزیل، Quran.com، مترجمان و بسیاری دیگر. برای فهرست کامل ضربه بزنید.",
        "Kebanyakan sumber QUL dihasilkan oleh komuniti: Kompleks Percetakan Al-Quran Raja Fahd, Tanzil, Quran.com, para penterjemah dan ramai lagi. Ketik untuk senarai penuh.",
        "La plupart des ressources de QUL ont été créées par la communauté : le Complexe du Roi Fahd pour l'impression du Coran, Tanzil, Quran.com, les traducteurs et bien d'autres. Touchez pour la liste complète.",
        "Большинство материалов QUL создано сообществом: Комплексом короля Фахда по печати Корана, Tanzil, Quran.com, переводчиками и многими другими. Нажмите, чтобы увидеть полный список.",
    ],
    "support": ["ادعم التطوير", "ترقی میں مدد کریں", "উন্নয়নে সহায়তা করুন", "Geliştirmeyi destekle", "حمایت از توسعه", "Sokong pembangunan", "Soutenir le développement", "Поддержать разработку"],
    "support_sub": ["اشترِ للمطوّر قهوة على Ko-fi", "Ko-fi پر ڈویلپر کو کافی پلائیں", "Ko-fi-তে ডেভেলপারকে একটি কফি কিনে দিন", "Ko-fi'de geliştiriciye bir kahve ısmarla", "در Ko-fi برای توسعه‌دهنده قهوه بخرید", "Belanja pembangun secawan kopi di Ko-fi", "Offrez un café au développeur sur Ko-fi", "Угостите разработчика кофе на Ko-fi"],
    "translation_label": per(lambda c: "%1$s - %2$s"),
    "amoled": per(lambda c: "AMOLED"),
}


def esc(text):
    return text.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"').replace("&", "&amp;").replace("<", "&lt;")


def base_keys():
    root = ET.parse(RES / "values/strings.xml").getroot()
    return [(e.get("name"), e.text or "") for e in root.findall("string") if e.get("translatable") != "false"]


def placeholders(text):
    return sorted(re.findall(r"%\d\$[sd]", text))


def main():
    keys = base_keys()
    missing = [k for k, _ in keys if k not in T]
    bad = [k for k, en in keys if k in T for v in T[k] if placeholders(v) != placeholders(en)]
    for k in T:
        assert len(T[k]) == len(LANGS), k
    if "--check" not in sys.argv:
        for i, code in enumerate(LANGS):
            lines = ['<?xml version="1.0" encoding="utf-8"?>', "<!-- Dibuat oleh tools/ui_translations.py; ubah di sana. -->", "<resources>"]
            lines += [f'    <string name="{k}">{esc(T[k][i])}</string>' for k, _ in keys if k in T]
            lines.append("</resources>")
            out = RES / f"values-{code}/strings.xml"
            out.parent.mkdir(exist_ok=True)
            out.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"ditulis: {len(LANGS)} bahasa, {len(keys) - len(missing)} kunci")
    if missing:
        print("belum diterjemahkan (memakai teks Inggris):", *missing, sep="\n  ")
    if bad:
        print("placeholder tidak cocok:", *sorted(set(bad)), sep="\n  ")
    return 1 if (missing or bad) and "--check" in sys.argv else 0


if __name__ == "__main__":
    sys.exit(main())

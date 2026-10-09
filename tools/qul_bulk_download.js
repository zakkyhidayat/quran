(async () => {
  const FORMATS = ["translation-with-footnote-tags.sqlite"];
  const FILTER = "";
  const DELAY_MS = 3000;

  const links = [...document.querySelectorAll("a")].filter(a =>
    FORMATS.some(f => a.textContent.trim().replace(/\s+/g, "") === "Download" + f) && a.href && !a.href.endsWith("#_"));
  if (!links.length) {
    console.warn("Tidak ada tautan unduhan. Sudah login?");
    return;
  }
  const picked = links.filter(a => {
    if (!FILTER) return true;
    let card = a;
    for (let i = 0; i < 8 && card.parentElement; i++) card = card.parentElement;
    return card.textContent.includes(FILTER);
  });
  const urls = [...new Set(picked.map(a => a.href))];
  console.log(urls.length + " file akan diunduh");
  for (let i = 0; i < urls.length; i++) {
    const a = document.createElement("a");
    a.href = urls[i];
    a.download = "";
    document.body.appendChild(a);
    a.click();
    a.remove();
    console.log((i + 1) + "/" + urls.length + " " + urls[i]);
    await new Promise(r => setTimeout(r, DELAY_MS));
  }
  console.log("Selesai.");
})();

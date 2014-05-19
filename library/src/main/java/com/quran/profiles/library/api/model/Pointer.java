package com.quran.profiles.library.api.model;

/**
 * Pointer to a place in the Quran
 * Created by ahmedre on 5/18/14.
 */
public class Pointer {
  int page;
  int chapter;
  int verse;

  public Pointer() {
  }

  public static Pointer fromPage(int page) {
    final Pointer p = new Pointer();
    p.page = page;
    return p;
  }

  public static Pointer fromAyah(int sura, int ayah) {
    final Pointer p = new Pointer();
    p.chapter = sura;
    p.verse = ayah;
    return p;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("");
    if (page > 0) {
      builder.append("Page: ").append(this.page);
    } else if (chapter > 0 && this.verse > 0) {
      builder.append("Sura: ").append(this.chapter)
          .append(", Ayah: ").append(this.verse);
    }
    return builder.toString();
  }
}

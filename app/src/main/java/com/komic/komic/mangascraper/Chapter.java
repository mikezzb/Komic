package com.komic.komic.mangascraper;

public abstract class Chapter {
    public String id;
    public String name;
    public int numPages;
    protected String[] pageUrls;

    public Chapter(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Chapter(String id, String name, String[] pageUrls) {
        this(id, name);
        this.setPageUrls(pageUrls);
    }

    public void setPageUrls(String[] pageUrls) {
        this.pageUrls = pageUrls;
        this.numPages = pageUrls.length;
    }

    protected abstract String[] inflateChapterPages();

    public String[] getChapterPages() {
        if (pageUrls == null) {
            pageUrls = inflateChapterPages();
        }
        return pageUrls;
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

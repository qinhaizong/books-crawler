package com.wxhledu.cn.books.crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.wxhledu.cn.books.crawler.domain.Book;
import com.wxhledu.cn.books.crawler.domain.Category;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

@Slf4j
public class RxApplication {

    static File sqlFile;

    static List<Integer> categoryids = new ArrayList<>();

    static {
        try {
            sqlFile = cleanAndCreateFile("book_category.sql");
            log.info("生成文件:{}", sqlFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {


        FileUtils.writeStringToFile(sqlFile, "set autocommit=0;\r\n", true);
        final List<Integer> bookids = new ArrayList<>();

        final Category root = new Category();
        root.setId(1);
        root.setPid(0);
        root.setHref("/list/1-1");
        root.setNum(74587);
        root.setName("全部图书");
        FileUtils.writeStringToFile(sqlFile, toSqlString(root), true);
        Observable.just(root.getHref()).flatMap(new Func1<String, Observable<Element>>() {
            @Override
            public Observable<Element> call(String s) {
                try {
                    Document doc = JsoupApplication.loadDocument(s);
                    Elements as = doc.select(".m-category-nav .level1 a");
                    return Observable.from(as);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Observable.empty();
            }
        }).map(new Func1<Element, Category>() {
            @Override
            public Category call(Element element) {
                String href = element.attr("href");
                //log.info("一级分类:{}", href);
                Category category = new Category();
                category.setId(Integer.parseInt(href.substring(href.lastIndexOf("/") + 1, href.indexOf("-"))));
                category.setPid(root.getId());
                category.setHref(href);
                category.setName(element.select("span").text());
                category.setNum(Integer.parseInt(element.select("em").text()));
                return category;
            }
        }).doOnNext(new Action1<Category>() {
            @Override
            public void call(Category category) {
                log.info("一级分类:\t{}", category);
                writeCategory(category);
            }
        }).flatMap(new Func1<Category, Observable<Element>>() {
            @Override
            public Observable<Element> call(Category c) {
                try {
                    Document document = JsoupApplication.loadDocument(c.getHref());
                    Elements elements = document.select(".m-category-nav .level2 a");
                    elements.attr("data-id", c.getId() + "");
                    return Observable.from(elements);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Observable.empty();
            }
        }).flatMap(new Func1<Element, Observable<Category>>() {
            @Override
            public Observable<Category> call(Element element) {
                Category category = new Category();
                String href = element.attr("href");
                //log.info("二级分类：{}", href);
                category.setId(Integer.parseInt(href.substring(href.lastIndexOf("/") + 1, href.indexOf("-"))));
                category.setPid(Integer.parseInt(element.attr("data-id")));
                category.setHref(href);
                category.setName(element.select("span").text());
                category.setNum(Integer.parseInt(element.select("em").text()));
                return Observable.just(category);
            }
        }).filter(new Func1<Category, Boolean>() {
            @Override
            public Boolean call(Category category) {
                return category.getPid() != root.getId();
            }
        }).doOnNext(new Action1<Category>() {
            @Override
            public void call(Category category) {
                log.info("\t二级分类:\t{}", category);
                writeCategory(category);
            }
        }).flatMap(new Func1<Category, Observable<Category>>() {
            @Override
            public Observable<Category> call(final Category category) {
                int max = category.getNum() % 14 == 0 ? category.getNum() / 14 : category.getNum() / 14 + 1;
                return Observable.range(1, max).map(new Func1<Integer, Category>() {
                    @Override
                    public Category call(Integer integer) {
                        Category c = new Category();
                        c.setId(category.getId());
                        c.setPid(category.getPid());
                        c.setHref("/list/" + category.getId() + "-" + integer);
                        return c;
                    }
                });
            }
        }).flatMap(new Func1<Category, Observable<Element>>() {
            @Override
            public Observable<Element> call(Category category) {
                try {
                    Document document = JsoupApplication.loadDocument(category.getHref());
                    Elements elements = document.select(".j-bookitm");
                    elements.attr("data-id", category.getId() + "");
                    return Observable.from(elements);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Observable.empty();
            }
        }).map(new Func1<Element, Book>() {
            @Override
            public Book call(Element bookitm) {
                String img = bookitm.select(".cover img").attr("src");
                Elements em = bookitm.select(".u-price em");
                double price = 0;
                double oldPrice = 0;
                if (null != em && !em.isEmpty()) { //非免费图书
                    price = JsoupApplication.parsePrice(em.text());
                    oldPrice = JsoupApplication.parsePrice(bookitm.select(".u-price del").text());
                    //log.info(">>>>>>>>>>>>>>>{}, {}", price, oldPrice);
                }
                Element a = bookitm.select(".info .wrap a").first();
                String href = a.attr("href");
                int id = Integer.parseInt(href.split("/book/")[1]);
                String name = a.text();
                String author = bookitm.select(".info .u-author span").text();
                String desc = bookitm.select(".info .desc").text();
                int grade = Integer.parseInt(bookitm.select(".info .icon").attr("class").split("grade")[1]);
                // log.info("id:{}, grade:{}, name:{}, author:{}, desc:{}, img:{}, href:{}", id, grade, name, author, desc, img, href);
                Book book = new Book();
                book.setCategoryid(Integer.parseInt(bookitm.attr("data-id")));
                book.setId(id);
                book.setGrade(grade);
                book.setAuthor(author);
                book.setName(name);
                book.setHref(href);
                book.setImg(img);
                book.setDesc(desc);
                book.setPrice(price);
                book.setOldPrice(oldPrice);
                return book;
            }
        }).doOnNext(new Action1<Book>() {
            @Override
            public void call(Book book) {
                //log.info("{}", book);
                if (!bookids.contains(book.getId())) {
                    bookids.add(book.getId());
                    try {
                        FileUtils.writeStringToFile(sqlFile, toSqlString(book), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                try {
                    FileUtils.writeStringToFile(sqlFile, "commit;\r\n", true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).subscribe();

    }

    public static File cleanAndCreateFile(String filename) throws IOException {
        File file = new File(filename);
        if (file.exists()) {
            FileUtils.forceDelete(file);
            file = new File(filename);
        }
        return file;
    }

    private static void writeCategory(Category category) {
        if (!categoryids.contains(category.getId())) {
            categoryids.add(category.getId());
            try {
                FileUtils.writeStringToFile(sqlFile, toSqlString(category), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static String toSqlString(Category c) {
        return String.format("INSERT INTO category_t(id,name,pid) VALUES(%d,'%s',%d);\r\n", c.getId(), StringEscapeUtils.escapeSql(c.getName()), c.getPid());
    }

    public static String toSqlString(Book b) {
        return String.format("INSERT INTO book_t (id,name,description,old_price, price, filename, author, star) VALUES (%d,'%s','%s',%f,%f,'%s','%s',%d);\r\n" +
                        "INSERT INTO book_category_rt (bid, cid) VALUES (%d,%d);\r\n",
                b.getId(), StringEscapeUtils.escapeSql(b.getName()), StringEscapeUtils.escapeSql(b.getDesc()), b.getOldPrice(), b.getPrice(), b.getImg(), b.getAuthor(), b.getGrade(), b.getId(), b.getCategoryid());
    }

}

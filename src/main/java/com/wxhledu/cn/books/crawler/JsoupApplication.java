package com.wxhledu.cn.books.crawler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.wxhledu.cn.books.crawler.domain.Book;
import com.wxhledu.cn.books.crawler.domain.Category;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsoupApplication {

	public static void main(String[] args) throws IOException {
		List<Category> list = new ArrayList<>();
		Category root = new Category();
		root.setId(1);
		root.setPid(0);
		root.setHref("/list/1-1");
		root.setNum(74587);
		root.setName("全部图书");
		//root.setChildren(new ArrayList<Category>());
		list.add(root);
		//Document doc = Jsoup.connect("http://www.duokan.com"+root.getHref()).get();
		Document doc = loadDocument(root.getHref());
		Elements as = doc.select(".m-category-nav .level1 a");
		for (Element a : as) {
			String href = a.attr("href");
			Category c = new Category();
			log.info("level1:{}", href);
			c.setId(Integer.parseInt(href.substring(href.lastIndexOf("/") + 1, href.indexOf("-"))));
			c.setPid(root.getId());
			c.setHref(href);
			c.setName(a.select("span").text());
			c.setNum(Integer.parseInt(a.select("em").text()));
			//List<Category> level1 = new ArrayList<Category>();
			list.add(c);
			//Document doc2 = Jsoup.connect("http://www.duokan.com" + c.getHref()).get();
			Document doc2 = loadDocument(c.getHref());
			Elements as2 = doc2.select(".m-category-nav .level2 a");
			for (Element a2 : as2) {
				String href2 = a2.attr("href");
				Category c2 = new Category();
				c2.setId(Integer.parseInt(href2.substring(href2.lastIndexOf("/") + 1, href2.indexOf("-"))));
				c2.setPid(c.getId());
				c2.setHref(href2);
				c2.setName(a2.select("span").text());
				c2.setNum(Integer.parseInt(a2.select("em").text()));
				//log.info("level2: {}", href2);
				//level1.add(c);
				list.add(c2);

				int max = c2.getNum() % 14 == 0 ? c2.getNum() / 14 : c2.getNum() / 14 + 1;
				log.info("href: {}, num:{}", c2.getHref(), max);
				for (int i = 1; i <= max; i++) {
					log.info("/list/{}-{}", c2.getId(), i);
					loadFile("/list/" + c2.getId() + "-" + i);
				}
			}

			//c.setChildren(level1);
			//root.getChildren().add(c);
		}

		/*for (Category category : list) {
			log.info("{}", category);
		}*/
	}

	/**
	 * 加载文档
	 * @param uri
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static Document loadDocument(String uri) throws MalformedURLException, IOException {
		File file = loadFile(uri);
		return Jsoup.parse(file, "utf-8");
	}

	/**
	 * 先查看本地有没有缓存此文件，有则返回该文件，无则从网络下载此文件
	 * @param uri
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static File loadFile(String uri) throws MalformedURLException, IOException {
		File file = new File("." + uri);
		if (!file.exists()) {
			log.info("文件{}不存在！创建文件", file.getAbsolutePath());
			FileUtils.copyURLToFile(new URL("http://www.duokan.com" + uri), file);
		}
		return file;
	}

	public static double parsePrice(String text) {
		if(StringUtils.isBlank(text)){
			return 0;
		}
		String price = text.split("\\s+")[1];
		return parseDouble(price, 0);
	}

	private static double parseDouble(String price, int defaultValue) {
		double result = defaultValue;
		try {
			result = Double.parseDouble(price);
		} catch (Exception e) {
			result = defaultValue;
		}
		return result;
	}

	public static List<Book> getBooks(String uri) throws MalformedURLException, IOException{
		if(StringUtils.isBlank(uri)){
			return null;
		}
		int categoryid = Integer.parseInt(uri.substring(uri.lastIndexOf("/") + 1, uri.indexOf("-")));
		Document document = loadDocument(uri);
		Elements bookitms = document.select(".j-bookitm");
		List<Book> books = new ArrayList<>();
		for (Element bookitm : bookitms) {
			String img = bookitm.select(".cover img").attr("src");
			Elements em = bookitm.select(".u-price em");
			double price = 0;
			double oldPrice = 0;
			if (null != em && !em.isEmpty()) { //非免费图书
				price = parsePrice(em.text());
				oldPrice = parsePrice(bookitm.select(".u-price del").text());
				log.info(">>>>>>>>>>>>>>>{}, {}", price, oldPrice);
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
			book.setCategoryid(categoryid);
			book.setId(id);
			book.setGrade(grade);
			book.setAuthor(author);
			book.setName(name);
			book.setHref(href);
			book.setImg(img);
			book.setDesc(desc);
			book.setPrice(price);
			book.setOldPrice(oldPrice);
			log.info("{}", book);
		}
		return books;
	}
}

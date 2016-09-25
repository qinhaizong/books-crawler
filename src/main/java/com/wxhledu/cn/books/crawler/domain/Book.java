package com.wxhledu.cn.books.crawler.domain;

import lombok.Data;

@Data
public class Book {

	private int id;
	
	private String name;
	
	private int grade;
	
	private String author;
	
	private String desc;
	
	private String img;
	
	private String href;
	
	private double price;
	
	private double oldPrice;
	
	private int categoryid;
	
}

package com.wxhledu.cn.books.crawler.domain;

import java.util.List;

import lombok.Data;

@Data
public class Category {

	private int id;
	
	private int pid;
	
	private String name;
	
	private String href;
	
	private int num;
	
	//private List<Category> children;
}

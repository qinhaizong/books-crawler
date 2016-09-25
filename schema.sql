CREATE TABLE category_t(
	id int PRIMARY KEY auto_increment COMMENT '分类编号',
	name VARCHAR(100) NOT NULL COMMENT '图书分类',
	pid int COMMENT '分类的父级编号',
	display_order int COMMENT '显示的顺序',
	status int DEFAULT 1 COMMENT '1：有效，0：无效'
);

CREATE TABLE book_t (
	id int PRIMARY KEY auto_increment COMMENT '图书编号',
	name varchar(200) NOT NULL COMMENT '图书的名称',
	description varchar(3000) NOT NULL COMMENT '内容描述',
	old_price DOUBLE COMMENT '原价',
	price DOUBLE COMMENT '现价',
	filename VARCHAR(200) COMMENT '图片名称',
	author VARCHAR(100) COMMENT '作者',
	amount INT COMMENT '库存',
	star INT COMMENT '图书星级',
	status int DEFAULT 1 COMMENT '图书状态 1有效，0删除'
);

CREATE TABLE book_category_rt(
	bid int not NULL COMMENT '图书的编号',
	cid int not null COMMENT '分类的编号'
);
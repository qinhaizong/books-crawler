# 一个爬虫程序，用来爬多看阅读网站里边的电子书籍信息

## 应用技术：

1. commons-io: 下载网页
2. jsoup: 解析网页
3. rxjava: 用于异步处理数据

## 程序入口：运行RxApplication.main

* JsoupApplication这是初版，这种方式处理的话里边的循环将指数上升，最后放弃用这种方式玩;
* 用rx 异步处理的方式比较爽：像写jquery, 单线程不阻塞，关注数据变换……



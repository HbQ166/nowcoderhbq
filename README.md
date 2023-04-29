# 社区论坛系统
项目描述:基于SpringBoot开发的具备基本功能的社区论坛系统开发项目。主要功能包括：基于邮件激活的注册方式，加入随
机验证码的登录验证，实现登录状态检查，根据权限控制展现不同的界面和功能，以及发帖，点赞，关注，私信，系统通知，
帖子按热度排序，搜索，访问量统计等功能。  
技术栈：SpringBoot，SSM（Spring，SpringMVC，
MyBaits）,Thymeleaf,Git,Redis,Kafka,Elasticsearch,SpringSecurity,Quartz,wkhtmltopdf，Caffeine。  
主要工作：  
在Spring Boot+SSM框架之上构建项目，并通过AOP与拦截器实现统一的进行了日志记录、事务管理、异常处理，使用
Git进行版本控制。  
对于访问频率较高的点赞和关注功能,以及登录凭证信息，利用Redis存储相关数据，单机可达5000TPS，并利用Kafka实现
了关注，点赞的异步站内通知功能，单机可达7000TPS。  
利用ElasticSearch实现了全文搜索功能，可准确匹配搜索结果，并高亮显示关键词。  
利用Quartz实现任务调度，实现了定时计算帖子分数的功能，并基于此实现了热门帖子的顺序显示；并利用
Redis+Caffeine对热门帖子查询实现了两级缓存，通过Jmeter压测有效提高了性能，单机可达8000QPS，同时一定程度
预防了缓存雪崩的发生。  
利用Spring Security实现了权限控制，实现了多重角色、URL级别的权限管理。  
利用HyperLogLog、Bitmap分别实现了UV、DAU的统计功能。  
利用Actuator对应用的Bean、缓存、日志、路径等多个维度进行了监控，并通过自定义的端口对数据库连接进行了监控。  
利用wkhtmltopdf模拟实现了分享功能。  
完成了项目在阿里云服务器上的部署。  

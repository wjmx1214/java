package com.boot.dao.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
	实体类映射关系注解<br>
	例：<pre>
	@EntityMeta(table = "xxx_stu")
	public class Stu{

		@EntityMeta(isId=true, idAuto=true)
		private Long id;

		@EntityMeta(column="stu_name")
		private String name;

		private Integer age;
	}
	</pre>
	@author 2020-12-01 create wang.jia.le
	@version 1.0.0
**/
@Retention(RetentionPolicy.RUNTIME) 			// 注解会在class字节码文件中存在，在运行时可以通过反射获取到
@Target({ElementType.FIELD,ElementType.TYPE}) 	// 定义注解的作用目标(类，常量，字段，方法等)
@Documented 									// 表示该注解将被包含在javadoc中  
public @interface EntityMeta {  

	/**
	 * 表名
	 * @return
	 */
	String table() default "";
	
	/**
	 * 列名
	 * @return
	 */
	String column() default "";
	
	/**
	 * 是否为ID(true=是)
	 * @return
	 */
	boolean isId() default false;
	
	/**
	 * ID是否为自增(true=是)
	 * @return
	 */
	boolean idAuto() default false;
	
	/**
	 * 是否映射(true=映射)<br>
	 * 屏蔽实体类字段与数据库字段的映射
	 * @return
	 */
	boolean isMapping() default true;
	
	/**
	 * 是否开启驼峰转换(true=开启)<br>
	 * 只有字段和类都为开启状态，才会转换
	 * @return
	 */
	boolean isHump() default true;
	
	/**
	 * <pre>
	 * 时间字段格式化(未配置时默认以yyyy-MM-dd HH:mm:ss格式化)
	 * 作用于以下两种场景：
	 * 1.查询时映射格式(针对字符串类型时间字段)
	 * 2.新增时映射格式(不限字段类型，但仅针对创建时间字段)
	 * 注意：
	 * MYSQL5.6之后的版本才支持毫秒微秒，若为旧版本且需要精确到毫秒之后
	 * 请将数据库字段类型设置为varchar(32)，否则会报超出长度异常
	 * </pre>
	 * @return
	 */
	String formatTime() default "";

}  

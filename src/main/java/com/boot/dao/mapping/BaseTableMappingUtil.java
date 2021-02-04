package com.boot.dao.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.boot.dao.api.EntityMeta;
import com.boot.dao.util.BaseDAOUtil;

/**
 * 表映射工具类
 * @author 2020-12-01 create wang.jia.le
 * @author yyyy-MM-dd update
 * @version 1.0.0
 */
@SuppressWarnings("unchecked")
abstract class BaseTableMappingUtil {

	//判断mybatis-plus或JPA框架包是否存在
	private static boolean mybatisPlusExist = false;
	private static boolean jpaExist = false;
	private static Map<String, Class<Annotation>> map1 = new HashMap<>();
	private static Map<String, Method> map2 = new HashMap<>();

	static {
		try {
			Class<Annotation> TableNameClass = (Class<Annotation>) Class.forName("com.baomidou.mybatisplus.annotation.TableName");
			Class<Annotation> TableFieldClass = (Class<Annotation>) Class.forName("com.baomidou.mybatisplus.annotation.TableField");
			Class<Annotation> TableIdClass = (Class<Annotation>) Class.forName("com.baomidou.mybatisplus.annotation.TableId");
			map1.put("TableName", 			TableNameClass);
			map1.put("TableField", 			TableFieldClass);
			map1.put("TableId", 			TableIdClass);
			map2.put("TableName_value", 	TableNameClass.getDeclaredMethod("value"));
			map2.put("TableField_exist", 	TableFieldClass.getDeclaredMethod("exist"));
			map2.put("TableField_value", 	TableFieldClass.getDeclaredMethod("value"));
			map2.put("TableId_type", 		TableIdClass.getDeclaredMethod("type"));
			mybatisPlusExist = true;
		} catch (Exception e) {}

		try {
			Class<Annotation> TableClass = (Class<Annotation>) Class.forName("javax.persistence.Table");
			Class<Annotation> ColumnClass = (Class<Annotation>) Class.forName("javax.persistence.Column");
			Class<Annotation> TransientClass = (Class<Annotation>) Class.forName("javax.persistence.Transient");
			Class<Annotation> IdClass = (Class<Annotation>) Class.forName("javax.persistence.Id");
			Class<Annotation> GeneratedValueClass = (Class<Annotation>) Class.forName("javax.persistence.GeneratedValue");
			map1.put("Table", 					TableClass);
			map1.put("Column", 					ColumnClass);
			map1.put("Transient", 				TransientClass);
			map1.put("Id", 						IdClass);
			map1.put("GeneratedValue", 			GeneratedValueClass);
			map2.put("Table_name", 				TableClass.getDeclaredMethod("name"));
			map2.put("Column_name", 			ColumnClass.getDeclaredMethod("name"));
			map2.put("GeneratedValue_strategy", GeneratedValueClass.getDeclaredMethod("strategy"));
			jpaExist = true;
		} catch (Exception e) {}
	}

	//创建表映射
	static BaseTableMapping createTableMapping(Class<?> clz) throws Exception{
		
		BaseTableMapping tm = new BaseTableMapping();
		if(clz.isAnnotationPresent(EntityMeta.class)){
			EntityMeta em = clz.getAnnotation(EntityMeta.class);
			tm.tableName = em.table();
			tm.isHump = em.isHump();
		}
		
		if(mybatisPlusExist && tm.tableName.length() == 0){
			if(clz.isAnnotationPresent(map1.get("TableName"))) {
				tm.tableName = (String)map2.get("TableName_value").invoke( clz.getAnnotation(map1.get("TableName")) );
			}
		}
		
		if(jpaExist && tm.tableName.length() == 0){
			if(clz.isAnnotationPresent(map1.get("Table"))) {
				tm.tableName = (String)map2.get("Table_name").invoke( clz.getAnnotation(map1.get("Table")) );
			}
		}
		
		//未找到表名映射时，以类名作为表名，根据驼峰配置转换下划线
		if(tm.tableName.length() == 0) {
			tm.tableName = !tm.isHump ? clz.getSimpleName() : BaseDAOUtil.humpToUnderline(clz.getSimpleName());
		}

		tm.tableName = tm.tableName.toLowerCase();
		createColumnMapping(clz, tm); //创建列映射
		return tm;
	}
	
	//创建列映射
	private static void createColumnMapping(Class<?> clz, BaseTableMapping tm) throws Exception{
		Field[] fields = clz.getDeclaredFields(); //获取该实体所有字段，包括私有字段，但不包括继承字段
		for (Field f : fields) {
			if(Modifier.isFinal(f.getModifiers()) || Modifier.isStatic(f.getModifiers()))
				continue; //当为final或static修饰时，则跳过
			
			String fieldName = f.getName();
			String columnName = ""; //列名
			String formatTime = null;
			boolean isHump = true; //是否开启驼峰转换
			boolean isFindId = false; //是否找到ID映射

			if(f.isAnnotationPresent(EntityMeta.class)){ //判断该字段是否使用了EntityMeta注解
				EntityMeta em = f.getAnnotation(EntityMeta.class);
				if(em.isMapping()) { //映射此字段
					columnName = em.column(); //列名
					isHump = em.isHump();
					formatTime = em.formatTime();
					if(em.isId()){
						isFindId = true;
						tm.metaType = 1;
						tm.idField = f;
						tm.idAuto = em.idAuto();
					}
				}else {
					continue;
				}
			}
			
			if(mybatisPlusExist && columnName.length() == 0){
				if(f.isAnnotationPresent(map1.get("TableField"))){
					Object TableField = f.getAnnotation(map1.get("TableField"));
					boolean exist = (boolean) map2.get("TableField_exist").invoke(TableField);
					String value = (String) map2.get("TableField_value").invoke(TableField);
					if(exist) { //映射此字段
						columnName = value; //列名
					}else {
						continue;
					}
				}
				if(!isFindId && f.isAnnotationPresent(map1.get("TableId"))){
					isFindId = true;
					tm.metaType = 2;
					tm.idField = f;
					String idType = map2.get("TableId_type").invoke( f.getAnnotation(map1.get("TableId")) ).toString();
					if("AUTO".equals(idType)) {
						tm.idAuto = true;
					}
				}
			}
			
			if(jpaExist && columnName.length() == 0){
				if(f.isAnnotationPresent(map1.get("Transient"))){ //判断是否不映射
					continue;
				}
				if(f.isAnnotationPresent(map1.get("Column"))){
					columnName = (String)map2.get("Column_name").invoke( f.getAnnotation(map1.get("Column")) );//列名
				}
				if(!isFindId && f.isAnnotationPresent(map1.get("Id"))){
					isFindId = true;
					tm.metaType = 3;
					tm.idField = f;
					if(f.isAnnotationPresent(map1.get("GeneratedValue"))){
						Object GeneratedValue = f.getAnnotation(map1.get("GeneratedValue"));
						String strategy = map2.get("GeneratedValue_strategy").invoke(GeneratedValue).toString();
						if("IDENTITY".equals(strategy)){
							tm.idAuto = true;
						}
					}
				}
			}

			//未配置映射列名时，则以和字段名相同的名称进行映射
			//若无注解映射，且与数据库的列名也不相同，可在SQL语句中使用 AS 使之对应
			if(columnName.length() == 0){
				columnName = fieldName;
				if(isHump && tm.isHump){ //只有字段和类都为开启状态，才会转换
					columnName = BaseDAOUtil.humpToUnderline(columnName);
				}
			}

			columnName = columnName.toLowerCase();
			if(isFindId) tm.idColumnName = columnName;
			f.setAccessible(true); //将字段设置为可强制访问

			BaseColumnMapping cm = new BaseColumnMapping(columnName, f, formatTime);
			tm.columnMappings.put(columnName, cm);
			tm.fieldMappings.put(fieldName, cm);

			if("createTime".equals(fieldName) || "createDate".equals(fieldName)) {
				tm.createTime = cm;
				tm.hasCreateTime = true;
			}
		}
	}
	
	//根据实体映射信息创建Dto或Vo类对应的映射信息
	static BaseTableMapping createByEntityMapping(Class<?> clz, BaseTableMapping entityTm) {
		BaseTableMapping tm = new BaseTableMapping();
		tm.tableName = entityTm.tableName;
		tm.isHump = entityTm.isHump;
		tm.isEntity = false;
		tm.metaType = entityTm.metaType;
		if(entityTm.createTime != null) {
			tm.createTime = new BaseColumnMapping(entityTm.createTime.columnName, null, entityTm.createTime.formatDate);
		}
		
		Field[] fields = clz.getDeclaredFields(); //获取该类型所有字段，包括私有字段，但不包括继承字段
		for (Field field : fields) {
			if(Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
				continue;//当为final或static修饰时，则跳过
			
			String fieldName = field.getName();
			BaseColumnMapping entityCm = entityTm.fieldMappings.get(fieldName);
			if(entityCm != null) {
				field.setAccessible(true); //将字段设置为可强制访问
				if(fieldName.equals(entityTm.idField.getName())) {
					tm.idColumnName = entityTm.idColumnName;
					tm.idAuto = entityTm.idAuto;
					tm.idField = field;
				}
				if("createTime".equals(fieldName) || "createDate".equals(fieldName)) {
					if(tm.createTime != null) {
						tm.createTime.field = field;
						tm.hasCreateTime = true;
					}
				}
				BaseColumnMapping cm = new BaseColumnMapping(entityCm.columnName, field, entityCm.formatDate);
				tm.columnMappings.put(cm.columnName, cm);
				tm.fieldMappings.put(fieldName, cm);
			}
		}
		return tm;
	}

}
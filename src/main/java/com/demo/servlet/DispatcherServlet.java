package com.demo.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.demo.annotation.OwnAutowired;
import com.demo.annotation.OwnRequestMapper;
import com.demo.annotation.OwnRequestParam;
import com.demo.annotation.OwnRestContoller;
import com.demo.annotation.OwnService;
import com.demo.controller.MvcController;

public class DispatcherServlet extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1501948593130051939L;
	private List<String> classPath = new ArrayList<>();
	private Map<String,Method> urlMap = new HashMap<String, Method>();
	private Map<String,Object> beans = new HashMap<String, Object>();
	public DispatcherServlet(){
		super();
		System.out.println("开始启动....");
	}
	public void init(ServletConfig config) throws ServletException{
		try {
			//扫描class文件
			String packageName = "com.demo";
			scanClass(packageName);
			//注解class类
			annotationClass();
			//注解class属性
			annotationAutowired();
			//注解class方法
			annotationMethod();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void annotationMethod() {
		for(Map.Entry<String,Object> entry : beans.entrySet()){
			Object instance = entry.getValue();
			Class<?> clazz = instance.getClass();
			if(clazz.isAnnotationPresent(OwnRestContoller.class)){
				String url = entry.getKey();
				Method[] methods = clazz.getMethods();
				for (Method method : methods) {
					if(method.isAnnotationPresent(OwnRequestMapper.class)){
						OwnRequestMapper request =  method.getAnnotation(OwnRequestMapper.class);
						String value = request.value();
						urlMap.put(url+value, method);
					}
				}
			}
		}
	}

	public void annotationAutowired() throws Exception{
		for(Map.Entry<String, Object> entry : beans.entrySet()){
			Object instance = entry.getValue();
			Class<?> clazz = instance.getClass();
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if(field.isAnnotationPresent(OwnAutowired.class)){
					OwnAutowired wired = field.getAnnotation(OwnAutowired.class);
					String key = wired.value();
					field.setAccessible(true);
					field.set(instance, beans.get(key));
				}
			}
		}
	}

	public void annotationClass() throws Exception {
		for (String className : classPath) {
			className = className.replace(".class", "");
			Class<?> clazz = Class.forName(className);
			if(clazz.isAnnotationPresent(OwnRestContoller.class)){
				Object instance = clazz.newInstance();
				OwnRequestMapper request = clazz.getAnnotation(OwnRequestMapper.class);
				String key = request.value();
				beans.put(key, instance);
			} else if(clazz.isAnnotationPresent(OwnService.class)){
				Object instance = clazz.newInstance();
				OwnService service = clazz.getAnnotation(OwnService.class);
				String key = service.value();
				beans.put(key, instance);
			}
		}
	}

	public void scanClass(String packageName) {
		String path = DispatcherServlet.class.getResource("/"+packageName.replaceAll("\\.", "/")).getPath();
		
		File file = new File(path);
		File[] f = file.listFiles();
		for (File fileName : f) {
			if(fileName.isDirectory()){
				scanClass(packageName + "." + fileName.getName());
			}else {
				classPath.add(packageName+"."+fileName.getName());
			}
		}
	}


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		this.doPost(req,resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String url = req.getRequestURI();
		String context = req.getContextPath();
		String path = url.replace(context, "");
		Method method = urlMap.get(path);
		MvcController instance = (MvcController) beans.get("/"+path.split("/")[1]);
		//获取参数值
		Object args[] = handParams(req,resp,method);
		try {
			method.invoke(instance, args);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	private Object[] handParams(HttpServletRequest req, HttpServletResponse resp,
			Method method) {
		Class<?>[] paramClazz = method.getParameterTypes();
		Object[] args = new Object[paramClazz.length];
		int args_i = 0;
		int index = 0;
		for (Class<?> clazz : paramClazz) {
			if(ServletRequest.class.isAssignableFrom(clazz)){
				args[args_i++] = req;
			} 
			if(ServletResponse.class.isAssignableFrom(clazz)){
				args[args_i++] = resp;
			}
			Annotation[] paramAns = method.getParameterAnnotations()[index];
			if(paramAns.length > 0){
				for (Annotation paramAn : paramAns) {
					if(OwnRequestParam.class.isAssignableFrom(paramAn.getClass())){
						OwnRequestParam param = (OwnRequestParam) paramAn;
						args[args_i++] = req.getParameter(param.value());
					}
				}
			}
			index++;
		}
		
		return args;
	}


	public static void main(String[] args) {
		try {
			String packageName = "com.demo";
			DispatcherServlet s = new DispatcherServlet();
			s.scanClass(packageName);
			s.annotationClass();
			s.annotationAutowired();
			s.annotationMethod();
			
		} catch (Exception e) {
		}
		
	}
}

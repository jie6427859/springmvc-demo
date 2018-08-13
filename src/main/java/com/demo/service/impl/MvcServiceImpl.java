package com.demo.service.impl;

import com.demo.annotation.OwnService;
import com.demo.service.MvcService;

@OwnService(value="mvcService")
public class MvcServiceImpl implements MvcService{

	@Override
	public String test(String name, String age) {
		return "name = " + name + "," + "age = " + age;
	}

}

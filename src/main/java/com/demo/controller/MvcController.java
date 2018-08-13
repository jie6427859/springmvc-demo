package com.demo.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.demo.annotation.OwnAutowired;
import com.demo.annotation.OwnRequestMapper;
import com.demo.annotation.OwnRequestParam;
import com.demo.annotation.OwnRestContoller;
import com.demo.service.MvcService;

@OwnRestContoller
@OwnRequestMapper("/mvc")
public class MvcController {
	
	@OwnAutowired(value="mvcService")
	MvcService mvcService;
	
	@OwnRequestMapper("/test")
	public String test(@OwnRequestParam("name")String name, @OwnRequestParam("age")String age,
							HttpServletRequest request, HttpServletResponse response) throws IOException{
		String msg = mvcService.test(name, age);
		PrintWriter out = response.getWriter();
		out.write(msg);
		return msg;
	}
}

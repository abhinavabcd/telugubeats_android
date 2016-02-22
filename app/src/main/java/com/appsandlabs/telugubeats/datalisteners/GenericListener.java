package com.appsandlabs.telugubeats.datalisteners;

public class GenericListener<T> {
	public void onData(T s){

	} // returns error message , if null then everything is alright

	public boolean onDataB(T s){return false;} // returns error message , if null then everything is alright

}


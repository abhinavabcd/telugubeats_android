package com.appsandlabs.telugubeats.datalisteners;

public interface GenericListener<T> {
	void onData(T s); // returns error message , if null then everything is alright
}


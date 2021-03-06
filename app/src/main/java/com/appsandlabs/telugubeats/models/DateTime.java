package com.appsandlabs.telugubeats.models;

import com.appsandlabs.telugubeats.config.Config;

import java.util.Date;

/**
 * Created by abhinav on 2/6/16.
 */
public class DateTime {
        Long $date;

        @Override
        public String toString() {
            Date date = getDate();
            if(date!=null)
                return Config.getDateFormatter().format(getDate());
            return "";
        }

        public Date getDate(){
            return $date==null?null:new Date($date);
        }
}
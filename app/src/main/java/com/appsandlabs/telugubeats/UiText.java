package com.appsandlabs.telugubeats;

import com.appsandlabs.telugubeats.models.Event;
import com.appsandlabs.telugubeats.models.Poll;
import com.appsandlabs.telugubeats.models.Song;
import com.appsandlabs.telugubeats.response_models.PollsChanged;

/**
 * Created by abhinav on 9/27/15.
 */
public enum UiText {
    COPY_RIGHTS("Copyrights."),
    CONNECTING("Connecting"),
    CHECKING_FOR_FRIENDS("Fetching friends"),
    UNABLE_TO_OPEN_INTENT("Unable to open intent"),
    NEW_TEXT_AVAILABLE("New notification from samosa");

    String value = null;
    UiText(String value){
        this.value = value;
    }
    public String getValue(){
        return value;
    }
    public String getValue(Object...args){
        return String.format(value,args);
    }


    public static String getFeedString(Event event) {
        String feed= null;
        switch (event.eventId) {

            case POLLS_CHANGED:
                PollsChanged pollsChanged = TeluguBeatsApp.gson.fromJson(event.payload, PollsChanged.class);
                feed = "voted up for " + Poll.getChangedPollSongTitle(pollsChanged);
                //TODO: indicate modified vote here
                break;
            case DEDICATE:
                feed ="dedicated this song to " + event.payload;
                break;

            case CHAT_MESSAGE:
                feed =  event.payload;
                break;
        }
        return feed;
    }


        public static String capitalize(final String str) {
                return capitalize(str, null);
            }

                /**
          * <p>Capitalizes all the delimiter separated words in a String.
          * Only the first letter of each word is changed. To convert the
          * rest of each word to lowercase at the same time,
          * use {@link #capitalizeFully(String, char[])}.</p>
          *
          * <p>The delimiters represent a set of characters understood to separate words.
          * The first string character and the first non-delimiter character after a
          * delimiter will be capitalized. </p>
          *
          * <p>A <code>null</code> input String returns <code>null</code>.
          * Capitalization uses the Unicode title case, normally equivalent to
          * upper case.</p>
          *
          * <pre>
          * WordUtils.capitalize(null, *)            = null
          * WordUtils.capitalize("", *)              = ""
          * WordUtils.capitalize(*, new char[0])     = *
          * WordUtils.capitalize("i am fine", null)  = "I Am Fine"
          * WordUtils.capitalize("i aM.fine", {'.'}) = "I aM.Fine"
          * </pre>
          *
          * @param str  the String to capitalize, may be null
          * @param delimiters  set of characters to determine capitalization, null means whitespace
          * @return capitalized String, <code>null</code> if null String input
          * @see #uncapitalize(String)
          * @see #capitalizeFully(String)
          * @since 2.1
          */
                public static String capitalize(String str, char[] delimiters) {
                        int delimLen = (delimiters == null ? -1 : delimiters.length);
                            if (str == null || str.length() == 0 || delimLen == 0) {
                                   return str;
                               }
                           int strLen = str.length();
                            StringBuffer buffer = new StringBuffer(strLen);
                        boolean capitalizeNext = true;
                               for (int i = 0; i < strLen; i++) {
                                      char ch = str.charAt(i);

                                     if (isDelimiter(ch, delimiters)) {
                                               buffer.append(ch);
                                             capitalizeNext = true;
                                      } else if (capitalizeNext) {
                                             buffer.append(Character.toTitleCase(ch));
                                              capitalizeNext = false;
                                      } else {
                                             buffer.append(ch);
                                         }
                                    }
                            return buffer.toString();
                           }

        private static boolean isDelimiter(final char ch, final char[] delimiters) {
                if (delimiters == null) {
                        return Character.isWhitespace(ch);
                    }
                for (final char delimiter : delimiters) {
                        if (ch == delimiter) {
                                return true;
                            }
                    }
                return false;
            }


    public static String capitalizeFully(String str) {
                 return capitalizeFully(str, null);
            }


     public static String capitalizeFully(String str, char[] delimiters) {
         int delimLen = (delimiters == null ? -1 : delimiters.length);
         if (str == null || str.length() == 0 || delimLen == 0) {
               return str;
         }
         str = str.toLowerCase();
         return capitalize(str, delimiters);
     }

    public static void songTitleCleanUp(Song currentSong) {
        if(currentSong.title.toLowerCase().trim().startsWith(currentSong.album.name.toLowerCase().trim())){
            currentSong.title = currentSong.title.substring(currentSong.album.name.length());
        }
        if(currentSong.title.toLowerCase().startsWith("artist")){
            currentSong.title = currentSong.title.substring("artist".length());
        }
    }
}
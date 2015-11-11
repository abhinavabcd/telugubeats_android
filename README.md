#TeluguBeats android
This is a real time radio/music app. Coupled with telugubeats server written over gevent and greenlets, you can stream music and ingeneral
audio stream in realtime to clients , implemented over a partially implemented icecast mp3 stream(sort of but not).
The code features a visualizer, from the spectrogram of the jlDecoder from javaZoom. This app radio features real time  polls for the next song.
So people can vote and maximum votes gets played next. Talk over radio feature is not implented yet in this public release.



#TeluguBeats Server
Written over the powerful python gevent and greenlets, and raw socket code. This is a real time audio streaming server.
If you are using this you would need albums and music database , the original bucket reads from google storage bucket the audio files streams and broadcasts on live.


You may want to see the app in working here
https://storage.googleapis.com/telugubeats_files/apk/telugubeats_app-release.apk


<img src="https://storage.googleapis.com/telugubeats_files/screenshots/1.jpg" width="150px" />
<img src="https://storage.googleapis.com/telugubeats_files/screenshots/2.jpg" width="150px" />
<img src="https://storage.googleapis.com/telugubeats_files/screenshots/3.jpg" width="150px" />
<img src="https://storage.googleapis.com/telugubeats_files/screenshots/4.jpg" width="150px" />

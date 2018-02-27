/**
   Copyright (c) 2014 Rory Hool
   
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
   
       http://www.apache.org/licenses/LICENSE-2.0
   
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 **/

package apollo.decoder;

import android.util.Log;

public class PlaybackTimer {

   boolean mIsRunning = false;

   long mCurrentTime;

   long mStartTime;

   long mTotalTime;

   public void reset(){
      mIsRunning = false;
      mStartTime = 0;
      mTotalTime = 0;
   }

   public void start() {
      mIsRunning = true;

      mStartTime = System.currentTimeMillis();
   }

   public void stop() {
      mIsRunning = false;

      long currentTime = System.currentTimeMillis();
      mTotalTime += currentTime - mStartTime;
      logTime("stop()");
   }

   public long getTime() {
      if ( mIsRunning ) {
         long currentTime = System.currentTimeMillis();
         long time = mTotalTime + ( currentTime - mStartTime );
         return time;
      } else {
         return mTotalTime;
      }
   }

   public boolean isRunning() {
      return mIsRunning;
   }

   public void setTime( long time ) {
      mTotalTime = time;
      mStartTime = System.currentTimeMillis();
      logTime("setTime()");
   }

   private void logTime(String whoCall){
      Log.d("PlaybackTimer", whoCall);
      Log.d("PlaybackTimer", "mTotalTime:" + mTotalTime);
      Log.d("PlaybackTimer", "mStartTime:" + mStartTime);
      Log.d("PlaybackTimer", "/////////////////////////\n\n");
   }
}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package my.home.common;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by legendmohe on 15/3/31.
 */
public abstract class CacheKeyValueStorageImpl implements KeyValueStorage.IKeyStringStorge {
    public static final String TAG = "CacheKeyValueStorage";

    private final int MSG_STORAGE_WHAT = 0;

    class CacheHandlerThread extends HandlerThread implements Handler.Callback {
        public CacheHandlerThread(String name) {
            super(name);
        }

        @Override
        public boolean handleMessage(Message msg) {
        	synchronized (mSyncLock) {
	        	for (String keyString : mSyncCache.keySet()) {
	        		storagePutString(keyString, mSyncCache.get(keyString));
				}
	        	mSyncCache.clear();
        	}
//            Log.d(TAG, "run msg: " + bundle.toString());
            return false;
        }
    }

    final ConcurrentHashMap<String, String> mCache = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, String> mSyncCache = new ConcurrentHashMap<>();
    private final Object mSyncLock = new Object();
    private Handler mHandler;

    public CacheKeyValueStorageImpl() {
        CacheHandlerThread cacheHandlerThread = new CacheHandlerThread("CacheKeyValueStorageImpl");
        cacheHandlerThread.setPriority(Thread.MIN_PRIORITY);
        cacheHandlerThread.start();
        mHandler = new Handler(cacheHandlerThread.getLooper(), cacheHandlerThread);
    }

    @Override
    public boolean hasKey(String key) {
        if (mCache.containsKey(key))
            return true;
        return storageHasKey(key);
    }

    @Override
    public void putString(String key, String value) {
        mCache.put(key, value);

        Message msg = Message.obtain();
        msg.what = MSG_STORAGE_WHAT;

        synchronized (mSyncLock) {
        	mSyncCache.put(key, value);
            mHandler.removeMessages(MSG_STORAGE_WHAT);
            mHandler.sendMessageDelayed(msg, 300);
        }
    }

    @Override
    public String getString(String key) {
        if (mCache.containsKey(key))
            return mCache.get(key);
        String value = storageGetString(key);
        if (value != null)
            mCache.put(key, value);
        return value;
    }
    
    @Override
    public void removeString(String key) {
    	mCache.remove(key);
    	storageRemoveString(key);
    }

    public abstract boolean storageHasKey(String key);

    public abstract void storagePutString(String key, String value);

    public abstract String storageGetString(String key);
    
    public abstract void storageRemoveString(String key);
}
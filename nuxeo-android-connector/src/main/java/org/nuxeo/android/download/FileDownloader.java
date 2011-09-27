package org.nuxeo.android.download;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.nuxeo.android.cache.blob.BlobStore;
import org.nuxeo.android.cache.blob.BlobStoreManager;
import org.nuxeo.ecm.automation.client.android.AndroidAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.AsyncCallback;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;

import android.util.Log;

public class FileDownloader {

	protected static final String BLOB_KEY = "blobs";
	protected static final String ICONS_KEY = "icons";

	protected final AndroidAutomationClient client;
	protected final BlobStoreManager blobStoreManager;

	protected CopyOnWriteArrayList<String> pendingDownload = new CopyOnWriteArrayList<String>();

	public FileDownloader(AndroidAutomationClient client) {
		this.client =client;
		this.blobStoreManager=client.getBlobStoreManager();
	}

	protected String getExecutionId(String url) {
		return "download:" + System.currentTimeMillis();
	}

	public FileBlob getBlob(String uid) {
		return getBlob(uid, 0);
	}

	public FileBlob getBlob(String uid, Integer idx) {
		if (idx==null) {
			idx = 0;
		}
		String pattern = "nxbigfile/default/" + uid + "/blobholder:" + idx + "/";
		String url = client.getServerConfig().getServerBaseUrl() + pattern;
		return getBlob(BLOB_KEY, url, null, null);
	}

	public String getBlob(String url, AsyncCallback<Blob> cb) {
		String execId = getExecutionId(url);
		Blob blob = null;
		try {
			blob= getBlob(BLOB_KEY, url, cb, execId);
			return execId;
		} finally {
			if (blob!=null) {
				cb.onSuccess(execId, blob);
			}
		}
	}

	protected String buildUrl(String urlPattern, String suffix ) {
		if (suffix.startsWith("http://") || suffix.startsWith("https://")) {
			return suffix;
		}
		String url = client.getServerConfig().getServerBaseUrl();

		if (!suffix.startsWith("/")) {
			suffix = "/" + suffix;
		}
		return url + urlPattern + suffix;
	}

	public FileBlob getIcon(String url) {
		url = buildUrl("icons", url);
		return getBlob(ICONS_KEY, url, null, null);
	}

	public String getIcon(String url, AsyncCallback<Blob> cb) {
		String execId = getExecutionId(url);
		Blob blob = null;
		try {
			blob= getBlob(ICONS_KEY, url, cb, execId);
			return execId;
		} finally {
			if (blob!=null) {
				cb.onSuccess(execId, blob);
			}
		}
	}

	protected String getRequestKey(String url) {
        MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        digest.update(url.getBytes());
        byte messageDigest[] = digest.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i=0; i<messageDigest.length; i++)
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        return hexString.toString();
	}


	protected FileBlob getBlob(String assetType, String url, AsyncCallback<Blob> cb, String execId) {
		BlobStore store = blobStoreManager.getBlobStore(assetType);

		String key = getRequestKey(url);
		FileBlob blob = store.getBlob(key);

		if (blob!=null) {
			return blob;
		}

		if (blob==null && !client.isOffline()) {
			blob = downloadAndStoreBlob(store, url, key, cb, execId );
		}
		return blob;
	}

	protected FileBlob downloadAndStoreBlob(final BlobStore store, final String url,final  String key, final AsyncCallback<Blob> cb, final String execId) {

		boolean added = pendingDownload.addIfAbsent(url);
		if (added) {
			client.asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						HttpResponse response = client.getConnector().executeSimpleHttp(new HttpGet(url));
						Blob file = null;
						if (response.getStatusLine().getStatusCode()==200) {
							file = store.storeBlob(key, new HttpBlob(response));
						} else {
							Log.e(FileDownloader.class.getSimpleName(), "Can not download file, return code " + response.getStatusLine().getStatusCode());
						}
						if (cb!=null) {
							cb.onSuccess(execId, file);
						}
					} catch (Exception e) {
						if (cb!=null) {
							cb.onError(execId, e);
						}
					} finally {
						pendingDownload.remove(url);
					}
				}
			});
		} else {
			// this resource is already downloading
			if (cb!=null) {
				client.asyncExec(new Runnable() {
					@Override
					public void run() {
						Blob blob = waitUntilCompletion(store, key, url);
						cb.onSuccess(execId, blob);
					}
				});
			}
		}
		if (cb==null) {
			// wait until completion
			return waitUntilCompletion(store, key, url);
		}
		return null;
	}

	protected FileBlob waitUntilCompletion(BlobStore store, String key, String url) {
		while (pendingDownload.contains(url)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return store.getBlob(key);
	}
}
package org.nuxeo.ecm.automation.client.android;

import java.util.List;

import org.nuxeo.android.broadcast.NuxeoBroadcastMessages;
import org.nuxeo.android.cache.sql.SQLStateManager;
import org.nuxeo.android.cache.sql.TransientStateTableWrapper;
import org.nuxeo.ecm.automation.client.cache.DocumentDeltaSet;
import org.nuxeo.ecm.automation.client.cache.OperationType;
import org.nuxeo.ecm.automation.client.cache.TransientStateManager;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class AndroidTransientStateManager extends BroadcastReceiver implements TransientStateManager {

	protected final SQLStateManager stateManager;

	public AndroidTransientStateManager(Context androidContext, SQLStateManager stateManager) {
		IntentFilter filter = new IntentFilter();
		filter.addAction(NuxeoBroadcastMessages.DOCUMENT_CREATED_CLIENT);
		filter.addAction(NuxeoBroadcastMessages.DOCUMENT_CREATED_SERVER);
		filter.addAction(NuxeoBroadcastMessages.DOCUMENT_UPDATED_CLIENT);
		filter.addAction(NuxeoBroadcastMessages.DOCUMENT_UPDATED_SERVER);
		filter.addAction(NuxeoBroadcastMessages.DOCUMENT_DELETED_CLIENT);
		filter.addAction(NuxeoBroadcastMessages.DOCUMENT_DELETED_SERVER);
		androidContext.registerReceiver(this, filter);
		stateManager.registerWrapper(new TransientStateTableWrapper());
		this.stateManager = stateManager;
	}

	protected TransientStateTableWrapper getTableWrapper() {
		return (TransientStateTableWrapper) stateManager.getTableWrapper(TransientStateTableWrapper.TBLNAME);
	}

	public void storeDocumentState(Document doc, OperationType opType) {
		DocumentDeltaSet delta = new DocumentDeltaSet(opType, doc);
		getTableWrapper().storeDeltaSet(delta);
		// XXX store Blobs too
	}

	public List<DocumentDeltaSet> getDeltaSets(List<String> ids) {
		List<DocumentDeltaSet> deltas = getTableWrapper().getDeltaSets(ids);
		// XXX get Blobs
		return deltas;
	}

	public Documents mergeTransientState(Documents docs, boolean add) {

		List<DocumentDeltaSet> deltas = getDeltaSets(docs.getIds());
		for (DocumentDeltaSet delta : deltas) {
			if (add && delta.getOperationType()== OperationType.CREATE && ! docs.containsDocWithId(delta.getId())) {
				docs.add(0, delta.apply(null));
			} else if (delta.getOperationType()== OperationType.UPDATE) {
				Document doc2Update = docs.getById(delta.getId());
				delta.apply(doc2Update);
			} else if (delta.getOperationType()== OperationType.DELETE) {
				docs.removeById(delta.getId());
			}
		}

		return docs;
	}


	public void flushTransientState(String uid) {
		getTableWrapper().deleteEntry(uid);
	}

	public void flushTransientState() {
		getTableWrapper().clearTable();
	}

	@Override
	public void onReceive(Context androidContext, Intent intent) {

		String eventName = intent.getAction();
		Document doc = (Document) intent.getExtras().get(NuxeoBroadcastMessages.EXTRA_DOCUMENT_PAYLOAD_KEY);
		if (eventName.equals(NuxeoBroadcastMessages.DOCUMENT_CREATED_CLIENT)) {
			storeDocumentState(doc, OperationType.CREATE);
		}
		else if (eventName.equals(NuxeoBroadcastMessages.DOCUMENT_UPDATED_CLIENT)) {
			storeDocumentState(doc, OperationType.UPDATE);
		}
		else if (eventName.equals(NuxeoBroadcastMessages.DOCUMENT_DELETED_CLIENT)) {
			storeDocumentState(doc, OperationType.DELETE);
		}
		else if (eventName.equals(NuxeoBroadcastMessages.DOCUMENT_CREATED_SERVER) || eventName.equals(NuxeoBroadcastMessages.DOCUMENT_UPDATED_SERVER) || eventName.equals(NuxeoBroadcastMessages.DOCUMENT_DELETED_SERVER) ) {
			if (doc!=null) {
				flushTransientState(doc.getId());
			}
		}
	}

}

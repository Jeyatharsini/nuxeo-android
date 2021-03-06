/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.android.simpleclient.listing.ui;

import java.text.SimpleDateFormat;

import org.nuxeo.android.simpleclient.NuxeoAndroidApplication;
import org.nuxeo.android.simpleclient.R;
import org.nuxeo.android.simpleclient.menus.SettingsActivity;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;

import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartnsoft.droid4me.download.ImageDownloader;

public final class DocumentItemAttributesUpdater implements
        ObjectItemViewUpdater {

    private final TextView title;

    private final TextView desc;

    private final ImageView icon;

    protected final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "dd/MM/yy");

    public DocumentItemAttributesUpdater(View view) {
        title = (TextView) view.findViewById(R.id.title);
        desc = (TextView) view.findViewById(R.id.desc);
        icon = (ImageView) view.findViewById(R.id.icon);
    }

    public void update(Context context, Handler handler, Object item) {

        Document doc = (Document) item;

        title.setText(doc.getTitle());
        String descString = doc.getProperties().getString("dc:description", "");
        if ("null".equals(descString)) {
            descString = "";
        }
        if ("".equals(descString)) {
            descString = "<b>Type </b>: " + doc.getType();
            descString += "&nbsp;<b> State </b>: " + doc.getState();
            descString += "&nbsp;<b> Modified </b>: "
                    + dateFormat.format(doc.getLastModified());
            desc.setText(Html.fromHtml(descString));
        } else {
            desc.setText(descString);
        }

        final String serverUrl = context.getSharedPreferences(
                "org.nuxeo.android.simpleclient_preferences", 0).getString(
                SettingsActivity.PREF_SERVER_URL, "");
        String urlImage = serverUrl + (serverUrl.endsWith("/") ? "" : "/")
                + doc.getString("common:icon", "");

        ImageDownloader.getInstance().get(icon, urlImage, null, handler,
                NuxeoAndroidApplication.CACHE_IMAGE_INSTRUCTIONS);

    }
}
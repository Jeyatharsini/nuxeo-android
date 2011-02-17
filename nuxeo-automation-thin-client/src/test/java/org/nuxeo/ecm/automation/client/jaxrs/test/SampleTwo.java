/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.test;

import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.adapters.DocumentService;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.automation.client.jaxrs.model.Documents;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SampleTwo {

    public static void main(String[] args) throws Exception {
        try {
            HttpAutomationClient client = new HttpAutomationClient(
                    "http://localhost:8080/nuxeo/site/automation");
            long start = System.currentTimeMillis();
            Session session = client.getSession("Administrator",
                    "Administrator");
            DocumentService rs = session.getAdapter(DocumentService.class);
            Document doc = rs.getDocument("/default-domain");
            System.out.println(doc + " - " + doc.getTitle());
            Documents docs = rs.getChildren(doc);
            System.out.println(docs);
            Document dd = null;
            for (Document d : docs) {
                if (d.getPath().endsWith("/workspaces")) {
                    dd = d;
                }
                System.out.println(d.getTitle() + " at " + d.getLastModified());
            }
            // doc = rs.createDocument(dd, "Workspace", "hello");
            // System.out.println(doc + " - "+doc.getTitle());
            System.out.println("@@@@@@@@@@@@@@@@@@@");
            DocRef wsRef = new DocRef("/default-domain/workspaces");
            docs = rs.getChildren(wsRef);
            System.out.println(docs);
            for (Document d : docs) {
                System.out.println(d.getTitle() + " at " + d.getLastModified()
                        + " state: " + d.getState());
            }
            doc = rs.getDocument("/default-domain/workspaces");
            System.out.println("----------------------------");
            System.out.println(doc + " - " + doc.getTitle());
            System.out.println("@@@@@@@@@@@@@@@@@@@");
            System.out.println("took: "
                    + ((double) System.currentTimeMillis() - start) / 1000);
            client.shutdown();
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println(e.getRemoteStackTrace());
        }
    }

}
